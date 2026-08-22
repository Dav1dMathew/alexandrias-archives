package net.thinkerthonker.alexandriasarchives.backend.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpClientConfiguration {
    @Bean
    RestClientCustomizer aiRequestTimeouts(
            @Value("${ai-service.connect-timeout}") Duration connectTimeout,
            @Value("${ai-service.read-timeout}") Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return builder -> builder.requestFactory(requestFactory);
    }
}
