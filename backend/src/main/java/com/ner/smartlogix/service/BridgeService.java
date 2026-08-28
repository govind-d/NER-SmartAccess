package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.BridgeRequest;
import com.ner.smartlogix.dto.response.BridgeResponse;
import com.ner.smartlogix.enums.BridgeStatus;
import java.util.List;

public interface BridgeService {

    List<BridgeResponse> findAll(Long roadId, BridgeStatus status);

    BridgeResponse findById(Long id);

    BridgeResponse create(BridgeRequest request);

    BridgeResponse update(Long id, BridgeRequest request);

    BridgeResponse changeStatus(Long id, BridgeStatus status);

    void delete(Long id);
}
