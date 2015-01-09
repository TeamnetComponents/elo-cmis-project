package org.cmis.server.elo;

import de.elo.ix.client.IXConnection;
import org.apache.chemistry.opencmis.commons.data.*;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractCmisService;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.server.support.wrapper.CallContextAwareCmisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Created by Lucian.Dragomir on 6/2/2014.
 */
public class EloCmisService extends AbstractCmisService implements CallContextAwareCmisService {
    private static final Logger LOG = LoggerFactory.getLogger(EloCmisService.class);

    private static final int BUFFER_SIZE = 64 * 1024;

    private CallContext context;
    private EloCmisConnectionManager eloCmisConnectionManager;
    //private EloConnectionPool eloConnectionPool;
    //private IXConnection eloConnection;
    private Map<String, EloCmisRepository> eloCmisRepositoryMap;

//    public EloCmisService(EloConnectionPool eloConnectionPool) {
//        this.eloConnectionPool = eloConnectionPool;
//        this.eloCmisRepositoryMap = null;
//    }

    public EloCmisService(EloCmisConnectionManager eloCmisConnectionManager) {
        this.eloCmisConnectionManager = eloCmisConnectionManager;
        this.eloCmisRepositoryMap = null;
    }

    // --- Call Context ---

    /**
     * Gets the call context.
     */
    @Override
    public CallContext getCallContext() {
        return this.context;
    }

    /**
     * Sets the call context.
     * <p/>
     * This method should only be called by the service factory.
     */
    @Override
    public void setCallContext(CallContext context) {
        this.context = context;
        //repository map depends on the context
        this.eloCmisRepositoryMap = EloCmisRepository.createEloCmisRepositoryMap(this, (ExtensionsData) null);
    }

    @Override
    public void close() {
        returnConnection();
        super.close();
    }

    /**
     * Gets the elo connection.
     * The retrieval is made using EloConnectionManager, this approach delegates to EloConnectionManager how the elo IXConnections are handled.
     * Different implementatio approach can be used: allocation for each EloCmisService instance or can be managed using a
     */

    protected IXConnection getConnection() {
        return eloCmisConnectionManager.getConnection(this.getCallContext());
    }


    private void returnConnection() {
        this.eloCmisConnectionManager.returnConnection(this.getCallContext());
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

    //DONE
    @Override
    public ObjectInFolderList getChildren(String repositoryId, String folderId, String filter, String orderBy,
                                          Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
                                          Boolean includePathSegment, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension)
                .getChildren(folderId, filter, orderBy,
                        includeAllowableActions, includeRelationships, renditionFilter,
                        includePathSegment, maxItems, skipCount);
    }

    //TODO
    @Override
    public List<ObjectInFolderContainer> getDescendants(String repositoryId, String folderId, BigInteger depth, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        return super.getDescendants(repositoryId, folderId, depth, filter, includeAllowableActions, includeRelationships, renditionFilter, includePathSegment, extension);
    }

    //DONE
    @Override
    public ObjectData getFolderParent(String repositoryId, String folderId, String filter, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension)
                .getFolderParent(folderId, filter);
    }

    //TODO
    @Override
    public List<ObjectInFolderContainer> getFolderTree(String repositoryId, String folderId, BigInteger depth, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePathSegment, ExtensionsData extension) {
        return super.getFolderTree(repositoryId, folderId, depth, filter, includeAllowableActions, includeRelationships, renditionFilter, includePathSegment, extension);
    }

    //DONE
    @Override
    public List<ObjectParentData> getObjectParents(String repositoryId, String objectId, String filter,
                                                   Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter,
                                                   Boolean includeRelativePathSegment, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension)
                .getObjectParents(objectId, filter, includeAllowableActions, includeRelationships, renditionFilter, includeRelativePathSegment);
    }


    // -----------------------------------------------------------------------------------------------------------------
    // REPOSITORY SERVICES
    // -----------------------------------------------------------------------------------------------------------------

    //DONE
    @Override
    public TypeDefinition createType(String repositoryId, TypeDefinition type, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension).createType(type);
    }

    //DONE
    @Override
    public void deleteType(String repositoryId, String typeId, ExtensionsData extension) {
        getEloCmisRepository(repositoryId, extension).deleteType(typeId);
    }


    //DONE
    @Override
    public RepositoryInfo getRepositoryInfo(String repositoryId, ExtensionsData extensionsData) {
        //return EloCmisRepository.getRepositoryInfo(this, extension, repositoryId);
        //if returning cached object we do not have Extension Data associated to it
        return eloCmisRepositoryMap.get(repositoryId).getRepositoryInfo();
    }

    //DONE
    @Override
    public List<RepositoryInfo> getRepositoryInfos(ExtensionsData extension) {
        return EloCmisRepository.getRepositoryInfos(this, extension);
    }

    //DONE
    private EloCmisRepository getEloCmisRepository(String repositoryId, ExtensionsData extensionsData) {
        EloCmisRepository eloCmisRepository = eloCmisRepositoryMap.get(repositoryId);
        eloCmisRepository.setExtensionsData(extensionsData);
        return eloCmisRepository;
    }


    //DONE
    @Override
    public TypeDefinitionList getTypeChildren(String repositoryId, String typeId, Boolean includePropertyDefinitions,
                                              BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension)
                .getTypeChildren(typeId, includePropertyDefinitions, maxItems, skipCount);
    }

    //DONE
    @Override
    public TypeDefinition getTypeDefinition(String repositoryId, String typeId, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension)
                .getTypeDefinition(typeId);
    }

    //TODO
    @Override
    public List<TypeDefinitionContainer> getTypeDescendants(String repositoryId, String typeId, BigInteger depth, Boolean includePropertyDefinitions, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension)
                .getTypeDescendants(typeId, depth, includePropertyDefinitions);
    }

    //TODO
    @Override
    public TypeDefinition updateType(String repositoryId, TypeDefinition type, ExtensionsData extension) {
        return super.updateType(repositoryId, type, extension);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // OBJECT SERVICES
    // -----------------------------------------------------------------------------------------------------------------

    //TODO
    @Override
    public void appendContentStream(String repositoryId, Holder<String> objectId, Holder<String> changeToken, ContentStream contentStream, boolean isLastChunk, ExtensionsData extension) {
        super.appendContentStream(repositoryId, objectId, changeToken, contentStream, isLastChunk, extension);
    }

    //TODO
    @Override
    public List<BulkUpdateObjectIdAndChangeToken> bulkUpdateProperties(String repositoryId, List<BulkUpdateObjectIdAndChangeToken> objectIdAndChangeToken, Properties properties, List<String> addSecondaryTypeIds, List<String> removeSecondaryTypeIds, ExtensionsData extension) {
        return super.bulkUpdateProperties(repositoryId, objectIdAndChangeToken, properties, addSecondaryTypeIds, removeSecondaryTypeIds, extension);
    }

    //DONE
    @Override
    public String createDocument(String repositoryId, Properties properties, String folderId, ContentStream contentStream, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension).
                createDocument(properties, folderId, contentStream, versioningState, policies, addAces, removeAces);
    }

    //TODO
    @Override
    public String createDocumentFromSource(String repositoryId, String sourceId, Properties properties, String folderId, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
        return super.createDocumentFromSource(repositoryId, sourceId, properties, folderId, versioningState, policies, addAces, removeAces, extension);
    }

    //DONE
    @Override
    public String createFolder(String repositoryId, Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension)
                .createFolder(properties, folderId, policies, addAces, removeAces);
    }

    //TODO
    @Override
    public String createItem(String repositoryId, Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
        return super.createItem(repositoryId, properties, folderId, policies, addAces, removeAces, extension);
    }

    //TODO
    @Override
    public String createPolicy(String repositoryId, Properties properties, String folderId, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
        return super.createPolicy(repositoryId, properties, folderId, policies, addAces, removeAces, extension);
    }

    //TODO
    @Override
    public String createRelationship(String repositoryId, Properties properties, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
        return super.createRelationship(repositoryId, properties, policies, addAces, removeAces, extension);
    }

    //TODO
    @Override
    public void deleteContentStream(String repositoryId, Holder<String> objectId, Holder<String> changeToken, ExtensionsData extension) {
        super.deleteContentStream(repositoryId, objectId, changeToken, extension);
    }

    //DONE
    @Override
    public void deleteObject(String repositoryId, String objectId, Boolean allVersions, ExtensionsData extension) {
        getEloCmisRepository(repositoryId, extension)
                .deleteObject(objectId, allVersions);
    }

    //TODO
    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId, Boolean allVersions, UnfileObject unfileObjects, Boolean continueOnFailure, ExtensionsData extension) {
        return super.deleteTree(repositoryId, folderId, allVersions, unfileObjects, continueOnFailure, extension);
    }

    //TODO
    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId, String objectId, Boolean allVersions, ExtensionsData extension) {

    }

    //TODO
    @Override
    public AllowableActions getAllowableActions(String repositoryId, String objectId, ExtensionsData extension) {
        return super.getAllowableActions(repositoryId, objectId, extension);
    }

    //DONE
    @Override
    public ContentStream getContentStream(String repositoryId, String objectId, String streamId, BigInteger offset, BigInteger length, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension)
                .getContentStream(objectId, streamId, offset, length);
    }

    //DONE
    @Override
    public ObjectData getObject(String repositoryId, String objectId, String filter, Boolean includeAllowableActions,
                                IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds,
                                Boolean includeAcl, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension)
                .getObject(objectId, filter, includeAllowableActions, includeRelationships,
                        renditionFilter, includePolicyIds, includeAcl);
    }

    //DONE
    @Override
    public ObjectData getObjectByPath(String repositoryId, String path, String filter, Boolean includeAllowableActions,
                                      IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds,
                                      Boolean includeAcl, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension)
                .getObjectByPath(path, filter, includeAllowableActions, includeRelationships,
                        renditionFilter, includePolicyIds, includeAcl);
    }

    //TODO
    @Override
    public Properties getProperties(String repositoryId, String objectId, String filter, ExtensionsData extension) {
        return super.getProperties(repositoryId, objectId, filter, extension);
    }

    //TODO
    @Override
    public List<RenditionData> getRenditions(String repositoryId, String objectId, String renditionFilter, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {
        return super.getRenditions(repositoryId, objectId, renditionFilter, maxItems, skipCount, extension);
    }

    //TODO
    @Override
    public void moveObject(String repositoryId, Holder<String> objectId, String targetFolderId, String sourceFolderId, ExtensionsData extension) {
        super.moveObject(repositoryId, objectId, targetFolderId, sourceFolderId, extension);
    }

    //TODO
    @Override
    public void setContentStream(String repositoryId, Holder<String> objectId, Boolean overwriteFlag, Holder<String> changeToken, ContentStream contentStream, ExtensionsData extension) {
        getEloCmisRepository(repositoryId, extension)
                .setContentStream(objectId, overwriteFlag, changeToken, contentStream);
    }

    //DONE
    @Override
    public void updateProperties(String repositoryId, Holder<String> objectId, Holder<String> changeToken, Properties properties, ExtensionsData extension) {
        getEloCmisRepository(repositoryId, extension)
                .updateProperties(objectId, changeToken, properties);
    }


    // -----------------------------------------------------------------------------------------------------------------
    // VERSIONING SERVICES
    // -----------------------------------------------------------------------------------------------------------------

    //DONE
    @Override
    public void cancelCheckOut(String repositoryId, String objectId, ExtensionsData extension) {
        /*ELO: If the document is only to be unlocked, see checkinSord.*/
        getEloCmisRepository(repositoryId, extension).cancelCheckOut(objectId);
    }

    //DONE
    @Override
    public void checkIn(String repositoryId, Holder<String> objectId, Boolean major, Properties properties, ContentStream contentStream, String checkinComment, List<String> policies, Acl addAces, Acl removeAces, ExtensionsData extension) {
        getEloCmisRepository(repositoryId, extension).
                checkIn(objectId, major, properties, contentStream, checkinComment, policies, addAces, removeAces);
    }

    /**
     * Create a private working copy of the document.
     *
     * @param repositoryId  the identifier for the repository
     * @param objectId      input: the identifier for the document that should be checked
     *                      out, output: the identifier for the newly created PWC
     * @param contentCopied output: indicator if the content of the original document has
     *                      been copied to the PWC
     */
    //TODO
    @Override
    public void checkOut(String repositoryId, Holder<String> objectId, ExtensionsData extension, Holder<Boolean> contentCopied) {
        getEloCmisRepository(repositoryId, extension).checkOut(objectId, contentCopied);
    }

    //TODO
    @Override
    public List<ObjectData> getAllVersions(String repositoryId, String objectId, String versionSeriesId, String filter, Boolean includeAllowableActions, ExtensionsData extension) {
        return getEloCmisRepository(repositoryId, extension).
                getAllVersions(objectId, versionSeriesId, filter, includeAllowableActions);
    }

    //TODO
    @Override
    public ObjectData getObjectOfLatestVersion(String repositoryId, String objectId, String versionSeriesId, Boolean major, String filter, Boolean includeAllowableActions, IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds, Boolean includeAcl, ExtensionsData extension) {
        return super.getObjectOfLatestVersion(repositoryId, objectId, versionSeriesId, major, filter, includeAllowableActions, includeRelationships, renditionFilter, includePolicyIds, includeAcl, extension);
    }

    //TODO
    @Override
    public Properties getPropertiesOfLatestVersion(String repositoryId, String objectId, String versionSeriesId, Boolean major, String filter, ExtensionsData extension) {
        return super.getPropertiesOfLatestVersion(repositoryId, objectId, versionSeriesId, major, filter, extension);
    }

}
