CREATE TABLE jobs
(
    id                  UUID         NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    name                VARCHAR(120) NOT NULL,
    description         TEXT,
    type                VARCHAR(255) NOT NULL,
    schedule_type       VARCHAR(255) NOT NULL,
    cron_expression     VARCHAR(255),
    run_at              TIMESTAMP WITHOUT TIME ZONE,
    status              VARCHAR(20)  NOT NULL,
    retry_strategy      VARCHAR(30)  NOT NULL,
    max_retries         INTEGER      NOT NULL,
    retry_delay_seconds BIGINT       NOT NULL,
    timeout_seconds     BIGINT       NOT NULL,
    payload_json        TEXT,
    last_scheduled_at   TIMESTAMP WITHOUT TIME ZONE,
    next_run_at         TIMESTAMP WITHOUT TIME ZONE,
    version             BIGINT,
    CONSTRAINT pk_jobs PRIMARY KEY (id)
);

ALTER TABLE jobs
    ADD CONSTRAINT uc_jobs_name UNIQUE (name);