package cqrses.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.serialization.Serializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Primary
    public Serializer axonSerializer(ObjectMapper objectMapper) {
        return JacksonSerializer.builder().objectMapper(objectMapper).build();
    }
}
