-- Queries (with soft delete support)
CREATE TABLE queries (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    datasource_id BINARY(16) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    sql_query TEXT NOT NULL,
    visual_config LONGTEXT,
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    category VARCHAR(50),
    tags JSON,
    version INT NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (datasource_id) REFERENCES data_sources(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_datasource_id (datasource_id),
    INDEX idx_is_public (is_public),
    INDEX idx_category (category),
    INDEX idx_is_template (is_template),
    INDEX idx_deleted (deleted),
    FULLTEXT idx_name_desc (name, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query versions (no soft delete - keep all history)
CREATE TABLE query_versions (
    id BINARY(16) PRIMARY KEY,
    query_id BINARY(16) NOT NULL,
    version_number INT NOT NULL,
    sql_query TEXT NOT NULL,
    visual_config LONGTEXT,
    change_summary VARCHAR(500),
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (query_id) REFERENCES queries(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_query_id (query_id),
    INDEX idx_version (query_id, version_number),
    UNIQUE KEY unique_query_version (query_id, version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query parameters (no soft delete - deleted when query deleted)
CREATE TABLE query_parameters (
    id BINARY(16) PRIMARY KEY,
    query_id BINARY(16) NOT NULL,
    param_name VARCHAR(100) NOT NULL,
    param_type ENUM('STRING', 'INTEGER', 'DECIMAL', 'DATE', 'BOOLEAN', 'ENUM') NOT NULL,
    default_value VARCHAR(255),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    validation_rule VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (query_id) REFERENCES queries(id) ON DELETE CASCADE,
    INDEX idx_query_id (query_id),
    INDEX idx_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query executions (no soft delete - keep all history for analytics)
CREATE TABLE query_executions (
    id BINARY(16) PRIMARY KEY,
    query_id BINARY(16),
    user_id BINARY(16) NOT NULL,
    datasource_id BINARY(16) NOT NULL,
    sql_query TEXT NOT NULL,
    execution_status ENUM('SUCCESS', 'FAILED', 'TIMEOUT', 'CANCELLED') NOT NULL,
    execution_time_ms INT,
    rows_returned INT,
    error_message TEXT,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (query_id) REFERENCES queries(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (datasource_id) REFERENCES data_sources(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_query_id (query_id),
    INDEX idx_datasource_id (datasource_id),
    INDEX idx_executed_at (executed_at),
    INDEX idx_status (execution_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query shares (no soft delete - just remove the share)
CREATE TABLE query_shares (
    id BINARY(16) PRIMARY KEY,
    query_id BINARY(16) NOT NULL,
    shared_with_user_id BINARY(16),
    permission ENUM('VIEW', 'EXECUTE', 'EDIT') NOT NULL DEFAULT 'VIEW',
    shared_by BINARY(16) NOT NULL,
    shared_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (query_id) REFERENCES queries(id) ON DELETE CASCADE,
    FOREIGN KEY (shared_with_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (shared_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_query_id (query_id),
    INDEX idx_shared_with (shared_with_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query favorites (no soft delete - just unfavorite)
CREATE TABLE query_favorites (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    query_id BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (query_id) REFERENCES queries(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_query (user_id, query_id),
    INDEX idx_user_id (user_id),
    INDEX idx_query_id (query_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;