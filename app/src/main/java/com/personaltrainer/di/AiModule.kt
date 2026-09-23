package com.personaltrainer.di

import com.personaltrainer.ai.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideTemplateProvider(): TemplateProvider = TemplateProvider()

    /**
     * ProviderRouter with just the template fallback by default.
     * The GeminiProvider is added when the user enters an API key in Settings.
     * See SettingsRepository for how the key is stored and retrieved.
     */
    @Provides
    @Singleton
    fun provideProviderRouter(templateProvider: TemplateProvider): ProviderRouter {
        // Start with template only — Gemini added dynamically when key is set
        return ProviderRouter(listOf(templateProvider))
    }
}
