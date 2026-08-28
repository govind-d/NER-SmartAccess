package com.ner.smartlogix.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ner.smartlogix.dto.request.RiskPredictionRequest;
import com.ner.smartlogix.dto.response.RiskAssessmentResponse;
import com.ner.smartlogix.entity.Road;
import com.ner.smartlogix.entity.RoadRiskAssessment;
import com.ner.smartlogix.entity.WeatherData;
import com.ner.smartlogix.enums.RiskLevel;
import com.ner.smartlogix.event.RiskLevelChangedEvent;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.ml.RiskPrediction;
import com.ner.smartlogix.ml.RiskPredictor;
import com.ner.smartlogix.ml.feature.FeatureExtractor;
import com.ner.smartlogix.ml.feature.RiskFeatureVector;
import com.ner.smartlogix.repository.RoadRepository;
import com.ner.smartlogix.repository.RoadRiskAssessmentRepository;
import com.ner.smartlogix.service.RiskService;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The service layer around the AI engine.
 *
 * <p>It knows nothing about rainfall thresholds or slopes. It gathers features, asks
 * whichever {@link RiskPredictor} is wired in, stores the answer with its full score card,
 * and publishes an event if the verdict changed. Replacing the rule engine with a trained
 * model in Phase 8 therefore does not touch this file.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService {

    private final RoadRepository roadRepository;
    private final RoadRiskAssessmentRepository assessmentRepository;
    private final FeatureExtractor featureExtractor;
    private final RiskPredictor riskPredictor;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RiskAssessmentResponse assessRoad(Long roadId) {
        Road road = requireRoad(roadId);
        RiskFeatureVector features = featureExtractor.extract(road);
        return runAndStore(road, features);
    }

    @Override
    @Transactional(readOnly = true)
    public RiskAssessmentResponse predictWhatIf(RiskPredictionRequest request) {
        Road road = requireRoad(request.roadId());

        // Build a throwaway weather reading from the request so the caller can explore
        // scenarios ("what if 200 mm falls tomorrow?") without polluting real observations.
        WeatherData hypothetical = null;
        if (request.rainfall24h() != null || request.rainfall72h() != null
                || request.weatherCondition() != null) {
            hypothetical = new WeatherData();
            hypothetical.setDistrict(road.getDistrict());
            hypothetical.setRainfallMm24h(request.rainfall24h() == null
                    ? 0 : request.rainfall24h());
            hypothetical.setRainfallMm72h(request.rainfall72h() == null
                    ? 0 : request.rainfall72h());
            hypothetical.setCondition(request.weatherCondition() == null
                    ? com.ner.smartlogix.enums.WeatherCondition.CLEAR
                    : request.weatherCondition());
            hypothetical.setSource("WHAT_IF");
            hypothetical.setRecordedAt(OffsetDateTime.now());
        }

        RiskFeatureVector features = featureExtractor.extract(road, hypothetical);
        RiskPrediction prediction = riskPredictor.predict(features);
        // Nothing is stored: a hypothetical must never become part of the record.
        return toResponse(road, prediction, OffsetDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public RiskAssessmentResponse currentRisk(Long roadId) {
        Road road = requireRoad(roadId);
        return assessmentRepository.findFirstByRoadIdOrderByAssessedAtDesc(roadId)
                .map(assessment -> toResponse(road, assessment))
                .orElseGet(() -> {
                    // Never assessed yet: compute one now rather than returning nothing.
                    RiskPrediction prediction = riskPredictor.predict(
                            featureExtractor.extract(road));
                    return toResponse(road, prediction, OffsetDateTime.now());
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiskAssessmentResponse> history(Long roadId, int days) {
        Road road = requireRoad(roadId);
        return assessmentRepository
                .findByRoadIdAndAssessedAtAfterOrderByAssessedAtAsc(
                        roadId, OffsetDateTime.now().minusDays(days))
                .stream()
                .map(assessment -> toResponse(road, assessment))
                .toList();
    }

    @Override
    @Transactional
    public int reassessAll(Long districtId) {
        List<Road> roads = districtId == null
                ? roadRepository.findAll()
                : roadRepository.findAll().stream()
                        .filter(road -> road.getDistrict().getId().equals(districtId))
                        .toList();

        int changed = 0;
        for (Road road : roads) {
            RiskLevel before = road.getCurrentRiskLevel();
            runAndStore(road, featureExtractor.extract(road));
            if (road.getCurrentRiskLevel() != before) {
                changed++;
            }
        }
        log.info("Reassessed {} road(s); {} changed risk level", roads.size(), changed);
        return changed;
    }

    @Override
    public String activeModelName() {
        return riskPredictor.modelName();
    }

    // ------------------------------------------------------------------ internals

    /** Predict, store the assessment with its score card, and react if the level moved. */
    private RiskAssessmentResponse runAndStore(Road road, RiskFeatureVector features) {
        RiskPrediction prediction = riskPredictor.predict(features);
        OffsetDateTime now = OffsetDateTime.now();

        RoadRiskAssessment assessment = new RoadRiskAssessment();
        assessment.setRoad(road);
        assessment.setDisruptionScore(prediction.disruptionScore());
        assessment.setRiskLevel(prediction.riskLevel());
        assessment.setModelName(prediction.modelName());
        assessment.setScoreCard(toJson(prediction));
        assessment.setAssessedAt(now);
        assessmentRepository.save(assessment);

        RiskLevel previous = road.getCurrentRiskLevel();
        if (previous != prediction.riskLevel()) {
            // Denormalised cache on the road, so the map and dashboard never join history.
            road.setCurrentRiskLevel(prediction.riskLevel());
            roadRepository.save(road);

            eventPublisher.publishEvent(new RiskLevelChangedEvent(
                    road.getId(), road.getCode(), previous, prediction.riskLevel(),
                    prediction.disruptionScore(), topReason(prediction)));
        }
        return toResponse(road, prediction, now);
    }

    private String topReason(RiskPrediction prediction) {
        return prediction.scoreCard().isEmpty()
                ? prediction.recommendation()
                : prediction.scoreCard().get(0).reason();
    }

    /** The score card is stored as JSONB so it stays queryable and human-readable in psql. */
    private String toJson(RiskPrediction prediction) {
        try {
            return objectMapper.writeValueAsString(prediction.scoreCard());
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialise the score card: {}", ex.getMessage());
            return null;
        }
    }

    private RiskAssessmentResponse toResponse(Road road, RiskPrediction prediction,
                                              OffsetDateTime at) {
        List<RiskAssessmentResponse.ScoreCardEntry> entries = prediction.scoreCard().stream()
                .map(contribution -> new RiskAssessmentResponse.ScoreCardEntry(
                        contribution.rule(), contribution.subScore(), contribution.weight(),
                        contribution.contribution(), contribution.reason()))
                .toList();

        return new RiskAssessmentResponse(
                road.getId(), road.getCode(), road.getName(),
                prediction.riskLevel(), prediction.disruptionScore(),
                prediction.probability(), prediction.modelName(),
                entries, prediction.recommendation(), at);
    }

    /** Rebuilds a response from a stored assessment, parsing the JSONB score card back. */
    private RiskAssessmentResponse toResponse(Road road, RoadRiskAssessment assessment) {
        List<RiskAssessmentResponse.ScoreCardEntry> entries = List.of();
        if (assessment.getScoreCard() != null) {
            try {
                entries = objectMapper.readValue(assessment.getScoreCard(),
                        objectMapper.getTypeFactory().constructCollectionType(
                                List.class, RiskAssessmentResponse.ScoreCardEntry.class));
            } catch (Exception ex) {
                log.debug("Stored score card for road {} could not be parsed", road.getId());
            }
        }
        return new RiskAssessmentResponse(
                road.getId(), road.getCode(), road.getName(),
                assessment.getRiskLevel(), assessment.getDisruptionScore(),
                assessment.getDisruptionScore() / 100.0, assessment.getModelName(),
                entries, "", assessment.getAssessedAt());
    }

    private Road requireRoad(Long roadId) {
        return roadRepository.findById(roadId)
                .orElseThrow(() -> new ResourceNotFoundException("Road", "id", roadId));
    }
}
