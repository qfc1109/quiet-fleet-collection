package com.qfc.config;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiDataSourceConfigurationTest {

    @Test
    void applicationDefinesFourSeparatedDatabases() throws Exception {
        StandardEnvironment environment = loadApplicationEnvironment();

        assertFalse(environment.containsProperty("spring.datasource.url"));
        assertDatabaseUrl(environment, "qfc.datasource.site.url", "qfc_site");
        assertDatabaseUrl(environment, "qfc.datasource.site-log.url", "qfc_site_log");
        assertDatabaseUrl(environment, "qfc.datasource.admin.url", "qfc_admin");
        assertDatabaseUrl(environment, "qfc.datasource.admin-log.url", "qfc_admin_log");
        assertEquals("root", environment.getProperty("qfc.datasource.site.username"));
        assertEquals("root", environment.getProperty("qfc.datasource.admin.username"));
    }

    private StandardEnvironment loadApplicationEnvironment() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);

        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (org.springframework.core.env.PropertySource<?> propertySource : loader.load(
            "application",
            new ClassPathResource("application.yml")
        )) {
            propertySources.addLast(propertySource);
        }
        propertySources.addFirst(new MapPropertySource("test-env", new java.util.HashMap<String, Object>()));
        return environment;
    }

    private void assertDatabaseUrl(StandardEnvironment environment, String propertyName, String databaseName) {
        String url = environment.getProperty(propertyName);
        assertTrue(url != null && url.contains("/" + databaseName + "?"), propertyName + " should target " + databaseName);
        assertTrue(
            Arrays.asList("useUnicode=true", "characterEncoding=utf8", "serverTimezone=Asia/Shanghai")
                .stream()
                .allMatch(url::contains),
            propertyName + " should keep the existing MySQL URL options"
        );
    }
}
