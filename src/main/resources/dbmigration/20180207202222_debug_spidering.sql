ALTER TABLE spidering_configuration ADD COLUMN debug_spidering BIT(1);
UPDATE spidering_configuration SET debug_spidering = b'0';