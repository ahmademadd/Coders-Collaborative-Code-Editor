# Coders - Collaborative Code Editor
## Features

- **Real-Time Collaboration**: 
  - Multiple developers can edit files simultaneously, with real-time updates powered by WebSockets.
  
- **Project and File Management**:
  - Users can create projects, invite collaborators, and track details like project name, size, and last modified date.
  - File storage is managed with MinIO, allowing easy deployment across various cloud providers.
  - Metadata for files and folders is stored in a MySQL database.

- **Code Execution**:
  - Run code securely in Docker containers. Users can write and execute code within the editor, with output displayed in real-time.

- **User Authentication**:
  - Secure sign-up and sign-in using JWT-based authentication.
  - Dashboard access is restricted to authenticated users only.

## Tech Stack

- **Frontend**: React
  - Utilizes React Router, with protected routes for secure access.
  - Includes vintage theme styling with a light/dark mode toggle.

- **Backend**: Spring Boot
  - Custom security configuration with JWT for stateless sessions and CORS handling.
  - REST API handles authentication, project management, and file operations.

- **Database**: MySQL
  - Stores user and project data, including relationships between users and projects (e.g., Owner, Editor, Viewer).
  - **Future Work**: Consider Amazon RDS for reliable, managed database hosting.

- **Storage**: MinIO
  - Stores project files and folders, making them retrievable as needed.
  - Compatible with cloud deployments and optimized for local testing and containerized environments.

- **Containerization**: Docker
  - Used for isolated code execution and simplified deployments, managed with Docker Compose.

## Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/ahmademadd/Coders-Collaborative-Code-Editor
   cd Coders-Collaborative-Code-Editor

2. **Deployment with Docker Compose**:
   - For end-to-end deployment in a Docker environment, run:
     ```bash
     docker-compose up --build
     ```
