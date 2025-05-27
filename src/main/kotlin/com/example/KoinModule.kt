package com.example

import com.example.repository.SubscriptionRepository
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
    single<ISubscriptionRepository> { SubscriptionRepository(get()) }
}

/**
 * All application modules
 */
val appModules = listOf(repositoryModule, serviceModule)
