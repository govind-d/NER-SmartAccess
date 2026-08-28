package com.ner.smartlogix.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Per-item outcome of an offline sync. Never all-or-nothing: the phone uses these lists
 * to decide exactly which rows it may clear from IndexedDB.
 */
public record SyncResultResponse(
        List<UUID> accepted,
        List<UUID> duplicates,
        List<Failure> failed) {

    public record Failure(UUID clientUuid, String reason) {
    }
}
