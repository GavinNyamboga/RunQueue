CREATE TABLE job_runs
(
    id            UUID        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    job_id        UUID        NOT NULL,
    status        VARCHAR(30) NOT NULL,
    attempt       INTEGER     NOT NULL,
    scheduled_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    queued_at     TIMESTAMP WITHOUT TIME ZONE,
    started_at    TIMESTAMP WITHOUT TIME ZONE,
    finished_at   TIMESTAMP WITHOUT TIME ZONE,
    next_retry_at TIMESTAMP WITHOUT TIME ZONE,
    worker_id     VARCHAR(255),
    error_message TEXT,
    result_json   TEXT,
    version       BIGINT,
    CONSTRAINT pk_job_runs PRIMARY KEY (id)
);

CREATE INDEX idx_job_runs_job_id ON job_runs (job_id);

CREATE INDEX idx_job_runs_scheduled_at ON job_runs (scheduled_at);

CREATE INDEX idx_job_runs_status ON job_runs (status);