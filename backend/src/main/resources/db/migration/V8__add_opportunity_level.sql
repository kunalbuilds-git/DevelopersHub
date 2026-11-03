ALTER TABLE opportunity ADD COLUMN level VARCHAR(20) NOT NULL DEFAULT 'ANY';

CREATE INDEX idx_opportunity_level ON opportunity (level);
