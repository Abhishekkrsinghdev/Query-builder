package com.querybuilder.backend.query.model;

/**
 * Share permissions for queries
 */
public enum SharePermission {
    VIEW,      // Can only view the query
    EXECUTE,   // Can view and execute
    EDIT       // Can view, execute, and edit
}