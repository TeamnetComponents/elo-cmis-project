/*
 * Copyright 2013 Florian Müller & Jay Brown
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * This code is based on the Apache Chemistry OpenCMIS FileShare project
 * <http://chemistry.apache.org/java/developing/repositories/dev-repositories-fileshare.html>.
 *
 * It is part of a training exercise and not intended for production use!
 *
 */
package org.cmis.server.elo;

import de.elo.extension.connection.EloUtilsConnection;
import de.elo.extension.service.EloUtilsService;
import de.elo.ix.client.*;
import de.elo.utils.net.RemoteException;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.*;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.*;
import org.apache.chemistry.opencmis.commons.exceptions.*;
import org.apache.chemistry.opencmis.commons.impl.MimeTypes;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.*;
import org.apache.chemistry.opencmis.commons.impl.server.ObjectInfoImpl;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.cmis.base.BaseRepository;
import org.cmis.server.filebridge.FileBridgeUtils;
import org.cmis.util.CmisUtils;
import org.cmis.util.DateUtil;
import org.platform.common.utils.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.cmis.server.elo.EloCmisPermission.*;

/**
 * Implements ELO repository operations.
 */
public class EloCmisRepository extends BaseRepository<EloCmisService> {
    private static final Logger LOG = LoggerFactory.getLogger(EloCmisRepository.class);

    private static final int BUFFER_SIZE = 64 * 1024;

    private final RepositoryInfo repositoryInfo;
    private final EloCmisTypeManager typeManager;
    private ExtensionsData extensionsData;

    //DONE
    public EloCmisRepository(EloCmisService cmisService, String repositoryId, ExtensionsData extensionsData) {
        super(cmisService);
        this.typeManager = new EloCmisTypeManager(cmisService);
        this.repositoryInfo = getRepositoryInfo(cmisService, extensionsData, repositoryId);

    }

    //DONE
    private EloCmisRepository(EloCmisService cmisService, RepositoryInfo repositoryInfo, ExtensionsData extensionsData) {
        super(cmisService);
        try {
            this.repositoryInfo = repositoryInfo;
            this.typeManager = new EloCmisTypeManager(cmisService);
        } catch (Exception e) {
            throw e;
        }
    }

    //DONE
    public static Map<String, EloCmisRepository> createEloCmisRepositoryMap(EloCmisService cmisService, ExtensionsData extensionsData) {
        Map<String, EloCmisRepository> eloCmisRepositoryMap = new HashMap<>();
        List<RepositoryInfo> repositoryInfoList = getRepositoryInfos(cmisService, extensionsData);
        for (RepositoryInfo ri : repositoryInfoList) {
            eloCmisRepositoryMap.put(ri.getId(), new EloCmisRepository(cmisService, ri, extensionsData));
        }
        return eloCmisRepositoryMap;
    }

    //DONE
    private static RepositoryCapabilities createRepositoryInfoCapabilities(EloCmisService cmisService, String repositoryId, ExtensionsData extensionsData) {
        CmisVersion cmisVersion = cmisService.getCallContext().getCmisVersion();

        RepositoryCapabilitiesImpl capabilities = new RepositoryCapabilitiesImpl();
        capabilities.setCapabilityAcl(CapabilityAcl.DISCOVER);
        capabilities.setAllVersionsSearchable(true); //false
        capabilities.setCapabilityJoin(CapabilityJoin.NONE);
        capabilities.setSupportsMultifiling(false);
        capabilities.setSupportsUnfiling(false);
        capabilities.setSupportsVersionSpecificFiling(true); //false
        capabilities.setIsPwcSearchable(false);
        capabilities.setIsPwcUpdatable(false);
        capabilities.setCapabilityQuery(CapabilityQuery.METADATAONLY);
        capabilities.setCapabilityChanges(CapabilityChanges.NONE);
        capabilities.setCapabilityContentStreamUpdates(CapabilityContentStreamUpdates.ANYTIME);
        capabilities.setSupportsGetDescendants(true);
        capabilities.setSupportsGetFolderTree(true);
        capabilities.setCapabilityRendition(CapabilityRenditions.NONE);

        if (cmisVersion != CmisVersion.CMIS_1_0) {
            capabilities.setOrderByCapability(CapabilityOrderBy.NONE);

            NewTypeSettableAttributesImpl typeSetAttributes = new NewTypeSettableAttributesImpl();
            typeSetAttributes.setCanSetControllableAcl(false);
            typeSetAttributes.setCanSetControllablePolicy(false);
            typeSetAttributes.setCanSetCreatable(false);
            typeSetAttributes.setCanSetDescription(false);
            typeSetAttributes.setCanSetDisplayName(false);
            typeSetAttributes.setCanSetFileable(false);
            typeSetAttributes.setCanSetFulltextIndexed(false);
            typeSetAttributes.setCanSetId(false);
            typeSetAttributes.setCanSetIncludedInSupertypeQuery(false);
            typeSetAttributes.setCanSetLocalName(false);
            typeSetAttributes.setCanSetLocalNamespace(false);
            typeSetAttributes.setCanSetQueryable(false);
            typeSetAttributes.setCanSetQueryName(false);
            capabilities.setNewTypeSettableAttributes(typeSetAttributes);
            CreatablePropertyTypesImpl creatablePropertyTypes = new CreatablePropertyTypesImpl();
            capabilities.setCreatablePropertyTypes(creatablePropertyTypes);
        }
        return capabilities;
    }

    //DONE
    private static Map<String, PermissionMapping> createPermissionMappingMap(EloCmisService cmisService, String repositoryId, ExtensionsData extensionsData) {
        List<PermissionMapping> permissionMappingList = new ArrayList<PermissionMapping>();
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_CREATE_DOCUMENT_FOLDER, CMIS_READ));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_CREATE_DOCUMENT_FOLDER, CMIS_READ));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_CREATE_FOLDER_FOLDER, CMIS_READ));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_DELETE_CONTENT_DOCUMENT, CMIS_WRITE));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_DELETE_OBJECT, CMIS_ALL));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_DELETE_TREE_FOLDER, CMIS_ALL));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_GET_ACL_OBJECT, CMIS_READ));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_GET_ALL_VERSIONS_VERSION_SERIES, CMIS_READ));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_GET_CHILDREN_FOLDER, CMIS_READ));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_GET_DESCENDENTS_FOLDER, CMIS_READ));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_GET_FOLDER_PARENT_OBJECT, CMIS_READ));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_GET_PARENTS_FOLDER, CMIS_READ));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_GET_PROPERTIES_OBJECT, CMIS_READ));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_MOVE_OBJECT, CMIS_WRITE));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_MOVE_SOURCE, CMIS_READ));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_MOVE_TARGET, CMIS_WRITE));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_SET_CONTENT_DOCUMENT, CMIS_WRITE));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_UPDATE_PROPERTIES_OBJECT, CMIS_WRITE));
        permissionMappingList.add(createPermissionMapping(PermissionMapping.CAN_VIEW_CONTENT_OBJECT, CMIS_READ));

        /* all cmis
        String CAN_GET_DESCENDENTS_FOLDER = "canGetDescendents.Folder";
        String CAN_GET_CHILDREN_FOLDER = "canGetChildren.Folder";
        String CAN_GET_PARENTS_FOLDER = "canGetParents.Folder";
        String CAN_GET_FOLDER_PARENT_OBJECT = "canGetFolderParent.Object";
        String CAN_CREATE_DOCUMENT_FOLDER = "canCreateDocument.Folder";
        String CAN_CREATE_FOLDER_FOLDER = "canCreateFolder.Folder";
        String CAN_CREATE_POLICY_FOLDER = "canCreatePolicy.Folder";
        String CAN_CREATE_RELATIONSHIP_SOURCE = "canCreateRelationship.Source";
        String CAN_CREATE_RELATIONSHIP_TARGET = "canCreateRelationship.Target";
        String CAN_GET_PROPERTIES_OBJECT = "canGetProperties.Object";
        String CAN_VIEW_CONTENT_OBJECT = "canViewContent.Object";
        String CAN_UPDATE_PROPERTIES_OBJECT = "canUpdateProperties.Object";
        String CAN_MOVE_OBJECT = "canMove.Object";
        String CAN_MOVE_TARGET = "canMove.Target";
        String CAN_MOVE_SOURCE = "canMove.Source";
        String CAN_DELETE_OBJECT = "canDelete.Object";
        String CAN_DELETE_TREE_FOLDER = "canDeleteTree.Folder";
        String CAN_SET_CONTENT_DOCUMENT = "canSetContent.Document";
        String CAN_DELETE_CONTENT_DOCUMENT = "canDeleteContent.Document";
        String CAN_ADD_TO_FOLDER_OBJECT = "canAddToFolder.Object";
        String CAN_ADD_TO_FOLDER_FOLDER = "canAddToFolder.Folder";
        String CAN_REMOVE_FROM_FOLDER_OBJECT = "canRemoveFromFolder.Object";
        String CAN_REMOVE_FROM_FOLDER_FOLDER = "canRemoveFromFolder.Folder";
        String CAN_CHECKOUT_DOCUMENT = "canCheckout.Document";
        String CAN_CANCEL_CHECKOUT_DOCUMENT = "canCancelCheckout.Document";
        String CAN_CHECKIN_DOCUMENT = "canCheckin.Document";
        String CAN_GET_ALL_VERSIONS_VERSION_SERIES = "canGetAllVersions.VersionSeries";
        String CAN_GET_OBJECT_RELATIONSHIPS_OBJECT = "canGetObjectRelationships.Object";
        String CAN_ADD_POLICY_OBJECT = "canAddPolicy.Object";
        String CAN_ADD_POLICY_POLICY = "canAddPolicy.Policy";
        String CAN_REMOVE_POLICY_OBJECT = "canRemovePolicy.Object";
        String CAN_REMOVE_POLICY_POLICY = "canRemovePolicy.Policy";
        String CAN_GET_APPLIED_POLICIES_OBJECT = "canGetAppliedPolicies.Object";
        String CAN_GET_ACL_OBJECT = "canGetACL.Object";
        String CAN_APPLY_ACL_OBJECT = "canApplyACL.Object";
        */

        Map<String, PermissionMapping> permissionMappingMap = new LinkedHashMap<String, PermissionMapping>();
        for (PermissionMapping permissionMapping : permissionMappingList) {
            permissionMappingMap.put(permissionMapping.getKey(), permissionMapping);
        }
        return permissionMappingMap;
    }

    //DONE
    private static AclCapabilities createRepositoryInfoAclCapabilities(EloCmisService cmisService, String repositoryId, ExtensionsData extensionsData) {

        AclCapabilitiesDataImpl aclCapability = new AclCapabilitiesDataImpl();
        aclCapability.setSupportedPermissions(SupportedPermissions.BASIC);
        aclCapability.setAclPropagation(AclPropagation.OBJECTONLY);

        // permission definitions
        boolean cmisStandardPermissionOnly = false;
        aclCapability.setPermissionDefinitionData(EloCmisPermission.getPermissionDefinitionList(cmisStandardPermissionOnly));

        // permission mapping aclCapabilities and permission definition
        aclCapability.setPermissionMappingData(createPermissionMappingMap(cmisService, repositoryId, extensionsData));

        return aclCapability;
    }

    private static String calcRepositoryId(ServerInfo serverInfo, IndexServerForArchive indexServerForArchive) {
        String serverName = serverInfo.getInstanceName();
        try {
            serverName = (new URL(indexServerForArchive.getUrl())).getHost();
        } catch (MalformedURLException e) {
            //keep initial servername
        }
        return serverName + "_" + indexServerForArchive.getArcName();
    }

    private static String calcRepositoryName(ServerInfo serverInfo, IndexServerForArchive indexServerForArchive) {
        return calcRepositoryId(serverInfo, indexServerForArchive);
    }

    //DONE
    private static List<RepositoryInfo> getRepositoryInfos(EloCmisService cmisService, ExtensionsData extensionsData, String repositoryId) {
        // repository info set up
        List<RepositoryInfo> repositoryInfoList = new ArrayList<>();
        CmisVersion cmisVersion = cmisService.getCallContext().getCmisVersion();
        IXConnection ixConnection = cmisService.getConnection();
        ServerInfo serverInfo = null;
        try {
            serverInfo = ixConnection.ix().getServerInfo();

            for (IndexServerForArchive indexServerForArchive : serverInfo.getIndexServers()) {
                if (repositoryId == null || repositoryId.equals(calcRepositoryId(serverInfo, indexServerForArchive))) {
                    RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();
                    repositoryInfo.setId(calcRepositoryId(serverInfo, indexServerForArchive));
                    repositoryInfo.setName(calcRepositoryName(serverInfo, indexServerForArchive));
                    repositoryInfo.setDescription("Select to connect to <" + repositoryInfo.getName() + "> archive.");
                    repositoryInfo.setProductName("ELO Digital Office Electronic Document Management Solution");
                    repositoryInfo.setProductVersion(serverInfo.getVersion());
                    repositoryInfo.setVendorName("ELO Digital Office");
                    repositoryInfo.setThinClientUri(indexServerForArchive.getUrl());
                    repositoryInfo.setRootFolder(EloCmisUtils.getFileUtilsElo().getRootPathDefault());
                    repositoryInfo.setChangesIncomplete(true);
                    repositoryInfo.setCapabilities(createRepositoryInfoCapabilities(cmisService, repositoryInfo.getId(), extensionsData));
                    repositoryInfo.setAclCapabilities(createRepositoryInfoAclCapabilities(cmisService, repositoryInfo.getId(), extensionsData));
                    repositoryInfo.setCmisVersion(CmisVersion.CMIS_1_1);
                    repositoryInfoList.add(repositoryInfo);
                }
            }
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
        return repositoryInfoList;
    }

    public static List<RepositoryInfo> getRepositoryInfos(EloCmisService cmisService, ExtensionsData extensionsData) {
        return getRepositoryInfos(cmisService, extensionsData, null);
    }

    //DONE
    public static RepositoryInfo getRepositoryInfo(EloCmisService cmisService, ExtensionsData extensionsData, String repositoryId) {
        RepositoryInfo repositoryInfo = null;

        List<RepositoryInfo> repositoryInfoList = getRepositoryInfos(cmisService, extensionsData, repositoryId);
        if (repositoryInfoList.size() == 1) {
            repositoryInfo = repositoryInfoList.get(0);
        } else {
            throw new CmisObjectNotFoundException("Repository '" + repositoryId + "' does not exist!");
        }
        return repositoryInfo;
    }

    //DONE
    public ExtensionsData getExtensionsData() {
        return extensionsData;
    }

    //DONE
    public void setExtensionsData(ExtensionsData extensionsData) {
        this.extensionsData = extensionsData;
    }


    /**
     * Returns the id of this repository.
     */
    public String getRepositoryId() {
        return this.repositoryInfo.getId();
    }

    /**
     * Returns the root directory of this repository
     */
    public String getRootFolderId() {
        return this.repositoryInfo.getRootFolderId();
    }


    /**
     * CMIS getObject.
     */

    private EloCmisObject prepareEloCmisObject(Object object, DocVersion documentVersion,
                                               String filter,
                                               boolean includeAllowableActions,
                                               IncludeRelationships includeRelationships,
                                               String renditionFilter,
                                               Boolean includePolicyIds,
                                               boolean includeAcl) {
        EloCmisObject eloCmisObject = new EloCmisObject();
        prepareCmisObjectProperties(eloCmisObject, object, documentVersion, filter);

        /*objectData.setAllowableActions();
        objectData.setAcl();
        objectData.setIsExactAcl();
        objectData.setPolicyIds();
        objectData.setRelationships();
        objectData.setRenditions();
        objectData.setExtensions();
        objectData.setChangeEventInfo();*/

        return eloCmisObject;
    }


    private void prepareCmisObjectProperties(final EloCmisObject eloCmisObject, final Object object, final DocVersion receivedDocumentVersion, final String filterStr) {
        if (eloCmisObject == null) {
            throw new IllegalArgumentException("ObjectData must not be null!");
        }
        EditInfo editInfo = null;
        Sord sord = null;
        DocVersion lastDocumentVersion = null;
        DocVersion docVersion = null;
        if (object instanceof EditInfo) {
            editInfo = (EditInfo) object;
            sord = editInfo.getSord();
            try {
                lastDocumentVersion = editInfo.getDocument().getDocs()[0];
            } catch (Exception e) {
                //do nothing
            }
            //if document version can not be retrieved from the editInfo try to get if from the sord
            if (lastDocumentVersion == null) {
                lastDocumentVersion = sord.getDocVersion();
            }
        } else if (object instanceof Sord) {
            editInfo = null;
            sord = (Sord) object;
            lastDocumentVersion = sord.getDocVersion();
        } else {
            throw new CmisNotSupportedException("Expecting editInfo or sord parameter.");
        }
        docVersion = (receivedDocumentVersion != null) ? receivedDocumentVersion : lastDocumentVersion;

        if (sord == null) {
            throw new IllegalArgumentException("Sord must not be null!");
        }

        PropertiesImpl properties = new PropertiesImpl();
        ObjectDataImpl objectData = eloCmisObject.getObjectData();
        ObjectInfoImpl objectInfo = eloCmisObject.getObjectInfo();

        // copy filter
        Set<String> filter = CmisUtils.splitFilter(filterStr);
        //Set<String> filter = (orgfilter == null ? null : new HashSet<String>(orgfilter));

        // find base type
        BaseTypeId baseTypeId = EloCmisTypeManager.getBaseTypeId(sord);

        if (baseTypeId.equals(BaseTypeId.CMIS_FOLDER)) {
            objectInfo.setContentType(null);
            objectInfo.setFileName(null);
            objectInfo.setHasAcl(true);
            objectInfo.setHasContent(false);
            objectInfo.setVersionSeriesId(null);
            objectInfo.setIsCurrentVersion(true);
            objectInfo.setRelationshipSourceIds(null);
            objectInfo.setRelationshipTargetIds(null);
            objectInfo.setRenditionInfos(null);
            objectInfo.setSupportsDescendants(true);
            objectInfo.setSupportsFolderTree(true);
            objectInfo.setSupportsPolicies(false);
            objectInfo.setSupportsRelationships(false);
            objectInfo.setWorkingCopyId(null);
            objectInfo.setWorkingCopyOriginalId(null);
        } else if (baseTypeId.equals(BaseTypeId.CMIS_DOCUMENT)) {
            objectInfo.setHasAcl(true);
            objectInfo.setHasContent(true);
            objectInfo.setHasParent(true);
            objectInfo.setVersionSeriesId(null);
            objectInfo.setIsCurrentVersion(true);
            objectInfo.setRelationshipSourceIds(null);
            objectInfo.setRelationshipTargetIds(null);
            objectInfo.setRenditionInfos(null);
            objectInfo.setSupportsDescendants(false);
            objectInfo.setSupportsFolderTree(false);
            objectInfo.setSupportsPolicies(false);
            objectInfo.setSupportsRelationships(false);
            objectInfo.setWorkingCopyId(null);
            objectInfo.setWorkingCopyOriginalId(null);
        }

        // let's prepare properties
        try {
            // id
            String id = EloUtilsService.calcObjectId(editInfo, sord, docVersion);
//            if (sord.getDocVersion() != null) {
//                id = EloCmisUtils.calcObjectId(String.valueOf(sord.getId()), String.valueOf(sord.getDoc()));
//            }
            addPropertyId(properties, baseTypeId.value(), filter, PropertyIds.OBJECT_ID, id);
            objectInfo.setId(id);

            // name
            String name = sord.getName();
            addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.NAME, name);
            objectInfo.setName(name);

            // base type id
            objectInfo.setBaseType(baseTypeId);
            addPropertyId(properties, baseTypeId.value(), filter, PropertyIds.BASE_TYPE_ID, baseTypeId.value());

            //type id
            String objectTypeId = EloCmisTypeManager.getTypeId(sord);
            objectInfo.setTypeId(objectTypeId);
            addPropertyId(properties, baseTypeId.value(), filter, PropertyIds.OBJECT_TYPE_ID, objectTypeId);

            // created and modified by
            String USER_UNKNOWN = "<unknown>";
            String createBy = sord.getOwnerName();
            objectInfo.setCreatedBy(createBy);
            addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.CREATED_BY, createBy);

            String updateBy = sord.getOwnerName();
            if (docVersion != null) {
                updateBy = docVersion.getOwnerName();
            }
            addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.LAST_MODIFIED_BY, updateBy);

            // creation and modification date
            String createDateStr = sord.getIDateIso();
            if (docVersion != null && docVersion.getCreateDateIso() != null && !docVersion.getCreateDateIso().isEmpty()) {
                createDateStr = docVersion.getCreateDateIso();
            }
            GregorianCalendar createDate = (GregorianCalendar) DateUtil.convertIso2Calendar(this.getCmisService().getConnection(), createDateStr);
            addPropertyDateTime(properties, baseTypeId.value(), filter, PropertyIds.CREATION_DATE, createDate);
            objectInfo.setCreationDate(createDate);

            String updateDateStr = sord.getIDateIso();
            if (docVersion != null && docVersion.getUpdateDateIso() != null && !docVersion.getUpdateDateIso().isEmpty()) {
                updateDateStr = docVersion.getUpdateDateIso();
            }
            GregorianCalendar updateDate = (GregorianCalendar) DateUtil.convertIso2Calendar(this.getCmisService().getConnection(), updateDateStr);
            addPropertyDateTime(properties, baseTypeId.value(), filter, PropertyIds.LAST_MODIFICATION_DATE, updateDate);
            objectInfo.setLastModificationDate(updateDate);

            // change token - always null
            addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.CHANGE_TOKEN, null);

            // CMIS 1.1 properties
            if (getCmisService().getCallContext().getCmisVersion() != CmisVersion.CMIS_1_0) {
                addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.DESCRIPTION, sord.getDesc());
                addPropertyIdList(properties, baseTypeId.value(), filter, PropertyIds.SECONDARY_OBJECT_TYPE_IDS, null);
            }


            if (baseTypeId.equals(BaseTypeId.CMIS_FOLDER)) {
                // base type and type name
                //String path = sord.getRefPaths()[0].getPathAsString() + (EloCmisUtils.isRootFolder(sord) ? "" : (EloCmisUtils.isRootFolder(sord.getParentId()) ? "" : EloCmisUtils.FOLDER_DELIMITER_ELO) + sord.getName());
                String path = EloUtilsService.getPathElo(sord);
                path = EloCmisUtils.convertEloPath2CmisPath(EloUtilsService.getPathElo(sord));
                addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.PATH, path);

//                // folder properties
                String parentId = EloUtilsService.isRootFolder(sord) ? null : String.valueOf(sord.getParentId());
                if (parentId != null) {
                    addPropertyId(properties, baseTypeId.value(), filter, PropertyIds.PARENT_ID, parentId);
                }
                objectInfo.setHasParent(parentId != null);

                addPropertyIdList(properties, baseTypeId.value(), filter, PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, null);
            } else if (baseTypeId.equals(BaseTypeId.CMIS_DOCUMENT)) {
                String path = EloCmisUtils.convertEloPath2CmisPath(EloUtilsService.getPathElo(sord, sord.getName()));
                //addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.PATH, path);

                addPropertyBoolean(properties, baseTypeId.value(), filter, PropertyIds.IS_IMMUTABLE, false);

                boolean isLatestVersion = (receivedDocumentVersion != null && lastDocumentVersion != null && receivedDocumentVersion.getId() == lastDocumentVersion.getId()) ? true : false;
                addPropertyBoolean(properties, baseTypeId.value(), filter, PropertyIds.IS_LATEST_VERSION, isLatestVersion);

                boolean isMajorVersion = (docVersion != null) && EloCmisUtils.isMajorVersion(docVersion.getVersion());
                addPropertyBoolean(properties, baseTypeId.value(), filter, PropertyIds.IS_MAJOR_VERSION, isMajorVersion);

                boolean isLatestMajorVersion = (isMajorVersion && receivedDocumentVersion != null && lastDocumentVersion != null && EloCmisUtils.getMajorVersion(receivedDocumentVersion.getVersion()).equals(EloCmisUtils.getMajorVersion(lastDocumentVersion.getVersion()))) ? true : false;
                addPropertyBoolean(properties, baseTypeId.value(), filter, PropertyIds.IS_LATEST_MAJOR_VERSION, isLatestMajorVersion);

                if (docVersion != null) {
                    addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.VERSION_LABEL, docVersion.getVersion());
                }

                addPropertyId(properties, baseTypeId.value(), filter, PropertyIds.VERSION_SERIES_ID, String.valueOf(sord.getId()));
                addPropertyBoolean(properties, baseTypeId.value(), filter, PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, false);
                addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null);
                addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null);
                if (docVersion != null) {
                    addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.CHECKIN_COMMENT, docVersion.getComment());
                }
                if (getCmisService().getCallContext().getCmisVersion() != CmisVersion.CMIS_1_0) {
                    addPropertyBoolean(properties, baseTypeId.value(), filter, PropertyIds.IS_PRIVATE_WORKING_COPY, false);
                }

                if (docVersion == null || docVersion.getSize() == 0) {
                    objectInfo.setHasContent(false);
                    addPropertyBigInteger(properties, baseTypeId.value(), filter, PropertyIds.CONTENT_STREAM_LENGTH, null);

                    objectInfo.setContentType(null);
                    addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.CONTENT_STREAM_MIME_TYPE, null);

                    objectInfo.setFileName(null);
                    addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.CONTENT_STREAM_FILE_NAME, null);
                } else {
                    objectInfo.setHasContent(true);
                    addPropertyLong(properties, baseTypeId.value(), filter, PropertyIds.CONTENT_STREAM_LENGTH, docVersion.getSize());

                    objectInfo.setContentType(docVersion.getContentType());
                    addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.CONTENT_STREAM_MIME_TYPE, docVersion.getContentType());

                    String fileName = calculateFileName(sord.getName(), docVersion.getExt(), docVersion.getContentType(), docVersion.getVersion());
                    objectInfo.setFileName(fileName);
                    addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.CONTENT_STREAM_FILE_NAME, fileName);
                }

                addPropertyId(properties, baseTypeId.value(), filter, PropertyIds.CONTENT_STREAM_ID, String.valueOf((docVersion != null) ? docVersion.getId() : null));
            }

            //add properties from elo masks
            addObjKeysElo2CmisProperties(properties, sord);

            objectData.setProperties(properties);

        } catch (CmisBaseException cbe) {
            throw cbe;
        } catch (Exception e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    private void addCmisProperties2ObjKeysElo(PropertiesImpl properties, Sord sord) {
        if (1 == 1) return;
        List<ObjKey> objKeyList = new ArrayList<>();
        try {
            String cmisTypeId = (String) properties.getProperties().get(PropertyIds.OBJECT_TYPE_ID).getValues().get(0);
            String maskId = String.valueOf(EloCmisTypeManager.getMaskId(getRepositoryId(), cmisTypeId));
            TypeDefinition typeDefinition = typeManager.getTypeDefinition(cmisTypeId);
            for (PropertyData propertyData : properties.getProperties().values()) {
                if (EloCmisTypeManager.isCustomProperty(propertyData.getId())) {
                    ObjKey objKey = new ObjKey();
                    int eloObjKeyId = EloCmisTypeManager.getObjKeyId(propertyData.getId());
                    objKey.setId(eloObjKeyId);
                    objKey.setName(propertyData.getDisplayName());
                    objKey.setData((String[]) propertyData.getValues().toArray());
                }
            }

            sord.setObjKeys((ObjKey[]) objKeyList.toArray());
        } catch (Exception e) {
            throw new CmisRuntimeException("Exception when executing addCmisProperties2ObjKeysElo for sord " + sord.getId() + ".", e);
        }
    }

    private Locale getServerDefaultLocale() {
        return EloUtilsConnection.getServerLocale(getCmisService().getCmisServiceParameters());
    }

    private void addObjKeysElo2CmisProperties(PropertiesImpl properties, Sord sord) {
        try {
            if (sord.getId() == 19) {
                System.out.println("***");
            }

            String cmisTypeId = EloCmisTypeManager.getTypeId(sord);
            String cmisPropertyId = null;
            TypeDefinition typeDefinition = typeManager.getTypeDefinition(cmisTypeId);
            if (typeDefinition == null) {
                System.out.println("***");
            }
            //IXConnection ixConnection = this.getCmisService().retrieveConnection();
            Locale locale = getServerDefaultLocale();
            for (ObjKey objKey : sord.getObjKeys()) {
                cmisPropertyId = EloCmisTypeManager.getPropertyId(objKey.getId());
                PropertyDefinition propertyDefinition = typeDefinition.getPropertyDefinitions().get(cmisPropertyId);
                PropertyData propertyData = null;
                if (propertyDefinition != null) {
                    if (propertyDefinition.getPropertyType().equals(PropertyType.DECIMAL)) {
                        if (objKey.getData() != null && objKey.getData().length > 0 && objKey.getData()[0] != null) {
//                          propertyData = new PropertyDecimalImpl(cmisPropertyId, BigDecimal.valueOf(Double.parseDouble(objKey.getData()[0])));
//                            Number objKeyData = EloUtilsService.getEloNumberValueAsDecimal(objKey.getData()[0]);
                            Number objKeyData = EloUtilsService.getEloNumberValueAsDecimal(locale, objKey.getData()[0]);
                            propertyData = new PropertyDecimalImpl(cmisPropertyId, new BigDecimal(objKeyData.toString()));
                        }
                    }
                    if (propertyDefinition.getPropertyType().equals(PropertyType.INTEGER)
                            && objKey.getData() != null && objKey.getData().length > 0 && objKey.getData()[0] != null) {
                        propertyData = new PropertyIntegerImpl(cmisPropertyId, BigInteger.valueOf(Integer.parseInt(objKey.getData()[0])));
                    }
                    if (propertyDefinition.getPropertyType().equals(PropertyType.STRING)
                            && objKey.getData() != null && objKey.getData().length > 0 && objKey.getData()[0] != null) {
                        propertyData = new PropertyStringImpl(cmisPropertyId, Arrays.asList(objKey.getData()));
                    }
                    if (propertyDefinition.getPropertyType().equals(PropertyType.DATETIME)
                            && objKey.getData() != null && objKey.getData().length > 0 && objKey.getData()[0] != null) {
                        GregorianCalendar gregorianCalendarDate = (GregorianCalendar) DateUtil.convertIso2Calendar(this.getCmisService().getConnection(), objKey.getData()[0]);
                        propertyData = new PropertyDateTimeImpl(cmisPropertyId, gregorianCalendarDate);
                    }
                    if (propertyData != null) {
                        MutablePropertyData mutablePropertyData = (MutablePropertyData) propertyData;
                        mutablePropertyData.setLocalName(propertyDefinition.getLocalName());
                        mutablePropertyData.setQueryName(propertyDefinition.getQueryName());
                        mutablePropertyData.setDisplayName(propertyDefinition.getDisplayName());
                        properties.addProperty(mutablePropertyData);
                    }
                }
            }
        } catch (Exception e) {
            throw new CmisRuntimeException("Exception when executing addObjKeysElo2CmisProperties for sord " + sord.getId() + ".", e);
        }
    }

//    private ObjectData prepareObjectData(EditInfo editInfo, String filter,
//                                         boolean includeAllowableActions,
//                                         IncludeRelationships includeRelationships,
//                                         String renditionFilter,
//                                         Boolean includePolicyIds,
//                                         boolean includeAcl) {
//        return prepareObjectData((Object) editInfo, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
//    }
//
//
//    private ObjectData prepareObjectData(Sord sord, String filter,
//                                         boolean includeAllowableActions,
//                                         IncludeRelationships includeRelationships,
//                                         String renditionFilter,
//                                         Boolean includePolicyIds,
//                                         boolean includeAcl) {
//        return prepareObjectData((Object) sord, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
//    }
//
//
//    private ObjectData prepareObjectData(Object object, String filter,
//                                         boolean includeAllowableActions,
//                                         IncludeRelationships includeRelationships,
//                                         String renditionFilter,
//                                         Boolean includePolicyIds,
//                                         boolean includeAcl) {
//        EloCmisObject eloCmisObject = new EloCmisObject();
//        prepareCmisObjectProperties(eloCmisObject, object, null, filter);
//
//        /*objectData.setAllowableActions();
//        objectData.setAcl();
//        objectData.setIsExactAcl();
//        objectData.setPolicyIds();
//        objectData.setRelationships();
//        objectData.setRenditions();
//        objectData.setExtensions();
//
//        objectData.setChangeEventInfo();*/
//
//
//        return eloCmisObject.getObjectData();
//    }

//    //TODO refactor all prepareObjectData to work with either Sord, EditInfo or DocVersion
//    private ObjectData prepareDocumentVersionData(int sordId, DocVersion docVersion, String filter, Boolean includeAllowableActions) {
//        EloCmisObject eloCmisObject = new EloCmisObject();
//
//        PropertiesImpl properties = new PropertiesImpl();
//        ObjectDataImpl objectData = eloCmisObject.getObjectData();
//        ObjectInfoImpl objectInfo = eloCmisObject.getObjectInfo();
//
//        String id = EloCmisUtils.calcObjectId(String.valueOf(sordId), String.valueOf(docVersion.getId()));
//        objectInfo.setId(id);
//        objectInfo.setCreatedBy(docVersion.getOwnerName());
//
//        objectInfo.setHasAcl(true);
//        objectInfo.setHasContent(true);
//        objectInfo.setHasParent(true);
//        objectInfo.setVersionSeriesId(null);
//        objectInfo.setIsCurrentVersion(true);
//        objectInfo.setRelationshipSourceIds(null);
//        objectInfo.setRelationshipTargetIds(null);
//        objectInfo.setRenditionInfos(null);
//        objectInfo.setSupportsDescendants(false);
//        objectInfo.setSupportsFolderTree(false);
//        objectInfo.setSupportsPolicies(false);
//        objectInfo.setSupportsRelationships(false);
//        objectInfo.setWorkingCopyId(null);
//        objectInfo.setWorkingCopyOriginalId(null);
//
//        // copy filter
//        Set<String> filterSet = CmisUtils.splitFilter(filter);
//
//        BaseTypeId baseTypeId = BaseTypeId.CMIS_DOCUMENT;
//
//        // identify if the file is a doc or a folder/directory
//        objectInfo.setBaseType(baseTypeId);
//        objectInfo.setTypeId(baseTypeId.value());
//
//        // id property
//        addPropertyId(properties, baseTypeId.value(), filterSet, PropertyIds.OBJECT_ID, id);
//        objectInfo.setId(id);
//
//        // created and modified by
//        String USER_UNKNOWN = "<unknown>";
//        //TODO - fill createdBy, updatedBy properties
//        addPropertyString(properties, baseTypeId.value(), filterSet, PropertyIds.CREATED_BY, docVersion.getOwnerName());
////        addPropertyString(properties, baseTypeId.value(), filter, PropertyIds.LAST_MODIFIED_BY, USER_UNKNOWN);
//        objectInfo.setCreatedBy(docVersion.getOwnerName());
//
//        // CMIS 1.1 properties
//        if (getCmisService().getCallContext().getCmisVersion() != CmisVersion.CMIS_1_0) {
//            addPropertyString(properties, baseTypeId.value(), filterSet, PropertyIds.DESCRIPTION, docVersion.getComment());
//            addPropertyIdList(properties, baseTypeId.value(), filterSet, PropertyIds.SECONDARY_OBJECT_TYPE_IDS, null);
//        }
//
//        // directory or file
//        addPropertyId(properties, baseTypeId.value(), filterSet, PropertyIds.BASE_TYPE_ID, baseTypeId.value());
//        addPropertyId(properties, baseTypeId.value(), filterSet, PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());  //todo convert
//
//        objectData.setProperties(properties);
//
//        return eloCmisObject.getObjectData();
//    }

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

    //DONE
    @Override
    public ObjectInFolderList getChildren(String folderId, String filter, String orderBy,
                                          Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
                                          Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount) {

        // prepare result
        ObjectInFolderListImpl objectInFolderList = new ObjectInFolderListImpl();
        objectInFolderList.setObjects(new ArrayList<ObjectInFolderData>());
        objectInFolderList.setHasMoreItems(false);
        int count = 0;

        // set defaults if values not set
        boolean iaa = CmisUtils.getBooleanParameter(includeAllowableActions, false);
        boolean ips = CmisUtils.getBooleanParameter(includePathSegment, false);
        Boolean includePolicyIds = false;
        boolean includeAcl = false;

        // get the folder
        IXConnection ixConnection;
        Sord sordParent = null;
        try {
            ixConnection = this.getCmisService().getConnection();
            sordParent = ixConnection.ix().checkoutSord(folderId, SordC.mbAll, LockC.NO);
            count = sordParent.getChildCount();
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }

        if (!EloCmisTypeManager.getBaseTypeId(sordParent).equals(BaseTypeId.CMIS_FOLDER)) {
            throw new CmisObjectNotFoundException("Not a folder!");
        }


        // set object info of the the folder
        if (getCmisService().getCallContext().isObjectInfoRequired()) {
            EloCmisObject eloCmisObject = prepareEloCmisObject(sordParent, null, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
            //ObjectData objectData = prepareObjectData(sordParent, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
        }

        // iterate through children
        // Prepare findInfo object
        FindInfo findInfo = new FindInfo();
        findInfo.setFindChildren(new FindChildren());
        findInfo.getFindChildren().setParentId(sordParent.getGuid());

        List<Sord> sordChildList = null;
        try {
            sordChildList = EloUtilsService.findSords(findInfo, maxItems, skipCount, ixConnection);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        List<Sord> sordCmisFilteredChildList = typeManager.filterCmisSords(sordChildList);
        for (Sord sordChild : sordCmisFilteredChildList) {

            // build and add child object
            ObjectInFolderDataImpl objectInFolder = new ObjectInFolderDataImpl();
            EloCmisObject eloCmisObject = prepareEloCmisObject(sordChild, null, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
            //ObjectData objectData = prepareObjectData(sordChild, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
            objectInFolder.setObject(eloCmisObject.getObjectData());
            if (ips) {
                objectInFolder.setPathSegment(sordChild.getName());
            }
            objectInFolderList.getObjects().add(objectInFolder);
        }

        if (maxItems != null && count >= maxItems.intValue()) {
            objectInFolderList.setHasMoreItems(true);
        }
        objectInFolderList.setNumItems(BigInteger.valueOf(count));
        return objectInFolderList;
    }

    //TODO
    //public List<ObjectInFolderContainer> getDescendants


    public ObjectData getFolderParent(String folderId, String filter) {
        List<ObjectParentData> parents = getObjectParents(folderId, filter, false, null, null, true);
        if (parents.size() == 0) {
            throw new CmisInvalidArgumentException("The root folder has no parent!");
        }
        return parents.get(0).getObject();
    }

    //TODO
    //public List<ObjectInFolderContainer> getFolderTree


    @Override
    public List<ObjectParentData> getObjectParents(String objectId, String filter,
                                                   Boolean includeAllowableActions, IncludeRelationships includeRelationships,
                                                   String renditionFilter, Boolean includeRelativePathSegment) {
        // split filter
        Set<String> filterCollection = FileBridgeUtils.splitFilter(filter);

        // set defaults if values not set
        boolean iaa = FileBridgeUtils.getBooleanParameter(includeAllowableActions, false);
        boolean irps = FileBridgeUtils.getBooleanParameter(includeRelativePathSegment, false);
        Boolean includePolicyIds = false;
        boolean includeAcl = false;

        //get object data
        IXConnection ixConnection = null;
        ObjectData objectData;
        Sord sord = null;
        try {
            ixConnection = this.getCmisService().getConnection();
            String documentId = EloUtilsService.getDocumentId(objectId);
            String sordId = EloUtilsService.getSordId(objectId);
            sord = ixConnection.ix().checkoutSord(sordId, SordC.mbAll, LockC.NO);
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }

        // don't climb above the root folder
        if (EloUtilsService.isRootFolder(sord)) {
            return Collections.emptyList();
        }

        // set object info of the the object
        if (getCmisService().getCallContext().isObjectInfoRequired()) {
            EloCmisObject eloCmisObject = prepareEloCmisObject(sord, null, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
            objectData = eloCmisObject.getObjectData();
            //objectData = prepareObjectData(sord, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
        }

        String parentObjectId = String.valueOf(sord.getParentId());
        Sord parentSord = null;
        try {
            parentSord = ixConnection.ix().checkoutSord(parentObjectId, SordC.mbAll, LockC.NO);
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }


        //ObjectData parentObjectData;
        //parentObjectData = prepareObjectData(parentSord, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);

        EloCmisObject eloCmisObject = prepareEloCmisObject(parentSord, null, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);

        ObjectParentDataImpl result = new ObjectParentDataImpl();
        result.setObject(eloCmisObject.getObjectData());
        if (irps) {
            //result.setRelativePathSegment(parentSord.getName());
            result.setRelativePathSegment(sord.getName());

        }

        return Collections.<ObjectParentData>singletonList(result);
    }


    // -----------------------------------------------------------------------------------------------------------------
    // REPOSITORY SERVICES
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    public TypeDefinition createType(TypeDefinition type) {
        return typeManager.createType(type);
    }

    @Override
    public void deleteType(String typeId) {
        typeManager.deleteType(typeId);
    }

    @Override
    public RepositoryInfo getRepositoryInfo() {
        return repositoryInfo;
    }

    @Override
    public TypeDefinitionList getTypeChildren(String typeId, Boolean includePropertyDefinitions, BigInteger maxItems, BigInteger skipCount) {
        return typeManager.getTypeChildren(typeId, includePropertyDefinitions, maxItems, skipCount);
    }

    @Override
    public TypeDefinition getTypeDefinition(String typeId) {
        return typeManager.getTypeDefinition(typeId);
    }

    @Override
    public List<TypeDefinitionContainer> getTypeDescendants(String typeId, BigInteger depth, Boolean includePropertyDefinitions) {
        return typeManager.getTypeDescendants(typeId, depth, includePropertyDefinitions);
    }

    @Override
    public TypeDefinition updateType(TypeDefinition type) {
        return typeManager.updateType(type);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // OBJECT SERVICES
    // -----------------------------------------------------------------------------------------------------------------

    //TODO
    //public abstract void appendContentStream(Holder<String> objectId, Holder<String> changeToken, ContentStream contentStream, boolean isLastChunk);

    //TODO
    //public abstract List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(List<BulkUpdateObjectIdAndChangeToken> objectIdAndChangeToken, Properties properties, List<String> addSecondaryTypeIds, List<String> removeSecondaryTypeIds);

    private int findObjKeyIndex(ObjKey[] objKeys, int id) {
        for (int index = 0; index < objKeys.length; index++) {
            if (objKeys[index].getId() == id) {
                return index;
            }
        }
        return -1;
    }

    private ObjKey[] updateProperties(Map<Integer, ObjKey> objKeyMap, Properties properties, String cmisTypeIdOrName) {
        for (String cmisPropertyIdOrName : properties.getProperties().keySet()) {
            if (EloCmisTypeManager.isCustomProperty(cmisPropertyIdOrName)) {
                String cmisPropertyId = EloCmisTypeManager.getPropertyId(getRepositoryId(), cmisTypeIdOrName, cmisPropertyIdOrName);
                int objKeyId = EloCmisTypeManager.getObjKeyId(cmisPropertyId);
                List<String> objKeyValue = new ArrayList<>();
                for (Object value : properties.getProperties().get(cmisPropertyId).getValues()) {
                    if (value != null) {
                        if (value instanceof GregorianCalendar) {
                            System.out.println(((GregorianCalendar) value).getTime());
                            objKeyValue.add(DateUtil.convertCalendar2Iso(this.getCmisService().getConnection(), (GregorianCalendar) value));
                        } else {
                            objKeyValue.add(String.valueOf(value));
                        }
                    }
                }
                if (objKeyValue.size() == 0) {
                    objKeyMap.remove(objKeyId);
                } else {
                    objKeyMap.get(objKeyId).setData((String[]) objKeyValue.toArray(new String[objKeyValue.size()]));
                }
            }
        }

        ObjKey[] objKey = (ObjKey[]) objKeyMap.values().toArray(new ObjKey[objKeyMap.size()]);
        Arrays.sort(objKey, new Comparator<ObjKey>() {
            @Override
            public int compare(ObjKey o1, ObjKey o2) {
                if (o1 == null || o2 == null) {
                    throw new NullPointerException("One of the compared objects is null.");
                }
                return (int) Math.signum(o1.getId() - o2.getId());
            }
        });

        return objKey;
    }

    @Override
    public String createDocument(Properties properties, String folderId, ContentStream contentStream, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces) {
        Sord sord = null;
        DocVersion docVersion = null;

        int objectId = -1;
        String eloPath = null;

        String sordName = (String) properties.getProperties().get(PropertyIds.NAME).getValues().get(0);
        if (!EloCmisUtils.isValidName(sordName)) {
            throw new CmisStorageException("Invalid sord file name '" + sordName + "'.");
        }

        String fileName = contentStream.getFileName();
        if (!EloCmisUtils.isValidName(fileName)) {
            throw new CmisStorageException("Invalid file name '" + fileName + "'.");
        }

        IXConnection ixConnection = this.getCmisService().getConnection();
        IXServicePortC CONST = null;
        EditInfo editInfo = null;
        try {
            CONST = ixConnection.getCONST();

            //check if folderid is folder type
            Sord parentSord = ixConnection.ix().checkoutSord(folderId, SordC.mbAll, LockC.NO);
            if (parentSord == null || !EloCmisTypeManager.getBaseTypeId(parentSord).equals(BaseTypeId.CMIS_FOLDER)) {
                throw new CmisObjectNotFoundException("Parent not found or is not a folder!");
            }

            String cmisTypeId = (String) properties.getProperties().get(PropertyIds.OBJECT_TYPE_ID).getValues().get(0);
            String maskId = String.valueOf(EloCmisTypeManager.getMaskId(getRepositoryId(), cmisTypeId));
            eloPath = EloUtilsService.getPathElo(parentSord, sordName);
            try {

                //editInfo = ixConnection.ix().checkoutSord(eloPath, EditInfoC.mbSord, LockC.NO);
                editInfo = ixConnection.ix().checkoutDoc(eloPath, null, EditInfoC.mbSordDocAtt, LockC.NO);
            } catch (RemoteException e) {
                IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
                if (ixError.code == 5023) {
                    try {
                        editInfo = ixConnection.ix().createDoc(folderId, maskId, null, EditInfoC.mbSordDocAtt);
                    } catch (RemoteException e1) {
                        throw new CmisRuntimeException(e1.getMessage(), e1);
                    }
                } else {
                    throw new CmisRuntimeException(e.getMessage(), e);
                }
            }
            sord = editInfo.getSord();
            sord.setName(sordName);

            Document document = null;
            document = editInfo.getDocument();
            if (document == null) {
                document = new Document();
            }

            // map properties to object
            Map<Integer, ObjKey> objKeyMap = new HashMap<>();
            for (ObjKey objKey : sord.getObjKeys()) {
                objKeyMap.put(objKey.getId(), objKey);
            }
            // Set original file name.
            if (!objKeyMap.containsKey(CONST.getDOC_MASK_LINE().getID_FILENAME())) {
                objKeyMap.put(CONST.getDOC_MASK_LINE().getID_FILENAME(), new ObjKey());
            }
            objKeyMap.get(CONST.getDOC_MASK_LINE().getID_FILENAME()).setId(CONST.getDOC_MASK_LINE().getID_FILENAME());
            objKeyMap.get(CONST.getDOC_MASK_LINE().getID_FILENAME()).setName(CONST.getDOC_MASK_LINE().getNAME_FILENAME());
            objKeyMap.get(CONST.getDOC_MASK_LINE().getID_FILENAME()).setData(new String[]{fileName});

            ObjKey[] objKeys = updateProperties(objKeyMap, properties, cmisTypeId);

            //set sord properties
            sord.setObjKeys(objKeys);


            // Supply the file extension in a DocVersion object.
            // Uses helper function from the IXClient.
            String currentVersion = null;
            if (document.getDocs() != null && document.getDocs().length > 0) {
                currentVersion = document.getDocs()[0].getVersion();
            }
            DocVersion dv = new DocVersion();
            dv.setExt(ixConnection.getFileExt(fileName));
            dv.setPathId(sord.getPath());
            document.setDocs(new DocVersion[]{dv});

            // Step 2: Let IX create a upload URL
            document = ixConnection.ix().checkinDocBegin(document);
            dv = document.getDocs()[0];

            // Step 3: Upload document. Use helper function of IXClient.
            String uploadResult = null;
            uploadResult = ixConnection.upload(dv.getUrl(), contentStream.getStream(), Integer.parseInt(String.valueOf(contentStream.getLength())), contentStream.getMimeType());

            // assign response to uploadResult - contains document ID
            dv.setUploadResult(uploadResult);

            // Step 4: Commit document.
            String version = EloCmisUtils.getNextVersion(currentVersion, versioningState);
            dv.setVersion(version);
            String versionComment = getPropertyString(properties, PropertyIds.CHECKIN_COMMENT);
            //(String) properties.getProperties().get(PropertyIds.CHECKIN_COMMENT).getValues().get(0);
            if (versionComment == null || versionComment.trim().length() == 0) {
                versionComment = "Version number " + version + "";
            }
            dv.setComment(versionComment);
            document = ixConnection.ix().checkinDocEnd(sord, SordC.mbAll, document, LockC.NO);

            docVersion = document.getDocs()[0];
            if (sord.getId() < 0) {
                editInfo = ixConnection.ix().checkoutDoc(eloPath, null, EditInfoC.mbSord, LockC.NO);
                sord = editInfo.getSord();
            }
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
        //return String.valueOf(sord.getId());
        //return String.valueOf(objectId);
        //return String.valueOf(sord.getId()) + ((objectId <= 0) ? "" : "-" + String.valueOf(objectId));
        return EloUtilsService.calcObjectId(sord, docVersion);
    }

    //TODO
    //public abstract String createDocumentFromSource(String sourceId, Properties properties, String folderId, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces);

    @Override
    public String createFolder(Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces) {
        Sord sord = null;
        int objectId = -1;

        String name = (String) properties.getProperties().get(PropertyIds.NAME).getValues().get(0);
        if (!EloCmisUtils.isValidName(name)) {
            throw new CmisStorageException("Invalid folder name '" + name + "'.");
        }

        IXConnection ixConnection = this.getCmisService().getConnection();
        IXServicePortC CONST = null;
        EditInfo editInfo = null;
        try {
            CONST = ixConnection.getCONST();

            //check if folderid is folder type
            Sord parentSord = ixConnection.ix().checkoutSord(folderId, SordC.mbAll, LockC.NO);
            if (parentSord == null || !EloCmisTypeManager.getBaseTypeId(parentSord).equals(BaseTypeId.CMIS_FOLDER)) {
                throw new CmisObjectNotFoundException("Parent not found or is not a folder!");
            }

            //check if the folder already exists
            boolean checkExists = EloCmisUtils.existsChildSord(parentSord, name, ixConnection);
            if (checkExists) {
                String checkCmisPath = EloCmisUtils.convertEloPath2CmisPath(EloUtilsService.getPathElo(parentSord, name));
                throw new CmisStorageException("Folder '" + checkCmisPath + "' already exists.");
            }

            String cmisTypeId = (String) properties.getProperties().get(PropertyIds.OBJECT_TYPE_ID).getValues().get(0);
            String maskId = String.valueOf(EloCmisTypeManager.getMaskId(getRepositoryId(), cmisTypeId));
            editInfo = ixConnection.ix().createSord(folderId, maskId, EditInfoC.mbSord);
            sord = editInfo.getSord();

            // Assign a name, "New Folder"
            sord.setName(name);


            // map properties to object
            Map<Integer, ObjKey> objKeyMap = new HashMap<>();
            for (ObjKey objKey : sord.getObjKeys()) {
                objKeyMap.put(objKey.getId(), objKey);
            }
            ObjKey[] objKeys = updateProperties(objKeyMap, properties, cmisTypeId);

            //set sord properties
            sord.setObjKeys(objKeys);

            //Store folder object in archive
            objectId = ixConnection.ix().checkinSord(sord, SordC.mbAll, LockC.NO);

        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
        return String.valueOf(objectId);
    }

    //public abstract String createItem(Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces);

    //public abstract String createPolicy(Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces);

    //public abstract String createRelationship(Properties properties, List<String> policies, Acl addAces, Acl removeAces);

    //public abstract void deleteContentStream(Holder<String> objectId, Holder<String> changeToken);

    public void deleteObject(String objectId, Boolean allVersions) {

        String documentId = EloUtilsService.getDocumentId(objectId);
        String sordId = EloUtilsService.getSordId(objectId);


        if (sordId == null || sordId.isEmpty()) {
            throw new CmisRuntimeException("ObjectId cannot be undefined or it couldn't be retrieved properly.");
        }

        IXConnection ixConnection = this.getCmisService().getConnection();
        try {
            //checkoutSord(ckeckPath, SordC.mbAll, LockC.NO);

            if (documentId.equals(EloUtilsService.DOCUMENT_ID_MISSING)) {
                //delete folder or delete all versions of a document
                Sord sord = ixConnection.ix().checkoutSord(sordId, SordC.mbAll, LockC.NO);
                String parentSordId = String.valueOf(sord.getParentId());
                deleteSord(ixConnection, sordId, parentSordId);
            } else if (!documentId.equals(EloUtilsService.DOCUMENT_ID_MISSING) && allVersions) {
                deleteDocument(ixConnection, sordId);
            } else {
                //delete a specific version of a document; the current working version of a document cannot be deleted
                deleteVersion(ixConnection, documentId);
            }
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    public void deleteSord(IXConnection ixConnection, String sordId, String parentSordId) throws RemoteException {
        // Delete folder containing sub-folder
        String parentId = null;
        //String parentId = String.valueOf(EloCmisUtils.FOLDER_ROOT_ID_ELO); //toplevel folder(archive)
        parentId = parentSordId;


        DeleteOptions delOpts = new DeleteOptions();
        delOpts.setDeleteFinally(true);
        //Delete logically
        ixConnection.ix().deleteSord(parentId, sordId, LockC.NO, null);
        // Delete permanently
        ixConnection.ix().deleteSord(parentId, sordId, LockC.NO, delOpts);
    }

    public void deleteDocument(IXConnection ixConnection, String sordId) throws RemoteException {
        // Delete folder containing sub-folder
        DeleteOptions delOpts = new DeleteOptions();
        delOpts.setDeleteFinally(true);
        //Delete logically
        ixConnection.ix().deleteSord(null, sordId, LockC.NO, null);
        // Delete permanently
        ixConnection.ix().deleteSord(null, sordId, LockC.NO, delOpts);
    }

    public void deleteVersion(IXConnection ixConnection, String documentId) throws RemoteException {
        // Checkout the version to be deleted
        EditInfo editInfo = ixConnection.ix().checkoutDoc(null, documentId, EditInfoC.mbSordDocAtt, LockC.YES);
        Sord sord = editInfo.getSord();
        Document doc = editInfo.getDocument();
        // Delete the retrieved version of the document
        DocVersion docVersion = doc.getDocs()[0];
        if (!docVersion.isWorkVersion()) {
            docVersion.setDeleted(true);
        } else {
            ixConnection.ix().checkinDocEnd(null, null, doc, LockC.NO);
            // Unlock using checkinSord
            ixConnection.ix().checkinSord(sord, SordC.mbOnlyUnlock, LockC.YES);
            throw new CmisRuntimeException("The working version of the document cannot be deleted!");
        }
        ixConnection.ix().checkinDocEnd(null, null, doc, LockC.NO);
        // Unlock using checkinSord
        ixConnection.ix().checkinSord(sord, SordC.mbOnlyUnlock, LockC.YES);
    }

    //public abstract FailedToDeleteData deleteTree(String folderId, Boolean allVersions, UnfileObject unfileObjects, Boolean continueOnFailure);

    //public abstract AllowableActions getAllowableActions(String objectId);


    public String calculateFileName(String fileName, String fileExtension, String mimeType, String fileVersion) {
        String fileBaseName = EloCmisUtils.getFileUtilsElo().getFileBaseName(EloCmisUtils.getFileUtilsElo().getFileName(fileName));
        if (fileExtension == null || fileExtension.isEmpty()) {
            fileExtension = EloCmisUtils.getFileUtilsElo().getFileExtension(EloCmisUtils.getFileUtilsElo().getFileName(fileName));
        }
        String fileExtFromMimeType = MimeTypes.getExtension(MimeTypes.getMIMEType(fileExtension));
        if ((!fileExtFromMimeType.equalsIgnoreCase(fileExtension)) && (MimeTypes.getMIMEType(fileExtension) == MimeTypes.getMIMEType(fileExtFromMimeType))) {
            fileExtension = fileExtFromMimeType;
        }
        if (fileExtension != null && fileExtension.startsWith(FileUtils.getExtensionDelimiter())) {
            fileExtension = fileExtension.substring(1);
        }
        return
                fileBaseName +
                        //((fileVersion == null || fileVersion.isEmpty()) ? "" : "[v" + fileVersion + "]") +
                        ((fileExtension == null || fileExtension.isEmpty()) ? "" : FileUtils.getExtensionDelimiter() + fileExtension);
    }

    @Override
    public ContentStream getContentStream(String objectId, String streamId, BigInteger offset, BigInteger length) {
        ContentStream contentStream = null;

        IXServicePortC CONST = null;
        IXConnection ixConnection;
        EditInfo editInfo = null;
        Sord sord;
        String url = null;


        if (streamId != null) {
            throw new CmisRuntimeException("StreamId not supported when getting object stream. StreamId = " + streamId + ".");
        }

        try {
            ixConnection = this.getCmisService().getConnection();
            String documentId = EloUtilsService.getDocumentId(objectId);
            String sordId = EloUtilsService.getSordId(objectId);

            if (streamId != null && !streamId.isEmpty()) {
                editInfo = ixConnection.ix().checkoutDoc(null, streamId, EditInfoC.mbSordDoc, LockC.NO);
            } else if (documentId.equals(EloUtilsService.DOCUMENT_ID_MISSING)) {
                editInfo = ixConnection.ix().checkoutSord(sordId, EditInfoC.mbSordDoc, LockC.NO);
            } else {
                editInfo = ixConnection.ix().checkoutDoc(null, documentId, EditInfoC.mbSordDoc, LockC.NO);
            }

            sord = editInfo.getSord();
            if (!EloCmisTypeManager.getBaseTypeId(sord).equals(BaseTypeId.CMIS_DOCUMENT)) {
                throw new CmisStreamNotSupportedException("The object from path " + EloCmisUtils.convertEloPath2CmisPath(EloUtilsService.getPathElo(sord)) + " is not a document.");
            }

            DocVersion docVersion = editInfo.getDocument().getDocs()[0];
            url = docVersion.getUrl();
            ContentStreamImpl contentStreamImpl = new ContentStreamImpl();

            String fileName = calculateFileName(sord.getName(), docVersion.getExt(), docVersion.getContentType(), docVersion.getVersion());

            contentStreamImpl.setMimeType(docVersion.getContentType());
            contentStreamImpl.setFileName(fileName);
            contentStreamImpl.setLength(length);
            contentStreamImpl.setStream(ixConnection.download(url, (offset == null ? 0 : offset.longValue()), (length == null ? -1 : length.longValue())));
            contentStream = contentStreamImpl;
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage() + "\nDownload url: " + url, e);
        }
        return contentStream;
    }

    @Override
    public ObjectData getObject(String objectId, String filter,
                                Boolean includeAllowableActions,
                                IncludeRelationships includeRelationships,
                                String renditionFilter,
                                Boolean includePolicyIds,
                                Boolean includeAcl) {
        ObjectData objectData = null;
        // check id
        if (objectId == null && filter == null) {
            throw new CmisInvalidArgumentException("Object Id   must be set.");
        }
        //get object data
        EditInfo editInfo = null;
        Sord sord = null;
        try {
            IXConnection ixConnection = this.getCmisService().getConnection();
            String documentId = EloUtilsService.getDocumentId(objectId);
            String sordId = EloUtilsService.getSordId(objectId);
            // having document id meanns we requesdted a specific version of the document
            if (documentId.equals(EloUtilsService.DOCUMENT_ID_MISSING)) {
                editInfo = ixConnection.ix().checkoutSord(sordId, EditInfoC.mbSordDoc, LockC.NO);
            } else {
                editInfo = ixConnection.ix().checkoutDoc(null, documentId, EditInfoC.mbSordDoc, LockC.NO);
            }
//            editInfo = ixConnection.ix().checkoutSord(objectId, ixConnection.getCONST().getEDIT_INFO().getMbSord(), LockC.NO);
            EloCmisObject eloCmisObject = prepareEloCmisObject(editInfo, null, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
            objectData = eloCmisObject.getObjectData();
            //objectData = prepareObjectData(editInfo, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return objectData;
    }

    @Override
    public ObjectData getObjectByPath(String path, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds, Boolean includeAcl) {
        ObjectData objectData = null;
        // check id
        if (path == null && filter == null) {
            throw new CmisInvalidArgumentException("Filter must be set.");
        }
        //convert cmisPath to eloPath
        String eloPath = EloCmisUtils.convertCmisPath2EloPath(path);

        //get object data
        EditInfo editInfo = null;
        Sord sord = null;
        try {
            IXConnection ixConnection = this.getCmisService().getConnection();
            editInfo = ixConnection.ix().checkoutSord(eloPath, EditInfoC.mbSordDoc, LockC.NO);

            sord = editInfo.getSord();
            EloCmisObject eloCmisObject = prepareEloCmisObject(editInfo, null, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
            objectData = eloCmisObject.getObjectData();
            //objectData = prepareObjectData(editInfo, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
        } catch (RemoteException e) {
            IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
            if (ixError.code == 5023) //cale incorecta
            {
                throw new CmisObjectNotFoundException(e.getMessage(), e);
            }
            throw new CmisRuntimeException(e.getMessage(), e);
        }
        return objectData;
    }

    //public abstract Properties getProperties(String objectId, String filter);

    //public abstract List<RenditionData> getRenditions(String objectId, String renditionFilter, BigInteger maxItems, BigInteger skipCount);

    @Override
    public void moveObject(Holder<String> objectId, String targetFolderId, String sourceFolderId) {
        if (objectId == null) {
            throw new CmisInvalidArgumentException("Object id is not valid!");
        }
        IXConnection ixConnection = this.getCmisService().getConnection();
        String sordId = EloUtilsService.getSordId(objectId.getValue());
        // MOVE r1 -> o2
        try {
            Sord objectSord = ixConnection.ix().checkoutSord(sordId, SordC.mbAll, LockC.NO);
            Sord targetSord = ixConnection.ix().checkoutSord(targetFolderId, SordC.mbAll, LockC.NO);

            ixConnection.ix().copySord(targetSord.getGuid(), objectSord.getGuid(), null, CopySordC.MOVE);
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage());
        }
    }

    @Override
    public void setContentStream(Holder<String> objectId, Boolean overwriteFlag, Holder<String> changeToken, ContentStream contentStream) {
        if (objectId == null) {
            throw new CmisInvalidArgumentException("Object id is not valid!");
        }
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public void updateProperties(Holder<String> objectId, Holder<String> changeToken, Properties properties) {

        IXConnection ixConnection = this.getCmisService().getConnection();
        String sordId = EloUtilsService.getSordId(objectId.getValue());

        try {
            Sord sord = ixConnection.ix().checkoutSord(sordId, SordC.mbAll, LockC.YES);
            String cmisTypeId = EloCmisTypeManager.getTypeId(sord);
            // map properties to object
            Map<Integer, ObjKey> objKeyMap = new HashMap<>();
            for (ObjKey objKey : sord.getObjKeys()) {
                objKeyMap.put(objKey.getId(), objKey);
            }
            ObjKey[] objKeys = updateProperties(objKeyMap, properties, cmisTypeId);
            sord.setObjKeys(objKeys);

            for (PropertyData propertyData : properties.getProperties().values()) {
                if (!EloCmisTypeManager.isCustomProperty(propertyData.getId())) {
                    if (propertyData.getId().equalsIgnoreCase(PropertyIds.NAME)) {
                        sord.setName(propertyData.getFirstValue().toString());
                    } else if (propertyData.getId().equalsIgnoreCase(PropertyIds.DESCRIPTION)) {
                        sord.setDesc(propertyData.getFirstValue().toString());
                    } else {
                        cancelCheckOut(objectId.getValue());
                        throw new CmisRuntimeException(String.format("Not supported! The property %s cannot be updated!", propertyData.getId()));
                    }
                }
            }
            ixConnection.ix().checkinSord(sord, SordC.mbAll, LockC.YES);

        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage());
        }
    }


    // -----------------------------------------------------------------------------------------------------------------
    // VERSIONING SERVICES
    // -----------------------------------------------------------------------------------------------------------------

    //DONE
    @Override
    public void cancelCheckOut(String objectId) {
        /* ELO: If the document is only to be unlocked, see checkinSord. */
        IXConnection ixConnection = this.getCmisService().getConnection();
        String documentId = EloUtilsService.getDocumentId(objectId);
        String sordId = EloUtilsService.getSordId(objectId);
        try {
            EditInfo editInfo = ixConnection.ix().checkoutSord(sordId, EditInfoC.mbSord, LockC.YES);
            ixConnection.ix().checkinSord(editInfo.getSord(), SordC.mbOnlyLock, LockC.YES);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /*
       * CMIS checkIn Checks-in the private working copy (PWC) document.
       * Creates new version of document
       * */
    @Override
    public void checkIn(Holder<String> objectId, Boolean major, Properties properties, ContentStream
            contentStream, String checkinComment, List<String> policies, Acl addAces, Acl removeAces) {
        IXConnection ixConnection = this.getCmisService().getConnection();
        EditInfo ed;
        try {

            String objectIdValue = objectId.getValue();
            String sordId = EloUtilsService.getSordId(objectIdValue);
            // Check document out
            ed = ixConnection.ix().checkoutDoc(sordId, null, EditInfoC.mbSordDocAtt, LockC.YES);
            Document doc = ed.getDocument();
            String fileName = contentStream.getFileName();
            String currentVersion = doc.getDocs()[0].getVersion();

            // Insert next version
            DocVersion dv = new DocVersion();
            dv.setExt(ixConnection.getFileExt(fileName));
            doc.setDocs(new DocVersion[]{dv});

            // The Index Server creates a URL where the document must be uploaded to
            doc = ixConnection.ix().checkinDocBegin(doc);
            dv = doc.getDocs()[0];

            // Upload document. Use helper function of IXClient.
            String uploadResult = ixConnection.upload(dv.getUrl(), contentStream.getStream(), Integer.parseInt(String.valueOf(contentStream.getLength())), contentStream.getMimeType());

            // Assign response to uploadResult - contains document ID
            dv.setUploadResult(uploadResult);

            // Commit document with checkinDocEnd method.
            // uploadResult contains the document information from the ELODM -
            // this must be passed to the Index Server
            dv.setVersion(EloCmisUtils.getNextVersion(currentVersion, major ? VersioningState.MAJOR : VersioningState.MINOR));
            dv.setComment(dv.getVersion() + " : " + checkinComment);
            Document newDocument = ixConnection.ix().checkinDocEnd(null, null, doc, LockC.YES);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /*
     * CMIS checkOut Creates a private working copy (PWC) of the document.
     *
     * After a successful checkOut operation is completed, and until such time when the PWC is deleted (via the cancelCheckOut service) or checked-in (via the checkIn service),
     * the eﬀects on the PWC or on other documents in the version series MUST be as follows:
       The repository MUST throw an exception if the checkOut service is invoked on any document in the version series. (I.e. there can only be one PWC for a version series at a time.)
       The value of the cmis:isVersionSeriesCheckedOut property MUST be TRUE.
       The value of the cmis:versionSeriesCheckedOutBy property SHOULD be set to a value indicating which user created the PWC. (The repository MAY still show the "not set" value for this property if, for example, the information is not available or the current user has not suﬃcient permissions.)
       The value of the cmis:versionSeriesCheckedOutId property SHOULD be set to the object id of the PWC. (The repository MAY still show the "not set" value for this property if the current user has no permissions to see the PWC).
       The repository MAY prevent operations that modify or delete the other documents in the version series.
     */
    //TODO in progress
    @Override
    public void checkOut(Holder<String> objectId, Holder<Boolean> contentCopied) {
        IXConnection ixConnection = this.getCmisService().getConnection();
        EditInfo ed;
        try {
            String objectIdValue = objectId.getValue();
            String sordId = EloUtilsService.getSordId(objectIdValue);
            /*Reads the indexing information and the download URL of a document from ELO. */
            ixConnection.ix().checkoutDoc(sordId, null, EditInfoC.mbSordDoc, LockC.YES);
            //todo create
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<ObjectData> getAllVersions(String objectId, String versionSeriesId, String filter, Boolean includeAllowableActions) {
        List<ObjectData> objectDataList = new ArrayList<>();
        // check id
        if (objectId == null && filter == null && versionSeriesId == null) {
            throw new CmisInvalidArgumentException("Either ObjectId or VersionSeriesId must be set.");
        }
        if (versionSeriesId == null && objectId == null) {
            throw new CmisInvalidArgumentException("Either ObjectId or VersionSeriesId must be set.");
        }

        //get object data
        EditInfo editInfo;
        ObjectData objectData;
        try {
            IXConnection ixConnection = this.getCmisService().getConnection();
            /* IXConnIXServicePortIF#checkoutDoc
            Reads the indexing information and the download URL of a document.
            At least one of the parameters objId and docId must be supplied. If only objId is supplied,
            the function reads the information for the current work version of the document.
            If docId is supplied, the function reads the information for a specific document or attachment version.
            All versions are returned if docId="-1" is supplied.
            The document member of the returned object contains the URL from where the document tempFile can be read.
            Use raw HTTP functions to download the tempFile. */
            //TODO De preferat sa se transmita docId si nu objectId la getContentStream, pentru a downloada ultima versiune
            String documentId = EloUtilsService.getDocumentId(objectId);
            String sordId = EloUtilsService.getSordId(objectId);
            editInfo = ixConnection.ix().checkoutDoc(sordId, "-1", EditInfoC.mbSordDocAtt, LockC.NO);
            Document document = editInfo.getDocument();
            DocVersion[] docVersions = document.getDocs();
            for (DocVersion docVersion : docVersions) {
                EloCmisObject eloCmisObject = prepareEloCmisObject(editInfo, docVersion, filter, includeAllowableActions, null, null, false, false);
                objectDataList.add(eloCmisObject.getObjectData());
                //objectDataList.add(prepareDocumentVersionData(editInfo.getSord().getId(), docVersion, filter, includeAllowableActions));
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return objectDataList;
    }

    //public abstract ObjectData getObjectOfLatestVersion(String objectId, String versionSeriesId, Boolean major, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds, Boolean includeAcl);

    //public abstract Properties getPropertiesOfLatestVersion(String objectId, String versionSeriesId, Boolean major, String filter);


    //    public Sord getSordByPath(String cmisPath) {
//        String eloPath = EloCmisUtils.convertCmisPath2EloPath(cmisPath);
//
//        //get object data
//        Sord sord = null;
//        try {
//            IXConnection ixConnection = this.getCmisService().retrieveConnection();
//            sord = ixConnection.ix().checkoutSord(eloPath, SordC.mbAll, LockC.NO);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//        return sord;
//    }


    /**
     * Sets read-only flag for the given user.
     */
//    public void setUserReadOnly(String user) {
//        if (user == null || user.length() == 0) {
//            return;
//        }
//
//        readWriteUserMap.put(user, true);
//    }

    /**
     * Sets read-write flag for the given user.
     */
//    public void setUserReadWrite(String user) {
//        if (user == null || user.length() == 0) {
//            return;
//        }
//
//        readWriteUserMap.put(user, false);
//    }

//    // --- CMIS operations ---
//
//    /**
//     * CMIS getRepositoryInfo.
//     */
//    public RepositoryInfo getRepositoryInfo() {
//        return this.repositoryInfo;
//    }

    /**
     * CMIS createType
     */


    /**
     * CMIS deleteType
     */

    /**
     * CMIS getTypesChildren.
     */


//    public TypeDefinitionList getTypeChildren(CallContext context,
//                                              String typeId, Boolean includePropertyDefinitions,
//                                              BigInteger maxItems, BigInteger skipCount) {
////        checkUser(context, false);
////
////        return typeManager.getTypeChildren(context, typeId,
////                includePropertyDefinitions, maxItems, skipCount);
//        return null;
//    }
//
//    /**
//     * CMIS getTypesDescendants.
//     */
//    public List<TypeDefinitionContainer> getTypeDescendants(
//            CallContext context, String typeId, BigInteger depth,
//            Boolean includePropertyDefinitions) {
////        checkUser(context, false);
////
////        return typeManager.getTypeDescendants(context, typeId, depth,
////                includePropertyDefinitions);
//        return null;
//    }

    /**
     * CMIS getTypeDefinition.
     */

//
//    /**
//     * Create* dispatch for AtomPub.
//     */
//    public ObjectData create(CallContext context, Properties properties,
//                             String folderId, ContentStream contentStream,
//                             VersioningState versioningState, ObjectInfoHandler objectInfos) {
//        boolean userReadOnly = checkUser(context, true);
//
//        String typeId = FileBridgeUtils.getObjectTypeId(properties);
//        TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
//        if (type == null) {
//            throw new CmisObjectNotFoundException("Type '" + typeId
//                    + "' is unknown!");
//        }
//
//        String objectId = null;
//        if (type.getBaseTypeId() == BaseTypeId.CMIS_DOCUMENT) {
//            objectId = createDocument(context, properties, folderId,
//                    contentStream, versioningState);
//        } else if (type.getBaseTypeId() == BaseTypeId.CMIS_FOLDER) {
//            objectId = createFolder(context, properties, folderId);
//        } else {
//            throw new CmisObjectNotFoundException(
//                    "Cannot create object of type '" + typeId + "'!");
//        }
//
//        return compileObjectData(context, getFile(objectId), null, false,
//                false, userReadOnly, objectInfos);
//    }

    //    /**
//     * CMIS createDocumentFromSource.
//     */
//    public String createDocumentFromSource(CallContext context,
//                                           String sourceId, Properties properties, String folderId,
//                                           VersioningState versioningState) {
//        checkUser(context, true);
//
//        // check versioning state
//        if (VersioningState.NONE != versioningState) {
//            throw new CmisConstraintException("Versioning not supported!");
//        }
//
//        // get parent File
//        File parent = getFile(folderId);
//        if (!parent.isDirectory()) {
//            throw new CmisObjectNotFoundException("Parent is not a folder!");
//        }
//
//        // get source File
//        File source = getFile(sourceId);
//        if (!source.isFile()) {
//            throw new CmisObjectNotFoundException("Source is not a document!");
//        }
//
//        // check properties
//        checkCopyProperties(properties, BaseTypeId.CMIS_DOCUMENT.value());
//
//        // check the name
//        String name = null;
//        if (properties != null && properties.getProperties() != null) {
//            name = FileBridgeUtils.getStringProperty(properties,
//                    PropertyIds.NAME);
//        }
//        if (name == null) {
//            name = source.getName();
//        }
//
//        File newFile = new File(parent, name);
//        if (newFile.exists()) {
//            throw new CmisNameConstraintViolationException(
//                    "Document already exists.");
//        }
//
//        // create the file
//        try {
//            newFile.createNewFile();
//        } catch (IOException e) {
//            throw new CmisStorageException("Could not create file: "
//                    + e.getMessage(), e);
//        }
//
//        // copy content
//        try {
//            writeContent(newFile, new FileInputStream(source));
//        } catch (IOException e) {
//            throw new CmisStorageException("Could not roead or write content: "
//                    + e.getMessage(), e);
//        }
//
//        return getId(newFile);
//    }
//
//    /**
//     * Writes the content to disc.
//     */
//    private void writeContent(File newFile, InputStream stream) {
//        OutputStream out = null;
//        InputStream in = null;
//        try {
//            out = new BufferedOutputStream(new FileOutputStream(newFile),
//                    BUFFER_SIZE);
//            in = new BufferedInputStream(stream, BUFFER_SIZE);
//
//            byte[] buffer = new byte[BUFFER_SIZE];
//            int b;
//            while ((b = in.read(buffer)) > -1) {
//                out.write(buffer, 0, b);
//            }
//
//            out.flush();
//        } catch (IOException e) {
//            throw new CmisStorageException("Could not write content: "
//                    + e.getMessage(), e);
//        } finally {
//            IOUtils.closeQuietly(out);
//            IOUtils.closeQuietly(in);
//        }
//    }
//


    /**
     * CMIS createFolder.
     */


//    /**
//     * CMIS moveObject.
//     */
//    public ObjectData moveObject(CallContext context, Holder<String> objectId,
//                                 String targetFolderId, ObjectInfoHandler objectInfos) {
//        boolean userReadOnly = checkUser(context, true);
//
//        if (objectId == null) {
//            throw new CmisInvalidArgumentException("Id is not valid!");
//        }
//
//        // get the file and parent
//        File file = getFile(objectId.getValue());
//        File parent = getFile(targetFolderId);
//
//        // build new path
//        File newFile = new File(parent, file.getName());
//        if (newFile.exists()) {
//            throw new CmisStorageException("Object already exists!");
//        }
//
//        // move it
//        if (!file.renameTo(newFile)) {
//            throw new CmisStorageException("Move failed!");
//        } else {
//            // set new id
//            objectId.setValue(getId(newFile));
//        }
//
//        return compileObjectData(context, newFile, null, false, false,
//                userReadOnly, objectInfos);
//    }
//
//    /**
//     * CMIS setContentStream, deleteContentStream, and appendContentStream.
//     */
//    public void changeContentStream(CallContext context,
//                                    Holder<String> objectId, Boolean overwriteFlag,
//                                    ContentStream contentStream, boolean append) {
//        checkUser(context, true);
//
//        if (objectId == null) {
//            throw new CmisInvalidArgumentException("Id is not valid!");
//        }
//
//        // get the file
//        File file = getFile(objectId.getValue());
//        if (!file.isFile()) {
//            throw new CmisStreamNotSupportedException("Not a file!");
//        }
//
//        // check overwrite
//        boolean owf = FileBridgeUtils.getBooleanParameter(overwriteFlag, true);
//        if (!owf && file.length() > 0) {
//            throw new CmisContentAlreadyExistsException(
//                    "Content already exists!");
//        }
//
//        OutputStream out = null;
//        InputStream in = null;
//        try {
//            out = new BufferedOutputStream(new FileOutputStream(file, append),
//                    BUFFER_SIZE);
//
//            if (contentStream == null || contentStream.getStream() == null) {
//                // delete content
//                out.write(new byte[0]);
//            } else {
//                // set content
//                in = new BufferedInputStream(contentStream.getStream(),
//                        BUFFER_SIZE);
//
//                byte[] buffer = new byte[BUFFER_SIZE];
//                int b;
//                while ((b = in.read(buffer)) > -1) {
//                    out.write(buffer, 0, b);
//                }
//            }
//        } catch (Exception e) {
//            throw new CmisStorageException("Could not write content: "
//                    + e.getMessage(), e);
//        } finally {
//            IOUtils.closeQuietly(out);
//            IOUtils.closeQuietly(in);
//        }
//    }
//
//    /**
//     * CMIS deleteObject.
//     */
//    public void deleteObject(CallContext context, String objectId) {
//        checkUser(context, true);
//
//        // get the file or folder
//        File file = getFile(objectId);
//        if (!file.exists()) {
//            throw new CmisObjectNotFoundException("Object not found!");
//        }
//
//        // check if it is a folder and if it is empty
//        if (!isFolderEmpty(file)) {
//            throw new CmisConstraintException("Folder is not empty!");
//        }
//
//        // delete file
//        if (!file.delete()) {
//            throw new CmisStorageException("Deletion failed!");
//        }
//    }
//
//    /**
//     * CMIS deleteTree.
//     */
//    public FailedToDeleteData deleteTree(CallContext context, String folderId,
//                                         Boolean continueOnFailure) {
//        checkUser(context, true);
//
//        boolean cof = FileBridgeUtils.getBooleanParameter(continueOnFailure,
//                false);
//
//        // get the file or folder
//        File file = getFile(folderId);
//
//        FailedToDeleteDataImpl result = new FailedToDeleteDataImpl();
//        result.setIds(new ArrayList<String>());
//
//        // if it is a folder, remove it recursively
//        if (file.isDirectory()) {
//            deleteFolder(file, cof, result);
//        } else {
//            throw new CmisConstraintException("Object is not a folder!");
//        }
//
//        return result;
//    }
//
//    /**
//     * Removes a folder and its content.
//     */
//    private boolean deleteFolder(File folder, boolean continueOnFailure,
//                                 FailedToDeleteDataImpl ftd) {
//        boolean success = true;
//
//        for (File file : folder.listFiles()) {
//            if (file.isDirectory()) {
//                if (!deleteFolder(file, continueOnFailure, ftd)) {
//                    if (!continueOnFailure) {
//                        return false;
//                    }
//                    success = false;
//                }
//            } else {
//                if (!file.delete()) {
//                    ftd.getIds().add(getId(file));
//                    if (!continueOnFailure) {
//                        return false;
//                    }
//                    success = false;
//                }
//            }
//        }
//
//        if (!folder.delete()) {
//            ftd.getIds().add(getId(folder));
//            success = false;
//        }
//
//        return success;
//    }
//
//    /**
//     * CMIS updateProperties.
//     */
//    public ObjectData updateProperties(CallContext context,
//                                       Holder<String> objectId, Properties properties,
//                                       ObjectInfoHandler objectInfos) {
//        boolean userReadOnly = checkUser(context, true);
//
//        // check object id
//        if (objectId == null || objectId.getValue() == null) {
//            throw new CmisInvalidArgumentException("Id is not valid!");
//        }
//
//        // get the file or folder
//        File file = getFile(objectId.getValue());
//
//        // check the properties
//        String typeId = (file.isDirectory() ? BaseTypeId.CMIS_FOLDER.value()
//                : BaseTypeId.CMIS_DOCUMENT.value());
//        checkUpdateProperties(properties, typeId);
//
//        // get and check the new name
//        String newName = FileBridgeUtils.getStringProperty(properties,
//                PropertyIds.NAME);
//        boolean isRename = (newName != null)
//                && (!file.getName().equals(newName));
//        if (isRename && !isValidName(newName)) {
//            throw new CmisNameConstraintViolationException("Name is not valid!");
//        }
//
//        // rename file or folder if necessary
//        File newFile = file;
//        if (isRename) {
//            File parent = file.getParentFile();
//            newFile = new File(parent, newName);
//            if (!file.renameTo(newFile)) {
//                // if something went wrong, throw an exception
//                throw new CmisUpdateConflictException(
//                        "Could not rename object!");
//            } else {
//                // set new id
//                objectId.setValue(getId(newFile));
//            }
//        }
//
//        return compileObjectData(context, newFile, null, false, false,
//                userReadOnly, objectInfos);
//    }
//
//    /**
//     * CMIS bulkUpdateProperties.
//     */
//    public List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(
//            CallContext context,
//            List<BulkUpdateObjectIdAndChangeToken> objectIdAndChangeToken,
//            Properties properties, ObjectInfoHandler objectInfos) {
//        checkUser(context, true);
//
//        if (objectIdAndChangeToken == null) {
//            throw new CmisInvalidArgumentException("No object ids provided!");
//        }
//
//        List<BulkUpdateObjectIdAndChangeToken> result = new ArrayList<BulkUpdateObjectIdAndChangeToken>();
//
//        for (BulkUpdateObjectIdAndChangeToken oid : objectIdAndChangeToken) {
//            if (oid == null) {
//                // ignore invalid ids
//                continue;
//            }
//            try {
//                Holder<String> oidHolder = new Holder<String>(oid.getId());
//                updateProperties(context, oidHolder, properties, objectInfos);
//
//                result.add(new BulkUpdateObjectIdAndChangeTokenImpl(
//                        oid.getId(), oidHolder.getValue(), null));
//            } catch (CmisBaseException e) {
//                // ignore exceptions - see specification
//            }
//        }
//
//        return result;
//    }
//
//    /**
//     * CMIS getObject.
//     */
//    public ObjectData getObject(CallContext context, String objectId,
//                                String versionServicesId, String filter,
//                                Boolean includeAllowableActions, Boolean includeAcl,
//                                ObjectInfoHandler objectInfos) {
//        boolean userReadOnly = checkUser(context, false);
//
//        // check id
//        if (objectId == null && versionServicesId == null) {
//            throw new CmisInvalidArgumentException("Object Id must be set.");
//        }
//
//        if (objectId == null) {
//            // this works only because there are no versions in a file system
//            // and the object id and version series id are the same
//            objectId = versionServicesId;
//        }
//
//        // get the file or folder
//        File file = getFile(objectId);
//
//        // set defaults if values not set
//        boolean iaa = FileBridgeUtils.getBooleanParameter(
//                includeAllowableActions, false);
//        boolean iacl = FileBridgeUtils.getBooleanParameter(includeAcl, false);
//
//        // split filter
//        Set<String> filterCollection = FileBridgeUtils.splitFilter(filter);
//
//        // gather properties
//        return compileObjectData(context, file, filterCollection, iaa, iacl,
//                userReadOnly, objectInfos);
//    }
//
//    /**
//     * CMIS getAllowableActions.
//     */
//    public AllowableActions getAllowableActions(CallContext context,
//                                                String objectId) {
//        boolean userReadOnly = checkUser(context, false);
//
//        // get the file or folder
//        File file = getFile(objectId);
//        if (!file.exists()) {
//            throw new CmisObjectNotFoundException("Object not found!");
//        }
//
//        return compileAllowableActions(file, userReadOnly);
//    }
//
//    /**
//     * CMIS getACL.
//     */
//    public Acl getAcl(CallContext context, String objectId) {
//        checkUser(context, false);
//
//        // get the file or folder
//        File file = getFile(objectId);
//        if (!file.exists()) {
//            throw new CmisObjectNotFoundException("Object not found!");
//        }
//
//        return compileAcl(file);
//    }

    /**
     * CMIS getContentStream.
     */


    /**
     * CMIS setContentStream, deleteContentStream, and appendContentStream.
     */
//    public void changeContentStream(Holder<String> objectId, Boolean overwriteFlag, Holder<String> changeToken,
//                                    ContentStream contentStream, boolean append) {
////        checkUser(context, true);
//
//        if (objectId == null) {
//            throw new CmisInvalidArgumentException("Object id is not valid!");
//        }
//
//        // TODO
//    }


    /**
     * CMIS getChildren.
     */


//
//    /**
//     * CMIS getDescendants.
//     */
//    public List<ObjectInFolderContainer> getDescendants(CallContext context,
//                                                        String folderId, BigInteger depth, String filter,
//                                                        Boolean includeAllowableActions, Boolean includePathSegment,
//                                                        ObjectInfoHandler objectInfos, boolean foldersOnly) {
//        boolean userReadOnly = checkUser(context, false);
//
//        // check depth
//        int d = (depth == null ? 2 : depth.intValue());
//        if (d == 0) {
//            throw new CmisInvalidArgumentException("Depth must not be 0!");
//        }
//        if (d < -1) {
//            d = -1;
//        }
//
//        // split filter
//        Set<String> filterCollection = FileBridgeUtils.splitFilter(filter);
//
//        // set defaults if values not set
//        boolean iaa = FileBridgeUtils.getBooleanParameter(
//                includeAllowableActions, false);
//        boolean ips = FileBridgeUtils.getBooleanParameter(includePathSegment,
//                false);
//
//        // get the folder
//        File folder = getFile(folderId);
//        if (!folder.isDirectory()) {
//            throw new CmisObjectNotFoundException("Not a folder!");
//        }
//
//        // set object info of the the folder
//        if (context.isObjectInfoRequired()) {
//            compileObjectData(context, folder, null, false, false,
//                    userReadOnly, objectInfos);
//        }
//
//        // get the tree
//        List<ObjectInFolderContainer> result = new ArrayList<ObjectInFolderContainer>();
//        gatherDescendants(context, folder, result, foldersOnly, d,
//                filterCollection, iaa, ips, userReadOnly, objectInfos);
//
//        return result;
//    }
//
//    /**
//     * Gather the children of a folder.
//     */
//    private void gatherDescendants(CallContext context, File folder,
//                                   List<ObjectInFolderContainer> list, boolean foldersOnly, int depth,
//                                   Set<String> filter, boolean includeAllowableActions,
//                                   boolean includePathSegments, boolean userReadOnly,
//                                   ObjectInfoHandler objectInfos) {
//        assert folder != null;
//        assert list != null;
//
//        // iterate through children
//        for (File child : folder.listFiles()) {
//            // skip hidden and shadow files
//            if (child.isHidden()) {
//                continue;
//            }
//
//            // folders only?
//            if (foldersOnly && !child.isDirectory()) {
//                continue;
//            }
//
//            // add to list
//            ObjectInFolderDataImpl objectInFolder = new ObjectInFolderDataImpl();
//            objectInFolder.setObject(compileObjectData(context, child, filter,
//                    includeAllowableActions, false, userReadOnly, objectInfos));
//            if (includePathSegments) {
//                objectInFolder.setPathSegment(child.getName());
//            }
//
//            ObjectInFolderContainerImpl container = new ObjectInFolderContainerImpl();
//            container.setObject(objectInFolder);
//
//            list.add(container);
//
//            // move to next level
//            if (depth != 1 && child.isDirectory()) {
//                container.setChildren(new ArrayList<ObjectInFolderContainer>());
//                gatherDescendants(context, child, container.getChildren(),
//                        foldersOnly, depth - 1, filter,
//                        includeAllowableActions, includePathSegments,
//                        userReadOnly, objectInfos);
//            }
//        }
//    }
//

    /**
     * CMIS getFolderParent.
     */


    /**
     * CMIS getObjectParents.
     */


//    /**
//     * CMIS getObjectByPath.
//     */
//    public ObjectData getObjectByPath(CallContext context, String path, String filter,
//                                      Boolean includeAllowableActions, IncludeRelationships includeRelationships,
//                                      String renditionFilter, Boolean includePolicyIds, Boolean includeAcl) {
//        EditInfo ed2 = null;
//        Sord sordResult;
//        IXConnection ixConnection = this.getCmisService().retrieveConnection();
//        // check path
//        if (path == null || path.length() == 0
//                || path.charAt(0) != '/') {
//            throw new CmisInvalidArgumentException("Invalid path!");
//        }
//        if (path.length() == 1) {
//            try {
//                ed2 = ixConnection.ix().checkoutSord(FOLDER_ROOT, EditInfoC.mbSord, LockC.NO);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//            if (ed2 != null) {
//                sordResult = ed2.getSord();
//            } else {
//                throw new CmisInvalidArgumentException("Invalid ROOT path!");
//            }
//        } else {
//            try {
//                ed2 = ixConnection.ix().checkoutSord(FOLDER_ROOT + path, EditInfoC.mbSord, LockC.NO);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//            if (ed2 != null) {
//                sordResult = ed2.getSord();
//            } else {
//                throw new CmisInvalidArgumentException("Invalid path!");
//            }
//        }
//        return prepareObjectData(sordResult, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl);
//    }

//    public ObjectData getObjectByPath(CallContext context, String folderPath,
//                                      String filter, boolean includeAllowableActions, boolean includeACL,
//                                      ObjectInfoHandler objectInfos) {
//        boolean userReadOnly = checkUser(context, false);
//
//        // split filter
//        Set<String> filterCollection = FileBridgeUtils.splitFilter(filter);
//
//        // check path
//        if (folderPath == null || folderPath.length() == 0
//                || folderPath.charAt(0) != '/') {
//            throw new CmisInvalidArgumentException("Invalid folder path!");
//        }
//
//        // get the file or folder
//        File file = null;
//        if (folderPath.length() == 1) {
//            file = root;
//        } else {
//            String path = folderPath.replace('/', File.separatorChar)
//                    .substring(1);
//            file = new File(root, path);
//        }
//
//        if (!file.exists()) {
//            throw new CmisObjectNotFoundException("Path doesn't exist.");
//        }
//
//        return compileObjectData(context, file, filterCollection,
//                includeAllowableActions, includeACL, userReadOnly, objectInfos);
//    }
//
//    /**
//     * CMIS query (simple IN_FOLDER queries only)
//     */
//    public ObjectList query(CallContext context, String statement,
//                            Boolean includeAllowableActions, BigInteger maxItems,
//                            BigInteger skipCount, ObjectInfoHandler objectInfos) {
//        boolean userReadOnly = checkUser(context, false);
//
//        Matcher matcher = IN_FOLDER_QUERY_PATTERN.matcher(statement.trim());
//
//        if (!matcher.matches()) {
//            throw new CmisInvalidArgumentException(
//                    "Invalid or unsupported query.");
//        }
//
//        String typeId = matcher.group(1);
//        String folderId = matcher.group(2);
//
//        TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
//        if (type == null) {
//            throw new CmisInvalidArgumentException("Unknown type.");
//        }
//
//        boolean queryFiles = (type.getBaseTypeId() == BaseTypeId.CMIS_DOCUMENT);
//
//        if (folderId.length() == 0) {
//            throw new CmisInvalidArgumentException("Invalid folder id.");
//        }
//
//        // set defaults if values not set
//        boolean iaa = FileBridgeUtils.getBooleanParameter(
//                includeAllowableActions, false);
//
//        // skip and max
//        int skip = (skipCount == null ? 0 : skipCount.intValue());
//        if (skip < 0) {
//            skip = 0;
//        }
//
//        int max = (maxItems == null ? Integer.MAX_VALUE : maxItems.intValue());
//        if (max < 0) {
//            max = Integer.MAX_VALUE;
//        }
//
//        // get the folder
//        File folder = getFile(folderId);
//        if (!folder.isDirectory()) {
//            throw new CmisInvalidArgumentException("Not a folder!");
//        }
//
//        // prepare result
//        ObjectListImpl result = new ObjectListImpl();
//        result.setObjects(new ArrayList<ObjectData>());
//        result.setHasMoreItems(false);
//        int count = 0;
//
//        // iterate through children
//        for (File hit : folder.listFiles()) {
//            // skip hidden files
//            if (hit.isHidden()) {
//                continue;
//            }
//
//            // skip directory if documents are requested
//            if (hit.isDirectory() && queryFiles) {
//                continue;
//            }
//
//            // skip files if folders are requested
//            if (hit.isFile() && !queryFiles) {
//                continue;
//            }
//
//            count++;
//
//            if (skip > 0) {
//                skip--;
//                continue;
//            }
//
//            if (result.getObjects().size() >= max) {
//                result.setHasMoreItems(true);
//                continue;
//            }
//
//            // build and add child object
//            ObjectData object = compileObjectData(context, hit, null, iaa,
//                    false, userReadOnly, objectInfos);
//
//            // set query names
//            for (PropertyData<?> prop : object.getProperties()
//                    .getPropertyList()) {
//                ((MutablePropertyData<?>) prop).setQueryName(type
//                        .getPropertyDefinitions().get(prop.getId())
//                        .getQueryName());
//            }
//
//            result.getObjects().add(object);
//        }
//
//        result.setNumItems(BigInteger.valueOf(count));
//
//        return result;
//    }
//
//    // --- helpers ---
//
//    /**
//     * Compiles an object type object from a file or folder.
//     */
//    private ObjectData compileObjectData(CallContext context, File file,
//                                         Set<String> filter, boolean includeAllowableActions,
//                                         boolean includeAcl, boolean userReadOnly,
//                                         ObjectInfoHandler objectInfos) {
//        ObjectDataImpl result = new ObjectDataImpl();
//        ObjectInfoImpl objectInfo = new ObjectInfoImpl();
//
//        result.setProperties(compileProperties(context, file, filter,
//                objectInfo));
//
//        if (includeAllowableActions) {
//            result.setAllowableActions(compileAllowableActions(file,
//                    userReadOnly));
//        }
//
//        if (includeAcl) {
//            result.setAcl(compileAcl(file));
//            result.setIsExactAcl(true);
//        }
//
//        if (context.isObjectInfoRequired()) {
//            objectInfo.setObject(result);
//            objectInfos.addObjectInfo(objectInfo);
//        }
//
//        return result;
//    }
//
//    /**
//     * Gathers all base properties of a file or folder.
//     */
//    private Properties compileProperties(CallContext context, File file,
//                                         Set<String> orgfilter, ObjectInfoImpl objectInfo) {
//        if (file == null) {
//            throw new IllegalArgumentException("File must not be null!");
//        }
//
//        // we can't gather properties if the file or folder doesn't exist
//        if (!file.exists()) {
//            throw new CmisObjectNotFoundException("Object not found!");
//        }
//
//        // copy filter
//        Set<String> filter = (orgfilter == null ? null : new HashSet<String>(
//                orgfilter));
//
//        // find base type
//        String typeId = null;
//
//        // identify if the file is a doc or a folder/directory
//        if (file.isDirectory()) {
//            typeId = BaseTypeId.CMIS_FOLDER.value();
//            objectInfo.setBaseType(BaseTypeId.CMIS_FOLDER);
//            objectInfo.setTypeId(typeId);
//            objectInfo.setContentType(null);
//            objectInfo.setFileName(null);
//            objectInfo.setHasAcl(true);
//            objectInfo.setHasContent(false);
//            objectInfo.setVersionSeriesId(null);
//            objectInfo.setIsCurrentVersion(true);
//            objectInfo.setRelationshipSourceIds(null);
//            objectInfo.setRelationshipTargetIds(null);
//            objectInfo.setRenditionInfos(null);
//            objectInfo.setSupportsDescendants(true);
//            objectInfo.setSupportsFolderTree(true);
//            objectInfo.setSupportsPolicies(false);
//            objectInfo.setSupportsRelationships(false);
//            objectInfo.setWorkingCopyId(null);
//            objectInfo.setWorkingCopyOriginalId(null);
//        } else {
//            typeId = BaseTypeId.CMIS_DOCUMENT.value();
//            objectInfo.setBaseType(BaseTypeId.CMIS_DOCUMENT);
//            objectInfo.setTypeId(typeId);
//            objectInfo.setHasAcl(true);
//            objectInfo.setHasContent(true);
//            objectInfo.setHasParent(true);
//            objectInfo.setVersionSeriesId(null);
//            objectInfo.setIsCurrentVersion(true);
//            objectInfo.setRelationshipSourceIds(null);
//            objectInfo.setRelationshipTargetIds(null);
//            objectInfo.setRenditionInfos(null);
//            objectInfo.setSupportsDescendants(false);
//            objectInfo.setSupportsFolderTree(false);
//            objectInfo.setSupportsPolicies(false);
//            objectInfo.setSupportsRelationships(false);
//            objectInfo.setWorkingCopyId(null);
//            objectInfo.setWorkingCopyOriginalId(null);
//        }
//
//        // let's do it
//        try {
//            PropertiesImpl result = new PropertiesImpl();
//
//            // id
//            String id = fileToId(file);
//            addPropertyId(result, typeId, filter, PropertyIds.OBJECT_ID, id);
//            objectInfo.setId(id);
//
//            // name
//            String name = file.getName();
//            addPropertyString(result, typeId, filter, PropertyIds.NAME, name);
//            objectInfo.setName(name);
//
//            // created and modified by
//            addPropertyString(result, typeId, filter, PropertyIds.CREATED_BY,
//                    USER_UNKNOWN);
//            addPropertyString(result, typeId, filter,
//                    PropertyIds.LAST_MODIFIED_BY, USER_UNKNOWN);
//            objectInfo.setCreatedBy(USER_UNKNOWN);
//
//            // creation and modification date
//            GregorianCalendar lastModified = FileBridgeUtils
//                    .millisToCalendar(file.lastModified());
//            addPropertyDateTime(result, typeId, filter,
//                    PropertyIds.CREATION_DATE, lastModified);
//            addPropertyDateTime(result, typeId, filter,
//                    PropertyIds.LAST_MODIFICATION_DATE, lastModified);
//            objectInfo.setCreationDate(lastModified);
//            objectInfo.setLastModificationDate(lastModified);
//
//            // change token - always null
//            addPropertyString(result, typeId, filter, PropertyIds.CHANGE_TOKEN,
//                    null);
//
//            // CMIS 1.1 properties
//            if (context.getCmisVersion() != CmisVersion.CMIS_1_0) {
//                addPropertyString(result, typeId, filter,
//                        PropertyIds.DESCRIPTION, null);
//                addPropertyIdList(result, typeId, filter,
//                        PropertyIds.SECONDARY_OBJECT_TYPE_IDS, null);
//            }
//
//            // directory or file
//            if (file.isDirectory()) {
//                // base type and type name
//                addPropertyId(result, typeId, filter, PropertyIds.BASE_TYPE_ID,
//                        BaseTypeId.CMIS_FOLDER.value());
//                addPropertyId(result, typeId, filter,
//                        PropertyIds.OBJECT_TYPE_ID,
//                        BaseTypeId.CMIS_FOLDER.value());
//                String path = getRepositoryPath(file);
//                addPropertyString(result, typeId, filter, PropertyIds.PATH,
//                        path);
//
//                // folder properties
//                if (!root.equals(file)) {
//                    addPropertyId(result, typeId, filter,
//                            PropertyIds.PARENT_ID,
//                            (root.equals(file.getParentFile()) ? ROOT_ID
//                                    : fileToId(file.getParentFile()))
//                    );
//                    objectInfo.setHasParent(true);
//                } else {
//                    addPropertyId(result, typeId, filter,
//                            PropertyIds.PARENT_ID, null);
//                    objectInfo.setHasParent(false);
//                }
//
//                addPropertyIdList(result, typeId, filter,
//                        PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, null);
//            } else {
//                // base type and type name
//                addPropertyId(result, typeId, filter, PropertyIds.BASE_TYPE_ID,
//                        BaseTypeId.CMIS_DOCUMENT.value());
//                addPropertyId(result, typeId, filter,
//                        PropertyIds.OBJECT_TYPE_ID,
//                        BaseTypeId.CMIS_DOCUMENT.value());
//
//                // file properties
//                addPropertyBoolean(result, typeId, filter,
//                        PropertyIds.IS_IMMUTABLE, false);
//                addPropertyBoolean(result, typeId, filter,
//                        PropertyIds.IS_LATEST_VERSION, true);
//                addPropertyBoolean(result, typeId, filter,
//                        PropertyIds.IS_MAJOR_VERSION, true);
//                addPropertyBoolean(result, typeId, filter,
//                        PropertyIds.IS_LATEST_MAJOR_VERSION, true);
//                addPropertyString(result, typeId, filter,
//                        PropertyIds.VERSION_LABEL, file.getName());
//                addPropertyId(result, typeId, filter,
//                        PropertyIds.VERSION_SERIES_ID, fileToId(file));
//                addPropertyBoolean(result, typeId, filter,
//                        PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, false);
//                addPropertyString(result, typeId, filter,
//                        PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null);
//                addPropertyString(result, typeId, filter,
//                        PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null);
//                addPropertyString(result, typeId, filter,
//                        PropertyIds.CHECKIN_COMMENT, "");
//                if (context.getCmisVersion() != CmisVersion.CMIS_1_0) {
//                    addPropertyBoolean(result, typeId, filter,
//                            PropertyIds.IS_PRIVATE_WORKING_COPY, false);
//                }
//
//                if (file.length() == 0) {
//                    addPropertyBigInteger(result, typeId, filter,
//                            PropertyIds.CONTENT_STREAM_LENGTH, null);
//                    addPropertyString(result, typeId, filter,
//                            PropertyIds.CONTENT_STREAM_MIME_TYPE, null);
//                    addPropertyString(result, typeId, filter,
//                            PropertyIds.CONTENT_STREAM_FILE_NAME, null);
//
//                    objectInfo.setHasContent(false);
//                    objectInfo.setContentType(null);
//                    objectInfo.setFileName(null);
//                } else {
//                    addPropertyInteger(result, typeId, filter,
//                            PropertyIds.CONTENT_STREAM_LENGTH, file.length());
//                    addPropertyString(result, typeId, filter,
//                            PropertyIds.CONTENT_STREAM_MIME_TYPE,
//                            MimeTypes.getMIMEType(file));
//                    addPropertyString(result, typeId, filter,
//                            PropertyIds.CONTENT_STREAM_FILE_NAME,
//                            file.getName());
//
//                    objectInfo.setHasContent(true);
//                    objectInfo.setContentType(MimeTypes.getMIMEType(file));
//                    objectInfo.setFileName(file.getName());
//                }
//
//                addPropertyId(result, typeId, filter,
//                        PropertyIds.CONTENT_STREAM_ID, null);
//            }
//
//            return result;
//        } catch (CmisBaseException cbe) {
//            throw cbe;
//        } catch (Exception e) {
//            throw new CmisRuntimeException(e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Checks a property set for a new object.
//     */
//    private void checkNewProperties(Properties properties) {
//        // check properties
//        if (properties == null || properties.getProperties() == null) {
//            throw new CmisInvalidArgumentException("Properties must be set!");
//        }
//
//        // check the name
//        String name = FileBridgeUtils.getStringProperty(properties,
//                PropertyIds.NAME);
//        if (!isValidName(name)) {
//            throw new CmisNameConstraintViolationException("Name is not valid!");
//        }
//
//        // check the type
//        String typeId = FileBridgeUtils.getObjectTypeId(properties);
//        if (typeId == null) {
//            throw new CmisNameConstraintViolationException(
//                    "Type Id is not set!");
//        }
//
//        TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
//        if (type == null) {
//            throw new CmisObjectNotFoundException("Type '" + typeId
//                    + "' is unknown!");
//        }
//
//        // check type properties
//        checkTypeProperties(properties, typeId, true);
//
//        // check if required properties are missing
//        for (PropertyDefinition<?> propDef : type.getPropertyDefinitions()
//                .values()) {
//            if (propDef.isRequired()
//                    && !properties.getProperties().containsKey(propDef.getId())
//                    && propDef.getUpdatability() != Updatability.READONLY) {
//                throw new CmisConstraintException("Property '"
//                        + propDef.getId() + "' is required!");
//            }
//        }
//    }
//
//    /**
//     * Checks a property set for a copied document.
//     */
//    private void checkCopyProperties(Properties properties, String sourceTypeId) {
//        // check properties
//        if (properties == null || properties.getProperties() == null) {
//            return;
//        }
//
//        String typeId = sourceTypeId;
//
//        // check the name
//        String name = FileBridgeUtils.getStringProperty(properties,
//                PropertyIds.NAME);
//        if (name != null) {
//            if (!isValidName(name)) {
//                throw new CmisNameConstraintViolationException(
//                        "Name is not valid!");
//            }
//        }
//
//        // check the type
//        typeId = FileBridgeUtils.getObjectTypeId(properties);
//        if (typeId == null) {
//            typeId = sourceTypeId;
//        }
//
//        TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
//        if (type == null) {
//            throw new CmisObjectNotFoundException("Type '" + typeId
//                    + "' is unknown!");
//        }
//
//        if (type.getBaseTypeId() != BaseTypeId.CMIS_DOCUMENT) {
//            throw new CmisInvalidArgumentException(
//                    "Target type must be a document type!");
//        }
//
//        // check type properties
//        checkTypeProperties(properties, typeId, true);
//
//        // check if required properties are missing
//        for (PropertyDefinition<?> propDef : type.getPropertyDefinitions()
//                .values()) {
//            if (propDef.isRequired()
//                    && !properties.getProperties().containsKey(propDef.getId())
//                    && propDef.getUpdatability() != Updatability.READONLY) {
//                throw new CmisConstraintException("Property '"
//                        + propDef.getId() + "' is required!");
//            }
//        }
//    }
//
//    /**
//     * Checks a property set for an update.
//     */
//    private void checkUpdateProperties(Properties properties, String typeId) {
//        // check properties
//        if (properties == null || properties.getProperties() == null) {
//            throw new CmisInvalidArgumentException("Properties must be set!");
//        }
//
//        // check the name
//        String name = FileBridgeUtils.getStringProperty(properties,
//                PropertyIds.NAME);
//        if (name != null) {
//            if (!isValidName(name)) {
//                throw new CmisNameConstraintViolationException(
//                        "Name is not valid!");
//            }
//        }
//
//        // check type properties
//        checkTypeProperties(properties, typeId, false);
//    }
//
//    /**
//     * Checks if the property belong to the type and are settable.
//     */
//    private void checkTypeProperties(Properties properties, String typeId,
//                                     boolean isCreate) {
//        // check type
//        TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
//        if (type == null) {
//            throw new CmisObjectNotFoundException("Type '" + typeId
//                    + "' is unknown!");
//        }
//
//        // check if all required properties are there
//        for (PropertyData<?> prop : properties.getProperties().values()) {
//            PropertyDefinition<?> propType = type.getPropertyDefinitions().get(
//                    prop.getId());
//
//            // do we know that property?
//            if (propType == null) {
//                throw new CmisConstraintException("Property '" + prop.getId()
//                        + "' is unknown!");
//            }
//
//            // can it be set?
//            if (propType.getUpdatability() == Updatability.READONLY) {
//                throw new CmisConstraintException("Property '" + prop.getId()
//                        + "' is readonly!");
//            }
//
//            if (!isCreate) {
//                // can it be set?
//                if (propType.getUpdatability() == Updatability.ONCREATE) {
//                    throw new CmisConstraintException("Property '"
//                            + prop.getId() + "' cannot be updated!");
//                }
//            }
//        }
//    }
//
//    private void addPropertyId(PropertiesImpl props, String typeId,
//                               Set<String> filter, String id, String value) {
//        if (!checkAddProperty(props, typeId, filter, id)) {
//            return;
//        }
//
//        props.addProperty(new PropertyIdImpl(id, value));
//    }
//
//    private void addPropertyIdList(PropertiesImpl props, String typeId,
//                                   Set<String> filter, String id, List<String> value) {
//        if (!checkAddProperty(props, typeId, filter, id)) {
//            return;
//        }
//
//        props.addProperty(new PropertyIdImpl(id, value));
//    }
//
//    private void addPropertyString(PropertiesImpl props, String typeId,
//                                   Set<String> filter, String id, String value) {
//        if (!checkAddProperty(props, typeId, filter, id)) {
//            return;
//        }
//
//        props.addProperty(new PropertyStringImpl(id, value));
//    }
//
//    private void addPropertyInteger(PropertiesImpl props, String typeId,
//                                    Set<String> filter, String id, long value) {
//        addPropertyBigInteger(props, typeId, filter, id,
//                BigInteger.valueOf(value));
//    }
//
//    private void addPropertyBigInteger(PropertiesImpl props, String typeId,
//                                       Set<String> filter, String id, BigInteger value) {
//        if (!checkAddProperty(props, typeId, filter, id)) {
//            return;
//        }
//
//        props.addProperty(new PropertyIntegerImpl(id, value));
//    }
//
//    private void addPropertyBoolean(PropertiesImpl props, String typeId,
//                                    Set<String> filter, String id, boolean value) {
//        if (!checkAddProperty(props, typeId, filter, id)) {
//            return;
//        }
//
//        props.addProperty(new PropertyBooleanImpl(id, value));
//    }
//
//    private void addPropertyDateTime(PropertiesImpl props, String typeId,
//                                     Set<String> filter, String id, GregorianCalendar value) {
//        if (!checkAddProperty(props, typeId, filter, id)) {
//            return;
//        }
//
//        props.addProperty(new PropertyDateTimeImpl(id, value));
//    }
//
//    private boolean checkAddProperty(Properties properties, String typeId,
//                                     Set<String> filter, String id) {
//        if ((properties == null) || (properties.getProperties() == null)) {
//            throw new IllegalArgumentException("Properties must not be null!");
//        }
//
//        if (id == null) {
//            throw new IllegalArgumentException("Id must not be null!");
//        }
//
//        TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
//        if (type == null) {
//            throw new IllegalArgumentException("Unknown type: " + typeId);
//        }
//        if (!type.getPropertyDefinitions().containsKey(id)) {
//            throw new IllegalArgumentException("Unknown property: " + id);
//        }
//
//        String queryName = type.getPropertyDefinitions().get(id).getQueryName();
//
//        if ((queryName != null) && (filter != null)) {
//            if (!filter.contains(queryName)) {
//                return false;
//            } else {
//                filter.remove(queryName);
//            }
//        }
//
//        return true;
//    }
//
//    /**
//     * Compiles the allowable actions for a file or folder.
//     */
//    private AllowableActions compileAllowableActions(File file,
//                                                     boolean userReadOnly) {
//        if (file == null) {
//            throw new IllegalArgumentException("File must not be null!");
//        }
//
//        // we can't gather allowable actions if the file or folder doesn't exist
//        if (!file.exists()) {
//            throw new CmisObjectNotFoundException("Object not found!");
//        }
//
//        boolean isReadOnly = !file.canWrite();
//        boolean isFolder = file.isDirectory();
//        boolean isRoot = root.equals(file);
//
//        Set<Action> aas = EnumSet.noneOf(Action.class);
//
//        addAction(aas, Action.CAN_GET_OBJECT_PARENTS, !isRoot);
//        addAction(aas, Action.CAN_GET_PROPERTIES, true);
//        addAction(aas, Action.CAN_UPDATE_PROPERTIES, !userReadOnly
//                && !isReadOnly);
//        addAction(aas, Action.CAN_MOVE_OBJECT, !userReadOnly && !isRoot);
//        addAction(aas, Action.CAN_DELETE_OBJECT, !userReadOnly && !isReadOnly
//                && !isRoot);
//        addAction(aas, Action.CAN_GET_ACL, true);
//
//        if (isFolder) {
//            addAction(aas, Action.CAN_GET_DESCENDANTS, true);
//            addAction(aas, Action.CAN_GET_CHILDREN, true);
//            addAction(aas, Action.CAN_GET_FOLDER_PARENT, !isRoot);
//            addAction(aas, Action.CAN_GET_FOLDER_TREE, true);
//            addAction(aas, Action.CAN_CREATE_DOCUMENT, !userReadOnly);
//            addAction(aas, Action.CAN_CREATE_FOLDER, !userReadOnly);
//            addAction(aas, Action.CAN_DELETE_TREE, !userReadOnly && !isReadOnly);
//        } else {
//            addAction(aas, Action.CAN_GET_CONTENT_STREAM, file.length() > 0);
//            addAction(aas, Action.CAN_SET_CONTENT_STREAM, !userReadOnly
//                    && !isReadOnly);
//            addAction(aas, Action.CAN_DELETE_CONTENT_STREAM, !userReadOnly
//                    && !isReadOnly);
//            addAction(aas, Action.CAN_GET_ALL_VERSIONS, true);
//        }
//
//        AllowableActionsImpl result = new AllowableActionsImpl();
//        result.setAllowableActions(aas);
//
//        return result;
//    }
//
//    private void addAction(Set<Action> aas, Action action, boolean condition) {
//        if (condition) {
//            aas.add(action);
//        }
//    }
//
//    /**
//     * Compiles the ACL for a file or folder.
//     */
//    private Acl compileAcl(File file) {
//        AccessControlListImpl result = new AccessControlListImpl();
//        result.setAces(new ArrayList<Ace>());
//
//        for (Map.Entry<String, Boolean> ue : readWriteUserMap.entrySet()) {
//            // create principal
//            AccessControlPrincipalDataImpl principal = new AccessControlPrincipalDataImpl();
//            principal.setPrincipalId(ue.getKey());
//
//            // create ACE
//            AccessControlEntryImpl entry = new AccessControlEntryImpl();
//            entry.setPrincipal(principal);
//            entry.setPermissions(new ArrayList<String>());
//            entry.getPermissions().add(CMIS_READ);
//            if (!ue.getValue().booleanValue() && file.canWrite()) {
//                entry.getPermissions().add(CMIS_WRITE);
//                entry.getPermissions().add(CMIS_ALL);
//            }
//
//            entry.setDirect(true);
//
//            // add ACE
//            result.getAces().add(entry);
//        }
//
//        return result;
//    }
//
//    /**
//     * Checks if the given name is valid for a file system.
//     *
//     * @param name the name to check
//     * @return <code>true</code> if the name is valid, <code>false</code>
//     * otherwise
//     */
//    private boolean isValidName(String name) {
//        if (name == null || name.length() == 0
//                || name.indexOf(File.separatorChar) != -1
//                || name.indexOf(File.pathSeparatorChar) != -1) {
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * Checks if a folder is empty. A folder is considered as empty if no files
//     * or only the shadow file reside in the folder.
//     *
//     * @param folder the folder
//     * @return <code>true</code> if the folder is empty.
//     */
//    private boolean isFolderEmpty(File folder) {
//        if (!folder.isDirectory()) {
//            return true;
//        }
//
//        String[] fileNames = folder.list();
//
//        if ((fileNames == null) || (fileNames.length == 0)) {
//            return true;
//        }
//
//        return false;
//    }
//
//    /**
//     * Checks if the user in the given context is valid for this repository and
//     * if the user has the required permissions.
//     */
//    private boolean checkUser(CallContext context, boolean writeRequired) {
//        if (context == null) {
//            throw new CmisPermissionDeniedException("No user context!");
//        }
//
//        Boolean readOnly = readWriteUserMap.get(context.getUsername());
//        if (readOnly == null) {
//            throw new CmisPermissionDeniedException("Unknown user!");
//        }
//
//        if (readOnly.booleanValue() && writeRequired) {
//            throw new CmisPermissionDeniedException("No write permission!");
//        }
//
//        return readOnly.booleanValue();
//    }
//
//    /**
//     * Returns the File object by id or throws an appropriate exception.
//     */
//    private File getFile(String id) {
//        try {
//            return idToFile(id);
//        } catch (Exception e) {
//            throw new CmisObjectNotFoundException(e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Converts an id to a File object. A simple and insecure implementation,
//     * but good enough for now.
//     */
//    private File idToFile(String id) throws IOException {
//        if (id == null || id.length() == 0) {
//            throw new CmisInvalidArgumentException("Id is not valid!");
//        }
//
//        if (id.equals(ROOT_ID)) {
//            return root;
//        }
//
//        return new File(root, (new String(
//                Base64.decode(id.getBytes("US-ASCII")), "UTF-8")).replace('/',
//                File.separatorChar));
//    }
//
//    /**
//     * Returns the id of a File object or throws an appropriate exception.
//     */
//    private String getId(File file) {
//        try {
//            return fileToId(file);
//        } catch (Exception e) {
//            throw new CmisRuntimeException(e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Creates a File object from an id. A simple and insecure implementation,
//     * but good enough for now.
//     */
//    private String fileToId(File file) throws IOException {
//        if (file == null) {
//            throw new IllegalArgumentException("File is not valid!");
//        }
//
//        if (root.equals(file)) {
//            return ROOT_ID;
//        }
//
//        String path = getRepositoryPath(file);
//
//        return Base64.encodeBytes(path.getBytes("UTF-8"));
//    }
//
//    private String getRepositoryPath(File file) {
//        String path = file.getAbsolutePath()
//                .substring(root.getAbsolutePath().length())
//                .replace(File.separatorChar, '/');
//        if (path.length() == 0) {
//            path = "/";
//        } else if (path.charAt(0) != '/') {
//            path = "/" + path;
//        }
//        return path;
//    }
//
//
//


    // --- helpers ---

    /**
     * Compiles an object type object from a file or folder.
     */


//    private ObjectData compileObjectData(CallContext context, File file,
//                                         Set<String> filter, boolean includeAllowableActions,
//                                         boolean includeAcl, boolean userReadOnly,
//                                         ObjectInfoHandler objectInfos) {
//        ObjectDataImpl result = new ObjectDataImpl();
//        ObjectInfoImpl objectInfo = new ObjectInfoImpl();
//
//        result.setProperties(compileProperties(context, file, filter,
//                objectInfo));
//
//        if (includeAllowableActions) {
//            result.setAllowableActions(compileAllowableActions(file,
//                    userReadOnly));
//        }
//
//        if (includeAcl) {
//            result.setAcl(compileAcl(file));
//            result.setIsExactAcl(true);
//        }
//
//        if (context.isObjectInfoRequired()) {
//            objectInfo.setObject(result);
//            objectInfos.addObjectInfo(objectInfo);
//        }
//
//        return result;
//    }

    /**
     * Gathers all base properties of a file or folder.
     */
//    private Properties compileProperties(CallContext context, File file,
//                                         Set<String> orgfilter, ObjectInfoImpl objectInfo) {
//        if (file == null) {
//            throw new IllegalArgumentException("File must not be null!");
//        }
//
//        // we can't gather properties if the file or folder doesn't exist
//        if (!file.exists()) {
//            throw new CmisObjectNotFoundException("Object not found!");
//        }
//
//        // copy filter
//        Set<String> filter = (orgfilter == null ? null : new HashSet<String>(
//                orgfilter));
//
//        // find base type
//        String typeId = null;
//
//        // identify if the file is a doc or a folder/directory
//        if (file.isDirectory()) {
//            typeId = BaseTypeId.CMIS_FOLDER.value();
//            objectInfo.setBaseType(BaseTypeId.CMIS_FOLDER);
//            objectInfo.setTypeId(typeId);
//            objectInfo.setContentType(null);
//            objectInfo.setFileName(null);
//            objectInfo.setHasAcl(true);
//            objectInfo.setHasContent(false);
//            objectInfo.setVersionSeriesId(null);
//            objectInfo.setIsCurrentVersion(true);
//            objectInfo.setRelationshipSourceIds(null);
//            objectInfo.setRelationshipTargetIds(null);
//            objectInfo.setRenditionInfos(null);
//            objectInfo.setSupportsDescendants(true);
//            objectInfo.setSupportsFolderTree(true);
//            objectInfo.setSupportsPolicies(false);
//            objectInfo.setSupportsRelationships(false);
//            objectInfo.setWorkingCopyId(null);
//            objectInfo.setWorkingCopyOriginalId(null);
//        } else {
//            typeId = BaseTypeId.CMIS_DOCUMENT.value();
//            objectInfo.setBaseType(BaseTypeId.CMIS_DOCUMENT);
//            objectInfo.setTypeId(typeId);
//            objectInfo.setHasAcl(true);
//            objectInfo.setHasContent(true);
//            objectInfo.setHasParent(true);
//            objectInfo.setVersionSeriesId(null);
//            objectInfo.setIsCurrentVersion(true);
//            objectInfo.setRelationshipSourceIds(null);
//            objectInfo.setRelationshipTargetIds(null);
//            objectInfo.setRenditionInfos(null);
//            objectInfo.setSupportsDescendants(false);
//            objectInfo.setSupportsFolderTree(false);
//            objectInfo.setSupportsPolicies(false);
//            objectInfo.setSupportsRelationships(false);
//            objectInfo.setWorkingCopyId(null);
//            objectInfo.setWorkingCopyOriginalId(null);
//        }
//
//        // let's do it
//        try {
//            PropertiesImpl result = new PropertiesImpl();
//
//            // id
//            String id = fileToId(file);
//            addPropertyId(result, typeId, filter, PropertyIds.OBJECT_ID, id);
//            objectInfo.setId(id);
//
//            // name
//            String name = file.getName();
//            addPropertyString(result, typeId, filter, PropertyIds.NAME, name);
//            objectInfo.setName(name);
//
//            // created and modified by
//            /// addPropertyString(result, typeId, filter, PropertyIds.CREATED_BY, USER_UNKNOWN);
//            /// addPropertyString(result, typeId, filter, PropertyIds.LAST_MODIFIED_BY, USER_UNKNOWN);
//            /// objectInfo.setCreatedBy(USER_UNKNOWN);
//
//            // creation and modification date
//            GregorianCalendar lastModified = FileBridgeUtils.millisToCalendar(file.lastModified());
//            addPropertyDateTime(result, typeId, filter, PropertyIds.CREATION_DATE, lastModified);
//            addPropertyDateTime(result, typeId, filter, PropertyIds.LAST_MODIFICATION_DATE, lastModified);
//            objectInfo.setCreationDate(lastModified);
//            objectInfo.setLastModificationDate(lastModified);
//
//            // change token - always null
//            addPropertyString(result, typeId, filter, PropertyIds.CHANGE_TOKEN, null);
//
//            // CMIS 1.1 properties
//            if (context.getCmisVersion() != CmisVersion.CMIS_1_0) {
//                addPropertyString(result, typeId, filter, PropertyIds.DESCRIPTION, null);
//                addPropertyIdList(result, typeId, filter, PropertyIds.SECONDARY_OBJECT_TYPE_IDS, null);
//            }
//
//            // directory or file
//            if (file.isDirectory()) {
//                // base type and type name
//                addPropertyId(result, typeId, filter, PropertyIds.BASE_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
//                addPropertyId(result, typeId, filter, PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
//                String path = getRepositoryPath(file);
//                addPropertyString(result, typeId, filter, PropertyIds.PATH, path);
//
//                // folder properties
////                if (!root.equals(file)) {
////                    addPropertyId(result, typeId, filter, PropertyIds.PARENT_ID, (root.equals(file.getParentFile()) ? ROOT_ID : fileToId(file.getParentFile())));
////                    objectInfo.setHasParent(true);
////                } else {
////                    addPropertyId(result, typeId, filter, PropertyIds.PARENT_ID, null);
////                    objectInfo.setHasParent(false);
////                }
//
//                addPropertyIdList(result, typeId, filter, PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, null);
//            } else {
//                // base type and type name
//                addPropertyId(result, typeId, filter, PropertyIds.BASE_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
//                addPropertyId(result, typeId, filter, PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
//
//                // file properties
//                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_IMMUTABLE, false);
//                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_LATEST_VERSION, true);
//                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_MAJOR_VERSION, true);
//                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_LATEST_MAJOR_VERSION, true);
//                addPropertyString(result, typeId, filter, PropertyIds.VERSION_LABEL, file.getName());
//                addPropertyId(result, typeId, filter, PropertyIds.VERSION_SERIES_ID, fileToId(file));
//                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, false);
//                addPropertyString(result, typeId, filter, PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null);
//                addPropertyString(result, typeId, filter, PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null);
//                addPropertyString(result, typeId, filter, PropertyIds.CHECKIN_COMMENT, "");
//                if (context.getCmisVersion() != CmisVersion.CMIS_1_0) {
//                    addPropertyBoolean(result, typeId, filter, PropertyIds.IS_PRIVATE_WORKING_COPY, false);
//                }
//
//                if (file.length() == 0) {
//                    addPropertyBigInteger(result, typeId, filter, PropertyIds.CONTENT_STREAM_LENGTH, null);
//                    addPropertyString(result, typeId, filter, PropertyIds.CONTENT_STREAM_MIME_TYPE, null);
//                    addPropertyString(result, typeId, filter, PropertyIds.CONTENT_STREAM_FILE_NAME, null);
//
//                    objectInfo.setHasContent(false);
//                    objectInfo.setContentType(null);
//                    objectInfo.setFileName(null);
//                } else {
//                    addPropertyLong(result, typeId, filter, PropertyIds.CONTENT_STREAM_LENGTH, file.length());
//                    addPropertyString(result, typeId, filter, PropertyIds.CONTENT_STREAM_MIME_TYPE, MimeTypes.getMIMEType(file));
//                    addPropertyString(result, typeId, filter, PropertyIds.CONTENT_STREAM_FILE_NAME, file.getName());
//
//                    objectInfo.setHasContent(true);
//                    objectInfo.setContentType(MimeTypes.getMIMEType(file));
//                    objectInfo.setFileName(file.getName());
//                }
//
//                addPropertyId(result, typeId, filter, PropertyIds.CONTENT_STREAM_ID, null);
//            }
//
//            return result;
//        } catch (CmisBaseException cbe) {
//            throw cbe;
//        } catch (Exception e) {
//            throw new CmisRuntimeException(e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Checks a property set for a new object.
//     */
//    private void checkNewProperties(Properties properties) {
//        // check properties
//        if (properties == null || properties.getProperties() == null) {
//            throw new CmisInvalidArgumentException("Properties must be set!");
//        }
//
//        // check the name
//        String name = FileBridgeUtils.getStringProperty(properties, PropertyIds.NAME);
//        if (!EloCmisUtils.isValidName(name)) {
//            throw new CmisNameConstraintViolationException("Name is not valid!");
//        }
//
//        // check the type
//        String typeId = FileBridgeUtils.getObjectTypeId(properties);
//        if (typeId == null) {
//            throw new CmisNameConstraintViolationException("Type Id is not set!");
//        }
//
//        //TODO
//        TypeDefinition type = null;
//        //TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
//        if (type == null) {
//            throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
//        }
//
//        // check type properties
//        checkTypeProperties(properties, typeId, true);
//
//        // check if required properties are missing
//        for (PropertyDefinition<?> propDef : type.getPropertyDefinitions()
//                .values()) {
//            if (propDef.isRequired()
//                    && !properties.getProperties().containsKey(propDef.getId())
//                    && propDef.getUpdatability() != Updatability.READONLY) {
//                throw new CmisConstraintException("Property '" + propDef.getId() + "' is required!");
//            }
//        }
//    }

    /**
     * Checks a property set for a copied document.
     */
//    private void checkCopyProperties(Properties properties, String sourceTypeId) {
//        // check properties
//        if (properties == null || properties.getProperties() == null) {
//            return;
//        }
//
//        String typeId = sourceTypeId;
//
//        // check the name
//        String name = FileBridgeUtils.getStringProperty(properties,
//                PropertyIds.NAME);
//        if (name != null) {
//            if (!EloCmisUtils.isValidName(name)) {
//                throw new CmisNameConstraintViolationException(
//                        "Name is not valid!");
//            }
//        }
//
//        // check the type
//        typeId = FileBridgeUtils.getObjectTypeId(properties);
//        if (typeId == null) {
//            typeId = sourceTypeId;
//        }
//
//        //TODO
//        TypeDefinition type = null;
//        //TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
//        if (type == null) {
//            throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
//        }
//
//        if (type.getBaseTypeId() != BaseTypeId.CMIS_DOCUMENT) {
//            throw new CmisInvalidArgumentException("Target type must be a document type!");
//        }
//
//        // check type properties
//        checkTypeProperties(properties, typeId, true);
//
//        // check if required properties are missing
//        for (PropertyDefinition<?> propDef : type.getPropertyDefinitions()
//                .values()) {
//            if (propDef.isRequired()
//                    && !properties.getProperties().containsKey(propDef.getId())
//                    && propDef.getUpdatability() != Updatability.READONLY) {
//                throw new CmisConstraintException("Property '" + propDef.getId() + "' is required!");
//            }
//        }
//    }

    /**
     * Checks a property set for an update.
     */
    private void checkUpdateProperties(Properties properties, String typeId) {
        // check properties
        if (properties == null || properties.getProperties() == null) {
            throw new CmisInvalidArgumentException("Properties must be set!");
        }

        // check the name
        String name = FileBridgeUtils.getStringProperty(properties, PropertyIds.NAME);
        if (name != null) {
            if (!EloCmisUtils.isValidName(name)) {
                throw new CmisNameConstraintViolationException(
                        "Name is not valid!");
            }
        }

        // check type properties
        checkTypeProperties(properties, typeId, false);
    }

    /**
     * Checks if the property belong to the type and are settable.
     */
    private void checkTypeProperties(Properties properties, String typeId, boolean isCreate) {
        // check type
        //TODO
        TypeDefinition type = null;
        //TypeDefinition type = typeManager.getInternalTypeDefinition(typeId);
        if (type == null) {
            throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
        }

        // check if all required properties are there
        for (PropertyData<?> prop : properties.getProperties().values()) {
            PropertyDefinition<?> propType = type.getPropertyDefinitions().get(
                    prop.getId());

            // do we know that property?
            if (propType == null) {
                throw new CmisConstraintException("Property '" + prop.getId()
                        + "' is unknown!");
            }

            // can it be set?
            if (propType.getUpdatability() == Updatability.READONLY) {
                throw new CmisConstraintException("Property '" + prop.getId()
                        + "' is readonly!");
            }

            if (!isCreate) {
                // can it be set?
                if (propType.getUpdatability() == Updatability.ONCREATE) {
                    throw new CmisConstraintException("Property '"
                            + prop.getId() + "' cannot be updated!");
                }
            }
        }
    }


    /**
     * Compiles the allowable actions for a file or folder.
     */
    private AllowableActions compileAllowableActions(File file,
                                                     boolean userReadOnly) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null!");
        }

        // we can't gather allowable actions if the file or folder doesn't exist
        if (!file.exists()) {
            throw new CmisObjectNotFoundException("Object not found!");
        }

        boolean isReadOnly = !file.canWrite();
        boolean isFolder = file.isDirectory();
//        boolean isRoot = root.equals(file);
        boolean isRoot = false;

        Set<Action> aas = EnumSet.noneOf(Action.class);

        addAction(aas, Action.CAN_GET_OBJECT_PARENTS, !isRoot);
        addAction(aas, Action.CAN_GET_PROPERTIES, true);
        addAction(aas, Action.CAN_UPDATE_PROPERTIES, !userReadOnly
                && !isReadOnly);
        addAction(aas, Action.CAN_MOVE_OBJECT, !userReadOnly && !isRoot);
        addAction(aas, Action.CAN_DELETE_OBJECT, !userReadOnly && !isReadOnly
                && !isRoot);
        addAction(aas, Action.CAN_GET_ACL, true);

        if (isFolder) {
            addAction(aas, Action.CAN_GET_DESCENDANTS, true);
            addAction(aas, Action.CAN_GET_CHILDREN, true);
            addAction(aas, Action.CAN_GET_FOLDER_PARENT, !isRoot);
            addAction(aas, Action.CAN_GET_FOLDER_TREE, true);
            addAction(aas, Action.CAN_CREATE_DOCUMENT, !userReadOnly);
            addAction(aas, Action.CAN_CREATE_FOLDER, !userReadOnly);
            addAction(aas, Action.CAN_DELETE_TREE, !userReadOnly && !isReadOnly);
        } else {
            addAction(aas, Action.CAN_GET_CONTENT_STREAM, file.length() > 0);
            addAction(aas, Action.CAN_SET_CONTENT_STREAM, !userReadOnly
                    && !isReadOnly);
            addAction(aas, Action.CAN_DELETE_CONTENT_STREAM, !userReadOnly
                    && !isReadOnly);
            addAction(aas, Action.CAN_GET_ALL_VERSIONS, true);
        }

        AllowableActionsImpl result = new AllowableActionsImpl();
        result.setAllowableActions(aas);

        return result;
    }

    private void addAction(Set<Action> aas, Action action, boolean condition) {
        if (condition) {
            aas.add(action);
        }
    }

    /**
     * Compiles the ACL for a file or folder.
     */
    private Acl compileAcl(File file) {
        AccessControlListImpl result = new AccessControlListImpl();
        result.setAces(new ArrayList<Ace>());

//        for (Map.Entry<String, Boolean> ue : readWriteUserMap.entrySet()) {
//            // create principal
//            AccessControlPrincipalDataImpl principal = new AccessControlPrincipalDataImpl();
//            principal.setPrincipalId(ue.getKey());
//
//            // create ACE
//            AccessControlEntryImpl entry = new AccessControlEntryImpl();
//            entry.setPrincipal(principal);
//            entry.setPermissions(new ArrayList<String>());
//            entry.getPermissions().add(CMIS_READ);
//            if (!ue.getValue().booleanValue() && file.canWrite()) {
//                entry.getPermissions().add(CMIS_WRITE);
//                entry.getPermissions().add(CMIS_ALL);
//            }
//
//            entry.setDirect(true);
//
//            // add ACE
//            result.getAces().add(entry);
//        }

        return result;
    }


    /**
     * Checks if the given name is valid for a file system.
     *
     * @param name the name to check
     * @return <code>true</code> if the name is valid, <code>false</code>
     * otherwise
     */


    /**
     * Checks if a folder is empty. A folder is considered as empty if no files
     * or only the shadow file reside in the folder.
     *
     * @param folder the folder
     * @return <code>true</code> if the folder is empty.
     */
//    private boolean isFolderEmpty(File folder) {
//        if (!folder.isDirectory()) {
//            return true;
//        }
//
//        String[] fileNames = folder.list();
//
//        if ((fileNames == null) || (fileNames.length == 0)) {
//            return true;
//        }
//
//        return false;
//    }

    /**
     * Checks if the user in the given context is valid for this repository and
     * if the user has the required permissions.
     */
//    private boolean checkUser(CallContext context, boolean writeRequired) {
//        if (context == null) {
//            throw new CmisPermissionDeniedException("No user context!");
//        }
//
//        Boolean readOnly = true;
////        Boolean readOnly = readWriteUserMap.get(context.getUsername());
//        if (readOnly == null) {
//            throw new CmisPermissionDeniedException("Unknown user!");
//        }
//
//        if (readOnly.booleanValue() && writeRequired) {
//            throw new CmisPermissionDeniedException("No write permission!");
//        }
//
//        return readOnly.booleanValue();
//    }

    /**
     * Returns the File object by id or throws an appropriate exception.
     */
//    private File getFile(String id) {
//        try {
//            return idToFile(id);
//        } catch (Exception e) {
//            throw new CmisObjectNotFoundException(e.getMessage(), e);
//        }
//    }

    /**
     * Converts an id to a File object. A simple and insecure implementation,
     * but good enough for now.
     */
//    private File idToFile(String id) throws IOException {
//        if (id == null || id.length() == 0) {
//            throw new CmisInvalidArgumentException("Id is not valid!");
//        }
//        return null;
////        if (id.equals(ROOT_ID)) {
////            return root;
////        }
////
////        return new File(root, (new String(
////                Base64.decode(id.getBytes("US-ASCII")), "UTF-8")).replace('/',
////                File.separatorChar));
//    }

    /**
     * Returns the id of a File object or throws an appropriate exception.
     */
//    private String getId(File file) {
//        try {
//            return fileToId(file);
//        } catch (Exception e) {
//            throw new CmisRuntimeException(e.getMessage(), e);
//        }
//    }

    /**
     * Creates a File object from an id. A simple and insecure implementation,
     * but good enough for now.
     */
//    private String fileToId(File file) throws IOException {
//        if (file == null) {
//            throw new IllegalArgumentException("File is not valid!");
//        }
//
//        return null;
//
////        if (root.equals(file)) {
////            return ROOT_ID;
////        }
////
////        String path = getRepositoryPath(file);
////
////        return Base64.encodeBytes(path.getBytes("UTF-8"));
//    }

//    private String getRepositoryPath(File file) {
//
////        String path = file.getAbsolutePath()
////                .substring(root.getAbsolutePath().length())
////                .replace(File.separatorChar, '/');
////        if (path.length() == 0) {
////            path = "/";
////        } else if (path.charAt(0) != '/') {
////            path = "/" + path;
////        }
////        return path;
//
//        return null;
//    }

//    @Override
//    protected boolean checkAddProperty(Properties properties, String typeId, Set<String> filter, String id) {
//        if ((properties == null) || (properties.getProperties() == null)) {
//            throw new IllegalArgumentException("Properties must not be null!");
//        }
//
//        if (id == null) {
//            throw new IllegalArgumentException("Id must not be null!");
//        }
//
//        TypeDefinition type = typeManager.getTypeDefinition(typeId);
//        if (type == null) {
//            throw new IllegalArgumentException("Unknown type: " + typeId);
//        }
//        if (!type.getPropertyDefinitions().containsKey(id)) {
//            throw new IllegalArgumentException("Unknown property: " + id);
//        }
//
//        String queryName = type.getPropertyDefinitions().get(id).getQueryName();
//
//        if ((queryName != null) && (filter != null)) {
//            if (!filter.contains(queryName)) {
//                return false;
//            } else {
//                filter.remove(queryName);
//            }
//        }
//        return true;
//    }
}
