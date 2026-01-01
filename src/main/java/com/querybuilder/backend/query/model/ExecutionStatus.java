package com.querybuilder.backend.query.model;

/**
 * Execution status of a query
 */
public enum ExecutionStatus {
    SUCCESS,
    FAILED,
    TIMEOUT,
    CANCELLED
}