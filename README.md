🛍️ E-Commerce Platform (Spring Boot + Thymeleaf)
Overview
This project is a full-featured e-commerce platform built with Spring Boot and Thymeleaf, accompanied by two supporting microservices:

📊 Analytics Service

📣 Notification Service

The system is production-ready and includes role-based admin access, JWT authentication, payment integration, real-time metrics, and asynchronous messaging with Kafka.

🧩 Architecture
🔧 Main Components
Spring Boot — main application framework

Thymeleaf — server-side rendering for frontend

PostgreSQL — primary database

Redis — used for:

caching external API data (e.g. CDEK widget)

storing session identifiers across sessions

Kafka — message broker for:

sending data to the Analytics service

triggering events in the Notification service

Grafana + Prometheus — monitoring and observability

YooKassa — integrated for secure payment processing

🔐 Security
Spring Security protects the admin interface with role-based access control.

JWT-based authentication is implemented across services:

In production, JWT is passed via HttpOnly cookies

In development, JWT is passed via Authorization headers

In the Analytics microservice:

JWT is parsed

user info is extracted and injected into the authentication context

🌐 Microservices Communication
1. Analytics Service
Receives user activity/events via Kafka

Processes and stores analytical data

Extracts user identity from JWT for user-specific analytics

2. Notification Service
Listens to Kafka topics (e.g., order events, user activity)

Sends notifications (email, etc.)

⚙️ Features
🛒 Full product catalog and cart functionality

📦 Integration with CDEK delivery widget (cached via Redis)

💳 Secure payment processing via YooKassa

👨‍💼 Admin panel with protected access and category/product management

📈 Real-time analytics collection and visualization

🔔 Asynchronous user notifications

📊 Monitoring via Grafana/Prometheus

🚀 Deployment Notes
All services are containerized and orchestrated using Docker Compose

Environment-specific configurations, including all secrets and tokens, are securely stored in a .env file (not committed to version control)

The .env file is used by Docker Compose to inject environment variables into containers at runtime

This setup is suitable for both local development and small-scale production deployments on a VPS
