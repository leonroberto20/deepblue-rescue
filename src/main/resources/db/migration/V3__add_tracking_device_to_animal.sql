-- ============================================================================
-- V3__add_tracking_device_to_animal.sql
-- Evolución del esquema: incorporación de dispositivo GPS de rastreo
-- ============================================================================

ALTER TABLE animals
    ADD COLUMN tracking_device_code VARCHAR(50) UNIQUE;
