# Coders - Collaborative Code Editor

## Login with GitHub
![Screenshot 2025-05-23 015116](https://github.com/user-attachments/assets/2ed3f0f3-b8ee-4080-ba0d-57802c147111)
![Screenshot 2025-05-23 015238](https://github.com/user-attachments/assets/874ef662-07cf-45ce-a2b7-e8a612240343)

## Projects Dashboard
All user projects are listed.

![Screenshot 2025-05-23 015304](https://github.com/user-attachments/assets/80c5d8f7-63af-4d39-8956-7a3e234a53a5)

## Create Project
User can create a project in java or python.

![Screenshot 2025-05-23 015324](https://github.com/user-attachments/assets/cb20314c-22b4-4c9c-a783-630f616faf58)

## Project
The project page lists all availble files and the code editor displays the selected file.
The file can be executed after being saved.
The developers and delete projects buttons are disabled for this user as the user does not own this project and was added as a collaborator by a diffrent developer.

![image](https://github.com/user-attachments/assets/3204a578-2334-4e9f-9a59-0b58e2645015)

## Adding developers
All registered users are listed and the project owner can add a selected user as an editor or a viewr.
Project owner can remove the developers he added.

![image](https://github.com/user-attachments/assets/fdfdae62-e0e4-456e-b819-77e9e1c5226f)


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
