-- liquibase formatted sql
-- changeset kstarosta:20170911222222

UPDATE importable_field SET name = 'Observation Time' WHERE name = 'Pricing Time';