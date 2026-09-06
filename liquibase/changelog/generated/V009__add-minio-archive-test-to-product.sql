--liquibase formatted sql

--changeset admin:V009-add-minio-archive-test-to-product
ALTER TABLE PRODUCT ADD MINIO_ARCHIVE_TEST VARCHAR2(30) DEFAULT 'ARCHIVED' NOT NULL;

--rollback ALTER TABLE PRODUCT DROP COLUMN MINIO_ARCHIVE_TEST;
