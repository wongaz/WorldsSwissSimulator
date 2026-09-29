CREATE TABLE IF NOT EXISTS simulation_runs (
    id UUID PRIMARY KEY,
    dataset_id TEXT NOT NULL CHECK (btrim(dataset_id) <> ''),
    iterations INTEGER NOT NULL CHECK (iterations > 0),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL CHECK (completed_at >= started_at)
);

CREATE INDEX IF NOT EXISTS simulation_runs_completed_at_id_idx
    ON simulation_runs (completed_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS team_run_results (
    run_id UUID NOT NULL REFERENCES simulation_runs(id) ON DELETE CASCADE,
    result_order INTEGER NOT NULL CHECK (result_order >= 0),
    team_signature TEXT NOT NULL CHECK (btrim(team_signature) <> ''),
    team_name TEXT NOT NULL CHECK (btrim(team_name) <> ''),
    region TEXT NOT NULL CHECK (btrim(region) <> ''),
    seed INTEGER NOT NULL CHECK (seed > 0),
    category TEXT NOT NULL CHECK (category IN ('EASTERN', 'WESTERN', 'WILDCARD')),
    elo INTEGER NOT NULL CHECK (elo >= 0),
    qualifications INTEGER NOT NULL CHECK (qualifications >= 0),
    PRIMARY KEY (run_id, result_order),
    UNIQUE (run_id, team_signature)
);
