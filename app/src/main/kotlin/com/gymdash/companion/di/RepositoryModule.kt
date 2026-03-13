package com.gymdash.companion.di

import com.gymdash.companion.data.repository.AuthRepositoryImpl
import com.gymdash.companion.data.repository.FoodDiaryRepositoryImpl
import com.gymdash.companion.data.repository.HealthRepositoryImpl
import com.gymdash.companion.data.repository.ThemeRepositoryImpl
import com.gymdash.companion.domain.repository.AuthRepository
import com.gymdash.companion.domain.repository.FoodDiaryRepository
import com.gymdash.companion.domain.repository.HealthRepository
import com.gymdash.companion.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindHealthRepository(impl: HealthRepositoryImpl): HealthRepository

    @Binds
    @Singleton
    abstract fun bindFoodDiaryRepository(impl: FoodDiaryRepositoryImpl): FoodDiaryRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository
}
