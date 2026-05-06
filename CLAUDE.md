# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PoomJobs (Poor Man's Jobs) is a distributed job queue framework built with Java/Maven. It provides REST APIs for job registration, runner management, and task processing with support for distributed execution across multiple runner nodes.

## Build Commands

```bash
# Full build with tests
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Build a specific module
mvn clean install -pl poomjobs-domain -am

# Run tests for a specific module
mvn test -pl poomjobs-runner-service

# Run a single test class
mvn test -pl poomjobs-domain -Dtest=JobValueCreationTest

# Run a single test method
mvn test -pl poomjobs-domain -Dtest=JobValueCreationTest#testMethodName
```

## Architecture

### Core Components

1. **Registries Service** (`poomjobs-registries-service`) - Central service hosting Job Registry and Runner Registry APIs
   - Entry point: `PoomjobRegistriesService.main()`
   - Endpoints: `/poomjobs-jobs/v1/*`, `/poomjobs-runners/v1/*`

2. **Runner Service** (`poomjobs-runner-service`) - Distributed worker nodes that execute jobs
   - Uses fluent builder: `RunnerService.setup().jobs(...).clients(...).concurrency(...).endpoint(...).build()`
   - Two pool implementations: experimental (`USE_EXPERIMENTAL_POOL=true`) and legacy

3. **Domain** (`poomjobs-domain`) - Value objects and repositories for Job and Runner entities

4. **Task Support** (`poom-task-support`) - Higher-level abstraction layer on top of jobs for async task processing

### Code Generation

The project uses extensive code generation from specifications:

- **RAML 1.0 → Java**: API types, handlers, and clients generated via `cdm-rest-maven-plugin`
  - Specs in: `api/poomjobs-api-spec/src/main/resources/*.raml`
  - Tasks specs in: `poom-task-support/poom-task-api/poom-task-api-spec/src/main/resources/tasks.raml`

- **YAML → Value Objects**: Immutable domain objects generated via `cdm-value-objects-maven-plugin`
  - Specs in: `poomjobs-domain/src/main/resources/*.yaml`

- **MongoDB Mappers**: Auto-generated via `flexio-mongo-io-maven-plugin`

### Key Interfaces

**JobProcessor** (`api/poomjobs-processor/.../JobProcessor.java`) - Implement to handle job types:
```java
public interface JobProcessor {
    Job process() throws JobProcessingException;

    interface Factory {
        JobProcessor createFor(Job job, JobMonitor monitor);
    }
}
```

**TaskJobProcessor** (`poom-task-support/.../TaskJobProcessor.java`) - Abstract base for task-based jobs that bridges tasks to jobs.

### Job Lifecycle

1. Job created via `POST /poomjobs-jobs/v1/jobs` → status: `PENDING`
2. `RunnerInvokerListener` delegates to available runner matching competencies
3. Runner reserves job → status: `RUNNING`
4. `JobProcessor.process()` executes
5. Job completed → status: `DONE`, exit: `SUCCESS`/`FAILURE`/`ABORTED`

### Domain Models

**JobValue** (from `poomjobs-domain/src/main/resources/job-value.yaml`):
- `category`, `name`: Job type identification
- `arguments`: String array passed to processor
- `status.run`: `PENDING` → `RUNNING` → `DONE`
- `status.exit`: `SUCCESS`, `FAILURE`
- `processing`: Timestamps for submitted/started/finished
- `accounting`: Account context (accountId, extension)

**RunnerValue** (from `poomjobs-domain/src/main/resources/runner-value.yaml`):
- `callback`: URL for job delegation
- `competencies`: Categories and names the runner handles
- `runtime.status`: `IDLE`, `RUNNING`, `DISCONNECTED`

### Module Dependencies

```
api/
├── poomjobs-api-spec      → RAML specifications
├── poomjobs-api           → Generated types (depends on spec)
├── poomjobs-clients       → Generated HTTP clients
└── poomjobs-processor     → JobProcessor interface

poomjobs-domain            → Value objects, repositories
poomjobs-service           → API handler implementations
poomjobs-runner            → Runner framework
poomjobs-runner-service    → Runner service entry point
poomjobs-registries-service → Central registry service

poom-task-support/         → Task abstraction layer
├── poom-task-api/         → Task API spec and types
├── poom-task-context/     → Thread-local logging context
└── poom-task-service-support/ → Task handlers and job bridge
```

### Environment Variables

- `SERVICE_HOST`, `SERVICE_PORT`: Registry service binding
- `SERVICE_URL`: Runner callback URL
- `CLIENT_POOL_SIZE`: HTTP connection pool size (default: 5)
- `USE_EXPERIMENTAL_POOL`: Enable new job pool implementation
- `SERVICE_RUNTIME`: `UNDERTOW` (default) or `NETTY`
- `PROCESS_SHUTDOWN_PROPERLY_TIMEOUT_IN_SECONDS`: Graceful shutdown timeout (default: 20)

## Parent Framework

This project extends `poom-services` (org.codingmatters.poom) which provides common infrastructure for REST microservices including HTTP containers (Undertow/Netty), repository patterns, and client generation.