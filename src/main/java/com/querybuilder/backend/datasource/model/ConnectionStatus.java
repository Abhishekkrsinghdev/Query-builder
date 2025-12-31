package com.querybuilder.backend.datasource.model;

/**
 * Connection status of a data source
 */
public enum ConnectionStatus {
    ACTIVE,      // Connection is working
    INACTIVE,    // User disabled it
    ERROR        // Connection failed
}