package com.company.aiwizard.service;

import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.metamodel.model.Range;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for extracting comprehensive metadata definitions from JPA entities.
 * Provides detailed introspection of entity structure including properties,
 * relationships, annotations, inheritance, and database mappings.
 *
 * Useful for AI-powered tools that need to understand the data model,
 * code generators, documentation tools, or admin interfaces.
 */
@Service("aiwizard_AIWizardEntityDefinitionService")  // Bean name with module prefix
public class AIWizardEntityDefinitionService {

    private final Metadata metadata;           // Jmix metadata registry
    private final MetadataTools metadataTools; // Utility class for metadata operations

    /**
     * Constructor injection for required dependencies.
     */
    public AIWizardEntityDefinitionService(Metadata metadata, MetadataTools metadataTools) {
        this.metadata = metadata;
        this.metadataTools = metadataTools;
    }

    /**
     * Returns complete entity definition with all metadata as a structured object.
     * Includes table mapping, properties, relationships, annotations, and inheritance info.
     *
     * @param entityName Jmix entity name (e.g., "User", "SampleData1")
     * @return EntityDefinition record containing all metadata
     * @throws IllegalArgumentException if entity is not found
     */
    public EntityDefinition getEntityDefinition(String entityName) {
        MetaClass metaClass = metadata.findClass(entityName);
        if (metaClass == null) {
            throw new IllegalArgumentException("Entity not found: " + entityName);
        }

        Class<?> javaClass = metaClass.getJavaClass();

        // Build comprehensive entity definition from various metadata sources
        return new EntityDefinition(
                metaClass.getName(),                          // Jmix entity name
                javaClass.getSimpleName(),                    // Short class name
                javaClass.getName(),                          // Fully qualified class name
                getTableName(javaClass),                      // Database table name
                getSchemaName(javaClass),                     // Database schema (if specified)
                metadataTools.isJpaEntity(metaClass),         // Is it a persistent JPA entity?
                isEmbeddable(javaClass),                      // Is it an embeddable component?
                metadataTools.isSoftDeletable(javaClass),     // Supports soft delete?
                hasVersionField(javaClass),                   // Has optimistic locking?
                getPrimaryKeyInfo(metaClass),                 // Primary key details
                getPropertyDefinitions(metaClass),            // All property definitions
                getClassAnnotations(javaClass),               // Class-level annotations
                getInheritanceInfo(javaClass)                 // Inheritance mapping info
        );
    }

    /**
     * Returns entity definition as a human-readable formatted string.
     * Useful for debugging, logging, or displaying in UI/console.
     *
     * @param entityName Jmix entity name
     * @return formatted string representation of entity metadata
     */
    public String getEntityDefinitionAsString(String entityName) {
        EntityDefinition def = getEntityDefinition(entityName);
        StringBuilder sb = new StringBuilder();

        // === Header section ===
        sb.append("=== Entity Definition ===\n");
        sb.append("Name: ").append(def.name()).append("\n");
        sb.append("Class: ").append(def.fullClassName()).append("\n");
        sb.append("Table: ").append(def.schemaName() != null ? def.schemaName() + "." : "").append(def.tableName()).append("\n");
        sb.append("JPA Entity: ").append(def.jpaEntity()).append("\n");
        sb.append("Embeddable: ").append(def.embeddable()).append("\n");
        sb.append("Soft Deletable: ").append(def.softDeletable()).append("\n");
        sb.append("Versioned: ").append(def.versioned()).append("\n");

        // === Inheritance section (if applicable) ===
        if (def.inheritanceInfo() != null) {
            sb.append("\n--- Inheritance ---\n");
            sb.append("Strategy: ").append(def.inheritanceInfo().strategy()).append("\n");
            if (def.inheritanceInfo().discriminatorColumn() != null) {
                sb.append("Discriminator Column: ").append(def.inheritanceInfo().discriminatorColumn()).append("\n");
            }
            if (def.inheritanceInfo().discriminatorValue() != null) {
                sb.append("Discriminator Value: ").append(def.inheritanceInfo().discriminatorValue()).append("\n");
            }
        }

        // === Primary key section ===
        if (def.primaryKey() != null) {
            sb.append("\n--- Primary Key ---\n");
            sb.append("Property: ").append(def.primaryKey().propertyName()).append("\n");
            sb.append("Type: ").append(def.primaryKey().javaType()).append("\n");
            sb.append("Column: ").append(def.primaryKey().columnName()).append("\n");
            sb.append("Generated: ").append(def.primaryKey().generated()).append("\n");
        }

        // === Properties section ===
        sb.append("\n--- Properties (").append(def.properties().size()).append(") ---\n");
        for (PropertyDefinition prop : def.properties()) {
            sb.append("\n").append(prop.name()).append(":\n");
            sb.append("  Type: ").append(prop.javaType()).append("\n");
            sb.append("  Property Type: ").append(prop.propertyType()).append("\n");

            // Column mapping info
            if (prop.columnName() != null) {
                sb.append("  Column: ").append(prop.columnName()).append("\n");
            }
            if (prop.columnDefinition() != null) {
                sb.append("  Column Definition: ").append(prop.columnDefinition()).append("\n");
            }

            // Constraints
            sb.append("  Mandatory: ").append(prop.mandatory()).append("\n");
            sb.append("  Read Only: ").append(prop.readOnly()).append("\n");
            if (prop.length() != null && prop.length() != 255) {  // Only show non-default length
                sb.append("  Length: ").append(prop.length()).append("\n");
            }

            // Relationship info (for associations)
            if (prop.relationType() != null) {
                sb.append("  Relation: ").append(prop.relationType()).append("\n");
                sb.append("  Related Entity: ").append(prop.relatedEntityName()).append("\n");
                if (prop.mappedBy() != null) {
                    sb.append("  Mapped By: ").append(prop.mappedBy()).append("\n");
                }
                if (prop.fetchType() != null) {
                    sb.append("  Fetch: ").append(prop.fetchType()).append("\n");
                }
                if (prop.cascadeTypes() != null && !prop.cascadeTypes().isEmpty()) {
                    sb.append("  Cascade: ").append(prop.cascadeTypes()).append("\n");
                }
            }

            // Enum info
            if (prop.enumClass() != null) {
                sb.append("  Enum Class: ").append(prop.enumClass()).append("\n");
                sb.append("  Enum Type: ").append(prop.enumType()).append("\n");
            }

            // Additional annotations (excluding common JPA ones)
            if (!prop.annotations().isEmpty()) {
                sb.append("  Annotations: ").append(prop.annotations()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Checks if the class is an embeddable component (not a standalone entity).
     */
    private boolean isEmbeddable(Class<?> javaClass) {
        return javaClass.isAnnotationPresent(Embeddable.class);
    }

    /**
     * Extracts database table name from @Table or @Entity annotations.
     * Falls back to uppercase class name if not specified.
     */
    private String getTableName(Class<?> javaClass) {
        Table table = javaClass.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        Entity entity = javaClass.getAnnotation(Entity.class);
        if (entity != null && !entity.name().isEmpty()) {
            return entity.name();
        }
        return javaClass.getSimpleName().toUpperCase();  // Default JPA naming
    }

    /**
     * Extracts database schema name from @Table annotation.
     * Returns null if not specified.
     */
    private String getSchemaName(Class<?> javaClass) {
        Table table = javaClass.getAnnotation(Table.class);
        if (table != null && !table.schema().isEmpty()) {
            return table.schema();
        }
        return null;
    }

    /**
     * Checks if entity has a @Version field for optimistic locking.
     * Searches through class hierarchy.
     */
    private boolean hasVersionField(Class<?> javaClass) {
        for (Field field : getAllFields(javaClass)) {
            if (field.isAnnotationPresent(Version.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts primary key information including column name and generation strategy.
     */
    private PrimaryKeyInfo getPrimaryKeyInfo(MetaClass metaClass) {
        MetaProperty pkProperty = metadataTools.getPrimaryKeyProperty(metaClass);
        if (pkProperty == null) {
            return null;
        }

        Field field = findField(metaClass.getJavaClass(), pkProperty.getName());
        String columnName = pkProperty.getName();
        boolean generated = false;

        if (field != null) {
            // Check for custom column name
            Column column = field.getAnnotation(Column.class);
            if (column != null && !column.name().isEmpty()) {
                columnName = column.name();
            }
            // Check if value is auto-generated (e.g., sequence, identity)
            generated = field.isAnnotationPresent(GeneratedValue.class);
        }

        return new PrimaryKeyInfo(
                pkProperty.getName(),
                pkProperty.getJavaType().getSimpleName(),
                columnName,
                generated
        );
    }

    /**
     * Builds property definitions for all properties in the entity.
     * Returns sorted list by property name.
     */
    private List<PropertyDefinition> getPropertyDefinitions(MetaClass metaClass) {
        List<PropertyDefinition> properties = new ArrayList<>();

        for (MetaProperty metaProperty : metaClass.getProperties()) {
            properties.add(createPropertyDefinition(metaClass, metaProperty));
        }

        properties.sort(Comparator.comparing(PropertyDefinition::name));
        return properties;
    }

    /**
     * Creates a detailed property definition by combining Jmix metadata
     * with JPA annotation information via reflection.
     */
    private PropertyDefinition createPropertyDefinition(MetaClass metaClass, MetaProperty metaProperty) {
        Field field = findField(metaClass.getJavaClass(), metaProperty.getName());

        // Initialize all optional fields
        String columnName = null;
        String columnDefinition = null;
        Integer length = null;
        Integer precision = null;
        Integer scale = null;
        String relationType = null;
        String relatedEntityName = null;
        String mappedBy = null;
        String fetchType = null;
        List<String> cascadeTypes = null;
        String enumClass = null;
        String enumType = null;
        List<String> annotations = new ArrayList<>();

        if (field != null) {
            // --- Extract @Column annotation details ---
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                columnName = column.name().isEmpty() ? metaProperty.getName() : column.name();
                if (!column.columnDefinition().isEmpty()) {
                    columnDefinition = column.columnDefinition();
                }
                length = column.length();
                precision = column.precision();
                scale = column.scale();
            }

            // --- Extract @JoinColumn for foreign key mappings ---
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn != null) {
                columnName = joinColumn.name().isEmpty() ? metaProperty.getName() + "_id" : joinColumn.name();
            }

            // --- Extract relationship annotations ---

            // Many-to-One: Most common relationship (e.g., Order -> Customer)
            ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
            if (manyToOne != null) {
                relationType = "ManyToOne";
                fetchType = manyToOne.fetch().name();
                cascadeTypes = Arrays.stream(manyToOne.cascade()).map(Enum::name).collect(Collectors.toList());
            }

            // One-to-Many: Inverse side of Many-to-One (e.g., Customer -> Orders)
            OneToMany oneToMany = field.getAnnotation(OneToMany.class);
            if (oneToMany != null) {
                relationType = "OneToMany";
                mappedBy = oneToMany.mappedBy().isEmpty() ? null : oneToMany.mappedBy();
                fetchType = oneToMany.fetch().name();
                cascadeTypes = Arrays.stream(oneToMany.cascade()).map(Enum::name).collect(Collectors.toList());
            }

            // One-to-One: Unique relationship (e.g., User -> UserProfile)
            OneToOne oneToOne = field.getAnnotation(OneToOne.class);
            if (oneToOne != null) {
                relationType = "OneToOne";
                mappedBy = oneToOne.mappedBy().isEmpty() ? null : oneToOne.mappedBy();
                fetchType = oneToOne.fetch().name();
                cascadeTypes = Arrays.stream(oneToOne.cascade()).map(Enum::name).collect(Collectors.toList());
            }

            // Many-to-Many: Join table relationship (e.g., Student <-> Course)
            ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
            if (manyToMany != null) {
                relationType = "ManyToMany";
                mappedBy = manyToMany.mappedBy().isEmpty() ? null : manyToMany.mappedBy();
                fetchType = manyToMany.fetch().name();
                cascadeTypes = Arrays.stream(manyToMany.cascade()).map(Enum::name).collect(Collectors.toList());
            }

            // --- Extract enum mapping info ---
            Enumerated enumerated = field.getAnnotation(Enumerated.class);
            if (enumerated != null && metaProperty.getJavaType().isEnum()) {
                enumClass = metaProperty.getJavaType().getName();
                enumType = enumerated.value().name();  // STRING or ORDINAL
            }

            // --- Collect non-standard annotations for reference ---
            for (Annotation annotation : field.getAnnotations()) {
                String annotationName = annotation.annotationType().getSimpleName();
                if (!isCommonAnnotation(annotationName)) {
                    annotations.add("@" + annotationName);
                }
            }
        }

        // Get related entity name from Jmix Range (for associations)
        Range range = metaProperty.getRange();
        if (range.isClass()) {
            relatedEntityName = range.asClass().getName();
        }

        return new PropertyDefinition(
                metaProperty.getName(),
                metaProperty.getJavaType().getSimpleName(),
                metaProperty.getType().name(),  // DATATYPE, ENUM, ASSOCIATION, COMPOSITION
                columnName,
                columnDefinition,
                length,
                precision,
                scale,
                metaProperty.isMandatory(),
                metaProperty.isReadOnly(),
                relationType,
                relatedEntityName,
                mappedBy,
                fetchType,
                cascadeTypes,
                enumClass,
                enumType,
                annotations
        );
    }

    /**
     * Filters out common JPA annotations to reduce noise in output.
     * Only non-standard annotations are included in property definitions.
     */
    private boolean isCommonAnnotation(String name) {
        return Set.of("Column", "JoinColumn", "ManyToOne", "OneToMany", "OneToOne",
                "ManyToMany", "Id", "GeneratedValue", "Version", "Enumerated",
                "Temporal", "Basic", "Transient").contains(name);
    }

    /**
     * Extracts all class-level annotations for reference.
     */
    private List<String> getClassAnnotations(Class<?> javaClass) {
        List<String> annotations = new ArrayList<>();
        for (Annotation annotation : javaClass.getAnnotations()) {
            annotations.add("@" + annotation.annotationType().getSimpleName());
        }
        return annotations;
    }

    /**
     * Extracts JPA inheritance mapping information if present.
     * Returns null for entities not using inheritance.
     */
    private InheritanceInfo getInheritanceInfo(Class<?> javaClass) {
        Inheritance inheritance = javaClass.getAnnotation(Inheritance.class);
        DiscriminatorColumn discriminatorColumn = javaClass.getAnnotation(DiscriminatorColumn.class);
        DiscriminatorValue discriminatorValue = javaClass.getAnnotation(DiscriminatorValue.class);

        // Return null if no inheritance annotations present
        if (inheritance == null && discriminatorColumn == null && discriminatorValue == null) {
            return null;
        }

        return new InheritanceInfo(
                inheritance != null ? inheritance.strategy().name() : null,  // SINGLE_TABLE, JOINED, TABLE_PER_CLASS
                discriminatorColumn != null ? discriminatorColumn.name() : null,
                discriminatorValue != null ? discriminatorValue.value() : null
        );
    }

    /**
     * Finds a field by name in the class or any of its superclasses.
     */
    private Field findField(Class<?> clazz, String fieldName) {
        for (Field field : getAllFields(clazz)) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Returns all fields from a class and its entire inheritance hierarchy.
     * Required because getDeclaredFields() only returns fields from the current class.
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    // ==================== Record Definitions ====================

    /**
     * Complete entity metadata definition.
     */
    public record EntityDefinition(
            String name,                        // Jmix entity name
            String simpleName,                  // Short class name
            String fullClassName,               // Fully qualified class name
            String tableName,                   // Database table name
            String schemaName,                  // Database schema (nullable)
            boolean jpaEntity,                  // Is JPA entity?
            boolean embeddable,                 // Is embeddable component?
            boolean softDeletable,              // Supports soft delete?
            boolean versioned,                  // Has @Version field?
            PrimaryKeyInfo primaryKey,          // Primary key details
            List<PropertyDefinition> properties, // All entity properties
            List<String> classAnnotations,      // Class-level annotations
            InheritanceInfo inheritanceInfo     // Inheritance mapping (nullable)
    ) {}

    /**
     * Primary key metadata.
     */
    public record PrimaryKeyInfo(
            String propertyName,  // Java property name
            String javaType,      // Java type (UUID, Long, etc.)
            String columnName,    // Database column name
            boolean generated     // Auto-generated value?
    ) {}

    /**
     * Property/attribute metadata including column mapping and relationship info.
     */
    public record PropertyDefinition(
            String name,              // Property name
            String javaType,          // Java type name
            String propertyType,      // Jmix type: DATATYPE, ENUM, ASSOCIATION, COMPOSITION
            String columnName,        // Database column (nullable)
            String columnDefinition,  // Custom SQL type definition (nullable)
            Integer length,           // String length constraint
            Integer precision,        // Numeric precision
            Integer scale,            // Numeric scale
            boolean mandatory,        // Required field?
            boolean readOnly,         // Read-only field?
            String relationType,      // ManyToOne, OneToMany, etc. (nullable)
            String relatedEntityName, // Target entity for associations (nullable)
            String mappedBy,          // Inverse side mapping (nullable)
            String fetchType,         // LAZY or EAGER (nullable)
            List<String> cascadeTypes, // Cascade operations (nullable)
            String enumClass,         // Enum class name (nullable)
            String enumType,          // STRING or ORDINAL (nullable)
            List<String> annotations  // Additional annotations
    ) {}

    /**
     * JPA inheritance mapping metadata.
     */
    public record InheritanceInfo(
            String strategy,            // SINGLE_TABLE, JOINED, TABLE_PER_CLASS
            String discriminatorColumn, // Column name for type discrimination
            String discriminatorValue   // Value identifying this entity type
    ) {}
}