package com.minecorerp.clinicerp.di

import com.minecorerp.clinicerp.data.repository.AuthRepository
import com.minecorerp.clinicerp.data.repository.CrmRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // Note: Since AuthRepository and CrmRepository are annotated with @Singleton and have @Inject constructors,
    // they don't strictly need to be bound in an abstract module if they are being used directly as implementation.
    // However, if we had interfaces, we would bind them here.
    // We'll keep this as a placeholder or remove it if not needed.
}
