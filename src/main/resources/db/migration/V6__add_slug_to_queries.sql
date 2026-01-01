-- Add slug column to queries table
ALTER TABLE queries
ADD COLUMN slug VARCHAR(255) UNIQUE AFTER name,
ADD INDEX idx_slug (slug);