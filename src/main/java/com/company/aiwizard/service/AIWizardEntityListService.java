package com.company.aiwizard.service;

import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving metadata about JPA entities in the application.
 * Provides methods to list all persistent entities by name or class.
 * Useful for dynamic entity discovery, such as AI-powered wizards or admin tools.
 */
@Service("aiwizard_AIWizardEntityListService")  // Bean name with module prefix
public class AIWizardEntityListService {

    private final Metadata metadata;          // Jmix metadata registry containing all entity definitions
    private final MetadataTools metadataTools; // Utility class for working with metadata

    /**
     * Constructor injection for required dependencies.
     */
    public AIWizardEntityListService(Metadata metadata, MetadataTools metadataTools) {
        this.metadata = metadata;
        this.metadataTools = metadataTools;
    }

    /**
     * Returns a sorted list of all JPA entity names in the application.
     * Entity names are in Jmix format (e.g., "SampleData1", "User").
     *
     * @return alphabetically sorted list of entity names
     */
    public List<String> getAllEntityNames() {
        return metadata.getClasses().stream()
                .filter(metadataTools::isJpaEntity)  // Exclude non-persistent classes (DTOs, embeddables, etc.)
                .map(MetaClass::getName)             // Extract entity name from MetaClass
                .sorted()                            // Sort alphabetically
                .collect(Collectors.toList());
    }

    /**
     * Returns a sorted list of all JPA entity Java classes in the application.
     * Useful when you need to work with entity types programmatically.
     *
     * @return list of entity classes sorted by simple class name
     */
    public List<Class<?>> getAllEntityClasses() {
        return metadata.getClasses().stream()
                .filter(metadataTools::isJpaEntity)  // Exclude non-persistent classes
                .map(MetaClass::getJavaClass)        // Extract Java class from MetaClass
                .sorted((c1, c2) -> c1.getSimpleName().compareTo(c2.getSimpleName()))  // Sort by class name
                .collect(Collectors.toList());
    }
}