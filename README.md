# Distributed Systems Project: FileStack

## Table of Contents
- [Project Overview](#project-overview)
  - [Purpose](#purpose)
  - [Key Objectives](#key-objectives)
- [Project Architecture](#project-architecture)
  - [FileStack Architecture](#filestack-architecture)
  - [ConsistentStore Architecture](#consistentstore-architecture)
- [Working Example](#working-example)
- [Technical Implementation](#technical-implementation)
  - [RMI Package](#rmi-package)
  - [Naming Package](#naming-package)
  - [Storage Package](#storage-package)
  - [Common Package](#common-package)
- [Conclusion](#conclusion)

## Project Overview
The FileStack project, which includes two subprojects (FileStack and ConsistentStore), is a distributed file system designed for efficient and reliable file storage and retrieval. It provides seamless file access across distributed servers, ensuring high availability, fault tolerance, and load balancing.

### Purpose
The purpose of the FileStack project is to develop a robust, scalable, and fault-tolerant distributed file system that overcomes the limitations of traditional file storage. Clients can access and manipulate files as though they were stored locally, abstracting away the complexities of distributed storage.

### Key Objectives
- **Efficient Data Access:** Enable clients to read and write files with minimal latency while maximizing throughput.
- **Fault Tolerance:** Ensure resilience by gracefully handling failures, maintaining uninterrupted file access.
- **Scalability:** Design the system to scale seamlessly with growing data and client volume.
- **Consistency and Reliability:** Maintain data accuracy and availability through consistency protocols across nodes.

## Project Architecture

### FileStack Architecture
- **Storage Nodes:** Store files and provide CRUD operations.
- **Metadata Node:** Centralized metadata management for file location and load balancing.
- **Client:** Routes user requests via the metadata node to storage nodes.

### ConsistentStore Architecture
- **Consensus Algorithm:** Implements a consensus protocol (e.g., Raft) for strong consistency.
- **Coordinator:** Manages transaction scheduling and ensures consistency across data nodes.
- **Data Nodes:** Store replicas and communicate via the consensus protocol.

### Communication
- **gRPC and RMI:** Communication between components is facilitated via gRPC (for Project 2) and Java RMI (Project 1).

## Working Example
A client needs to read the file "abc." The client:
1. Contacts the Naming Server to retrieve the StorageStub associated with the file.
2. Uses the StorageStub to communicate with the appropriate Storage Server for the file content.

## Technical Implementation

### RMI Package
The Remote Method Invocation (RMI) package enables inter-component communication. It includes:
- **Skeleton:** Handles incoming client requests.
- **Stub:** A dynamic proxy that redirects method calls to the appropriate Skeleton.

### Naming Package
The Naming package manages directory tree structure and file metadata. It includes operations such as registration, listing, creation, and stub retrieval. The Naming Server maintains the directory tree and allows various client operations.

### Storage Package
The Storage package manages local file systems on the Storage Servers and provides access, directory operations, and file deletion. Each Storage Server communicates with the Naming Server to maintain consistency.

### Common Package
The Common package includes utility functions for path manipulation, used by both Naming and Storage packages.

## Conclusion
The FileStack project aims to provide an efficient and reliable distributed file system through replication, intelligent naming mechanisms, and distributed storage. It is robust and fault-tolerant, offering seamless access to files in distributed computing environments.

