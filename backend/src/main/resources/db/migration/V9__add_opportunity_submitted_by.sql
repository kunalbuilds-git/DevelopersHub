ALTER TABLE opportunity ADD COLUMN submitted_by BIGINT REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_opportunity_submitted_by ON opportunity (submitted_by);
