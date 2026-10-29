ALTER TABLE opportunity ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

CREATE INDEX idx_opportunity_status ON opportunity (status);
