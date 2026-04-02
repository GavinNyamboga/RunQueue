ALTER TABLE execution_logs
    ADD COLUMN job_id UUID;

ALTER TABLE execution_logs
    ADD COLUMN worker_id VARCHAR(255);

ALTER TABLE execution_logs
    ADD COLUMN attempt INTEGER;

ALTER TABLE execution_logs
    ADD COLUMN event_type VARCHAR(40);

ALTER TABLE execution_logs
    ADD COLUMN run_status VARCHAR(30);

ALTER TABLE execution_logs
    ADD COLUMN error_code VARCHAR(100);

ALTER TABLE execution_logs
    ADD COLUMN details_json TEXT;

UPDATE execution_logs
SET job_id = '00000000-0000-0000-0000-000000000000',
    attempt = 1,
    event_type = 'LEGACY',
    run_status = 'UNKNOWN'
WHERE job_id IS NULL;

ALTER TABLE execution_logs
    ALTER COLUMN job_id SET NOT NULL;

ALTER TABLE execution_logs
    ALTER COLUMN attempt SET NOT NULL;

ALTER TABLE execution_logs
    ALTER COLUMN event_type SET NOT NULL;

ALTER TABLE execution_logs
    ALTER COLUMN run_status SET NOT NULL;

CREATE INDEX idx_execution_logs_run_id ON execution_logs (run_id);
CREATE INDEX idx_execution_logs_job_id ON execution_logs (job_id);
CREATE INDEX idx_execution_logs_logged_at ON execution_logs (logged_at);
