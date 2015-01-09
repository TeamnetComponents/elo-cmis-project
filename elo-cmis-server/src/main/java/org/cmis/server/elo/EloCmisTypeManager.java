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
import org.apache.commons.lang.StringUtils;
import org.cmis.base.BaseTypeManager;

import java.math.BigInteger;
import java.util.*;

/**
 * Manages the type definitions for all cmis elo repositories.
 */
public class EloCmisTypeManager extends BaseTypeManager<EloCmisService> {

    //private final static Map<String, Map<Integer, MaskName>> typeEloMaskNamesMap = new ConcurrentHashMap<>();
    private final static Map<String, Map<String, TypeDefinition>> typeDefinitionsMap = new HashMap<>();
    private final static Map<String, Date> typeDateMap = new HashMap<>();

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

    private static int getRootBaseTypeIdElo(BaseTypeId cmisBaseTypeId, IXConnection ixConnection) {
        if (BaseTypeId.CMIS_FOLDER.equals(cmisBaseTypeId)) {
            return 0;
        }
        if (BaseTypeId.CMIS_DOCUMENT.equals(cmisBaseTypeId)) {
            return 0;
        }
        throw new CmisNotSupportedException("The type " + cmisBaseTypeId + " is not supported in ELO");
    }

    public static int convertCmisType2EloMaskId(String repositoryId, String cmisTypeIdOrName) {
        //fix to accept as types cmisTypeId and cmisTypeName
        String cmisTypeId = calculateCmisTypeId(repositoryId, cmisTypeIdOrName);
        return convertCmisTypeId2EloMaskId(cmisTypeId);
    }

    private static int convertCmisTypeId2EloMaskId(String cmisTypeId) {
        if (cmisTypeId.contains(MASK_ID_PREFIX_ELO)) {
            return Integer.parseInt(cmisTypeId.substring(cmisTypeId.indexOf(MASK_ID_PREFIX_ELO) + MASK_ID_PREFIX_ELO.length()));
        }
        return 0;
    }

    //eloMask to cmisTypeId
    public static String convertEloMask2CmisTypeId(MaskName maskName, BaseTypeId baseTypeId, IXConnection ixConnection) {
        return baseTypeId.value() + (maskName.getId() == getRootBaseTypeIdElo(baseTypeId, ixConnection) ? "" : MASK_ID_PREFIX_ELO + maskName.getId());
    }

    //eloMask to cmisTypeName
    public static String convertEloMask2CmisTypeName(MaskName maskName, BaseTypeId baseTypeId, IXConnection ixConnection) {
        return baseTypeId.value() + (maskName.getId() == getRootBaseTypeIdElo(baseTypeId, ixConnection) ? "" : MASK_NAME_PREFIX_ELO + maskName.getName());
    }

    public static String calculateCmisPropertyId(String repositoryId, String cmisTypeIdOrName, String cmisPropertyIdOrName) {
        String cmisPropertyId = cmisPropertyIdOrName;
        String cmisTypeId = calculateCmisTypeId(repositoryId, cmisTypeIdOrName);
        TypeDefinition typeDefinition = getTypeDefinitionCache(repositoryId, cmisTypeId);
        if (typeDefinition != null) {
            if (!typeDefinition.getPropertyDefinitions().containsKey(cmisPropertyIdOrName)) {
                for (PropertyDefinition propertyDefinition : typeDefinition.getPropertyDefinitions().values()) {
                    if (cmisPropertyIdOrName.equals(PROPERTY_NAME_PREFIX_ELO + typeDefinition.getDisplayName())) {
                        return propertyDefinition.getId();
                    }
                }
            }
        }
        return cmisPropertyId;
    }

    public static String calculateCmisTypeId(String repositoryId, String cmisTypeIdOrName) {
        if (StringUtils.isEmpty(cmisTypeIdOrName)) {
            return cmisTypeIdOrName;
        }
        Map<String, TypeDefinition> typeDefinitionsMapCache = getTypeDefinitionsMapCache(repositoryId);
        if (typeDefinitionsMapCache != null) {
            BaseTypeId baseTypeId = null;
            if (cmisTypeIdOrName.startsWith(BaseTypeId.CMIS_FOLDER.value())) {
                baseTypeId = BaseTypeId.CMIS_FOLDER;
            }
            if (cmisTypeIdOrName.startsWith(BaseTypeId.CMIS_DOCUMENT.value())) {
                baseTypeId = BaseTypeId.CMIS_DOCUMENT;
            }
            if (baseTypeId != null) {
                for (TypeDefinition typeDefinition : typeDefinitionsMapCache.values()) {
                    if (cmisTypeIdOrName.equals(baseTypeId.value() + MASK_NAME_PREFIX_ELO + typeDefinition.getLocalName())) {
                        return typeDefinition.getId();
                    }
                }
            }
        }
        return cmisTypeIdOrName;
    }

    public static String convertSord2CmisTypeId(Sord sord) {
        return EloCmisUtils.getBaseTypeId(sord).value() + (sord.getMask() == getRootBaseTypeIdElo(EloCmisUtils.getBaseTypeId(sord), null) ? "" : MASK_ID_PREFIX_ELO + sord.getMask());
    }

    public static String convertObjKeyIdElo2CmisPropertyId(int id) {
        return PROPERTY_ID_PREFIX_ELO + id;
    }

    public static boolean isCustomCmisProperty(String cmisProperyId) {
        return cmisProperyId.startsWith(PROPERTY_ID_PREFIX_ELO);
    }

    public static int convertCmisPropertyId2ObjKeyIdElo(String cmisPropertyId) {
        return Integer.parseInt(cmisPropertyId.substring(PROPERTY_ID_PREFIX_ELO.length()));
    }


//    private static String getRootBaseTypeGuidElo(BaseTypeId cmisBaseTypeId, IXConnection ixConnection) {
//        try {
//            if (BaseTypeId.CMIS_FOLDER.equals(cmisBaseTypeId)) {
//                return ixConnection.getCONST().getDOC_MASK().getGUID_BASIC();
//            }
//            if (BaseTypeId.CMIS_DOCUMENT.equals(cmisBaseTypeId)) {
//                return ixConnection.getCONST().getDOC_MASK().getGUID_BASIC();
//            }
//
//        } catch (RemoteException e) {
//            throw new CmisRuntimeException(e.getMessage(), e);
//        }
//        return null;
//    }

    private static MaskName[] retrieveMaskNameArrayElo(IXConnection ixConnection) {
        MaskName[] maskNames = null;
        try {
            EditInfo editInfo = ixConnection.ix().createSord(null, null, EditInfoC.mbBasicData);
            maskNames = editInfo.getMaskNames();
            Arrays.sort(editInfo.getMaskNames(), new Comparator<MaskName>() {
                @Override
                public int compare(MaskName m1, MaskName m2) {
                    return m1.getId() > m2.getId() ? +1 : m1.getId() < m2.getId() ? -1 : 0;
                }
            });
        } catch (RemoteException e) {
            throw new CmisRuntimeException(e.getMessage(), e);
        }
        return maskNames;
    }

//    private static Map<Integer, MaskName> retrieveTypeEloMaskNamesMap(IXConnection ixConnection) {
//        MaskName[] maskNames = retrieveMaskNameArrayElo(ixConnection);
//        Map<Integer, MaskName> repositoryMaskMap = new ConcurrentHashMap<>();
//        for (MaskName maskName : maskNames) {
//            repositoryMaskMap.put(maskName.getId(), maskName);
//        }
//        return repositoryMaskMap;
//    }

    private static Map<String, TypeDefinition> retrieveTypeDefinitionMapElo(IXConnection ixConnection) {
        Map<String, TypeDefinition> typeDefinitionMap = new HashMap<>();
        MaskName[] maskNames = retrieveMaskNameArrayElo(ixConnection);
        for (MaskName maskName : maskNames) {
            String cmisTypeId = null;
            if (maskName.isFolderMask()) {
                cmisTypeId = convertEloMask2CmisTypeId(maskName, BaseTypeId.CMIS_FOLDER, ixConnection);
                //cmisTypeId = BaseTypeId.CMIS_FOLDER.value() + (maskName.getGuid().equals(getRootFolderElo(ixConnection)) ? "" : MASK_ID_PREFIX_ELO + maskName.getName());
                TypeDefinition typeDefinition = retrieveTypeDefinitionElo(typeDefinitionMap, ixConnection, cmisTypeId);
                typeDefinitionMap.put(cmisTypeId, typeDefinition);
            }
            if (maskName.isDocumentMask()) {
                cmisTypeId = convertEloMask2CmisTypeId(maskName, BaseTypeId.CMIS_DOCUMENT, ixConnection);
                //cmisTypeId = BaseTypeId.CMIS_DOCUMENT.value() + (maskName.getGuid().equals(getRootDocumentElo(ixConnection)) ? "" : MASK_ID_PREFIX_ELO + maskName.getName());
                TypeDefinition typeDefinition = retrieveTypeDefinitionElo(typeDefinitionMap, ixConnection, cmisTypeId);
                typeDefinitionMap.put(cmisTypeId, typeDefinition);
            }
        }
        return typeDefinitionMap;
    }

    private static TypeDefinition retrieveTypeDefinitionElo(Map<String, TypeDefinition> typeDefinitionMap, IXConnection ixConnection, String typeId) {
        TypeDefinition typeDefinition = null;
        String parentTypeId = null;
        String eloTypeId = null;
        BaseTypeId cmisBaseTypeId = null;

        //try get it from imput variable (cached)
        typeDefinition = typeDefinitionMap.get(typeId);
        if (typeDefinition != null) {
            return typeDefinition;
        }

        if (eloTypeId == null && typeId.startsWith(BaseTypeId.CMIS_FOLDER.value())) {
            cmisBaseTypeId = BaseTypeId.CMIS_FOLDER;
            if (typeId.equals(cmisBaseTypeId.value())) {
                eloTypeId = String.valueOf(getRootBaseTypeIdElo(cmisBaseTypeId, ixConnection));
            } else {
                eloTypeId = typeId.substring((cmisBaseTypeId.value() + MASK_ID_PREFIX_ELO).length());
            }
        }
        if (eloTypeId == null && typeId.startsWith(BaseTypeId.CMIS_DOCUMENT.value())) {
            cmisBaseTypeId = BaseTypeId.CMIS_DOCUMENT;
            if (typeId.equals(cmisBaseTypeId.value())) {
                eloTypeId = String.valueOf(getRootBaseTypeIdElo(cmisBaseTypeId, ixConnection));
            } else {
                eloTypeId = typeId.substring((cmisBaseTypeId.value() + MASK_ID_PREFIX_ELO).length());
            }
        }
        if (eloTypeId == null) {
            throw new CmisRuntimeException("Unable to find type '" + typeId + "'.");
        }

        try {
            DocMask docMask = ixConnection.ix().checkoutDocMask(eloTypeId, DocMaskC.mbAll, LockC.NO);
            if (typeDefinition == null && cmisBaseTypeId.equals(BaseTypeId.CMIS_FOLDER) && docMask.getDetails().isFolderMask()) {
                MutableFolderTypeDefinition folderType = null;
                parentTypeId = BaseTypeId.CMIS_FOLDER.value();
                if (typeId.equals(parentTypeId)) {
                    folderType = getDefaultTypeDefinitionFactory().createBaseFolderTypeDefinition(CmisVersion.CMIS_1_1);
                } else {
                    folderType = getDefaultTypeDefinitionFactory().createFolderTypeDefinition(CmisVersion.CMIS_1_1, parentTypeId);
                    folderType.setId(typeId); //parentTypeId + MASK_ID_PREFIX_ELO + docMask.getName()
                    folderType.setBaseTypeId(BaseTypeId.CMIS_FOLDER);
                    folderType.setLocalName(docMask.getName());
                    folderType.setDisplayName(docMask.getName());
                    folderType.setDescription(docMask.getText());
                }
                removeQueryableAndOrderableFlags(folderType);
                typeDefinition = folderType;
            }

            if (typeDefinition == null && cmisBaseTypeId.equals(BaseTypeId.CMIS_DOCUMENT) && docMask.getDetails().isDocumentMask()) {
                MutableDocumentTypeDefinition documentType = null;
                parentTypeId = BaseTypeId.CMIS_DOCUMENT.value();
                if (typeId.equals(parentTypeId)) {
                    documentType = getDefaultTypeDefinitionFactory().createBaseDocumentTypeDefinition(CmisVersion.CMIS_1_1);
                } else {
                    documentType = getDefaultTypeDefinitionFactory().createDocumentTypeDefinition(CmisVersion.CMIS_1_1, parentTypeId);
                    documentType.setId(typeId); //parentTypeId + MASK_ID_PREFIX_ELO + docMask.getName()
                    documentType.setBaseTypeId(BaseTypeId.CMIS_DOCUMENT);
                    documentType.setLocalName(docMask.getName());
                    documentType.setDisplayName(docMask.getName());
                    documentType.setDescription(docMask.getText());
                }
                removeQueryableAndOrderableFlags(documentType);
                typeDefinition = documentType;
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
                    mutablePropertyDefinition.setId(convertObjKeyIdElo2CmisPropertyId(docMaskLine.getId()));
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

        } catch (
                RemoteException e
                )

        {
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

//    private void refreshEloMaskNameCache() {
//        String repositoryId = getRepositoryId();
//        if (repositoryId == null) {
//            return;
//        }
//        IXConnection ixConnection = getIXConnection();
//        MaskName[] maskNames = null;
//        Map<Integer, MaskName> repositoryMaskMap = new ConcurrentHashMap<>();
//        try {
//            EditInfo editInfo = ixConnection.ix().createSord(null, null, EditInfoC.mbBasicData);
//            maskNames = editInfo.getMaskNames();
//            for (MaskName maskName : maskNames) {
//                repositoryMaskMap.put(maskName.getId(), maskName);
//            }
//            Map<Integer, MaskName> repositoryMaskMapOld = typeEloMaskNamesMap.get(repositoryId);
//            typeEloMaskNamesMap.put(repositoryId, repositoryMaskMap);
//            if (repositoryMaskMapOld != null) {
//                repositoryMaskMapOld.clear();
//            }
//        } catch (RemoteException e) {
//            throw new CmisRuntimeException(e.getMessage(), e);
//        }
//    }
//
//    private MaskName getEloMaskNameCache(EloMaskIdentifier maskIdentifier) {
//        String repositoryId = getRepositoryId();
//        if (repositoryId == null) {
//            return null;
//        }
//        if (typeEloMaskNamesMap.get(repositoryId) == null) {
//            return null;
//        }
//        if (maskIdentifier.getId() != null) {
//            return typeEloMaskNamesMap.get(repositoryId).get(maskIdentifier.getId());
//        }
//        if (maskIdentifier.getName() != null) {
//            for (int maskId : typeEloMaskNamesMap.get(repositoryId).keySet()) {
//                MaskName mask = typeEloMaskNamesMap.get(repositoryId).get(maskId);
//                if (mask.getName().equals(maskIdentifier.getName())) {
//                    return mask;
//                }
//            }
//        }
//        return null;
//    }


    //constructor and service methods

    public String getCmisTypeName(Sord sord) {
        String cmisTypeId = convertSord2CmisTypeId(sord);
        return getTypeDefinition(cmisTypeId).getDisplayName();
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
        typeId = calculateCmisTypeId(getRepositoryId(), typeId);
        deleteEloStorageMask(typeId);
    }

    @Override
    public TypeDefinitionList getTypeChildren(String typeId, Boolean includePropertyDefinitions, BigInteger maxItems, BigInteger skipCount) {
        //fix to accept as types cmisTypeId and cmisTypeName
        typeId = calculateCmisTypeId(getRepositoryId(), typeId);
        return getDefaultTypeDefinitionFactory().createTypeDefinitionList(getTypeDefinitions(), typeId, includePropertyDefinitions, maxItems, skipCount, this.getCmisService().getCallContext().getCmisVersion());
    }

    @Override
    public TypeDefinition getTypeDefinition(String typeId) {
        Map<String, TypeDefinition> typeDefinitionMap = getTypeDefinitions();

        //fix to accept as types cmisTypeId and cmisTypeName
        typeId = calculateCmisTypeId(getRepositoryId(), typeId);

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
        typeId = calculateCmisTypeId(getRepositoryId(), typeId);

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
