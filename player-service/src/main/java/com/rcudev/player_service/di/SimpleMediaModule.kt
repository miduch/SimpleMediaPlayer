package com.rcudev.player_service.di

import android.content.Context
import com.rcudev.player_service.service.SimpleMediaServiceHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SimpleMediaModule {
    @Provides
    @Singleton
    fun provideServiceHandler(
        @ApplicationContext appContext: Context,
    ): SimpleMediaServiceHandler {
        return SimpleMediaServiceHandler(
            appContext = appContext,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        )
    }
}
