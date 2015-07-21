package org.cmis.base;

import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.definitions.*;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BaseTypeManager - Utils for cmis type manager
 */
public abstract class BaseTypeManager<T extends CallContextAwareCmisService> {

    private static final String NAMESPACE = "http://org.cmis";
    private static final TypeDefinitionFactory defaultTypeDefinitionFactory = TypeDefinitionFactory.newInstance();

    private final T cmisService;

    static {
        // set up TypeDefinitionFactory
        defaultTypeDefinitionFactory.setDefaultNamespace(NAMESPACE);
        defaultTypeDefinitionFactory.setDefaultControllableAcl(false);
        defaultTypeDefinitionFactory.setDefaultControllablePolicy(false);
        defaultTypeDefinitionFactory.setDefaultQueryable(false);
        defaultTypeDefinitionFactory.setDefaultFulltextIndexed(false);
        defaultTypeDefinitionFactory.setDefaultTypeMutability(defaultTypeDefinitionFactory.createTypeMutability(false, false, false));
    }

    protected BaseTypeManager(T cmisService) {
        // set up the cmis service
        this.cmisService = cmisService;

//        // set up TypeDefinitionFactory
//        typeDefinitionFactory = TypeDefinitionFactory.newInstance();
//        typeDefinitionFactory.setDefaultNamespace(NAMESPACE);
//        typeDefinitionFactory.setDefaultControllableAcl(false);
//        typeDefinitionFactory.setDefaultControllablePolicy(false);
//        typeDefinitionFactory.setDefaultQueryable(false);
//        typeDefinitionFactory.setDefaultFulltextIndexed(false);
//        typeDefinitionFactory.setDefaultTypeMutability(typeDefinitionFactory.createTypeMutability(false, false, false));
//
//        // set up definitions map
//        typeDefinitions = new HashMap<String, TypeDefinition>();
//
//        // add base folder type
//        MutableFolderTypeDefinition folderType = typeDefinitionFactory.createBaseFolderTypeDefinition(CmisVersion.CMIS_1_1);
//        removeQueryableAndOrderableFlags(folderType);
//        typeDefinitions.put(folderType.getId(), folderType);
//
//        // add base document type
//        MutableDocumentTypeDefinition documentType = typeDefinitionFactory.createBaseDocumentTypeDefinition(CmisVersion.CMIS_1_1);
//        documentType.setTypeMutability(typeDefinitionFactory.createTypeMutability(true, false, true));
//        removeQueryableAndOrderableFlags(documentType);
//        typeDefinitions.put(documentType.getId(), documentType);
    }

    protected T getCmisService() {
        return this.cmisService;
    }

    protected static TypeDefinitionFactory getDefaultTypeDefinitionFactory() {
        return defaultTypeDefinitionFactory;
    }

    protected abstract Map<String, TypeDefinition> getTypeDefinitions();

    /**
     * Removes the queryable and orderable flags from the property definitions
     * of a type definition because this implementations does neither support
     * queries nor can order objects.
     */
    protected static void removeQueryableAndOrderableFlags(MutableTypeDefinition type) {
        for (PropertyDefinition<?> propDef : type.getPropertyDefinitions().values()) {
            MutablePropertyDefinition<?> mutablePropDef = (MutablePropertyDefinition<?>) propDef;
            mutablePropDef.setIsQueryable(false);
            mutablePropDef.setIsOrderable(false);
        }
    }

    // service methods to be implemented
    public abstract TypeDefinition createType(TypeDefinition type);

    public abstract TypeDefinition updateType(TypeDefinition type);

    public abstract void deleteType(String typeId);

    public abstract TypeDefinitionList getTypeChildren(String typeId, Boolean includePropertyDefinitions, BigInteger maxItems, BigInteger skipCount);

    public abstract TypeDefinition getTypeDefinition(String typeId);

    public abstract List<TypeDefinitionContainer> getTypeDescendants(String typeId, BigInteger depth, Boolean includePropertyDefinitions);


//    /**
//     * Adds a type definition.
//     */
//    public synchronized void addTypeDefinition(TypeDefinition type) {
//        if (type == null) {
//            throw new IllegalArgumentException("Type must be set!");
//        }
//
//        if (type.getId() == null || type.getId().trim().length() == 0) {
//            throw new IllegalArgumentException("Type must have a valid id!");
//        }
//
//        if (type.getParentTypeId() == null || type.getParentTypeId().trim().length() == 0) {
//            throw new IllegalArgumentException("Type must have a valid parent id!");
//        }
//
//        TypeDefinition parentType = typeDefinitions.get(type.getParentTypeId());
//        if (parentType == null) {
//            throw new IllegalArgumentException("Parent type doesn't exist!");
//        }
//
//        MutableTypeDefinition newType = defaultTypeDefinitionFactory.copy(type, true);
//
//        // copy parent type property definitions and mark them as inherited
//        for (PropertyDefinition<?> propDef : parentType.getPropertyDefinitions().values()) {
//            MutablePropertyDefinition<?> basePropDef = defaultTypeDefinitionFactory.copy(propDef);
//            basePropDef.setIsInherited(true);
//            newType.addPropertyDefinition(basePropDef);
//        }
//
//        typeDefinitions.put(newType.getId(), newType);
//
//        if (LOG.isDebugEnabled()) {
//            LOG.debug("Added type '{}'.", type.getId());
//        }
//    }
//
//
//
//    /**
//     * Returns the internal type definition.
//     */
//    public synchronized TypeDefinition getInternalTypeDefinition(String typeId) {
//        return typeDefinitions.get(typeId);
//    }
//
//    /**
//     * Returns all internal type definitions.
//     */
//    public synchronized Collection<TypeDefinition> getInternalTypeDefinitions() {
//        return typeDefinitions.values();
//    }
//
//    // --- service methods ---
//
//    public TypeDefinition getTypeDefinition(String typeId) {
//        TypeDefinition type = typeDefinitions.get(typeId);
//        if (type == null) {
//            throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
//        }
//
//        return defaultTypeDefinitionFactory.copy(type, true, this.cmisService.getCallContext().getCmisVersion());
//    }
//
//    public TypeDefinitionList getTypeChildren(String typeId, Boolean includePropertyDefinitions,
//                                              BigInteger maxItems, BigInteger skipCount) {
//        return defaultTypeDefinitionFactory.createTypeDefinitionList(typeDefinitions, typeId, includePropertyDefinitions,
//                maxItems, skipCount, this.cmisService.getCallContext().getCmisVersion());
//    }
//
//    public List<TypeDefinitionContainer> getTypeDescendants(CallContext context, String typeId, BigInteger depth,
//                                                            Boolean includePropertyDefinitions) {
//        return defaultTypeDefinitionFactory.createTypeDescendants(typeDefinitions, typeId, depth, includePropertyDefinitions,
//                context.getCmisVersion());
//    }
//
//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//
//        for (TypeDefinition type : typeDefinitions.values()) {
//            sb.append('[');
//            sb.append(type.getId());
//            sb.append(" (");
//            sb.append(type.getBaseTypeId().value());
//            sb.append(")]");
//        }
//
//        return sb.toString();
//    }
//
//    public abstract List<TypeDefinitionContainer> getTypeDescendants(String typeId, BigInteger depth, Boolean includePropertyDefinitions);
}
