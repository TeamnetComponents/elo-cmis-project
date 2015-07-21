package de.elo.extension.service;

import de.elo.ix.client.*;
import de.elo.utils.net.RemoteException;
import org.apache.commons.lang3.StringUtils;
import org.platform.common.utils.file.FileUtils;

import java.io.InputStream;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;


/**
 * Created by Lucian.Dragomir on 3/4/2015.
 */
public class EloUtilsService {

    public static final int FOLDER_ROOT_ID_ELO = 1;
    public static final String FOLDER_ROOT_ELO = "ARCPATH:";
    public static final String FOLDER_DELIMITER_ELO = String.valueOf((char) 182);


    public final static FileUtils fileUtilsOS = new FileUtils(FileUtils.getOsRootPattern(), FileUtils.getOsFileSeparator(), FileUtils.getOsRootPathDefault());
    public final static FileUtils fileUtilsElo = new FileUtils(FOLDER_ROOT_ELO, FOLDER_DELIMITER_ELO);
    public final static FileUtils fileUtilsRegular = new FileUtils("/", "/");


    private final static int NUMBER_OF_RESULTS = 50;
    private final static Integer ACTIVE_WORKFLOWS_MAX_NUMBER = Integer.MAX_VALUE;
    public static String DOCUMENT_ID_MISSING = "-1";
    private static String DOCUMENT_DELIMITER = "-";
    private static String DOCUMENT_DELIMITER_PATTERN = Pattern.quote(DOCUMENT_DELIMITER);

    //------------------------------------------------------------------------------------------------------------------
    //--GENERIC METHODS-------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    public static boolean isArray(Object obj) {
        return obj != null && obj.getClass().isArray();
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

    public static Date getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        return currentDate;
    }

    public static Calendar convertIso2Calendar(IXConnection ixConnection, String isoDate) {
        if (isoDate == null || isoDate.equals("")) {
            return null;
        }
        try {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(ixConnection.isoToDate(isoDate));
            return calendar;
        } catch (Exception e) {
            return null;
        }
    }

    public static String convertCalendar2Iso(IXConnection ixConnection, GregorianCalendar date) {
        return ixConnection.dateToIso(date.getTime());
    }

    public static Number getEloNumberValueAsDecimal(Locale locale, String eloNumberVal) {
        DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance(locale);
        Number number = null;
        try {
            number = decimalFormat.parse(eloNumberVal);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return number;
    }

    //------------------------------------------------------------------------------------------------------------------
    //--PATH CALCULATIONS-----------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    public static String getPathElo(Sord sord) {
        String eloPath = fileUtilsElo.concatenate(sord.getRefPaths()[0].getPathAsString(), isRootFolder(sord) ? "" : sord.getName());
        return fileUtilsRegular.convertPathName(fileUtilsElo.convertPathName(eloPath, fileUtilsRegular),fileUtilsElo);
    }

    public static String getPathElo(Sord parentSord, String name) {
        String parentPath = getPathElo(parentSord);
        return fileUtilsElo.concatenate(parentPath, name);
    }

    //------------------------------------------------------------------------------------------------------------------
    //--OBJECT IDENTIFIERS CALCULATION AND PARSING----------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    public static String getIdParts(String objectId, int partId, String defaultValue) {
        String partValue = defaultValue;
        String[] idParts = objectId.split(DOCUMENT_DELIMITER_PATTERN);
        if (idParts.length <= partId) {
            partValue = defaultValue;
        } else {
            partValue = idParts[partId];
            if (partValue.isEmpty()) {
                partValue = defaultValue;
            }
        }
        return partValue;
    }

    public static String getSordId(String objectId) {
        return getIdParts(objectId, 0, null);
    }

    public static String getDocumentId(String objectId) {
        return getIdParts(objectId, 1, DOCUMENT_ID_MISSING);
    }

    public static String calcObjectId(String... ids) {
        return StringUtils.join(ids, DOCUMENT_DELIMITER);
    }

    public static String calcObjectId(Object... objects) {
        EditInfo editInfo = null;
        Sord sord = null;
        DocVersion docVersion = null;

        for (Object object : objects) {
            if (object instanceof EditInfo) {
                editInfo = (EditInfo) object;
                sord = editInfo.getSord();
            } else if (object instanceof Sord) {
                editInfo = null;
                sord = (Sord) object;
            } else if (object instanceof DocVersion) {
                docVersion = (DocVersion) object;
            }
        }
        if (docVersion == null) {
            docVersion = sord.getDocVersion();
        }
        if (sord == null) {
            throw new RuntimeException("Sord must be provided.");
        }
        //if (docVersion == null) {
        //    throw new RuntimeException("DocVersion must be provided.");
        //}
        return sord.getId() + ((docVersion != null) ? DOCUMENT_DELIMITER + docVersion.getId() : "");
    }

    //------------------------------------------------------------------------------------------------------------------
    //--TYPE METHODS----------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    public static MaskName[] getMasks(IXConnection ixConnection) throws RemoteException {
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
        } catch (de.elo.utils.net.RemoteException e) {
            throw e;
        }
        return maskNames;
    }

    //------------------------------------------------------------------------------------------------------------------
    //--DMS METHODS-----------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    public static Sord createSord(IXConnection ixConnection, String parentPathNameOrId, String maskId, String name) throws de.elo.utils.net.RemoteException {
        Sord sord = null;
        sord = ixConnection.ix().createSord(parentPathNameOrId, maskId, SordC.mbAll);
        sord.setName(name);
        return sord;
    }

    public static String createSord(IXConnection ixConnection, String fullPathName, String maskId, Map<String, Object> attributes) throws de.elo.utils.net.RemoteException {
        String sordId;
        Sord sord;
        String sordDirectory = fileUtilsElo.getFolderName(fullPathName);
        String sordName = fileUtilsElo.getParentFolderPathName(fullPathName);

        String[] pathNames = sordDirectory.split(FOLDER_DELIMITER_ELO);
        String pathName = null;
        for (int i = 1; i < pathNames.length; i++) {
            if (StringUtils.isNotEmpty(pathNames[i])) {
                if (i == 1) {
                    pathName = pathNames[0] + FOLDER_DELIMITER_ELO + pathNames[i];
                    if (!existSord(ixConnection, pathName)) {
                        Sord newSord = createSord(ixConnection, "1", maskId, pathNames[i]);
                        saveSord(ixConnection, newSord, SordC.mbAll, LockC.YES);
                    }
                } else {
                    if (existSord(ixConnection, pathName + FOLDER_DELIMITER_ELO + pathNames[i])) {
                        pathName = pathName + FOLDER_DELIMITER_ELO + pathNames[i];
                    } else {
                        Sord newSord = createSord(ixConnection, pathName, maskId, pathNames[i]);
                        saveSord(ixConnection, newSord, SordC.mbAll, LockC.YES);
                        pathName = pathName + FOLDER_DELIMITER_ELO + pathNames[i];
                    }
                }
            }
        }
        sord = createSord(ixConnection, fullPathName, maskId, sordName);
        if (attributes != null) {
            updateSordObjKeys(sord, attributes);
        }
        sordId = String.valueOf(saveSord(ixConnection, sord, SordC.mbAll, LockC.YES));
        return sordId;
    }

    public static Sord getSord(IXConnection ixConnection, String pathNameOrId, SordZ sordZ, LockZ lockZ) throws de.elo.utils.net.RemoteException {
        Sord sord = null;
        if (!isInteger(pathNameOrId)) {
            pathNameOrId = fileUtilsRegular.convertPathName(pathNameOrId, fileUtilsElo);
        }
        try {
            sord = ixConnection.ix().checkoutSord(pathNameOrId, sordZ, lockZ);
        } catch (RemoteException e) {
            IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
            if (ixError.code != 5023) //cale incorecta
            {
                throw e;
            } else {
                // return null because the sord does not exists
            }
        }
        return sord;
    }

    public static boolean existSord(IXConnection ixConnection, String pathNameOrId) {
        boolean existSord = false;
        try {
            ixConnection.ix().checkoutSord(pathNameOrId, SordC.mbAll, LockC.NO);
            existSord = true;
        } catch (de.elo.utils.net.RemoteException e) {
            existSord = false;
        }
        return existSord;
    }

    public static boolean existsChildSord(IXConnection ixConnection, Sord parentSord, String name) throws de.elo.utils.net.RemoteException {
        boolean exists = false;
        String ckeckPath = getPathElo(parentSord, name);
        try {
            Sord checkSord = ixConnection.ix().checkoutSord(ckeckPath, SordC.mbAll, LockC.NO);
            exists = true;
        } catch (RemoteException e) {
            IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
            if (ixError.code != 5023) //cale incorecta
            {
                throw e;
            }
        }
        return exists;
    }


    public static Map<Integer, ObjKey> getSordObjKeys(Sord sord) {
        ObjKey[] objKeys = sord.getObjKeys();
        // map properties to object
        Map<Integer, ObjKey> objKeyMap = new HashMap<>();
        for (ObjKey objKey : objKeys) {
            objKeyMap.put(objKey.getId(), objKey);
        }
        return objKeyMap;
    }

    public static void setObjKey(Map<Integer, ObjKey> objKeyMap, int id, String name, String[] data) {
        if (!objKeyMap.containsKey(id)) {
            objKeyMap.put(id, new ObjKey());
        }
        objKeyMap.get(id).setName(name);
        objKeyMap.get(id).setData(data);
    }

    public static void setObjKey(Map<Integer, ObjKey> objKeyMap, ObjKey objKey) {
        if (objKeyMap.containsKey(objKey.getId())) {
            objKeyMap.remove(objKey.getId());
        }
        objKeyMap.put(objKey.getId(), objKey);
    }

    public static int getObjKeyIndex(Map<Integer, ObjKey> objKeyMap, String name) {
        for (int index : objKeyMap.keySet()) {
            if (objKeyMap.get(index).getName().equals(name)) {
                return index;
            }
        }
        return -1;
    }

    public static ObjKey getObjKey(Map<Integer, ObjKey> objKeyMap, int id) {
        return objKeyMap.get(id);
    }

    public static ObjKey getObjKey(Map<Integer, ObjKey> objKeyMap, String name) {
        return getObjKey(objKeyMap, getObjKeyIndex(objKeyMap, name));
    }

    public static void setObjKey(Map<Integer, ObjKey> objKeyMap, Map<String, Object> attributes) {
        if (attributes == null) {
            return;
        }
        String name;
        for (int index : objKeyMap.keySet()) {
            name = objKeyMap.get(index).getName();
            if (attributes.containsKey(name)) {
                if (isArray(attributes.get(name))) {
                    List<String> objKeyValue = new ArrayList<>();
                    for (Object value : ((String[]) attributes.get(name))) {
                        objKeyValue.add(String.valueOf(value));
                    }
                    objKeyMap.get(index).setData(objKeyValue.toArray(new String[objKeyValue.size()]));
                } else {
                    objKeyMap.get(index).setData(new String[]{attributes.get(name).toString()});
                }
            }
        }
    }

    public static void setSordObjKeyMap(Sord sord, Map<Integer, ObjKey> objKeyMap) {
        ObjKey[] objKeys = (ObjKey[]) objKeyMap.values().toArray(new ObjKey[objKeyMap.size()]);
        Arrays.sort(objKeys, new Comparator<ObjKey>() {
            @Override
            public int compare(ObjKey o1, ObjKey o2) {
                if (o1 == null || o2 == null) {
                    throw new NullPointerException("One of the compared objects is null.");
                }
                return (int) Math.signum(o1.getId() - o2.getId());
            }
        });
        sord.setObjKeys(objKeys);
    }

    public static void updateSordObjKeys(Sord sord, Map<String, Object> attributes) {
        ObjKey[] objKeys = sord.getObjKeys();
        for (int i = 0; i < objKeys.length; i++) {
            if (attributes.containsKey(objKeys[i].getName())) {
                if (isArray(attributes.get(objKeys[i].getName()))) {
                    List<String> objKeyValue = new ArrayList<>();
                    for (Object value : ((String[]) attributes.get(objKeys[i].getName()))) {
                        objKeyValue.add(String.valueOf(value));
                    }
                    objKeys[i].setData(objKeyValue.toArray(new String[objKeyValue.size()]));
                } else {
                    objKeys[i].setData(new String[]{attributes.get(objKeys[i].getName()).toString()});
                }
            }
        }
        sord.setObjKeys(objKeys);
    }

    public static boolean sordContainsAttribute(Sord sord, String attributeName) {
        ObjKey[] objKeys = sord.getObjKeys();
        for (int i = 0; i < objKeys.length; i++) {
            if (objKeys[i].getName().equals(attributeName)) {
                return true;
            }
        }
        return false;
    }

    public static int saveSord(IXConnection ixConnection, Sord sord, SordZ sordZ, LockZ unlockZ) throws de.elo.utils.net.RemoteException {
        return ixConnection.ix().checkinSord(sord, sordZ, unlockZ);
    }

    public static List<Sord> findSords(FindInfo findInfo, BigInteger maxItems, BigInteger skipCount, IXConnection ixConnection) throws RemoteException {
        final List<Sord> sordList = new ArrayList<Sord>();

        // skip and max
        int added = 0;
        int skipped = 0;
        int skip = (skipCount == null ? 0 : skipCount.intValue());
        if (skip < 0) {
            skip = 0;
        }
        int max = (maxItems == null ? Integer.MAX_VALUE : maxItems.intValue());
        if (max < 0) {
            max = Integer.MAX_VALUE;
        }

        FindResult findResult;
        try {
            findResult = ixConnection.ix().findFirstSords(findInfo, NUMBER_OF_RESULTS, SordC.mbAll);
            List<Sord> sordPartialList = Arrays.asList(findResult.getSords());
            if (skip == 0 && max == Integer.MAX_VALUE) {
                sordList.addAll(sordPartialList);
            } else {
                int toSkip = 0;
                int toAdd = 0;
                if (skip > 0) {
                    toSkip = Math.min(sordPartialList.size(), skip - skipped);
                }
                toAdd = Math.min(sordPartialList.size() - toSkip, max - added);
                sordList.addAll(sordPartialList.subList(toSkip, toSkip + toAdd));
                skipped += toSkip;
                added += toAdd;
            }
        } catch (RemoteException e) {
            throw e;
        }

        int count = 1;
        while (findResult.isMoreResults() && (added < max)) {
            List<Sord> sordPartialList;
            try {
                findResult = ixConnection.ix().findNextSords(findResult.getSearchId(), count * NUMBER_OF_RESULTS, NUMBER_OF_RESULTS, SordC.mbAll);
                sordPartialList = Arrays.asList(findResult.getSords());
            } catch (RemoteException e) {
                throw e;
            }
            count++;
            if (skip == 0 && max == Integer.MAX_VALUE) {
                sordList.addAll(sordPartialList);
            } else {
                int toSkip = 0;
                int toAdd = 0;
                if (skip > 0) {
                    toSkip = Math.min(sordPartialList.size(), skip - skipped);
                }
                toAdd = Math.min(sordPartialList.size() - toSkip, max - added);
                sordList.addAll(sordPartialList.subList(toSkip, toSkip + toAdd));
                skipped += toSkip;
                added += toAdd;
            }
        }
        try {
            ixConnection.ix().findClose(findResult.getSearchId());
        } catch (RemoteException e) {
            findResult = null;
        }

        return sordList;
    }

    public static List<Sord> listFolderContent(IXConnection ixConnection, String pathNameOrId) throws de.elo.utils.net.RemoteException {
        Sord sordParent;

        sordParent = ixConnection.ix().checkoutSord(pathNameOrId, SordC.mbAll, LockC.NO);
        if (!isFolder(sordParent)) {
            throw new RemoteException("Not a folder!");
        }

        // iterate through children
        // Prepare findInfo object
        FindInfo findInfo = new FindInfo();
        findInfo.setFindChildren(new FindChildren());
        findInfo.getFindChildren().setParentId(sordParent.getGuid());

        return findSords(findInfo, null, null, ixConnection);
    }

    //public String createDocument(org.apache.chemistry.opencmis.commons.data.Properties properties, String folderId, ContentStream contentStream, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces) {
    public static String uploadDocument(IXConnection ixConnection, String pathNameOrId, String documentName, int maskId, String fileName, InputStream inputStream, int inputStreamLength, Map<String, Object> attributes, String version, String versionComment) throws RemoteException {
        Sord sord = null;
        DocVersion docVersion = null;
        int objectId = -1;
        String eloPath = null;
        IXServicePortC CONST = null;
        EditInfo editInfo = null;
        try {
            CONST = ixConnection.getCONST();

            //check if folderid is folder type
            Sord parentSord = getSord(ixConnection, pathNameOrId, SordC.mbAll, LockC.NO);
            if (parentSord == null || !isFolder(parentSord)) {
                throw new RemoteException("Parent not found or is not a folder!");
            }

            eloPath = getPathElo(parentSord, documentName);
            try {
                editInfo = ixConnection.ix().checkoutDoc(eloPath, null, EditInfoC.mbSordDocAtt, LockC.NO);
            } catch (de.elo.utils.net.RemoteException e) {
                IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
                if (ixError.code == 5023) {
                    try {
                        editInfo = ixConnection.ix().createDoc(pathNameOrId, String.valueOf(maskId), null, EditInfoC.mbSordDocAtt);
                    } catch (de.elo.utils.net.RemoteException e1) {
                        throw e1;
                    }
                } else {
                    throw e;
                }
            }
            sord = editInfo.getSord();
            sord.setName(documentName);

            Document document = null;
            document = editInfo.getDocument();
            if (document == null) {
                document = new Document();
            }

            Map<Integer, ObjKey> objKeyMap = getSordObjKeys(sord);
            setObjKey(objKeyMap, CONST.getDOC_MASK_LINE().getID_FILENAME(), CONST.getDOC_MASK_LINE().getNAME_FILENAME(), new String[]{fileName});
            setObjKey(objKeyMap, attributes);
            setSordObjKeyMap(sord, objKeyMap);

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
            uploadResult = ixConnection.upload(dv.getUrl(), inputStream, inputStreamLength, fileUtilsElo.getMimeType(fileName));

            // assign response to uploadResult - contains document ID
            dv.setUploadResult(uploadResult);

            // Step 4: Commit document.
            dv.setVersion(version);
            dv.setComment(versionComment);
            document = ixConnection.ix().checkinDocEnd(sord, SordC.mbAll, document, LockC.NO);

            docVersion = document.getDocs()[0];
            if (sord.getId() < 0) {
                editInfo = ixConnection.ix().checkoutDoc(eloPath, null, EditInfoC.mbSord, LockC.NO);
                sord = editInfo.getSord();
            }
        } catch (de.elo.utils.net.RemoteException e) {
            throw e;
        }
        return calcObjectId(sord, docVersion);
    }

    public static boolean isDocument(Sord sord) {
        return (SordC.LBT_DOCUMENT <= sord.getType() && sord.getType() <= SordC.LBT_DOCUMENT_MAX);
    }

    public static boolean isFolder(Sord sord) {
        return ((sord.getType() == SordC.LBT_ARCHIVE) || (0 < sord.getType() && sord.getType() < SordC.LBT_DOCUMENT));
    }

    public static boolean isRootFolder(Sord sord) {
        return (sord.getType() == SordC.LBT_ARCHIVE);
    }


    public static boolean isRootFolder(int id) {
        return (id == FOLDER_ROOT_ID_ELO);
    }


    //------------------------------------------------------------------------------------------------------------------
    //--WORKFLOW METHODS------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    public static WFDiagram getWorkFlowTemplate(IXConnection ixConnection, String processDefinitionId, String versionId, WFDiagramZ wfDiagramZ, LockZ lockZ) throws de.elo.utils.net.RemoteException {
        WFDiagram wfDiagram = null;
        try {

            wfDiagram = ixConnection.ix().checkoutWorkflowTemplate(processDefinitionId, null, WFDiagramC.mbAll, LockC.NO);
        } catch (RemoteException e) {
            IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
            if (ixError.code != 5023) //cale incorecta
            {
                throw e;
            } else {
                return null;
            }
        } catch (NullPointerException e) {
            throw e;
        }
        return wfDiagram;
    }

    public static WFDiagram getWorkFlow(IXConnection ixConnection, String flowId, WFTypeZ wfTypeZ, WFDiagramZ wfDiagramZ, LockZ lockZ) throws de.elo.utils.net.RemoteException {
        WFDiagram wfDiagram = null;
        try {
            wfDiagram = ixConnection.ix().checkoutWorkFlow(flowId, wfTypeZ, wfDiagramZ, lockZ);
        } catch (RemoteException e) {
            IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
            if (ixError.code != 5023) //cale incorecta
            {
                throw e;
            } else {
                return null;
            }
        } catch (NullPointerException e) {
            throw e;
        }
        return wfDiagram;
    }

    public static String startWorkFlow(IXConnection ixConnection, String templateId, String name, String sordId) throws de.elo.utils.net.RemoteException {
        String workflowGUID = null;
        int workflowId = ixConnection.ix().startWorkFlow(templateId, name, sordId);
        WFDiagram wfDiagram = ixConnection.ix().checkoutWorkFlow(String.valueOf(workflowId), WFTypeC.ACTIVE, WFDiagramC.mbAll, LockC.NO);
        workflowGUID = wfDiagram.getGuid();
        // workflowGUID = workflowGUID.replace("(", "").replace(")", "");
        return workflowGUID;
    }

    public static WFNode getNode(IXConnection ixConnection, String workflowId, Integer nodeId) throws de.elo.utils.net.RemoteException {
        for (WFNode wFNode : getWorkFlow(ixConnection, workflowId, WFTypeC.ACTIVE, WFDiagramC.mbAll, LockC.NO).getNodes()) {
            if (wFNode.getId() == nodeId) {
                return wFNode;
            }
        }
        return null;
    }

    public static List<WFNode> getCurrentNodesFromWFDiagram(WFDiagram wfDiagram) {
        List<WFNodeAssoc> nodeAssocs = Arrays.asList(wfDiagram.getMatrix().getAssocs());
        List<Integer> nodesFromNotDone = new ArrayList<Integer>();
        List<Integer> nodesToDone = new ArrayList<Integer>();
        for (WFNodeAssoc assoc : nodeAssocs) {
            if (assoc.isDone()) {
                nodesToDone.add(assoc.getNodeTo());
            } else {
                nodesFromNotDone.add(assoc.getNodeFrom());
            }
        }
        nodesFromNotDone.retainAll(nodesToDone);
        List<WFNode> allNodes = Arrays.asList(wfDiagram.getNodes());
        List<WFNode> currentNodesList = new ArrayList<>();
        for (Integer currentNodeId : nodesFromNotDone) {
            for (WFNode wfNode : allNodes) {
                if (currentNodeId.compareTo(wfNode.getId()) == 0) {
                    currentNodesList.add(wfNode);
                }
            }
        }
        return currentNodesList;
    }


    //------------------------------------------------------------------------------------------------------------------
    //--USERS, GROUPS---------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    public static String[] splitLoginUsers(String userIdentification) {
        String[] userNames = userIdentification.split("@");
        return userNames;
    }

    public static Integer createUserGroup(IXConnection ixConnection, String groupName, String rightsAsUserId) {
        try {
            UserInfo group = ixConnection.ix().createUser(rightsAsUserId);
            group.setType(UserInfoC.TYPE_GROUP);
            group.setName(groupName);
            int[] ints = ixConnection.ix().checkinUsers(new UserInfo[]{group}, CheckinUsersC.NEW_USER, LockC.YES);
            return ints[0];
        } catch (de.elo.utils.net.RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean checkIfGroupExist(IXConnection ixConnection, String groupName) {
        try {
            ixConnection.ix().checkoutUsers(new String[]{groupName}, CheckoutUsersC.ALL_GROUPS, LockC.NO);
            return true;
        } catch (de.elo.utils.net.RemoteException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        String strNumber = "12312343.1234312";

        Locale locale = Locale.getDefault();
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);

        decimalFormatSymbols = DecimalFormatSymbols.getInstance(
                (new Locale.Builder())
                        .setRegion(locale.getCountry())
                        .setLanguage(locale.getLanguage())
                        .setVariant(locale.getVariant()).build()
        );

        DecimalFormat decimalFormat = new DecimalFormat("#", decimalFormatSymbols);
        decimalFormat.setMaximumFractionDigits(2);

        Number number = null;
        try {
            number = decimalFormat.parse(strNumber);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println(number);

    }

}
