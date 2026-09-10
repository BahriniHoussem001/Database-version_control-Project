# DBVC Deployment Environment Configuration

## Purpose

The DBVC backend must not store real production secrets directly in Git.

The Git repository contains:

- application code
- safe default values for local development
- environment variable names
- example templates such as `.env.example`

The real production values must be provided by the runtime environment.

Examples:

- cloud environment variables
- server `.env` file
- Docker Compose environment section
- Kubernetes Secret
- AWS IAM Role

## Runtime Environment Variables

At runtime, Spring Boot reads values from environment variables.

Example:

```properties
dbvc.artifact-storage.bucket=${DBVC_ARTIFACT_STORAGE_BUCKET:dbvc-artifacts}