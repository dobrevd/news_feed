# News Feed

## Technologies Used
- **Spring Boot** – primary framework.
- **PostgreSQL** – relational database, running as a separate service [infra].
- **Redis** – used for caching and as a message queue through pub/sub.
- **Kafka** – serves as the message broker.
- **Testcontainers** – for isolated database testing.
- **Liquibase** – for managing database schema migrations.
- **Gradle** – application build system.

---

## Feature Overview

**News Feed** is a key feature in social networks, allowing users to stay updated with real-time posts from their subscriptions. Popular platforms like Instagram and Twitter handle millions of simultaneous requests, which can heavily burden databases. To alleviate this, **Redis** is used for caching the news feed.

**Redis** is a distributed key-value store operating in memory, providing fast access to data. Posts are stored in Redis and asynchronously added to the subscriber feeds, which are also cached in Redis. This reduces the load on the database.

To optimize caching, only **post IDs** are stored in Redis, not the full post data, which minimizes memory usage and duplication. The full post content and author details are stored in separate Redis caches.

Asynchronous post processing via **Kafka** ensures reliable delivery of messages, even in case of server failures. Kafka guarantees **at least once** delivery, so handling potential duplicate messages requires operations to be **idempotent**.

---

## Post Publication

Posts are published **asynchronously**. After a post is saved in the database, it is added to Redis with a key as the post’s `ID` and the value as the post itself. **RedisZSet**, a thread-safe collection, is used to store posts, ensuring no duplicates and maintaining idempotency.

Once the post is successfully saved, an event is published to **Kafka**.

---

## Likes, Views, and Comments

Likes, views, and comments are updated both in the database and in Redis. For each post in Redis, the following is stored:
- **Like count**
- **View count**
- **The last three comments**

These values are updated in a thread-safe manner through Kafka consumers, which handle corresponding events. To save memory, only the last three comments are cached; additional comments are fetched from the database on demand.

---

## Synchronization

Under high loads, it is crucial that updates in Redis are applied **sequentially**. Since Redis does not support ACID, **optimistic locking** can be applied to prevent conflicts during simultaneous updates of likes, views, and comments.

In this implementation:
- **Redis' built-in increment** mechanism is used for thread-safe increments of likes and views.
- **CopyOnWriteArraySet** is used for adding and removing comments, ensuring thread safety.

---

## User Caching

Users are cached in Redis with a **TTL (Time To Live)** of one day, allowing quick access to data about the author of a post or comment. Once the TTL expires, data is reloaded from the database.

---

## Feed Generation

Each new post triggers an event in **Kafka**, which updates the subscriber feeds stored in Redis. Only the **post IDs** are kept in the feed; the actual post data is stored in a separate Redis collection. The feed is built in **chronological order** and is paginated for efficient retrieval. If a post or user is not found in the cache (**cache miss**), the data is fetched from the database.

---

## Cache Warmup

To speed up system performance during startup, a **cache warmup** mechanism (FeedHeat) is implemented. It gathers posts from the database and generates Kafka events to add them to the user feeds.

Additionally, all users are asynchronously cached by fetching data from the **user-service** microservice via **FeignClient**.

---

## Summary

This implementation leverages a combination of asynchronous processing, Redis caching, and Kafka-based messaging to ensure the system can handle high loads while maintaining reliability and performance.

Key Features:
- **Asynchronous processing**: Ensures non-blocking operations and improves scalability.
- **Redis caching**: Enhances read performance by storing frequently accessed data in memory, reducing database load.
- **Kafka-based messaging**: Guarantees reliable message delivery, supporting "at least once" delivery semantics, ensuring message persistence even in case of system failures.
