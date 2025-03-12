package com.aionemu.commons.database;

import com.aionemu.commons.configs.DatabaseConfig;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据库连接工厂类
 * Database Connection Factory Class
 *
 * 该类负责管理数据库连接池，提供数据库连接的获取和释放功能。
 * This class manages the database connection pool and provides functionality for obtaining and releasing database connections.
 *
 * 使用BoneCP作为连接池实现，支持连接池的配置和管理。
 * Uses BoneCP as the connection pool implementation, supporting connection pool configuration and management.
 */
public class DatabaseFactory {
    private static final Logger log = LoggerFactory.getLogger(DatabaseFactory.class);
    private static BoneCP connectionPool;
    private static String databaseName;
    private static int databaseMajorVersion;
    private static int databaseMinorVersion;

    /**
     * 初始化数据库连接池
     * Initialize the database connection pool
     */
    public static synchronized void init() {
        if (connectionPool == null) {
            try {
                DatabaseConfig.DATABASE_DRIVER.newInstance();
            } catch (Exception var5) {
                log.error("Error obtaining DB driver", var5);
                throw new Error("DB Driver doesnt exist!");
            }

            if (DatabaseConfig.DATABASE_BONECP_PARTITION_CONNECTIONS_MIN > DatabaseConfig.DATABASE_BONECP_PARTITION_CONNECTIONS_MAX) {
                log.error("Please check your database configuration. Minimum amount of connections is > maximum");
                DatabaseConfig.DATABASE_BONECP_PARTITION_CONNECTIONS_MAX = DatabaseConfig.DATABASE_BONECP_PARTITION_CONNECTIONS_MIN;
            }

            BoneCPConfig config = new BoneCPConfig();
            config.setPartitionCount(DatabaseConfig.DATABASE_BONECP_PARTITION_COUNT);
            config.setMinConnectionsPerPartition(DatabaseConfig.DATABASE_BONECP_PARTITION_CONNECTIONS_MIN);
            config.setMaxConnectionsPerPartition(DatabaseConfig.DATABASE_BONECP_PARTITION_CONNECTIONS_MAX);
            config.setUsername(DatabaseConfig.DATABASE_USER);
            config.setPassword(DatabaseConfig.DATABASE_PASSWORD);
            config.setJdbcUrl(DatabaseConfig.DATABASE_URL);
            config.setDisableJMX(true);

            try {
                connectionPool = new BoneCP(config);
            } catch (SQLException var4) {
                log.error("Error while creating DB Connection pool", var4);
                throw new Error("DatabaseFactory not initialized!", var4);
            }

            try {
                Connection c = getConnection();
                DatabaseMetaData dmd = c.getMetaData();
                databaseName = dmd.getDatabaseProductName();
                databaseMajorVersion = dmd.getDatabaseMajorVersion();
                databaseMinorVersion = dmd.getDatabaseMinorVersion();
                c.close();
            } catch (Exception var3) {
                log.error("Error with connection string: " + DatabaseConfig.DATABASE_URL, var3);
                throw new Error("DatabaseFactory not initialized!");
            }

            log.info("Successfully connected to database");
        }
    }

    /**
     * 获取数据库连接
     * Get a database connection
     *
     * @return 数据库连接 / Database connection
     * @throws SQLException 如果获取连接时发生错误 / If an error occurs while getting the connection
     */
    public static Connection getConnection() throws SQLException {
        Connection con = connectionPool.getConnection();
        if (!con.getAutoCommit()) {
            log.error("Connection Settings Error: Connection obtained from database factory should be in auto-commit mode. Forcing auto-commit to true. Please check source code for connections being not properly closed.");
            con.setAutoCommit(true);
        }
        return con;
    }

    /**
     * 获取活动连接数
     * Get the number of active connections
     *
     * @return 活动连接数 / Number of active connections
     */
    public int getActiveConnections() {
        return connectionPool.getTotalLeased();
    }

    /**
     * 获取空闲连接数
     * Get the number of idle connections
     *
     * @return 空闲连接数 / Number of idle connections
     */
    public int getIdleConnections() {
        return connectionPool.getStatistics().getTotalFree();
    }

    /**
     * 关闭数据库连接池
     * Shutdown the database connection pool
     */
    public static synchronized void shutdown() {
        try {
            connectionPool.shutdown();
        } catch (Exception var1) {
            log.warn("Failed to shutdown DatabaseFactory", var1);
        }
        connectionPool = null;
    }

    /**
     * 关闭PreparedStatement和Connection
     * Close PreparedStatement and Connection
     *
     * @param st PreparedStatement对象 / PreparedStatement object
     * @param con 数据库连接 / Database connection
     */
    public static void close(PreparedStatement st, Connection con) {
        close(st);
        close(con);
    }

    /**
     * 关闭PreparedStatement
     * Close PreparedStatement
     *
     * @param st PreparedStatement对象 / PreparedStatement object
     */
    public static void close(PreparedStatement st) {
        if (st != null) {
            try {
                if (!st.isClosed()) {
                    st.close();
                }
            } catch (SQLException var2) {
                log.error("Can't close Prepared Statement", var2);
            }
        }
    }

    /**
     * 关闭数据库连接
     * Close database connection
     *
     * @param con 数据库连接 / Database connection
     */
    public static void close(Connection con) {
        if (con != null) {
            try {
                if (!con.getAutoCommit()) {
                    con.setAutoCommit(true);
                }
            } catch (SQLException var3) {
                log.error("Failed to set autocommit to true while closing connection: ", var3);
            }

            try {
                con.close();
            } catch (SQLException var2) {
                log.error("DatabaseFactory: Failed to close database connection!", var2);
            }
        }
    }

    /**
     * 获取数据库名称
     * Get database name
     *
     * @return 数据库名称 / Database name
     */
    public static String getDatabaseName() {
        return databaseName;
    }

    /**
     * 获取数据库主版本号
     * Get database major version
     *
     * @return 数据库主版本号 / Database major version
     */
    public static int getDatabaseMajorVersion() {
        return databaseMajorVersion;
    }

    /**
     * 获取数据库次版本号
     * Get database minor version
     *
     * @return 数据库次版本号 / Database minor version
     */
    public static int getDatabaseMinorVersion() {
        return databaseMinorVersion;
    }

    private DatabaseFactory() {
    }
}
