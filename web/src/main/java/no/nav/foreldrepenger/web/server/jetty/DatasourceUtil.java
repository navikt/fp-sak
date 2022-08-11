package no.nav.foreldrepenger.web.server.jetty;

import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.micrometer.core.instrument.Metrics;
import no.nav.foreldrepenger.konfig.Environment;

public class DatasourceUtil {
    private static final Environment ENV = Environment.current();

    public static HikariDataSource createDatasource(String schemaName, int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(ENV.getRequiredProperty(schemaName + ".url"));
        config.setUsername(ENV.getRequiredProperty(schemaName + ".username"));
        config.setPassword(ENV.getRequiredProperty(schemaName + ".password"));
        config.setConnectionTimeout(1000);
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTestQuery("select 1 from dual");
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setMetricRegistry(Metrics.globalRegistry);

        Properties dsProperties = new Properties();
        config.setDataSourceProperties(dsProperties);

        return new HikariDataSource(config);
    }
}
