-- AI interactions (no soft delete - keep all for analytics)
CREATE TABLE ai_interactions (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    interaction_type ENUM('NL_TO_SQL', 'OPTIMIZATION', 'EXPLANATION', 'SUGGESTION') NOT NULL,
    input_text TEXT NOT NULL,
    output_text TEXT NOT NULL,
    datasource_id BINARY(16),
    confidence_score DECIMAL(3,2),
    tokens_used INT,
    response_time_ms INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (datasource_id) REFERENCES data_sources(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_type (interaction_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query analytics (no soft delete - aggregate data)
CREATE TABLE query_analytics (
    id BINARY(16) PRIMARY KEY,
    query_id BINARY(16) NOT NULL,
    total_executions INT NOT NULL DEFAULT 0,
    successful_executions INT NOT NULL DEFAULT 0,
    failed_executions INT NOT NULL DEFAULT 0,
    avg_execution_time_ms INT,
    max_execution_time_ms INT,
    min_execution_time_ms INT,
    last_executed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (query_id) REFERENCES queries(id) ON DELETE CASCADE,
    UNIQUE KEY unique_query (query_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;