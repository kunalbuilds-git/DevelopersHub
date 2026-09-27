CREATE TABLE tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE opportunity_tag (
    opportunity_id BIGINT NOT NULL REFERENCES opportunity(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    PRIMARY KEY (opportunity_id, tag_id)
);
