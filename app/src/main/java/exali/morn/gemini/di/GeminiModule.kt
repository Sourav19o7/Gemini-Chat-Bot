package exali.morn.gemini.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import exali.morn.gemini.data.GeminiRepositoryImpl
import exali.morn.gemini.domain.GeminiRepository

@Module
@InstallIn(ViewModelComponent::class)
object GeminiModule {

    @Provides
    @ViewModelScoped
    fun provideGeminiRepo(): GeminiRepository {
        return GeminiRepositoryImpl()
    }
}