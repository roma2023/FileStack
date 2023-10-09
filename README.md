# Distributed File System Project

## Project Overview
The Distributed File System project, also known as FileStack, is a software system designed to enable efficient and reliable file storage and retrieval in a distributed environment. It facilitates seamless access to files from various clients by interacting with a centralized Naming Server and distributed Storage Servers.

### Purpose
The purpose of this project is to develop a robust, scalable, and fault-tolerant distributed file system. This system aims to overcome the limitations of a traditional file system by distributing data across multiple servers, providing high availability, load balancing, and fault tolerance. Clients can access and manipulate files as if they were stored on a single local machine, abstracting the complexities of distributed storage.

### Key Objectives
- **Efficient Data Access**: Enable clients to read and write files efficiently by minimizing latency and maximizing throughput.
- **Fault Tolerance**: Ensure system resilience by handling failures gracefully, allowing uninterrupted file access even in the presence of failures.
- **Scalability**: Design the system to scale seamlessly as the number of clients and the volume of data grows, without compromising performance.
- **Consistency and Reliability**: Maintain data consistency and reliability across distributed nodes, ensuring that data remains accurate and available.

## Working Example
To illustrate the system's functionality, let's consider a scenario where a client needs to read a file named "abc". The client first contacts the Naming Server to obtain a StorageStub associated with the file. Using this stub, the client communicates with the appropriate Storage Server to read the contents of the file.

## Technical Implementation

### RMI Package
The Remote Method Invocation (RMI) package plays a crucial role in facilitating communication between different entities in the distributed file system. It consists of two main components: Skeleton and Stub. The Skeleton is responsible for handling incoming client requests, whereas the Stub acts as a dynamic proxy, redirecting method calls to the appropriate Skeleton. These components utilize Java's built-in RMI capabilities to enable remote communication seamlessly.

### Naming Package
The Naming package manages the directory tree structure and associated metadata for the files stored in the system. It handles operations such as registration, file listing, creation, and stub retrieval. The Naming Server is a central component that builds and maintains the directory tree, allowing clients to perform various operations on the stored files.

### Storage Package
The Storage package deals with managing the local file systems on the Storage Servers. It provides functionalities for file access, registration, directory operations, and file deletion. Each Storage Server hosts its local file system and communicates with the Naming Server to ensure consistent file management across the distributed environment.

### Common Package
The Common package contains utility functions to manipulate paths, used by both the Naming and Storage packages. These utility functions are essential for handling and resolving file paths efficiently.

## Conclusion
The Distributed File System project aims to offer a highly efficient and reliable file storage solution in a distributed setting. By leveraging RMI, intelligent naming mechanisms, and distributed storage management, the system ensures seamless file access and robustness even in the face of failures, making it an invaluable tool for distributed computing environments.

