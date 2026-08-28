package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.request.BridgeRequest;
import com.ner.smartlogix.dto.response.BridgeResponse;
import com.ner.smartlogix.entity.Bridge;
import com.ner.smartlogix.enums.BridgeStatus;
import com.ner.smartlogix.exception.DuplicateResourceException;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.mapper.RoadMapper;
import com.ner.smartlogix.repository.BridgeRepository;
import com.ner.smartlogix.repository.RoadRepository;
import com.ner.smartlogix.service.BridgeService;
import com.ner.smartlogix.util.GeometryUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bridges. Small service, but a closed bridge is often what actually cuts a district off. */
@Slf4j
@Service
@RequiredArgsConstructor
public class BridgeServiceImpl implements BridgeService {

    private final BridgeRepository bridgeRepository;
    private final RoadRepository roadRepository;
    private final RoadMapper roadMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BridgeResponse> findAll(Long roadId, BridgeStatus status) {
        List<Bridge> bridges;
        if (roadId != null) {
            bridges = bridgeRepository.findByRoadId(roadId);
        } else if (status != null) {
            bridges = bridgeRepository.findByStatus(status);
        } else {
            bridges = bridgeRepository.findAll();
        }
        return bridges.stream().map(roadMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BridgeResponse findById(Long id) {
        return roadMapper.toResponse(requireBridge(id));
    }

    @Override
    @Transactional
    public BridgeResponse create(BridgeRequest request) {
        if (bridgeRepository.findByCode(request.code()).isPresent()) {
            throw new DuplicateResourceException("Bridge", "code", request.code());
        }
        Bridge bridge = new Bridge();
        apply(bridge, request);
        return roadMapper.toResponse(bridgeRepository.save(bridge));
    }

    @Override
    @Transactional
    public BridgeResponse update(Long id, BridgeRequest request) {
        Bridge bridge = requireBridge(id);
        if (!bridge.getCode().equals(request.code())
                && bridgeRepository.findByCode(request.code()).isPresent()) {
            throw new DuplicateResourceException("Bridge", "code", request.code());
        }
        apply(bridge, request);
        return roadMapper.toResponse(bridgeRepository.save(bridge));
    }

    @Override
    @Transactional
    public BridgeResponse changeStatus(Long id, BridgeStatus status) {
        Bridge bridge = requireBridge(id);
        bridge.setStatus(status);
        log.info("Bridge {} set to {}", bridge.getCode(), status);
        return roadMapper.toResponse(bridgeRepository.save(bridge));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        bridgeRepository.delete(requireBridge(id));
    }

    private void apply(Bridge bridge, BridgeRequest request) {
        bridge.setCode(request.code());
        bridge.setName(request.name());
        bridge.setRoad(roadRepository.findByCode(request.roadCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Road", "code", request.roadCode())));
        bridge.setLocation(GeometryUtils.point(
                request.location().latitude(), request.location().longitude()));
        bridge.setLoadCapacityTons(request.loadCapacityTons());
        bridge.setCondition(request.condition());
        bridge.setStatus(request.status());
        bridge.setLastInspectionDate(request.lastInspectionDate());
    }

    private Bridge requireBridge(Long id) {
        return bridgeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bridge", "id", id));
    }
}
