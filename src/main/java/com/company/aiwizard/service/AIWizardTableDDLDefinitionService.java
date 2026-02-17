package com.company.aiwizard.service;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Service for extracting complete DDL (Data Definition Language) definitions from database tables.
 * Uses JDBC DatabaseMetaData to introspect table structure including columns, constraints, and indexes.
 *
 * Provides output in three formats:
 * - Structured record objects (TableDDLDefinition)
 * - SQL CREATE TABLE statement
 * - Human-readable metadata summary
 *
 * Useful for schema documentation, migration tools, AI-powered database analysis,
 * or reverse-engineering database structures.
 */
@Service("aiwizard_AIWizardTableDDLDefinitionService")  // Bean name with module prefix
public class AIWizardTableDDLDefinitionService {

    private final DataSource dataSource;  // JDBC connection pool

    /**
     * Constructor injection for the configured DataSource.
     */
    public AIWizardTableDDLDefinitionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns complete DDL definition for a table as a structured object.
     * Includes columns, primary key, foreign keys, indexes, and unique constraints.
     *
     * @param tableName table name (case-insensitive search)
     * @return TableDDLDefinition containing all structural metadata
     * @throws IllegalArgumentException if table is not found
     * @throws RuntimeException if database access fails
     */
    public TableDDLDefinition getTableDDLDefinition(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();  // Database name (MySQL) or null (PostgreSQL)
            String schema = connection.getSchema();    // Schema name (e.g., "public" in PostgreSQL)

            // Find actual table name with case-insensitive search
            String actualTableName = findActualTableName(metaData, catalog, schema, tableName);
            if (actualTableName == null) {
                throw new IllegalArgumentException("Table not found: " + tableName);
            }

            // Extract all metadata components
            List<ColumnDefinition> columns = getColumns(metaData, catalog, schema, actualTableName);
            PrimaryKeyDefinition primaryKey = getPrimaryKey(metaData, catalog, schema, actualTableName);
            List<ForeignKeyDefinition> foreignKeys = getForeignKeys(metaData, catalog, schema, actualTableName);
            List<IndexDefinition> indexes = getIndexes(metaData, catalog, schema, actualTableName);
            List<UniqueConstraintDefinition> uniqueConstraints = getUniqueConstraints(metaData, catalog, schema, actualTableName);

            return new TableDDLDefinition(
                    catalog,
                    schema,
                    actualTableName,
                    columns,
                    primaryKey,
                    foreignKeys,
                    indexes,
                    uniqueConstraints
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve DDL for table: " + tableName, e);
        }
    }

    /**
     * Returns table DDL as a SQL CREATE TABLE statement.
     * Generates valid SQL syntax including all constraints and indexes.
     *
     * @param tableName table name
     * @return SQL CREATE TABLE statement with constraints and CREATE INDEX statements
     */
    public String getTableDDLAsString(String tableName) {
        TableDDLDefinition def = getTableDDLDefinition(tableName);
        StringBuilder sb = new StringBuilder();

        // === CREATE TABLE header ===
        sb.append("CREATE TABLE ");
        if (def.schema() != null && !def.schema().isEmpty()) {
            sb.append(def.schema()).append(".");
        }
        sb.append(def.tableName()).append(" (\n");

        // Collect all column and constraint definitions
        List<String> columnDefs = new ArrayList<>();

        // --- Column definitions ---
        for (ColumnDefinition col : def.columns()) {
            StringBuilder colDef = new StringBuilder();
            colDef.append("    ").append(col.columnName()).append(" ").append(col.typeName());

            // Add size/precision for types that need it (VARCHAR, NUMERIC, etc.)
            if (col.columnSize() != null && needsSize(col.typeName())) {
                if (col.decimalDigits() != null && col.decimalDigits() > 0) {
                    // NUMERIC(10, 2) format for decimals
                    colDef.append("(").append(col.columnSize()).append(", ").append(col.decimalDigits()).append(")");
                } else {
                    // VARCHAR(255) format
                    colDef.append("(").append(col.columnSize()).append(")");
                }
            }

            // NOT NULL constraint
            if (!col.nullable()) {
                colDef.append(" NOT NULL");
            }

            // Default value
            if (col.defaultValue() != null && !col.defaultValue().isEmpty()) {
                colDef.append(" DEFAULT ").append(col.defaultValue());
            }

            // Auto-increment (PostgreSQL IDENTITY syntax)
            if (col.autoIncrement()) {
                colDef.append(" GENERATED ALWAYS AS IDENTITY");
            }

            columnDefs.add(colDef.toString());
        }

        // --- Primary key constraint ---
        if (def.primaryKey() != null && !def.primaryKey().columns().isEmpty()) {
            StringBuilder pkDef = new StringBuilder();
            pkDef.append("    CONSTRAINT ").append(def.primaryKey().constraintName());
            pkDef.append(" PRIMARY KEY (");
            pkDef.append(String.join(", ", def.primaryKey().columns()));
            pkDef.append(")");
            columnDefs.add(pkDef.toString());
        }

        // --- Unique constraints ---
        for (UniqueConstraintDefinition uc : def.uniqueConstraints()) {
            StringBuilder ucDef = new StringBuilder();
            ucDef.append("    CONSTRAINT ").append(uc.constraintName());
            ucDef.append(" UNIQUE (");
            ucDef.append(String.join(", ", uc.columns()));
            ucDef.append(")");
            columnDefs.add(ucDef.toString());
        }

        // --- Foreign key constraints ---
        for (ForeignKeyDefinition fk : def.foreignKeys()) {
            StringBuilder fkDef = new StringBuilder();
            fkDef.append("    CONSTRAINT ").append(fk.constraintName());
            fkDef.append(" FOREIGN KEY (").append(fk.fkColumnName()).append(")");
            fkDef.append(" REFERENCES ");

            // Include schema if present
            if (fk.pkTableSchema() != null && !fk.pkTableSchema().isEmpty()) {
                fkDef.append(fk.pkTableSchema()).append(".");
            }
            fkDef.append(fk.pkTableName()).append("(").append(fk.pkColumnName()).append(")");

            // Referential actions (only if not default NO ACTION)
            if (fk.deleteRule() != null && !fk.deleteRule().equals("NO ACTION")) {
                fkDef.append(" ON DELETE ").append(fk.deleteRule());
            }
            if (fk.updateRule() != null && !fk.updateRule().equals("NO ACTION")) {
                fkDef.append(" ON UPDATE ").append(fk.updateRule());
            }

            columnDefs.add(fkDef.toString());
        }

        // Join all definitions with commas
        sb.append(String.join(",\n", columnDefs));
        sb.append("\n);\n");

        // === CREATE INDEX statements (separate from table) ===
        for (IndexDefinition idx : def.indexes()) {
            // Skip unique indexes (already handled as constraints) and primary key indexes
            if (!idx.unique() && !idx.indexName().toLowerCase().contains("pkey")) {
                sb.append("\nCREATE INDEX ").append(idx.indexName());
                sb.append(" ON ");
                if (def.schema() != null && !def.schema().isEmpty()) {
                    sb.append(def.schema()).append(".");
                }
                sb.append(def.tableName());
                sb.append(" (").append(String.join(", ", idx.columns())).append(");");
            }
        }

        return sb.toString();
    }

    /**
     * Returns table metadata as a human-readable formatted string.
     * Useful for debugging, documentation, or display in UI/console.
     *
     * @param tableName table name
     * @return formatted string with all table metadata
     */
    public String getTableMetadataAsString(String tableName) {
        TableDDLDefinition def = getTableDDLDefinition(tableName);
        StringBuilder sb = new StringBuilder();

        // === Header section ===
        sb.append("=== Table Metadata ===\n");
        sb.append("Catalog: ").append(def.catalog()).append("\n");
        sb.append("Schema: ").append(def.schema()).append("\n");
        sb.append("Table: ").append(def.tableName()).append("\n");

        // === Columns section ===
        sb.append("\n--- Columns (").append(def.columns().size()).append(") ---\n");
        for (ColumnDefinition col : def.columns()) {
            sb.append("\n").append(col.columnName()).append(":\n");

            // Type with size/precision
            sb.append("  Type: ").append(col.typeName());
            if (col.columnSize() != null) {
                sb.append("(").append(col.columnSize());
                if (col.decimalDigits() != null && col.decimalDigits() > 0) {
                    sb.append(", ").append(col.decimalDigits());
                }
                sb.append(")");
            }
            sb.append("\n");

            sb.append("  Nullable: ").append(col.nullable()).append("\n");

            if (col.defaultValue() != null && !col.defaultValue().isEmpty()) {
                sb.append("  Default: ").append(col.defaultValue()).append("\n");
            }
            if (col.autoIncrement()) {
                sb.append("  Auto Increment: true\n");
            }
            sb.append("  Ordinal Position: ").append(col.ordinalPosition()).append("\n");
            if (col.remarks() != null && !col.remarks().isEmpty()) {
                sb.append("  Remarks: ").append(col.remarks()).append("\n");
            }
        }

        // === Primary key section ===
        if (def.primaryKey() != null) {
            sb.append("\n--- Primary Key ---\n");
            sb.append("Constraint: ").append(def.primaryKey().constraintName()).append("\n");
            sb.append("Columns: ").append(String.join(", ", def.primaryKey().columns())).append("\n");
        }

        // === Foreign keys section ===
        if (!def.foreignKeys().isEmpty()) {
            sb.append("\n--- Foreign Keys (").append(def.foreignKeys().size()).append(") ---\n");
            for (ForeignKeyDefinition fk : def.foreignKeys()) {
                sb.append("\n").append(fk.constraintName()).append(":\n");
                sb.append("  Column: ").append(fk.fkColumnName()).append("\n");
                sb.append("  References: ").append(fk.pkTableName()).append(".").append(fk.pkColumnName()).append("\n");
                sb.append("  On Delete: ").append(fk.deleteRule()).append("\n");
                sb.append("  On Update: ").append(fk.updateRule()).append("\n");
            }
        }

        // === Unique constraints section ===
        if (!def.uniqueConstraints().isEmpty()) {
            sb.append("\n--- Unique Constraints (").append(def.uniqueConstraints().size()).append(") ---\n");
            for (UniqueConstraintDefinition uc : def.uniqueConstraints()) {
                sb.append(uc.constraintName()).append(": (");
                sb.append(String.join(", ", uc.columns())).append(")\n");
            }
        }

        // === Indexes section ===
        if (!def.indexes().isEmpty()) {
            sb.append("\n--- Indexes (").append(def.indexes().size()).append(") ---\n");
            for (IndexDefinition idx : def.indexes()) {
                sb.append(idx.indexName()).append(":\n");
                sb.append("  Columns: ").append(String.join(", ", idx.columns())).append("\n");
                sb.append("  Unique: ").append(idx.unique()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Finds actual table name with case-insensitive search.
     * Tries exact match, then uppercase, then lowercase.
     * Handles databases with different case sensitivity (PostgreSQL vs MySQL vs Oracle).
     */
    private String findActualTableName(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        // Try exact match first
        try (ResultSet rs = metaData.getTables(catalog, schema, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return rs.getString("TABLE_NAME");
            }
        }

        // Try uppercase (Oracle, H2 default)
        try (ResultSet rs = metaData.getTables(catalog, schema, tableName.toUpperCase(), new String[]{"TABLE"})) {
            if (rs.next()) {
                return rs.getString("TABLE_NAME");
            }
        }

        // Try lowercase (PostgreSQL default)
        try (ResultSet rs = metaData.getTables(catalog, schema, tableName.toLowerCase(), new String[]{"TABLE"})) {
            if (rs.next()) {
                return rs.getString("TABLE_NAME");
            }
        }

        return null;  // Table not found
    }

    /**
     * Extracts column definitions from table metadata.
     * Returns columns sorted by ordinal position (as defined in table).
     */
    private List<ColumnDefinition> getColumns(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        List<ColumnDefinition> columns = new ArrayList<>();

        // Query all columns (% = wildcard for column name)
        try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (rs.next()) {
                columns.add(new ColumnDefinition(
                        rs.getString("COLUMN_NAME"),
                        rs.getString("TYPE_NAME"),                               // SQL type name (VARCHAR, INTEGER, etc.)
                        rs.getInt("DATA_TYPE"),                                  // java.sql.Types constant
                        getIntOrNull(rs, "COLUMN_SIZE"),                         // Length/precision
                        getIntOrNull(rs, "DECIMAL_DIGITS"),                      // Scale for decimals
                        rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable,
                        rs.getString("COLUMN_DEF"),                              // Default value expression
                        rs.getInt("ORDINAL_POSITION"),                           // Column order in table
                        rs.getString("REMARKS"),                                 // Column comment
                        "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"))
                ));
            }
        }

        // Sort by ordinal position to match table definition order
        columns.sort(Comparator.comparing(ColumnDefinition::ordinalPosition));
        return columns;
    }

    /**
     * Extracts primary key definition including composite keys.
     * Returns null if table has no primary key.
     */
    private PrimaryKeyDefinition getPrimaryKey(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        String pkName = null;

        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            // Use TreeMap to maintain column order for composite keys
            Map<Integer, String> orderedColumns = new TreeMap<>();
            while (rs.next()) {
                pkName = rs.getString("PK_NAME");
                int keySeq = rs.getInt("KEY_SEQ");      // Position in composite key (1-based)
                String columnName = rs.getString("COLUMN_NAME");
                orderedColumns.put(keySeq, columnName);
            }
            columns.addAll(orderedColumns.values());
        }

        if (columns.isEmpty()) {
            return null;
        }

        return new PrimaryKeyDefinition(pkName, columns);
    }

    /**
     * Extracts foreign key definitions (imported keys = FK columns in this table).
     */
    private List<ForeignKeyDefinition> getForeignKeys(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();

        // getImportedKeys returns FKs where this table is the child (referencing) table
        try (ResultSet rs = metaData.getImportedKeys(catalog, schema, tableName)) {
            while (rs.next()) {
                foreignKeys.add(new ForeignKeyDefinition(
                        rs.getString("FK_NAME"),           // Constraint name
                        rs.getString("FKCOLUMN_NAME"),     // Column in this table
                        rs.getString("PKTABLE_SCHEM"),     // Referenced table's schema
                        rs.getString("PKTABLE_NAME"),      // Referenced table
                        rs.getString("PKCOLUMN_NAME"),     // Referenced column
                        mapReferentialAction(rs.getInt("DELETE_RULE")),  // ON DELETE action
                        mapReferentialAction(rs.getInt("UPDATE_RULE"))   // ON UPDATE action
                ));
            }
        }

        return foreignKeys;
    }

    /**
     * Extracts all index definitions including composite indexes.
     * Uses LinkedHashMap to preserve index discovery order.
     */
    private List<IndexDefinition> getIndexes(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        Map<String, IndexDefinition> indexMap = new LinkedHashMap<>();

        // Parameters: unique=false (get all indexes), approximate=false (accurate stats)
        try (ResultSet rs = metaData.getIndexInfo(catalog, schema, tableName, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) {
                    continue;  // Skip table statistics rows
                }

                String columnName = rs.getString("COLUMN_NAME");
                boolean unique = !rs.getBoolean("NON_UNIQUE");  // Note: inverted logic
                String ascDesc = rs.getString("ASC_OR_DESC");   // A=ascending, D=descending, null=unsorted

                // Handle composite indexes (multiple columns per index)
                IndexDefinition existing = indexMap.get(indexName);
                if (existing != null) {
                    existing.columns().add(columnName);  // Add column to existing index
                } else {
                    List<String> columns = new ArrayList<>();
                    columns.add(columnName);
                    indexMap.put(indexName, new IndexDefinition(indexName, columns, unique, ascDesc));
                }
            }
        }

        return new ArrayList<>(indexMap.values());
    }

    /**
     * Extracts unique constraints (excluding primary key).
     * Derived from unique indexes, filtering out PK-related indexes.
     */
    private List<UniqueConstraintDefinition> getUniqueConstraints(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        Map<String, UniqueConstraintDefinition> constraintMap = new LinkedHashMap<>();

        // Query unique indexes only
        try (ResultSet rs = metaData.getIndexInfo(catalog, schema, tableName, true, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) {
                    continue;
                }

                // Skip primary key indexes (naming convention varies by database)
                if (indexName.toLowerCase().contains("pkey") || indexName.toLowerCase().contains("pk_")) {
                    continue;
                }

                String columnName = rs.getString("COLUMN_NAME");

                // Handle composite unique constraints
                UniqueConstraintDefinition existing = constraintMap.get(indexName);
                if (existing != null) {
                    existing.columns().add(columnName);
                } else {
                    List<String> columns = new ArrayList<>();
                    columns.add(columnName);
                    constraintMap.put(indexName, new UniqueConstraintDefinition(indexName, columns));
                }
            }
        }

        return new ArrayList<>(constraintMap.values());
    }

    /**
     * Maps JDBC referential action constants to SQL keywords.
     */
    private String mapReferentialAction(int action) {
        return switch (action) {
            case DatabaseMetaData.importedKeyCascade -> "CASCADE";
            case DatabaseMetaData.importedKeySetNull -> "SET NULL";
            case DatabaseMetaData.importedKeySetDefault -> "SET DEFAULT";
            case DatabaseMetaData.importedKeyRestrict -> "RESTRICT";
            default -> "NO ACTION";
        };
    }

    /**
     * Determines if a SQL type needs size specification in DDL.
     * Returns true for character types, numeric types with precision, and binary types.
     */
    private boolean needsSize(String typeName) {
        String upper = typeName.toUpperCase();
        return upper.contains("CHAR") || upper.contains("VARCHAR") ||
                upper.contains("NUMERIC") || upper.contains("DECIMAL") ||
                upper.contains("BINARY") || upper.contains("VARBINARY");
    }

    /**
     * Safely reads integer column, returning null if database value was NULL.
     */
    private Integer getIntOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    // ==================== Record Definitions ====================

    /**
     * Complete table DDL definition with all structural metadata.
     */
    public record TableDDLDefinition(
            String catalog,                              // Database/catalog name
            String schema,                               // Schema name
            String tableName,                            // Table name
            List<ColumnDefinition> columns,              // All column definitions
            PrimaryKeyDefinition primaryKey,             // Primary key (nullable)
            List<ForeignKeyDefinition> foreignKeys,      // Foreign key constraints
            List<IndexDefinition> indexes,               // All indexes
            List<UniqueConstraintDefinition> uniqueConstraints  // Unique constraints (excl. PK)
    ) {}

    /**
     * Column metadata including type, constraints, and default value.
     */
    public record ColumnDefinition(
            String columnName,      // Column name
            String typeName,        // SQL type name (VARCHAR, INTEGER, etc.)
            int dataType,           // java.sql.Types constant
            Integer columnSize,     // Length (chars) or precision (numbers)
            Integer decimalDigits,  // Scale for decimal types
            boolean nullable,       // Allows NULL?
            String defaultValue,    // Default value expression
            int ordinalPosition,    // Position in table (1-based)
            String remarks,         // Column comment
            boolean autoIncrement   // Auto-generated value?
    ) {}

    /**
     * Primary key constraint metadata.
     */
    public record PrimaryKeyDefinition(
            String constraintName,  // Constraint name
            List<String> columns    // Column(s) in key order
    ) {}

    /**
     * Foreign key constraint metadata.
     */
    public record ForeignKeyDefinition(
            String constraintName,  // FK constraint name
            String fkColumnName,    // Column in this table
            String pkTableSchema,   // Referenced table's schema
            String pkTableName,     // Referenced table name
            String pkColumnName,    // Referenced column name
            String deleteRule,      // ON DELETE action (CASCADE, SET NULL, etc.)
            String updateRule       // ON UPDATE action
    ) {}

    /**
     * Index metadata (may be unique or non-unique).
     */
    public record IndexDefinition(
            String indexName,       // Index name
            List<String> columns,   // Indexed columns in order
            boolean unique,         // Is unique index?
            String sortOrder        // A=ascending, D=descending
    ) {}

    /**
     * Unique constraint metadata (excluding primary key).
     */
    public record UniqueConstraintDefinition(
            String constraintName,  // Constraint name
            List<String> columns    // Columns in constraint
    ) {}
}