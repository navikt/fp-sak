package no.nav.foreldrepenger.web.server.jetty;
import static io.micrometer.core.instrument.Metrics.globalRegistry;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import no.nav.foreldrepenger.konfig.Environment;

public class DataSourceKonfig {

    private static final Environment ENV = Environment.current();
    public static final String DEFAULT_DS_NAME = "defaultDS";

    private final DBConnProp defaultDS;
    private final List<DBConnProp> dataSources;

    DataSourceKonfig() {
        var defaultDSName = DEFAULT_DS_NAME;
        this.defaultDS = new DBConnProp(ds(defaultDSName), defaultDSName);
        var dvhDSName = "dvhDS";
        dataSources = List.of(this.defaultDS, new DBConnProp(ds(dvhDSName), dvhDSName));
    }

    private static DataSource ds(String dataSourceName) {
        var config = new HikariConfig();
        config.setJdbcUrl(ENV.getProperty(dataSourceName + ".url"));
        config.setUsername(ENV.getProperty(dataSourceName + ".username"));
        config.setPassword(ENV.getProperty(dataSourceName + ".password"));
        config.setConnectionTimeout(1000);
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(30);
        config.setConnectionTestQuery("select 1 from dual");
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setMetricRegistry(globalRegistry);

        var dsProperties = new Properties();
        config.setDataSourceProperties(dsProperties);

        return new HikariDataSource(config);
    }

    List<DBConnProp> getDataSources() {
        return dataSources;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [defaultDS=" + defaultDS + ", dataSources=" + dataSources + "]";
    }

    public DataSource defaultDS() {
        return defaultDS.getDatasource();
    }
}
