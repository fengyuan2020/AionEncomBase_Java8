package com.aionemu.commons.configs;

import com.aionemu.commons.configuration.Property;
import java.io.File;

/**
 * Database Configuration Class
 * 数据库配置类
 * <p>
 * This class manages database connection settings and connection pool configurations.
 * 该类管理数据库连接设置和连接池配置。
 * </p>
 */
public class DatabaseConfig {
    /**
     * Database connection URL
     * 数据库连接URL
     * <p>
     * The JDBC URL for connecting to the MySQL database.
     * 用于连接MySQL数据库的JDBC URL。
     * </p>
     */
    @Property(
        key = "database.url",
        defaultValue = "jdbc:mysql://localhost:3306/aion_uni"
    )
    public static String DATABASE_URL;

    /**
     * Database driver class
     * 数据库驱动类
     * <p>
     * The JDBC driver class for MySQL database connection.
     * MySQL数据库连接的JDBC驱动类。
     * </p>
     */
    @Property(
        key = "database.driver",
        defaultValue = "com.mysql.jdbc.Driver"
    )
    public static Class<?> DATABASE_DRIVER;

    /**
     * Database username
     * 数据库用户名
     * <p>
     * The username for database authentication.
     * 用于数据库认证的用户名。
     * </p>
     */
    @Property(
        key = "database.user",
        defaultValue = "root"
    )
    public static String DATABASE_USER;

    /**
     * Database password
     * 数据库密码
     * <p>
     * The password for database authentication.
     * 用于数据库认证的密码。
     * </p>
     */
    @Property(
        key = "database.password",
        defaultValue = "root"
    )
    public static String DATABASE_PASSWORD;

    /**
     * BoneCP partition count
     * BoneCP分区数量
     * <p>
     * The number of partitions in the connection pool.
     * 连接池中的分区数量。
     * </p>
     */
    @Property(
        key = "database.bonecp.partition.count",
        defaultValue = "2"
    )
    public static int DATABASE_BONECP_PARTITION_COUNT;

    /**
     * Minimum connections per partition
     * 每个分区的最小连接数
     * <p>
     * The minimum number of connections that will be contained in each partition.
     * 每个分区中将包含的最小连接数。
     * </p>
     */
    @Property(
        key = "database.bonecp.partition.connections.min",
        defaultValue = "2"
    )
    public static int DATABASE_BONECP_PARTITION_CONNECTIONS_MIN;

    /**
     * Maximum connections per partition
     * 每个分区的最大连接数
     * <p>
     * The maximum number of connections that will be contained in each partition.
     * 每个分区中将包含的最大连接数。
     * </p>
     */
    @Property(
        key = "database.bonecp.partition.connections.max",
        defaultValue = "5"
    )
    public static int DATABASE_BONECP_PARTITION_CONNECTIONS_MAX;

    /**
     * Database script context descriptor
     * 数据库脚本上下文描述符
     * <p>
     * The file path to the database script context descriptor XML file.
     * 数据库脚本上下文描述符XML文件的路径。
     * </p>
     */
    @Property(
        key = "database.scriptcontext.descriptor",
        defaultValue = "./data/scripts/system/database/database.xml"
    )
    public static File DATABASE_SCRIPTCONTEXT_DESCRIPTOR;
}
