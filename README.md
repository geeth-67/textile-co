# TextileCo Microservices Integration System

## Overview

TextileCo Microservices is a Spring Boot based microservices system developed for microservices integration and security evaluation.

The project consists of:

- Product Service
- Order Service
- Customer Service
- API Gateway
- Keycloak Authentication Server
- PostgreSQL Databases

All client requests are routed through the API Gateway, which validates JWT access tokens issued by Keycloak before forwarding requests to downstream services.

### Technologies

- Java 21
- Spring Boot 3
- Spring Cloud Gateway
- Spring Security
- Keycloak
- PostgreSQL
- Docker
- Docker Compose
- Maven
- JPA/Hibernate
