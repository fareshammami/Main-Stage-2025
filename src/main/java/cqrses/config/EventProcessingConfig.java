package cqrses.config;

import org.axonframework.config.EventProcessingConfigurer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventProcessingConfig {
    public EventProcessingConfig(EventProcessingConfigurer configurer) {
        // Just for this group, use Subscribing (live dispatch, no replay)
        configurer.registerSubscribingEventProcessor("userInduState");
    }
}
