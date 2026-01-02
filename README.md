# Pixl

A modern pixel art creation and sharing platform.

## Overview

**Pixl** is a full-stack web application that enables users to create, share, and manage pixel art. Design artwork on a customizable canvas, save your creations, and share them with the community or via private shareable links.

## Key Features

- 🎨 Interactive pixel art canvas (default 16x16 pixels)
- 👤 User authentication and profile management
- 💾 Save and organize artwork collections
- 🌐 Public galleries and private shareable links

## Technology Stack

- **Backend**: Java 21, Jakarta EE (Servlet API), Apache Tomcat 10, MySQL 8
- **Frontend**: HTML5/CSS3/JavaScript, Nginx
- **Testing**: JUnit 5, Mockito, H2 Database
- **DevOps**: Docker, Docker Compose, Maven

## Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd pixl
   ```

2. **Set up environment variables**
   
   Create a `.env` file in the root directory, see [.env.example](.env.example) for reference:
   ```env
   MYSQL_ROOT_PASSWORD=your_root_password
   MYSQL_DATABASE=pixl
   MYSQL_USER=pixl_user
   MYSQL_PASSWORD=your_password
   ```

3. **Start the application**
   ```bash
   docker-compose up -d
   ```

4. **Access the application**
   - Frontend: http://localhost
   - Backend API: http://localhost:8080/api/v1/
   - Database: localhost:3306

## Development

**Build the backend**
```bash
cd backend
mvn clean install
```

**Run tests**
```bash
mvn test
```

## Documentation

For detailed documentation, see:
- [Full Documentation](DOCUMENTATION.md) - Architecture, API endpoints, features
- [Test Suite Documentation](backend/TEST_SUITE_README.md)
- [API Examples](api-test.http)

## License

See the [LICENSE](LICENSE) file for details.

## Author

Mugtaba Mohamed