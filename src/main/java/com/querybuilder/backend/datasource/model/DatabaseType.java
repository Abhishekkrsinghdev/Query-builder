package com.querybuilder.backend.datasource.model;

/**
 * Supported database types
 */
public enum DatabaseType {
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", 3306),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", 5432),
    SQLSERVER("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433),
    ORACLE("Oracle", "oracle.jdbc.driver.OracleDriver", 1521);

    private final String displayName;
    private final String driverClassName;
    private final int defaultPort;

    DatabaseType(String displayName, String driverClassName, int defaultPort) {
        this.displayName = displayName;
        this.driverClassName = driverClassName;
        this.defaultPort = defaultPort;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public int getDefaultPort() {
        return defaultPort;
    }
}