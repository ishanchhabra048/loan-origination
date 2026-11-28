
package com.ishanchhabra.loan_origination.config;

import com.ishanchhabra.loan_origination.utility.DbConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {

    @Bean
    public DbConnectionFactory dbConnectionFactory(
            @Value("${db.url}") String url,
            @Value("${db.user}") String user,
            @Value("${DB_PASSWORD}") String password) {

        return new DbConnectionFactory(url, user, password);
    }
}
