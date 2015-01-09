package org.cmis.base;

import de.elo.utils.net.RemoteException;
import org.apache.chemistry.opencmis.commons.data.*;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.definitions.*;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.*;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;


/**
 * Base Repository - Utils for repositories
 */
public abstract class BaseRepository<T extends CallContextAwareCmisService> {
    private static final Logger LOG = LoggerFactory.getLogger(BaseRepository.class);

    private T cmisService;

    private BaseRepository() {
    }

    protected BaseRepository(T cmisService) {
        this.cmisService = cmisService;
    }

    /**
     * Create permission
     *
     * @param permission  The permission
     * @param description The description
     * @return
     */
    protected static PermissionDefinition createPermissionDefinition(String permission, String description) {
        PermissionDefinitionDataImpl pd = new PermissionDefinitionDataImpl();
        pd.setId(permission);
        pd.setDescription(description);

        return pd;
    }

    /**
     * Create a mapping
     *
     * @param key         The key
     * @param permissions The permission
     * @return
     */
    protected static PermissionMapping createPermissionMapping(String key, BasePermission... permissions) {
        List<String> permissionList = new ArrayList<>();
        for (BasePermission permission : permissions) {
            permissionList.add(permission.getValue());
        }
        PermissionMappingDataImpl permissionMappingData = new PermissionMappingDataImpl();
        permissionMappingData.setKey(key);
        permissionMappingData.setPermissions(permissionList);

        return permissionMappingData;
    }


    /**
     * Adds the default value of property if defined.
     *
     * @param properties
     * @param propDef
     * @return
     */
    protected static boolean addPropertyDefault(PropertiesImpl properties, PropertyDefinition<?> propDef) {
        if ((properties == null) || (properties.getProperties() == null)) {
            throw new IllegalArgumentException("properties must not be null!");
        }
        if (propDef == null) {
            return false;
        }
        List<?> defaultValue = propDef.getDefaultValue();
        if ((defaultValue != null) && (!defaultValue.isEmpty())) {
            switch (propDef.getPropertyType()) {
                case BOOLEAN:
                    properties.addProperty(new PropertyBooleanImpl(propDef.getId(), (List<Boolean>) defaultValue));
                    break;
                case DATETIME:
                    properties.addProperty(new PropertyDateTimeImpl(propDef.getId(), (List<GregorianCalendar>) defaultValue));
                    break;
                case DECIMAL:
                    properties.addProperty(new PropertyDecimalImpl(propDef.getId(), (List<BigDecimal>) defaultValue));
                    break;
                case HTML:
                    properties.addProperty(new PropertyHtmlImpl(propDef.getId(), (List<String>) defaultValue));
                    break;
                case ID:
                    properties.addProperty(new PropertyIdImpl(propDef.getId(), (List<String>) defaultValue));
                    break;
                case INTEGER:
                    properties.addProperty(new PropertyIntegerImpl(propDef.getId(), (List<BigInteger>) defaultValue));
                    break;
                case STRING:
                    properties.addProperty(new PropertyStringImpl(propDef.getId(), (List<String>) defaultValue));
                    break;
                case URI:
                    properties.addProperty(new PropertyUriImpl(propDef.getId(), (List<String>) defaultValue));
                    break;
                default:
                    throw new RuntimeException("Unknown datatype!");
            }
            return true;
        }
        return false;
    }


    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    // properties helper methods
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Converts milliseconds into a calendar object.
     *
     * @param millis
     * @return
     */
    protected static GregorianCalendar millisToCalendar(long millis) {
        GregorianCalendar result = new GregorianCalendar();
        result.setTimeZone(TimeZone.getTimeZone("GMT"));
        result.setTimeInMillis((long) (Math.ceil(millis / 1000) * 1000));
        return result;
    }

    public T getCmisService() {
        return this.cmisService;
    }

    /**
     * Test is a property exists in the properties list.
     *
     * @param properties
     * @param id
     * @return
     */
    protected boolean existsProperty(Properties properties, String id) {
        return properties.getProperties().containsKey(id);
    }

    /**
     * Checks the eligibility of adding a property to properties set.
     *
     * @param properties
     * @param typeId
     * @param filter
     * @param id
     * @return
     */
    protected boolean checkAddProperty(Properties properties, String typeId, Set<String> filter, String id) {
        if ((properties == null) || (properties.getProperties() == null)) {
            throw new IllegalArgumentException("Properties must not be null!");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null!");
        }
        return true;
    }

    /**
     * Add an Id property
     *
     * @param properties The properties
     * @param typeId     The typeId
     * @param filter     the Filter
     * @param id         The id
     * @param value      The value
     */
    protected void addPropertyId(PropertiesImpl properties, String typeId, Set<String> filter, String id, String value) {
        if (!checkAddProperty(properties, typeId, filter, id)) {
            return;
        }
        properties.addProperty(new PropertyIdImpl(id, value));
    }

    /**
     * Returns the first value of an id property.
     *
     * @param properties
     * @param id
     * @return
     */
    protected String getPropertyId(Properties properties, String id) {
        PropertyData<?> propertyData = properties.getProperties().get(id);
        if (!(propertyData instanceof PropertyId)) {
            return null;
        }
        return ((PropertyId) propertyData).getFirstValue();
    }

    /**
     * Add an Id list property
     *
     * @param properties The properties
     * @param typeId     The typeId
     * @param filter     the Filter
     * @param id         The id
     * @param value      The value
     */
    protected void addPropertyIdList(PropertiesImpl properties, String typeId, Set<String> filter, String id, List<String> value) {
        if (!checkAddProperty(properties, typeId, filter, id)) {
            return;
        }
        properties.addProperty(new PropertyIdImpl(id, value));
    }

    /**
     * Return the list of ids value
     *
     * @param properties
     * @param id
     * @return
     */
    protected List<String> getPropertyIdList(Properties properties, String id) {
        PropertyData<?> propertyData = properties.getProperties().get(id);
        if (!(propertyData instanceof PropertyId)) {
            return null;
        }
        return ((PropertyId) propertyData).getValues();
    }

    /**
     * Add a String property
     *
     * @param properties The properties
     * @param typeId     The typeId
     * @param filter     the Filter
     * @param id         The id
     * @param value      The value
     */
    protected void addPropertyString(PropertiesImpl properties, String typeId, Set<String> filter, String id, String value) {
        if (!checkAddProperty(properties, typeId, filter, id)) {
            return;
        }
        properties.addProperty(new PropertyStringImpl(id, value));
    }

    /**
     * Returns the first value of a string property.
     *
     * @param properties
     * @param id
     * @return
     */
    protected String getPropertyString(Properties properties, String id) {
        PropertyData<?> propertyData = properties.getProperties().get(id);
        if (!(propertyData instanceof PropertyString)) {
            return null;
        }
        return ((PropertyString) propertyData).getFirstValue();
    }

    /**
     * Add an Integer property
     *
     * @param properties The properties
     * @param typeId     The typeId
     * @param filter     the Filter
     * @param id         The id
     * @param value      The value
     */
    protected void addPropertyInteger(PropertiesImpl properties, String typeId, Set<String> filter, String id, int value) {
        addPropertyBigInteger(properties, typeId, filter, id, BigInteger.valueOf(value));
    }

    /**
     * Retrieve an Integer property value
     *
     * @param properties
     * @param id
     * @return
     */
    protected Integer getPropertyInteger(Properties properties, String id) {
        PropertyData<?> propertyData = properties.getProperties().get(id);
        if (!(propertyData instanceof PropertyString)) {
            return null;
        }
        return Integer.valueOf(((PropertyString) propertyData).getFirstValue());
    }

    /**
     * Add an Long property
     *
     * @param properties The properties
     * @param typeId     The typeId
     * @param filter     the Filter
     * @param id         The id
     * @param value      The value
     */
    protected void addPropertyLong(PropertiesImpl properties, String typeId, Set<String> filter, String id, long value) {
        addPropertyBigInteger(properties, typeId, filter, id, BigInteger.valueOf(value));
    }

    /**
     * Retrieve an Long property value
     *
     * @param properties
     * @param id
     * @return
     */
    protected Long getPropertyLong(Properties properties, String id) {
        PropertyData<?> propertyData = properties.getProperties().get(id);
        if (!(propertyData instanceof PropertyString)) {
            return null;
        }
        return Long.valueOf(((PropertyString) propertyData).getFirstValue());
    }

    /**
     * Add a Big Integer property
     *
     * @param properties The properties
     * @param typeId     The typeId
     * @param filter     the Filter
     * @param id         The id
     * @param value      The value
     */
    protected void addPropertyBigInteger(PropertiesImpl properties, String typeId, Set<String> filter, String id, BigInteger value) {
        if (!checkAddProperty(properties, typeId, filter, id)) {
            return;
        }
        properties.addProperty(new PropertyIntegerImpl(id, value));
    }

    /**
     * Retrieve an Big Integer property value
     *
     * @param properties
     * @param id
     * @return
     */
    protected BigInteger getPropertyBigInteger(Properties properties, String id) {
        PropertyData<?> propertyData = properties.getProperties().get(id);
        if (!(propertyData instanceof PropertyString)) {
            return null;
        }
        return BigInteger.valueOf(Long.parseLong(((PropertyString) propertyData).getFirstValue()));
    }

    /**
     * Add a Boolean property
     *
     * @param properties The properties
     * @param typeId     The typeId
     * @param filter     the Filter
     * @param id         The id
     * @param value      The value
     */
    protected void addPropertyBoolean(PropertiesImpl properties, String typeId, Set<String> filter, String id, boolean value) {
        if (!checkAddProperty(properties, typeId, filter, id)) {
            return;
        }
        properties.addProperty(new PropertyBooleanImpl(id, value));
    }

    /**
     * Retrieve an Boolean property value
     *
     * @param properties
     * @param id
     * @return
     */
    protected Boolean getPropertyBoolean(Properties properties, String id) {
        PropertyData<?> propertyData = properties.getProperties().get(id);
        if (!(propertyData instanceof PropertyString)) {
            return null;
        }
        return Boolean.parseBoolean(((PropertyString) propertyData).getFirstValue());
    }

    /**
     * Add a DateTime property
     *
     * @param properties The properties
     * @param typeId     The typeId
     * @param filter     the Filter
     * @param id         The id
     * @param value      The value
     */
    protected void addPropertyDateTime(PropertiesImpl properties, String typeId, Set<String> filter, String id, GregorianCalendar value) {
        if (!checkAddProperty(properties, typeId, filter, id)) {
            return;
        }
        properties.addProperty(new PropertyDateTimeImpl(id, value));
    }

    /**
     * Returns the first value of a datetime property.
     *
     * @param properties
     * @param name
     * @return
     */
    protected GregorianCalendar getDateTimeProperty(Properties properties, String name) {
        PropertyData<?> propertyData = properties.getProperties().get(name);
        if (!(propertyData instanceof PropertyDateTime)) {
            return null;
        }
        return ((PropertyDateTime) propertyData).getFirstValue();
    }


    // --- CMIS Operations ---
    // --- CMIS Operations ---
    // --- CMIS Operations ---
    // --- CMIS Operations ---
    // --- CMIS Operations ---
    // --- CMIS Operations ---
    // --- CMIS Operations ---

    // -----------------------------------------------------------------------------------------------------------------
    // NAVIGATION SERVICES
    // -----------------------------------------------------------------------------------------------------------------

    public abstract ObjectInFolderList getChildren(String folderId, String filter, String orderBy,
                                                   Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
                                                   Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount);

    //public List<ObjectInFolderContainer> getDescendants

    public abstract ObjectData getFolderParent(String folderId, String filter);

    //public List<ObjectInFolderContainer> getFolderTree

    public abstract List<ObjectParentData> getObjectParents(String objectId, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includeRelativePathSegment);


    // -----------------------------------------------------------------------------------------------------------------
    // REPOSITORY SERVICES
    // -----------------------------------------------------------------------------------------------------------------

    public abstract TypeDefinition createType(TypeDefinition type);

    public abstract void deleteType(String typeId);

    public abstract RepositoryInfo getRepositoryInfo();

    //public List<RepositoryInfo> getRepositoryInfos

    public abstract TypeDefinitionList getTypeChildren(String typeId, Boolean includePropertyDefinitions, BigInteger maxItems, BigInteger skipCount);

    public abstract TypeDefinition getTypeDefinition(String typeId);

    public abstract List<TypeDefinitionContainer> getTypeDescendants(String typeId, BigInteger depth, Boolean includePropertyDefinitions);

    public abstract TypeDefinition updateType(TypeDefinition type);


    // -----------------------------------------------------------------------------------------------------------------
    // OBJECT SERVICES
    // -----------------------------------------------------------------------------------------------------------------

    //public abstract void appendContentStream(Holder<String> objectId, Holder<String> changeToken, ContentStream contentStream, boolean isLastChunk);

    //public abstract List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(List<BulkUpdateObjectIdAndChangeToken> objectIdAndChangeToken, Properties properties, List<String> addSecondaryTypeIds, List<String> removeSecondaryTypeIds);

    public abstract String createDocument(Properties properties, String folderId, ContentStream contentStream, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces);

    //public abstract String createDocumentFromSource(String sourceId, Properties properties, String folderId, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces);

    public abstract String createFolder(Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces);

    //public abstract String createItem(Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces);

    //public abstract String createPolicy(Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces);

    //public abstract String createRelationship(Properties properties, List<String> policies, Acl addAces, Acl removeAces);

    //public abstract void deleteContentStream(Holder<String> objectId, Holder<String> changeToken);

    public abstract void deleteObject(String objectId, Boolean allVersions);

    //public abstract FailedToDeleteData deleteTree(String folderId, Boolean allVersions, UnfileObject unfileObjects, Boolean continueOnFailure);

    //public abstract AllowableActions getAllowableActions(String objectId);

    public abstract ContentStream getContentStream(String objectId, String streamId, BigInteger offset, BigInteger length);

    public abstract ObjectData getObject(String objectId, String filter, Boolean includeAllowableActions,
                                         IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds,
                                         Boolean includeAcl);

    public abstract ObjectData getObjectByPath(String path, String filter, Boolean includeAllowableActions,
                                               IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds,
                                               Boolean includeAcl);

    //public abstract Properties getProperties(String objectId, String filter);

    //public abstract List<RenditionData> getRenditions(String objectId, String renditionFilter, BigInteger maxItems, BigInteger skipCount);

    //public abstract void moveObject(Holder<String> objectId, String targetFolderId, String sourceFolderId);

    public abstract void setContentStream(Holder<String> objectId, Boolean overwriteFlag, Holder<String> changeToken, ContentStream contentStream);

    public abstract void updateProperties(Holder<String> objectId, Holder<String> changeToken, Properties properties) throws RemoteException;


    // -----------------------------------------------------------------------------------------------------------------
    // VERSIONING SERVICES
    // -----------------------------------------------------------------------------------------------------------------

    public abstract void cancelCheckOut(String objectId);

    public abstract void checkIn(Holder<String> objectId, Boolean major, Properties properties, ContentStream contentStream, String checkinComment, List<String> policies, Acl addAces, Acl removeAces);

    public abstract void checkOut(Holder<String> objectId, Holder<Boolean> contentCopied);

    public abstract List<ObjectData> getAllVersions(String objectId, String versionSeriesId, String filter, Boolean includeAllowableActions);

    //public abstract ObjectData getObjectOfLatestVersion(String objectId, String versionSeriesId, Boolean major, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds, Boolean includeAcl);

    //public abstract Properties getPropertiesOfLatestVersion(String objectId, String versionSeriesId, Boolean major, String filter);

}
