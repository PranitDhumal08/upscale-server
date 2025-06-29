# Upscale Server

A Spring Boot-based backend server application for project and task management, with MongoDB integration, security features, and email capabilities.

## 🚀 Features

- RESTful API endpoints for projects, tasks, users, sections, goals, people, messages, and more
- MongoDB database integration
- JWT-based authentication and authorization
- Email service integration
- Spring Security implementation
- Lombok for reduced boilerplate code

## 🛠️ Technology Stack

- Java 17
- Spring Boot 3.4.5
- MongoDB
- Spring Security
- JWT (JSON Web Tokens)
- Lombok
- Maven

## 📋 Prerequisites

- Java 17 or higher
- Maven
- MongoDB instance
- SMTP server for email functionality

## 🔧 Configuration

The application requires the following configurations in `application.properties` or `application.yml`:

```properties
# MongoDB Configuration
spring.data.mongodb.uri=your_mongodb_uri

# JWT Configuration
jwt.secret=your_jwt_secret
jwt.expiration=86400000

# Email Configuration
spring.mail.host=your_smtp_host
spring.mail.port=your_smtp_port
spring.mail.username=your_email_username
spring.mail.password=your_email_password
```

## 🏗️ Project Structure

```
src/main/java/com/upscale/upscale/
├── config/         # Configuration classes
├── controller/     # REST API controllers
├── dto/            # Data Transfer Objects
├── entity/         # MongoDB entities
├── repository/     # MongoDB repositories
├── security/       # Security configuration and JWT
└── service/        # Business logic services
```

## 🚀 Getting Started

1. Clone the repository
2. Configure the application properties
3. Build the project:
   ```bash
   mvn clean install
   ```
4. Run the application:
   ```bash
   mvn spring-boot:run
   ```

## 🔒 Security

The application uses Spring Security with JWT authentication. All endpoints (except public ones) require a valid JWT token in the Authorization header.

## 📧 Email Service

The application includes email service capabilities using Spring Boot Mail Starter. Configure your SMTP settings in the application properties to enable email functionality.

## 🧪 Testing

Run the test suite using:
```bash
mvn test
```

## 📚 API Endpoints

### User APIs (`/api/users`)
- `POST /send-otp` — Send OTP to user email
- `POST /login-user` — Login with email and password
- `POST /verify-otp` — Verify OTP for user
- `POST /create-user` — Create a new user
- `GET /user-info` — Get user profile info
- `PUT /profile-update` — Update user profile

### Project APIs (`/api/project`)
- `POST /create-project` — Create a new project
- `GET /dashboard/{project-id}` — Get project dashboard data
- `GET /list/{project-id}` — List all sections and tasks for a project
- `GET /board/{projectId}` — Get board view for a project
- `POST /add-section/{project-id}` — Add a section to a project
- `DELETE /delete/{project-id}` — Delete a project
- `DELETE /delete/section/{section-id}` — Delete a section from a project
- `GET /{projectId}/dashboard-stats` — Get dashboard statistics for a project
- `GET /{projectId}/calendar-tasks` — Get all tasks for a project, grouped by date (calendar view)
- `GET /{projectId}/timeline` — Get all sections and tasks for a project, grouped by section, including sectionId

### Task APIs (`/api/task`)
- `POST /set-task` — Create a new task with details
- `GET /get-task` — Get all tasks created by the user
- `GET /get-assign-task` — Get all tasks assigned to the user
- `POST /complete/{task-id}` — Mark a task as completed
- `DELETE /delete/{task-id}` — Delete a task
- `POST /create-name` — Create a task with just a name (minimal)
- `POST /create-task/details/{task-id}` — Add or update details for a specific task and move it to a section

### Goal APIs (`/api/goal`)
- `POST /set-goal` — Set a goal for the user
- `GET /get-goal` — Get the user's goal

### People APIs (`/api/people`)
- `POST /invite` — Invite a person to a project

### Message APIs (`/api/message`)
- `POST /send` — Send a message to another user

## 📝 Notes

- All endpoints (except authentication and OTP) require a valid JWT token in the `Authorization` header.
- For endpoints that require project or section IDs, use the IDs returned from the relevant create/list APIs.
- The timeline and calendar endpoints are designed for frontend visualizations (Gantt, calendar, etc.).
- All task and section modifications are persisted in MongoDB.

If you need more details on request/response formats for any endpoint, see the controller code or ask for a specific example!
