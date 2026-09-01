ALTER TABLE notification_event ADD COLUMN webhook_url VARCHAR(1024);

UPDATE notification_event ne
SET webhook_url = (SELECT c.webhook_url FROM client c WHERE c.id = ne.client_id);

ALTER TABLE notification_event ALTER COLUMN webhook_url VARCHAR(1024) NOT NULL;

ALTER TABLE client ADD COLUMN api_key VARCHAR(255) NOT NULL DEFAULT '';

UPDATE client SET api_key = 'key-client001' WHERE id = 'CLIENT001';
UPDATE client SET api_key = 'key-client002' WHERE id = 'CLIENT002';
UPDATE client SET api_key = 'key-client003' WHERE id = 'CLIENT003';
