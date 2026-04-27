-- Cloudflare D1 schema untuk tutorial_dashboard.
-- Status alur: draft -> generated -> published.
-- Mobile app hanya melihat baris dengan status='published'.

CREATE TABLE IF NOT EXISTS excels (
    id                    TEXT PRIMARY KEY,           -- slug (mis. "nasi-goreng")
    title                 TEXT NOT NULL,
    category              TEXT NOT NULL,              -- enum string (MainCourse, Soup, dst)
    description           TEXT NOT NULL DEFAULT '',
    difficulty            TEXT NOT NULL DEFAULT 'Medium',
    cooking_time_minutes  INTEGER NOT NULL DEFAULT 30,
    servings              INTEGER NOT NULL DEFAULT 4,
    tags_csv              TEXT NOT NULL DEFAULT '',   -- "minang,pedas,gurih"
    image_key             TEXT,                       -- KV object key
    status                TEXT NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','generated','published')),
    generated_at          INTEGER,                    -- epoch ms terakhir generate teks
    image_generated_at    INTEGER,                    -- epoch ms terakhir generate image
    published_at          INTEGER,                    -- epoch ms publish
    updated_at            INTEGER NOT NULL DEFAULT (unixepoch() * 1000),
    report_count          INTEGER NOT NULL DEFAULT 0  -- auto-unpublish threshold
);

CREATE INDEX IF NOT EXISTS idx_excels_status ON excels(status);
CREATE INDEX IF NOT EXISTS idx_excels_category ON excels(category);

CREATE TABLE IF NOT EXISTS ingredients (
    excel_id   TEXT NOT NULL,
    position    INTEGER NOT NULL,
    name        TEXT NOT NULL,
    quantity    TEXT NOT NULL DEFAULT '',
    unit        TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (excel_id, position),
    FOREIGN KEY (excel_id) REFERENCES excels(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS excel_steps (
    excel_id   TEXT NOT NULL,
    step_order  INTEGER NOT NULL,
    instruction TEXT NOT NULL,
    PRIMARY KEY (excel_id, step_order),
    FOREIGN KEY (excel_id) REFERENCES excels(id) ON DELETE CASCADE
);

-- Job tracker auto-generate tutorial dari halaman pencarian mobile.
-- User search miss → POST /api/requests → server insert processing + dispatch background →
-- mobile poll GET /api/requests/:id sampai completed/failed.
CREATE TABLE IF NOT EXISTS generation_requests (
    id                   TEXT PRIMARY KEY,
    query                TEXT NOT NULL,
    slug_target          TEXT NOT NULL,
    status               TEXT NOT NULL DEFAULT 'processing'
                         CHECK (status IN ('processing','completed','failed')),
    client_ip            TEXT,
    requested_at         INTEGER NOT NULL,
    completed_at         INTEGER,
    error_message        TEXT,
    resulting_excel_id  TEXT REFERENCES excels(id)
);

CREATE INDEX IF NOT EXISTS idx_requests_status
    ON generation_requests(status, requested_at);
CREATE INDEX IF NOT EXISTS idx_requests_slug
    ON generation_requests(slug_target);

-- In-app user reports (Google Play UGC & Generative AI compliance).
CREATE TABLE IF NOT EXISTS excel_reports (
    id          TEXT PRIMARY KEY,
    excel_id   TEXT NOT NULL REFERENCES excels(id) ON DELETE CASCADE,
    reason      TEXT NOT NULL,
    detail      TEXT NOT NULL DEFAULT '',
    client_ip   TEXT,
    created_at  INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reports_excel  ON excel_reports(excel_id);
CREATE INDEX IF NOT EXISTS idx_reports_created ON excel_reports(created_at DESC);
