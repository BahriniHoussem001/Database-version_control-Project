--liquibase formatted sql

--changeset admin:V011-improve-artifact-display-test-to-product
ALTER TABLE PRODUCT ADD Improve_ARTIFACT_DISPLAY_TEST VARCHAR2(30) DEFAULT 'VISIBLE' NOT NULL;

--rollback ALTER TABLE PRODUCT DROP COLUMN Improve_ARTIFACT_DISPLAY_TEST;
