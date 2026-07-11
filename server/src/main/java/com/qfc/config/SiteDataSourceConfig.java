package com.qfc.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@MapperScan(
    basePackages = {"com.qfc.user", "com.qfc.project", "com.qfc.file", "com.qfc.issue", "com.qfc.feedback"},
    sqlSessionTemplateRef = "siteSqlSessionTemplate"
)
public class SiteDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("qfc.datasource.site")
    public DataSourceProperties siteDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource siteDataSource(@Qualifier("siteDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public SqlSessionFactory siteSqlSessionFactory(@Qualifier("siteDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(mybatisConfiguration());
        return factory.getObject();
    }

    @Bean
    @Primary
    public SqlSessionTemplate siteSqlSessionTemplate(
        @Qualifier("siteSqlSessionFactory") SqlSessionFactory sqlSessionFactory
    ) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    @Primary
    public PlatformTransactionManager siteTransactionManager(@Qualifier("siteDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    private MybatisConfiguration mybatisConfiguration() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        return configuration;
    }
}
