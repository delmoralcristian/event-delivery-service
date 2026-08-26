-- Insert clients
INSERT INTO client (id, name, webhook_url, active) VALUES
('CLIENT001', 'Client A', 'https://eoypzscft3z87sj.m.pipedream.net/', true),
('CLIENT002', 'Client B', 'https://eoypzscft3z87sj.m.pipedream.net/', true),
('CLIENT003', 'Client C', 'https://eoypzscft3z87sj.m.pipedream.net/', true);

-- Insert notification events
INSERT INTO notification_event (event_id, event_type, content, delivery_date, delivery_status, client_id) VALUES
('EVT001', 'credit_card_payment',   'Credit card payment received for $150.00',                   '2024-03-15 09:30:22', 'COMPLETED', 'CLIENT001'),
('EVT002', 'debit_card_withdrawal', 'ATM withdrawal of $200.00',                                  '2024-03-15 10:15:45', 'COMPLETED', 'CLIENT001'),
('EVT003', 'credit_transfer',       'Bank transfer received from Account #4567 for $1,500.00',   '2024-03-15 11:20:18', 'FAILED',    'CLIENT002'),
('EVT004', 'debit_automatic_payment','Monthly utility bill payment of $85.50',                    '2024-03-15 12:05:33', 'COMPLETED', 'CLIENT002'),
('EVT005', 'credit_refund',         'Refund processed for order #789 for $45.99',                '2024-03-15 13:45:10', 'FAILED',    'CLIENT003'),
('EVT006', 'debit_transfer',        'Money transfer sent to Account #8901 for $500.00',          '2024-03-15 14:30:55', 'COMPLETED', 'CLIENT003'),
('EVT007', 'credit_deposit',        'Direct deposit received from Employer XYZ for $2,500.00',   '2024-03-15 15:20:40', 'COMPLETED', 'CLIENT001'),
('EVT008', 'debit_purchase',        'Point of sale purchase at Store ABC for $75.25',            '2024-03-15 16:10:15', 'COMPLETED', 'CLIENT002'),
('EVT009', 'credit_cashback',       'Cashback reward credited for $25.00',                       '2024-03-15 17:25:30', 'FAILED',    'CLIENT003'),
('EVT010', 'debit_subscription',    'Monthly streaming service payment of $14.99',               '2024-03-15 18:05:12', 'COMPLETED', 'CLIENT001');
