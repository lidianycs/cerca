package com.cerca;

import com.cerca.service.ConfigService;
import com.cerca.service.extraction.*;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Dependency Injection Container
 */
public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PdfExtractor.class).to(CerminePdfExtractor.class).in(Singleton.class);
        bind(TextExtractor.class).to(GrobidTextExtractor.class).in(Singleton.class);
    }

    @Provides
    @Named("USER_EMAIL")
    public String provideUserEmail(ConfigService configService) {
        return configService.getProperty("USER_EMAIL");
    }
}
