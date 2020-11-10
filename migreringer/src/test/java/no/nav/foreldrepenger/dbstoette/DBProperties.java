package no.nav.foreldrepenger.dbstoette;

import java.io.File;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class DBProperties {

    private final String datasource;
    private final String schema;
    private final String url;
    private final String user;
    private final String password;
    private final boolean isDefaultDS;
    private final DataSource ds;

    private DBProperties(Builder builder) {
        this.datasource = builder.datasource;
        this.schema = builder.schema;
        this.url = builder.url;
        this.user = builder.user;
        this.password = builder.password;
        this.isDefaultDS = builder.isDefaultDS;
        this.ds = ds();
    }

    public DataSource getDs() {
        return ds;
    }

    String getScriptLocation() {
        if (DBTestUtil.kjøresAvMaven()) {
            return classpathScriptLocation();
        }
        return fileScriptLocation();
    }

    private String classpathScriptLocation() {
        return "classpath:/db/migration/" + getDatasource();
    }

    private String fileScriptLocation() {
        String relativePath = "migreringer/src/main/resources/db/migration/" + datasource;
        File baseDir = new File(".").getAbsoluteFile();
        File location = new File(baseDir, relativePath);
        while (!location.exists()) {
            baseDir = baseDir.getParentFile();
            if (baseDir == null || !baseDir.isDirectory()) {
                throw new IllegalArgumentException("Klarte ikke finne : " + baseDir);
            }
            location = new File(baseDir, relativePath);
        }
        return "filesystem:" + location.getPath();
    }

    String getDatasource() {
        return datasource;
    }

    public String getSchema() {
        return schema;
    }

    String getUrl() {
        return url;
    }

    String getUser() {
        return user;
    }

    String getPassword() {
        return password;
    }

    boolean isDefaultDS() {
        return isDefaultDS;
    }

    private DataSource ds() {
        var ds = new HikariDataSource(hikariConfig());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ds.close()));
        return ds;
    }

    private HikariConfig hikariConfig() {
        var cfg = new HikariConfig();
        cfg.setJdbcUrl(this.getUrl());
        cfg.setUsername(this.getUser());
        cfg.setPassword(this.getPassword());
        cfg.setConnectionTimeout(1000);
        cfg.setMinimumIdle(0);
        cfg.setMaximumPoolSize(4);
        cfg.setAutoCommit(false);
        return cfg;
    }

    static class Builder {
        private String datasource;
        private String schema;
        private String url;
        private String user;
        private String password;
        private boolean isDefaultDS;

        Builder datasource(String datasource) {
            this.datasource = datasource;
            return this;
        }

        Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        Builder url(String url) {
            this.url = url;
            return this;
        }

        Builder user(String user) {
            this.user = user;
            return this;
        }

        Builder password(String password) {
            this.password = password;
            return this;
        }

        Builder defaultDataSource(boolean defaultDataSource) {
            this.isDefaultDS = defaultDataSource;
            return this;
        }

        DBProperties build() {
            return new DBProperties(this);
        }
    }
}
