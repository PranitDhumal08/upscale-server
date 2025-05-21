# Upscale Server

A Spring Boot-based backend server application with MongoDB integration, security features, and email capabilities.

## 🚀 Features

- RESTful API endpoints
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
├── dto/           # Data Transfer Objects
├── entity/        # MongoDB entities
├── repository/    # MongoDB repositories
├── security/      # Security configuration and JWT
└── service/       # Business logic services
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

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request