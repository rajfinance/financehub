-- Category icons are stored only in icon_data / icon_content_type.
-- Run once after deploying the build that removes the legacy icon path column:

ALTER TABLE expense_categories DROP COLUMN IF EXISTS icon;

-- Ensure binary columns exist (no-op if already added):
ALTER TABLE expense_categories ADD COLUMN IF NOT EXISTS icon_data BYTEA;
ALTER TABLE expense_categories ADD COLUMN IF NOT EXISTS icon_content_type VARCHAR(64);
