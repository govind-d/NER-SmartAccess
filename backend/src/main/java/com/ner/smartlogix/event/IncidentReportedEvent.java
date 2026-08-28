package com.ner.smartlogix.event;

/** Published after an incident is persisted. Carries only the id: listeners re-read the
 *  entity inside their own transaction rather than passing a detached object around. */
public record IncidentReportedEvent(Long incidentId) {
}
