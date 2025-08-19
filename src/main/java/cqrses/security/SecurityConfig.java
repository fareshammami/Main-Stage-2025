package cqrses.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.client.provider.keycloak.jwk-set-uri}")
    private String jwkSetUri;
    @Bean
    public CustomKeycloakConverter customKeycloakConverter() {
        return new CustomKeycloakConverter();
    }
    /**
     * This Bean is responsible for decoding authorization server(Keycloak in our case) token
     *
     * @return JwtDecoder
     */
    @Bean
    public JwtDecoder decode() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
    /**
     * This Bean is responsible for JwtAuthenticationProvider registration
     *
     * @return JwtAuthenticationProvider
     */

    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider() {
        return new JwtAuthenticationProvider(decode());
    }
    /**
     * This is the security Filter chain main Bean responsible for filtering Http requests against allowed patterns
     *
     * @param httpSecurity Filter chain
     * @return SecurityFilterChain bean
     * @throws Exception for CSRF handling
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, HandlerMappingIntrospector introspector) throws Exception {

        MvcRequestMatcher.Builder mvcMatcherBuilder =
                new MvcRequestMatcher.Builder(introspector);
        httpSecurity
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/swagger-ui.html")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/swagger/**")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/swagger-ui/**")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/v3/api-docs/**")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/health/**")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/info")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.GET, "/prometheus")).permitAll()
                                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.OPTIONS, "/**")).permitAll()
                                .anyRequest().authenticated()
                ).oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(customKeycloakConverter()).decoder(decode())))
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        ;
        return httpSecurity.build();


    }

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new NullAuthenticatedSessionStrategy();
    }
}