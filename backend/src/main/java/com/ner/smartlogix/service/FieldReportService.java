package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.FieldReportSyncRequest;
import com.ner.smartlogix.dto.response.FieldReportResponse;
import com.ner.smartlogix.dto.response.SyncResultResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FieldReportService {

    /** Batch upload from a phone that has been offline. Reports individual outcomes. */
    SyncResultResponse sync(FieldReportSyncRequest request);

    Page<FieldReportResponse> findMine(Pageable pageable);

    Page<FieldReportResponse> findPendingReview(Pageable pageable);

    /** Turns a reviewed field report into a real incident. */
    FieldReportResponse promoteToIncident(Long reportId);
}
