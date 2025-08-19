package cqrses.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.axonframework.eventhandling.TrackingEventProcessorConfiguration;
import org.axonframework.extensions.mongo.DefaultMongoTemplate;
import org.axonframework.extensions.mongo.MongoTemplate;
import org.axonframework.extensions.mongo.eventsourcing.tokenstore.MongoTokenStore;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.serialization.Serializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AxonMongoConfig {

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://localhost:27017");
    }

    @Bean
    public MongoTemplate axonMongoTemplate(MongoClient mongoClient) {
        return DefaultMongoTemplate.builder()
                .mongoDatabase(mongoClient, "dewdrop_db2")  // use same DB as Spring Data
                .build();
    }

    @Bean
    public TokenStore tokenStore(Serializer serializer, MongoTemplate axonMongoTemplate) {
        return MongoTokenStore.builder()
                .serializer(serializer)
                .mongoTemplate(axonMongoTemplate)
                .build();
    }

}
