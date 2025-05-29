package com.example

import com.example.repository.MongoSubscriptionRepository
import com.example.repository.interfaces.ISubscriptionRepository
import com.example.service.CacheService
import com.example.service.NotificationConsumerService
import com.example.service.NotificationQueueService
import com.example.service.PubSubHubbubService
import com.example.service.RateLimitService
import com.example.service.interfaces.ICacheService
import com.example.service.interfaces.INotificationConsumerService
import com.example.service.interfaces.INotificationQueueService
import com.example.service.interfaces.IPubSubHubbubService
import com.example.service.interfaces.IRateLimitService
import org.koin.dsl.module

/**
 * Koin module definitions for dependency injection.
 */

/**
 * Module for services
 */
val serviceModule = module {
    // Cache service
    single<ICacheService> { CacheService() }

    // Notification services
    single<INotificationQueueService> { NotificationQueueService() }
    single<INotificationConsumerService> { NotificationConsumerService() }

    // Rate limit service
    single<IRateLimitService> { RateLimitService(get()) }

    // PubSubHubbub service
    single<IPubSubHubbubService> { PubSubHubbubService() }
}

/**
 * Module for repositories
 */
val repositoryModule = module {
    // Subscription repository
    single<ISubscriptionRepository> { 
        val mongoConnectionString = System.getenv("MONGODB_CONNECTION_STRING") ?: "mongodb+srv://nagyta01:rc1k1CiJpVnW80St@rsscluster.kqiw7.mongodb.net/?retryWrites=true&w=majority&appName=RssCluster"
        MongoSubscriptionRepository(get(), mongoConnectionString) 
    }
}

/**
 * All application modules
 */
val appModules = listOf(repositoryModule, serviceModule)
