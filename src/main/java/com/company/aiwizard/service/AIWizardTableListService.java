package com.company.aiwizard.service;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for retrieving database table names directly from JDBC metadata.
 * Provides low-level database introspection independent of JPA/Jmix entity mappings.
 *
 * Useful for discovering tables that may not be mapped to entities,
 * database administration tools, or AI-powered schema analysis.
 */
@Service("aiwizard_AIWizardTableListService")  // Bean name with module prefix
public class AIWizardTableListService {

    private final DataSource dataSource;  // JDBC connection pool

    /**
     * Constructor injection for the configured DataSource.
     */
    public AIWizardTableListService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns all table names from the database, sorted alphabetically (case-insensitive).
     * Queries across all schemas/catalogs.
     *
     * @return sorted list of all table names
     * @throws RuntimeException if database access fails
     */
    public List<String> getAllTableNames() {
        List<String> tableNames = new ArrayList<>();

        // Use try-with-resources to ensure connection is properly closed
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Query for TABLE type only (excludes views, system tables, etc.)
            // Parameters: catalog, schemaPattern, tableNamePattern, types
            try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    tableNames.add(tables.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve table list", e);
        }

        // Sort alphabetically, ignoring case
        tableNames.sort(String::compareToIgnoreCase);
        return tableNames;
    }

    /**
     * Returns table names for a specific database schema, sorted alphabetically.
     * Useful when working with multi-schema databases (e.g., PostgreSQL).
     *
     * @param schema database schema name (e.g., "public", "app_data")
     * @return sorted list of table names in the specified schema
     * @throws RuntimeException if database access fails
     */
    public List<String> getTableNamesBySchema(String schema) {
        List<String> tableNames = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Query tables filtered by schema
            // Parameters: catalog (null=any), schema, tableNamePattern (% = wildcard), types
            try (ResultSet tables = metaData.getTables(null, schema, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    tableNames.add(tables.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve table list for schema: " + schema, e);
        }

        tableNames.sort(String::compareToIgnoreCase);
        return tableNames;
    }
}