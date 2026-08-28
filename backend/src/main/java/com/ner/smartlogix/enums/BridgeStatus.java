package com.ner.smartlogix.enums;

/** Bridges are modelled separately because a single weak bridge can block a whole corridor. */
public enum BridgeStatus {
    OPEN,
    WEIGHT_RESTRICTED,
    CLOSED
}
