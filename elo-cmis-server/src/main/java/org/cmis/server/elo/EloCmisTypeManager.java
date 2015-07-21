package org.cmis.server.elo;

import de.elo.extension.service.EloUtilsService;
import de.elo.ix.client.*;
import de.elo.utils.net.RemoteException;
import org.apache.chemistry.opencmis.commons.definitions.*;
import org.apache.chemistry.opencmis.commons.enums.*;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.commons.lang3.StringUtils;
import org.cmis.base.BaseTypeManager;

import java.math.BigInteger;
import java.util.*;

/**
 * Manages the type definitions for all cmis elo repositories.
 */
public class EloCmisTypeManager extends BaseTypeManager<EloCmisService> {

    private final static Map<String, Map<String, TypeDefinition>> typeDefinitionsMap = new HashMap<>();
    private final static Map<String, Date> typeDateMap = new HashMap<>();

    private static final int BASE_DOCUMENT_MASK_ID = 0;
    private static final int BASE_FOLDER_MASK_ID = 0;
    public static final int NOT_FOUND_MASK_ID = -1;


    private static final String NAMESPACE = "elo";
    private static final String PROPERTY_ID_PREFIX_ELO = NAMESPACE + ":" + "property-";
    private static final String PROPERTY_NAME_PREFIX_ELO = NAMESPACE + ":" + "property~";
    private static final String MASK_ID_PREFIX_ELO = "-mask-";
    private static final String MASK_NAME_PREFIX_ELO = "~mask~";

    public EloCmisTypeManager(EloCmisService cmisService) {
        super(cmisService);
        String repositoryId = getRepositoryId();
        if (repositoryId != null) {
            Map<String, TypeDefinition> typeDefinitionMap = getTypeDefinitions();
        }
    }

    //TODO - to be replaced using spring cache
    private static void setTypeDefinitionsMapCache(String repositoryKey, Map<String, TypeDefinition> typeDefinitionMap) {
        synchronized (typeDefinitionsMap) {
            typeDateMap.put(repositoryKey, new Date());
            typeDefinitionsMap.put(repositoryKey, typeDefinitionMap);
        }
    }

    //TODO - to be replaced using spring cache
    private static Map<String, TypeDefinition> getTypeDefinitionsMapCache(String repositoryKey) {
        synchronized (typeDefinitionsMap) {
            return typeDefinitionsMap.get(repositoryKey);
        }
    }

    //TODO - to be replaced using spring cache
    private static void setTypeDefinitionCache(String repositoryKey, String typeId, TypeDefinition typeDefinition) {
        synchronized (typeDefinitionsMap) {
            if (typeDefinitionsMap.get(repositoryKey) == null) {
                typeDefinitionsMap.put(repositoryKey, new HashMap<String, TypeDefinition>());
            }
            if (typeDefinition != null) {
                typeDefinitionsMap.get(repositoryKey).put(typeId, typeDefinition);
            } else {
                typeDefinitionsMap.get(repositoryKey).remove(typeId);
            }
        }
    }

    //TODO - add spring/ehcache cache
    private static TypeDefinition getTypeDefinitionCache(String repositoryKey, String typeId) {
        try {
            synchronized (typeDefinitionsMap) {
                return typeDefinitionsMap.get(repositoryKey).get(typeId);
            }
        } catch (NullPointerException e) {
            //
        }
        return null;
    }


    //------------------------------------------------------------------------------------------------------------------
    public List<Sord> filterCmisSords(List<Sord> sordList) {
        List<Sord> sordFilterList = new ArrayList<Sord>();
        for (Sord sord : sordList) {
            if (isCmisSord(sord)) {
                sordFilterList.add(sord);
            }
        }
        return sordFilterList;
    }

    public boolean isCmisSord(Sord sord) {
        boolean isCmisSord;
        BaseTypeId baseTypeId = getBaseTypeId(sord);
        String typeId = getTypeId(sord);
        isCmisSord = (baseTypeId != null && (getTypeDefinition(typeId) != null));
        return isCmisSord;
    }


    //------------------------------------------------------------------------------------------------------------------
    private static BaseTypeId getBaseTypeId(String typeIdOrName) {
        if (typeIdOrName == null) {
            throw new CmisNotSupportedException("The type identifier can not be null.");
        }
        for (BaseTypeId baseTypeId : BaseTypeId.values()) {
            if (typeIdOrName.startsWith(baseTypeId.value())) {
                return baseTypeId;
            }
        }
        throw new CmisNotSupportedException("The provided type identifier [" + typeIdOrName + "] is not supported. Accepted format is {baseType}[-mask-{maskId}] or {baseType}[~mask~{maskName}].");
    }

    public static BaseTypeId getBaseTypeId(Sord sord) {
        //sord is root folder
        if (EloUtilsService.isRootFolder(sord)) {
            return BaseTypeId.CMIS_FOLDER;
        }
        //sord is folder
        if (EloUtilsService.isFolder(sord)) {
            return BaseTypeId.CMIS_FOLDER;
        }
        //sord is document
        if (EloUtilsService.isDocument(sord)) {
            return BaseTypeId.CMIS_DOCUMENT;
        }
        return null;
    }

    public static String getTypeName(BaseTypeId baseTypeId, int maskId, String maskName) {
        return baseTypeId.value() + (maskId == getBaseMaskId(baseTypeId) ? "" : MASK_NAME_PREFIX_ELO + maskName);
    }

    public static String getTypeId(BaseTypeId baseTypeId, int maskId) {
        return baseTypeId.value() + (maskId == getBaseMaskId(baseTypeId) ? "" : MASK_ID_PREFIX_ELO + maskId);
    }

    public static String getTypeId(Sord sord) {
        return getTypeId(getBaseTypeId(sord), sord.getMask());
    }

    public static String getTypeId(String repositoryId, String typeIdOrName) {
        if (StringUtils.isEmpty(typeIdOrName)) {
            return typeIdOrName;
        }
        Map<String, TypeDefinition> typeDefinitionsMapCache = getTypeDefinitionsMapCache(repositoryId);
        if (typeDefinitionsMapCache != null) {
            BaseTypeId baseTypeId = getBaseTypeId(typeIdOrName);
            if (baseTypeId != null) {
                for (TypeDefinition typeDefinition : typeDefinitionsMapCache.values()) {
                    if (typeIdOrName.equals(baseTypeId.value() + MASK_NAME_PREFIX_ELO + typeDefinition.getLocalName())) {
                        return typeDefinition.getId();
                    }
                }
            }
        }
        return typeIdOrName;
    }

    //------------------------------------------------------------------------------------------------------------------
    private static int getBaseMaskId(BaseTypeId baseTypeId) {
        if (BaseTypeId.CMIS_FOLDER.equals(baseTypeId)) {
            return BASE_FOLDER_MASK_ID;
        }
        if (BaseTypeId.CMIS_DOCUMENT.equals(baseTypeId)) {
            return BASE_DOCUMENT_MASK_ID;
        }
        throw new CmisNotSupportedException("The type " + baseTypeId + " is not supported in ELO");
    }

    private static int getMaskId(String typeId) {
        //TypeId format must be {baseType}[-mask-{maskId}]
        int maskId = NOT_FOUND_MASK_ID;
        BaseTypeId baseTypeId;
        if (typeId != null) {
            baseTypeId = getBaseTypeId(typeId);
            if (baseTypeId != null) {
                if (typeId.equals(baseTypeId.value())) {
                    maskId = getBaseMaskId(baseTypeId);
                } else {
                    if (typeId.contains(MASK_ID_PREFIX_ELO)) {
                        maskId = Integer.parseInt(typeId.substring(typeId.indexOf(MASK_ID_PREFIX_ELO) + MASK_ID_PREFIX_ELO.length()));
                    }
                }
            }
        }
        if (maskId == NOT_FOUND_MASK_ID) {
            throw new CmisNotSupportedException("Unable to find corespondent maskId for the [" + typeId + "] cmis type. Accepted format is {baseType}[-mask-{maskId}].");
        }
        return maskId;
    }

    public static int getMaskId(String repositoryId, String typeIdOrName) {
        //fix to accept as types cmisTypeId and cmisTypeName
        String typeId = getTypeId(repositoryId, typeIdOrName);
        return getMaskId(typeId);
    }

    //------------------------------------------------------------------------------------------------------------------

    public static String getPropertyId(String repositoryId, String typeIdOrName, String propertyIdOrName) {
        String cmisPropertyId = propertyIdOrName;
        String cmisTypeId = getTypeId(repositoryId, typeIdOrName);
        TypeDefinition typeDefinition = getTypeDefinitionCache(repositoryId, cmisTypeId);
        if (typeDefinition != null) {
            if (!typeDefinition.getPropertyDefinitions().containsKey(propertyIdOrName)) {
                for (PropertyDefinition propertyDefinition : typeDefinition.getPropertyDefinitions().values()) {
                    if (propertyIdOrName.equals(PROPERTY_NAME_PREFIX_ELO + typeDefinition.getDisplayName())) {
                        return propertyDefinition.getId();
                    }
                }
            }
        }
        return cmisPropertyId;
    }

    public static String getPropertyId(int objKeyId) {
        return PROPERTY_ID_PREFIX_ELO + objKeyId;
    }

    public static boolean isCustomProperty(String propertyId) {
        return propertyId.startsWith(PROPERTY_ID_PREFIX_ELO);
    }

    public static int getObjKeyId(String propertyId) {
        return Integer.parseInt(propertyId.substring(PROPERTY_ID_PREFIX_ELO.length()));
    }

    private static Map<String, TypeDefinition> retrieveTypeDefinitionMapElo(IXConnection ixConnection) {
        Map<String, TypeDefinition> typeDefinitionMap = new HashMap<String, TypeDefinition>();
        MaskName[] maskNames;
        boolean hasBaseFolderMaskId = false;
        boolean hasBaseDocumentMaskId = false;
        String typeId;

        try {
            maskNames = EloUtilsService.getMasks(ixConnection);
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }

        //validate existence of the base mask folder and base mask document
        for (MaskName maskName : maskNames) {
            if ((maskName.getId() == BASE_FOLDER_MASK_ID) && maskName.isFolderMask()) {
                hasBaseFolderMaskId = true;
            }
            if ((maskName.getId() == BASE_DOCUMENT_MASK_ID) && maskName.isDocumentMask()) {
                hasBaseDocumentMaskId = true;
            }
        }
        if (!hasBaseDocumentMaskId) {
            typeId = getTypeId(BaseTypeId.CMIS_DOCUMENT, BASE_DOCUMENT_MASK_ID);
            if (!(typeDefinitionMap.containsKey(typeId))) {
                throw new CmisRuntimeException("The default document type [" + BaseTypeId.CMIS_DOCUMENT.value() + "] can not be mapped on maskId=" + BASE_DOCUMENT_MASK_ID + ". Please check that the maskId exists and is configured as document mask.");
            }
        }
        if (!hasBaseFolderMaskId) {
            typeId = getTypeId(BaseTypeId.CMIS_FOLDER, BASE_FOLDER_MASK_ID);
            if (!(typeDefinitionMap.containsKey(typeId))) {
                throw new CmisRuntimeException("The default folder type [" + BaseTypeId.CMIS_FOLDER.value() + "] can not be mapped on maskId=" + BASE_FOLDER_MASK_ID + ". Please check that the maskId exists and is configured as folder mask.");
            }
        }

        //add to CMISTypes only masks that are relevant for DMS functionality (FOLDER and DOCUMENT) (for example SEARCH masks are ignored for DMS)
        for (MaskName maskName : maskNames) {
            if (maskName.isFolderMask()) {
                typeId = getTypeId(BaseTypeId.CMIS_FOLDER, maskName.getId());
                TypeDefinition typeDefinition = retrieveTypeDefinitionElo(typeDefinitionMap, ixConnection, typeId);
                typeDefinitionMap.put(typeId, typeDefinition);
            }
            if (maskName.isDocumentMask()) {
                typeId = getTypeId(BaseTypeId.CMIS_DOCUMENT, maskName.getId());
                TypeDefinition typeDefinition = retrieveTypeDefinitionElo(typeDefinitionMap, ixConnection, typeId);
                typeDefinitionMap.put(typeId, typeDefinition);
            }
        }
        return typeDefinitionMap;
    }

    private static TypeDefinition retrieveTypeDefinitionElo(Map<String, TypeDefinition> typeDefinitionMap, IXConnection ixConnection, String typeId) {
        TypeDefinition typeDefinition = null;
        String parentTypeId = null;
        int maskId = NOT_FOUND_MASK_ID;
        BaseTypeId baseTypeId = null;

        //try get it from imput variable (cached)
        baseTypeId = getBaseTypeId(typeId);
        maskId = getMaskId(typeId);
        typeDefinition = typeDefinitionMap.get(typeId);
        if (typeDefinition != null) {
            return typeDefinition;
        }

        try {
            DocMask docMask = ixConnection.ix().checkoutDocMask(String.valueOf(maskId), DocMaskC.mbAll, LockC.NO);
            parentTypeId = baseTypeId.value();

            if (typeDefinition == null && baseTypeId.equals(BaseTypeId.CMIS_DOCUMENT) && docMask.getDetails().isDocumentMask()) {
                MutableDocumentTypeDefinition documentType = null;
                if (typeId.equals(parentTypeId)) {
                    documentType = getDefaultTypeDefinitionFactory().createBaseDocumentTypeDefinition(CmisVersion.CMIS_1_1);
                } else {
                    documentType = getDefaultTypeDefinitionFactory().createDocumentTypeDefinition(CmisVersion.CMIS_1_1, parentTypeId);
                    documentType.setId(typeId); //parentTypeId + MASK_ID_PREFIX_ELO + docMask.getName()
                    documentType.setBaseTypeId(baseTypeId);
                    documentType.setLocalName(docMask.getName());
                    documentType.setDisplayName(docMask.getName());
                    documentType.setDescription(docMask.getText());
                }
                removeQueryableAndOrderableFlags(documentType);
                typeDefinition = documentType;
            }

            if (typeDefinition == null && baseTypeId.equals(BaseTypeId.CMIS_FOLDER) && docMask.getDetails().isFolderMask()) {
                MutableFolderTypeDefinition folderType = null;
                if (typeId.equals(parentTypeId)) {
                    folderType = getDefaultTypeDefinitionFactory().createBaseFolderTypeDefinition(CmisVersion.CMIS_1_1);
                } else {
                    folderType = getDefaultTypeDefinitionFactory().createFolderTypeDefinition(CmisVersion.CMIS_1_1, parentTypeId);
                    folderType.setId(typeId); //parentTypeId + MASK_ID_PREFIX_ELO + docMask.getName()
                    folderType.setBaseTypeId(baseTypeId);
                    folderType.setLocalName(docMask.getName());
                    folderType.setDisplayName(docMask.getName());
                    folderType.setDescription(docMask.getText());
                }
                removeQueryableAndOrderableFlags(folderType);
                typeDefinition = folderType;
            }

            //add property definitions
            MutableTypeDefinition mutableTypeDefinition = (MutableTypeDefinition) typeDefinition;

            // copy parent type property definitions and mark them as inherited
            if (!typeId.equals(parentTypeId)) {
                TypeDefinition parentTypeDefinition = retrieveTypeDefinitionElo(typeDefinitionMap, ixConnection, parentTypeId);
                for (PropertyDefinition<?> propertyDefinition : parentTypeDefinition.getPropertyDefinitions().values()) {
                    MutablePropertyDefinition<?> mutablePropertyDefinition = getDefaultTypeDefinitionFactory().copy(propertyDefinition);
                    mutablePropertyDefinition.setIsInherited(true);
                    mutableTypeDefinition.addPropertyDefinition(mutablePropertyDefinition);
                }
            }

            // add specific mask properties
            for (DocMaskLine docMaskLine : docMask.getLines()) {
                //PropertyDefinition propertyDefinition = null;
                MutablePropertyDefinition mutablePropertyDefinition = null;
                if (docMaskLine.getType() == DocMaskLineC.TYPE_NUMBER ||
                        docMaskLine.getType() == DocMaskLineC.TYPE_NUMBER_F1 ||
                        docMaskLine.getType() == DocMaskLineC.TYPE_NUMBER_F2 ||
                        docMaskLine.getType() == DocMaskLineC.TYPE_NUMBER_F4 ||
                        docMaskLine.getType() == DocMaskLineC.TYPE_NUMBER_F6) {
                    MutablePropertyDecimalDefinition propertyDecimalDefinition = new PropertyDecimalDefinitionImpl();
                    propertyDecimalDefinition.setPropertyType(PropertyType.DECIMAL);
                    //propertyDecimalDefinition.setMinValue(BigDecimal.valueOf(docMaskLine.getMin()));
                    //propertyDecimalDefinition.setMaxValue(BigDecimal.valueOf(docMaskLine.getMax()));
                    if (docMaskLine.getDefaultValue() != null) {
                        //propertyDecimalDefinition.setDefaultValue((List<BigDecimal>) BigDecimal.valueOf(Double.valueOf(docMaskLine.getDefaultValue())));
                    }
                    mutablePropertyDefinition = propertyDecimalDefinition;
                }
                if (docMaskLine.getType() == DocMaskLineC.TYPE_NUMBER_F0) {
                    MutablePropertyIntegerDefinition propertyIntegerDefinition = new PropertyIntegerDefinitionImpl();
                    propertyIntegerDefinition.setPropertyType(PropertyType.INTEGER);
                    //propertyDecimalDefinition.setMinValue(BigDecimal.valueOf(docMaskLine.getMin()));
                    //propertyDecimalDefinition.setMaxValue(BigDecimal.valueOf(docMaskLine.getMax()));
                    if (docMaskLine.getDefaultValue() != null) {
                        //propertyDecimalDefinition.setDefaultValue((List<BigDecimal>) BigDecimal.valueOf(Double.valueOf(docMaskLine.getDefaultValue())));
                    }
                    mutablePropertyDefinition = propertyIntegerDefinition;
                }
                if (docMaskLine.getType() == DocMaskLineC.TYPE_DATE || docMaskLine.getType() == DocMaskLineC.TYPE_ISO_DATE) {
                    MutablePropertyDateTimeDefinition propertyDateTimeDefinition = new PropertyDateTimeDefinitionImpl();
                    propertyDateTimeDefinition.setPropertyType(PropertyType.DATETIME);
                    mutablePropertyDefinition = propertyDateTimeDefinition;

                }
                if (docMaskLine.getType() == DocMaskLineC.TYPE_USER) {
                    MutablePropertyStringDefinition propertyStringDefinition = new PropertyStringDefinitionImpl();
                    propertyStringDefinition.setPropertyType(PropertyType.STRING);
                    mutablePropertyDefinition = propertyStringDefinition;
                }


                if (docMaskLine.getType() == DocMaskLineC.TYPE_TEXT) {
                    MutablePropertyStringDefinition propertyStringDefinition = new PropertyStringDefinitionImpl();
                    propertyStringDefinition.setPropertyType(PropertyType.STRING);
                    propertyStringDefinition.setMaxLength(BigInteger.valueOf(docMaskLine.getMax()));
                    if (docMaskLine.getDefaultValue() != null) {
                        //propertyStringDefinition.setDefaultValue(Arrays.asList(docMaskLine.getDefaultValue()));
                    }

                    mutablePropertyDefinition = propertyStringDefinition;
                }


                if (mutablePropertyDefinition != null) {
                    //String propertyId = StringUtils.uncapitalize(WordUtils.capitalizeFully(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(docMaskLine.getName()), " "), " ".toCharArray())).replaceAll("[^A-Za-z0-9]", "");
                    //String propertyId = String.valueOf(docMaskLine.getId());
                    mutablePropertyDefinition.setId(getPropertyId(docMaskLine.getId()));
                    mutablePropertyDefinition.setLocalNamespace(NAMESPACE);
                    mutablePropertyDefinition.setLocalName(docMaskLine.getKey());
                    mutablePropertyDefinition.setDisplayName(docMaskLine.getName());
                    mutablePropertyDefinition.setDescription(docMaskLine.getComment());
                    mutablePropertyDefinition.setUpdatability(docMaskLine.isReadOnly() ? Updatability.READONLY : (docMaskLine.isCanEdit() ? Updatability.READWRITE : Updatability.ONCREATE));
                    mutablePropertyDefinition.setQueryName(mutablePropertyDefinition.getId());
                    mutablePropertyDefinition.setIsInherited(false);
                    mutablePropertyDefinition.setCardinality(docMaskLine.getType() == DocMaskLineC.TYPE_LIST ? Cardinality.MULTI : Cardinality.SINGLE);
                    mutablePropertyDefinition.setIsQueryable(false);
                    mutablePropertyDefinition.setIsOrderable(false);
                    mutablePropertyDefinition.setIsRequired(docMaskLine.getMin() > 0);
                    mutableTypeDefinition.addPropertyDefinition(mutablePropertyDefinition);
                }
            }

        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }

        typeDefinitionMap.put(typeDefinition.getId(), typeDefinition);
        return typeDefinition;
    }

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    private String getRepositoryId() {
        return this.getCmisService().getCallContext().getRepositoryId();
    }

    private IXConnection getIXConnection() {
        return this.getCmisService().getConnection();
    }

    private Map<String, TypeDefinition> getTypeDefinitions(boolean forceRefresh) {
        String repositoryId = this.getCmisService().getCallContext().getRepositoryId();
        Map<String, TypeDefinition> typeDefinitionMap = getTypeDefinitionsMapCache(repositoryId);
        if (typeDefinitionMap == null || forceRefresh) {
            IXConnection ixConnection = this.getCmisService().getConnection();
            typeDefinitionMap = retrieveTypeDefinitionMapElo(ixConnection);
            setTypeDefinitionsMapCache(repositoryId, typeDefinitionMap);
        }
        return typeDefinitionMap;
    }

    @Override
    protected Map<String, TypeDefinition> getTypeDefinitions() {
        return getTypeDefinitions(false);
        //getDefaultTypeDefinitionFactory().copy(type, true, this.cmisService.getCallContext().getCmisVersion());
    }

    @Override
    public TypeDefinition createType(TypeDefinition type) {
        createEloStorageMask(type);
        //todo - call CMIS getType (cca)
        return type;
    }

    @Override
    public TypeDefinition updateType(TypeDefinition type) {
        throw new CmisNotSupportedException("Not supported!");
    }

    @Override
    public void deleteType(String typeId) {
        //fix to accept as types cmisTypeId and cmisTypeName
        typeId = getTypeId(getRepositoryId(), typeId);
        deleteEloStorageMask(typeId);
    }

    @Override
    public TypeDefinitionList getTypeChildren(String typeId, Boolean includePropertyDefinitions, BigInteger maxItems, BigInteger skipCount) {
        //fix to accept as types cmisTypeId and cmisTypeName
        typeId = getTypeId(getRepositoryId(), typeId);
        return getDefaultTypeDefinitionFactory().createTypeDefinitionList(getTypeDefinitions(), typeId, includePropertyDefinitions, maxItems, skipCount, this.getCmisService().getCallContext().getCmisVersion());
    }

    @Override
    public TypeDefinition getTypeDefinition(String typeId) {
        Map<String, TypeDefinition> typeDefinitionMap = getTypeDefinitions();

        //fix to accept as types cmisTypeId and cmisTypeName
        typeId = getTypeId(getRepositoryId(), typeId);

        TypeDefinition typeDefinition = typeDefinitionMap.get(typeId);
        if (typeDefinition == null) {
            IXConnection ixConnection = this.getCmisService().getConnection();
            typeDefinition = retrieveTypeDefinitionElo(typeDefinitionMap, ixConnection, typeId);
            if (typeDefinition == null) {
                throw new CmisObjectNotFoundException("Type '" + typeId + "' is unknown!");
            } else {
                String repositoryId = this.getCmisService().getCallContext().getRepositoryId();
                setTypeDefinitionCache(repositoryId, typeId, typeDefinition);
            }
        }
        return getDefaultTypeDefinitionFactory().copy(typeDefinition, true, this.getCmisService().getCallContext().getCmisVersion());
    }

    @Override
    public List<TypeDefinitionContainer> getTypeDescendants(String typeId, BigInteger depth, Boolean includePropertyDefinitions) {
        Map<String, TypeDefinition> typeDefinitionMap = getTypeDefinitions();

        //fix to accept as types cmisTypeId and cmisTypeName
        typeId = getTypeId(getRepositoryId(), typeId);

        return getDefaultTypeDefinitionFactory().createTypeDescendants(typeDefinitionMap, typeId, depth, includePropertyDefinitions, this.getCmisService().getCallContext().getCmisVersion());
    }


    private DocMask createEloStorageMask(TypeDefinition typeDefinition) {
        IXConnection ixConnection = this.getCmisService().getConnection();
        DocMask docMask = null;
        try {
            // create storage mask
            docMask = ixConnection.ix().createDocMask(null);

            // set mask values
            docMask.setName(typeDefinition.getLocalName());
            docMask.setText(typeDefinition.getDescription());
            BaseTypeId baseTypeId = typeDefinition.getBaseTypeId();
            if (BaseTypeId.CMIS_DOCUMENT.equals(baseTypeId)) {
                docMask.getDetails().setDocumentMask(true); // can be used as template for documents and structure elements
                docMask.getDetails().setArchivingMode(ArchivingModeC.VERSION); // documents of this mask are stored with version controll
            } else if (BaseTypeId.CMIS_FOLDER.equals(baseTypeId)) {
                docMask.getDetails().setFolderMask(true);
            } else {
                throw new CmisNotSupportedException(
                        String.format("New types in ELO-CMIS server must inherit from %s or %s",
                                BaseTypeId.CMIS_FOLDER.value(), BaseTypeId.CMIS_DOCUMENT.value()));
            }
            docMask.getDetails().setSearchMask(true); // can be used to search for archive items
            // get type properties; every property is equivalent to a DocMaskLine object
            Map<String, PropertyDefinition<?>> propertyDefinitions = typeDefinition.getPropertyDefinitions();
            DocMaskLine[] maskLines = new DocMaskLine[typeDefinition.getPropertyDefinitions().size()];
            docMask.setLines(maskLines);

            /* the property cmisId must be present in type's properties and assigned to the mask
            because ELO generates integer ids for masks; cmisId will be the maskId in CMIS (a mask will have two ids) */
            int step = 0;
            for (Map.Entry<String, PropertyDefinition<?>> entry : propertyDefinitions.entrySet()) {
                System.out.println(entry.getKey() + "/" + entry.getValue());
                maskLines[step] = new DocMaskLine();
                maskLines[step].setId(step);
                maskLines[step].setName(entry.getValue().getDisplayName());
                maskLines[step].setKey(entry.getValue().getLocalName());
                maskLines[step].setComment(entry.getValue().getDescription());
                Updatability updatability = entry.getValue().getUpdatability();
                switch (updatability) {
                    case READONLY:
                        maskLines[step].setReadOnly(true);
                        maskLines[step].setCanEdit(false);
                        break;
                    case READWRITE:
                        maskLines[step].setReadOnly(false);
                        maskLines[step].setCanEdit(true);
                        break;
                    case WHENCHECKEDOUT:
                        throw new CmisNotSupportedException("Updatability WHENCHECKEDOUT not supported!");
                    case ONCREATE:
                        maskLines[step].setReadOnly(false);
                        maskLines[step].setCanEdit(false);
                        break;
                }

                PropertyType propertyType = entry.getValue().getPropertyType();
                switch (propertyType) {
                    case STRING:
                        maskLines[step].setType(DocMaskLineC.TYPE_TEXT);
                        break;
                    case BOOLEAN:
                        //DocMaskLineC.TYPE_NUMBER_F0 : Index line contains a number value without a fraction part.
                        //{0,1} values will be used for {false, true}
                        maskLines[step].setType(DocMaskLineC.TYPE_NUMBER_F0);
                        break;
                    case ID:
                        maskLines[step].setType(DocMaskLineC.TYPE_NUMBER_F0);
                        break;
                    case INTEGER:
                        maskLines[step].setType(DocMaskLineC.TYPE_NUMBER_F0);
                        break;
                    case DATETIME:
                        maskLines[step].setType(DocMaskLineC.TYPE_DATE);
                        break;
                    case DECIMAL:
                        maskLines[step].setType(DocMaskLineC.TYPE_NUMBER);
                        break;
                    case HTML:
                        maskLines[step].setType(DocMaskLineC.TYPE_TEXT);
                        break;
                    case URI:
                        maskLines[step].setType(DocMaskLineC.TYPE_TEXT);
                        break;
                }

                maskLines[step].setLabelCol(1);
                maskLines[step].setLabelRow(4 + step);
                maskLines[step].setEditCol(14);
                maskLines[step].setEditRow(4 + step);
                maskLines[step].setEditWidth(20);

                step++;
            }

            // checkin
            int id = ixConnection.ix().checkinDocMask(docMask, DocMaskC.mbAll, LockC.NO);
            // checkout
            docMask = ixConnection.ix().checkoutDocMask(docMask.getName(), DocMaskC.mbAll, LockC.NO);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return docMask;
    }

    private boolean deleteEloStorageMask(String maskId) {
        IXConnection ixConnection = this.getCmisService().getConnection();
        try {
            /*Deletes a storage mask.
            If archive entries connected to the mask still exist in the database, another mask assignMaskId can be assigned to them.
            If assignMaskId is not defined and there are objects connected to the mask in the database the method throws an exception.*/
            return ixConnection.ix().deleteDocMask(maskId, null, LockC.NO);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }
}
