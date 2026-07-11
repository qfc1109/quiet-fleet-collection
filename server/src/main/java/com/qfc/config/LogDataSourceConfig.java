package com.qfc.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class LogDataSourceConfig {

    @Bean
    @ConfigurationProperties("qfc.datasource.site-log")
    public DataSourceProperties siteLogDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource siteLogDataSource(@Qualifier("siteLogDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcTemplate siteLogJdbcTemplate(@Qualifier("siteLogDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager siteLogTransactionManager(@Qualifier("siteLogDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConfigurationProperties("qfc.datasource.admin-log")
    public DataSourceProperties adminLogDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource adminLogDataSource(@Qualifier("adminLogDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcTemplate adminLogJdbcTemplate(@Qualifier("adminLogDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager adminLogTransactionManager(@Qualifier("adminLogDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
