# Database Version Control System

This project aims to design and implement a **Database Version Control System** for managing Oracle database schema changes.

The goal is to provide a structured and traceable process for applying database migrations, tracking schema evolution, and preparing the project for future integration with a web dashboard, S3/MinIO artifact storage, and CI/CD automation.

---

## Project Objective

In many systems, database schema changes are applied manually and are difficult to track over time.

This project solves that problem by introducing a version-controlled migration workflow for Oracle databases.

The system should allow us to:

- create versioned migration scripts;
- apply schema changes in a controlled way;
- track executed migrations;
- identify pending migrations;
- support rollback strategies;
- prepare the system for audit, reporting, and deployment automation.

---

## Current Development Phase

We are currently working on the first proof of concept:

> Apply a versioned SQL migration to an Oracle database using Liquibase and track it in the Liquibase changelog tables.

At this stage, the system uses:

- Oracle Database Free in Docker;
- Liquibase in Docker;
- Oracle JDBC driver;
- Docker Compose;
- SQL migration scripts.

---

## Project Structure

```text
database-version-control/
│
├── docs/
│   └── development-notes.md
│
├── drivers/
│   └── ojdbc11.jar
│
├── liquibase/
│   ├── changelog/
│   │   └── db.changelog-master.sql
│   └── liquibase.properties
│
├── migrations/
│   └── V001__create_customer_table.sql
│
├── rollback/
│   └── U001__drop_customer_table.sql
│
├── docker-compose.yml
└── README.md