# YouTube PubSubHubbub Service

This microservice handles YouTube PubSubHubbub requests and logs new content titles. It was created using the [Ktor Framework](https://ktor.io).

## What is PubSubHubbub?

[PubSubHubbub](https://github.com/pubsubhubbub/PubSubHubbub) (now also known as WebSub) is a protocol that enables real-time notifications when content is published. For YouTube, this means getting instant notifications when a channel uploads a new video.

## Features

| Feature                      | Description                                                         |
|------------------------------|---------------------------------------------------------------------|
| Subscription Verification    | Handles YouTube's subscription verification requests                |
| Content Notifications        | Processes notifications when new videos are published               |
| XML Parsing                  | Parses YouTube's Atom XML feed format                              |
| JSON Support                 | Supports JSON for API requests from third-party clients             |
| Title Logging                | Logs the title of newly published videos                            |
| Subscription Persistence     | Stores subscription data in a database for management               |
| Notification Queueing        | Queues notifications for reliable processing                        |
| Notification Processing      | Consumes and processes notifications from the queue (Phase 3)       |
| Containerization             | Docker and Docker Compose support for easy deployment (Phase 3)     |
| Rate Limiting                | Protects against DoS attacks and excessive resource usage (Phase 3) |
| Caching                      | Caches frequently accessed data for improved performance            |

## Endpoints

### General
- `GET /` - Home page

### Health Checks
- `GET /health` - Basic health check for load balancing and Kubernetes
- `GET /health/ready` - Readiness check that verifies all required services are available

### PubSubHubbub
- `GET /pubsub/youtube` - Handles subscription verification from YouTube
- `POST /pubsub/youtube` - Receives content notifications when new videos are published

### Subscription Management (Phase 4)
- `GET /api/subscriptions` - Retrieves all active subscriptions
- `GET /api/subscriptions/all` - Retrieves all subscriptions (including inactive)
- `GET /api/subscriptions/{channelId}` - Retrieves a specific subscription by channel ID
- `POST /api/subscriptions` - Creates a new subscription and sends request to YouTube hub
- `PUT /api/subscriptions/{channelId}` - Updates an existing subscription
- `PUT /api/subscriptions/{channelId}/status` - Updates a subscription's status (active/inactive)
- `DELETE /api/subscriptions/{channelId}` - Deletes a subscription

### Notification Management (Phase 3)
- `GET /api/notifications/consumer/status` - Checks the status of the notification consumer
- `POST /api/notifications/consumer/start` - Starts the notification consumer
- `POST /api/notifications/consumer/stop` - Stops the notification consumer

### Service Configuration (Phase 4)
- `GET /api/config` - Gets the current service configuration (cache and rate limiting)
- `PUT /api/config` - Updates the service configuration

## API Examples

### Creating a Subscription with JSON

You can create a new subscription by sending a POST request to the `/api/subscriptions` endpoint with a JSON payload:

```bash
curl -X POST http://localhost:8080/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "channelId": "YOUR_YOUTUBE_CHANNEL_ID",
    "topic": "https://www.youtube.com/feeds/videos.xml?channel_id=YOUR_YOUTUBE_CHANNEL_ID",
    "callbackUrl": "http://pubsub.wernernagy.hu/pubsub/youtube",
    "leaseSeconds": 3600
  }'
```

### Updating Service Configuration with JSON

You can update the service configuration by sending a PUT request to the `/api/config` endpoint with a JSON payload:

```bash
curl -X PUT http://localhost:8080/api/config \
  -H "Content-Type: application/json" \
  -d '{
    "cacheEnabled": true,
    "cacheHeapSize": 200,
    "cacheTtlSeconds": 1800,
    "rateLimitEnabled": true,
    "rateLimitPerMinute": 60
  }'
```

## Setting Up YouTube PubSubHubbub Subscriptions

To receive notifications from YouTube:

1. **Deploy this service** to a publicly accessible URL (e.g., using a service like ngrok for testing)
2. **Subscribe to a topic** by sending a POST request to YouTube's hub:

```
POST https://pubsubhubbub.appspot.com/subscribe
Content-Type: application/x-www-form-urlencoded

hub.callback=https://your-service-url.com/pubsub/youtube
hub.topic=https://www.youtube.com/xml/feeds/videos.xml?channel_id=CHANNEL_ID
hub.verify=sync
hub.mode=subscribe
```

Replace `CHANNEL_ID` with the YouTube channel ID you want to monitor and `your-service-url.com` with your deployed service URL.

3. **Verification**: YouTube will send a GET request to your callback URL with a `hub.challenge` parameter
4. **Notifications**: When a new video is published, YouTube will send a POST request with the video details

## Building & Running

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
|-------------------------------|----------------------------------------------------------------------|
| `./gradlew test`              | Run the TestNG tests                                                 |
| `./gradlew build`             | Build everything                                                     |
| `./gradlew run`               | Run the server                                                       |
| `./gradlew buildFatJar`       | Build an executable JAR with all dependencies included               |

If the server starts successfully, you'll see the following output:

```
INFO  Application - Application started in 0.303 seconds.
INFO  Application - Responding at http://0.0.0.0:8080
```

## Docker Containerization (Phase 3)

As part of Phase 3, the application has been containerized to facilitate deployment and scaling in cloud environments.

### Using Docker

To build and run the application using Docker:

```bash
# Build the Docker image
docker build -t pubsub-service .

# Run the container
docker run -p 8080:8080 -e RABBITMQ_HOST=localhost pubsub-service
```

### Using Docker Compose

For a complete environment with RabbitMQ:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

The docker-compose.yml file includes:
- The application service
- RabbitMQ with management UI (accessible at http://localhost:15672)
- Persistent volumes for both services

### Scaled Deployment with Load Balancing

For production environments, a scaled deployment with load balancing is available:

```bash
# Start the scaled deployment
docker-compose -f docker-compose-scaled.yml up -d

# View logs
docker-compose -f docker-compose-scaled.yml logs -f

# Stop all services
docker-compose -f docker-compose-scaled.yml down
```

The docker-compose-scaled.yml file includes:
- Three application instances for high availability
- Caddy as a load balancer (reverse proxy)
- PostgreSQL for persistent storage
- RabbitMQ for message queuing
- Redis for distributed caching
- Proper networking between all services

#### Caddy Configuration

The project uses Caddy as a modern, efficient reverse proxy and load balancer. Caddy offers several advantages:
- Automatic HTTPS with Let's Encrypt
- Simple, declarative configuration
- HTTP/2 and HTTP/3 support
- Built-in load balancing

The Caddy configuration is stored in the `Caddyfile` at the root of the project.

### Environment Variables

The following environment variables can be configured:

| Variable            | Description                   | Default Value      |
|---------------------|-------------------------------|-------------------|
| RABBITMQ_HOST       | RabbitMQ server hostname      | localhost         |
| RABBITMQ_PORT       | RabbitMQ server port          | 5672              |
| RABBITMQ_USERNAME   | RabbitMQ username             | guest             |
| RABBITMQ_PASSWORD   | RabbitMQ password             | guest             |
| H2_DB_PATH          | H2 database file path         | ./build/pubsub-db |

### Rate Limiting (Phase 3)

As part of Phase 3, the application implements rate limiting to protect against DoS attacks and excessive resource usage. The rate limiting features include:

- **IP-based rate limiting**: Limits requests based on client IP address
- **Endpoint-specific limits**: Different limits for different types of endpoints
  - API endpoints: 30 requests per minute
  - PubSubHubbub endpoints: 120 requests per minute (higher to accommodate YouTube)
  - Other endpoints: 60 requests per minute
- **Sliding window approach**: Counts are reset after the window period (60 seconds)
- **429 Too Many Requests responses**: Returns appropriate HTTP status code with helpful message

The rate limiting implementation leverages the existing caching infrastructure for efficiency and minimal overhead.

## Logs

When a new video is published, you'll see a log entry like:

```
INFO  com.example.Routing - New YouTube content: 'Video Title' by Channel Name (ID: video_id)
```

## Tests

The project uses TestNG for testing. The tests are organized into the following classes:

| Test Class                    | Description                                                          |
|-------------------------------|----------------------------------------------------------------------|
| `ApplicationTest`             | Tests the application configuration and plugins                      |
| `RoutingTest`                 | Tests the routing functionality and endpoints                        |
| `YouTubeFeedTest`             | Tests the parsing of YouTube's Atom XML feed format                  |

To run the tests, use the following command:

```
./gradlew test
```

## original Junie instructions
this project is a microservice that handles youtube pubsubhubbub requests and logs the new content's title. Please create the project structure and add the necessary ktor plugins, and implement the basic funcionality

## Junie's plan
1. Examine the current project structure and understand existing files
2. Check the build.gradle.kts file to understand dependencies and add necessary ones
3. Review the Application.kt and Routing.kt files to understand the current setup
4. Check the application.yaml configuration file
5. Check the logging configuration
6. Update build.gradle.kts with necessary dependencies
7. Modify Application.kt to add required plugins
8. Create data models for PubSubHubbub requests
9. Implement the PubSubHubbub endpoint in Routing.kt
10. Create a README with instructions for setting up YouTube PubSubHubbub subscriptions
11. Build the project to verify there are no compilation errors
12. Summarize the implementation and provide final thoughts

## Summary
A microservice was successfully implemented to handle YouTube PubSubHubbub requests, logging new content titles. The project structure was established with necessary Ktor plugins, and all functionalities were implemented without errors, including subscription verification and content notification handling. 

Phase 3 added a notification consumer service that processes notifications from the queue, with API endpoints to check and control its status. Additionally, Phase 3 implemented containerization with Docker and Docker Compose, making the application easier to deploy and scale. The application now supports environment variables for configuration, allowing for flexible deployment in different environments.

The application built successfully, confirming that there are no compilation issues.
