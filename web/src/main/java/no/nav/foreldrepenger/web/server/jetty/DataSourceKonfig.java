package no.nav.foreldrepenger.web.server.jetty;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import no.nav.vedtak.util.env.Environment;

class DataSourceKonfig {

    private static final Environment ENV = Environment.current();

    private static final String MIGRATIONS_LOCATION = "classpath:/db/migration/";
    private final DBConnProp defaultDS;
    private final List<DBConnProp> dataSources;

    DataSourceKonfig() {
        defaultDS = new DBConnProp(createDatasource("defaultDS"), MIGRATIONS_LOCATION + "defaultDS");
        dataSources = List.of(defaultDS, new DBConnProp(createDatasource("dvhDS"), MIGRATIONS_LOCATION + "dvhDS"));
    }

    private static DataSource createDatasource(String dataSourceName) {
        HikariConfig config = new HikariConfig();
        var schema = ENV.getRequiredProperty(dataSourceName + ".schema");
        config.setJdbcUrl(ENV.getProperty(dataSourceName + ".url"));
        config.setUsername(ENV.getProperty(dataSourceName + ".username", schema));
        config.setPassword(ENV.getProperty(dataSourceName + ".password", schema));

        config.setConnectionTimeout(1000);
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(30);
        config.setConnectionTestQuery("select 1 from dual");
        config.setDriverClassName("oracle.jdbc.OracleDriver");

        Properties dsProperties = new Properties();
        config.setDataSourceProperties(dsProperties);

        return new HikariDataSource(config);
    }

    DBConnProp getDefaultDatasource() {
        return defaultDS;
    }

    List<DBConnProp> getDataSources() {
        return dataSources;
    }

    static final class DBConnProp {
        private final DataSource datasource;
        private final String migrationScripts;

        public DBConnProp(DataSource datasource, String migrationScripts) {
            this.datasource = datasource;
            this.migrationScripts = migrationScripts;
        }

        public DataSource getDatasource() {
            return datasource;
        }

        public String getMigrationScripts() {
            return migrationScripts;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [datasource=" + datasource + ", migrationScripts=" + migrationScripts + "]";
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [defaultDS=" + defaultDS + ", dataSources=" + dataSources + "]";
    }
}
