package config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import util.Constants;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author xiaorui
 */
public class DatabaseConfig {
    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(Constants.DB_URL);
            config.setUsername(Constants.DB_USER);
            config.setPassword(Constants.DB_PWD);

            config.setMaximumPoolSize(300);
            config.setDriverClassName("org.postgresql.Driver");
            // Number of statement executions before preparing
            config.addDataSourceProperty("prepareThreshold", "5");
            // The number of prepared statements that the driver will cache per connection
            config.addDataSourceProperty("preparedStatementCacheQueries", "250");
            // The size of the prepared statement cache in megabytes (MiB)
            config.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
            // Minimum number of idle connections in the pool
            config.setMinimumIdle(100);
            // Idle timeout (10 minutes)
            config.setIdleTimeout(600000);
            // Max lifetime of a connection in the pool (30 minutes)
            config.setMaxLifetime(1800000);
            // Connection timeout (30 seconds)
            config.setConnectionTimeout(30000);

            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
