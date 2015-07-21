//package org.cmis.server.elo.connection;
//
//import de.elo.ix.client.*;
//import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
//import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
//import org.apache.commons.lang.StringUtils;
//import org.cmis.server.elo.EloCmisUtils;
//import org.platform.common.utils.file.FileUtils;
//
//import java.io.InputStream;
//import java.rmi.RemoteException;
//import java.utils.*;
//
//
///**
// * Created by Lucian.Dragomir on 3/4/2015.
// */
//public class EloUtilsService {
//
//    public static final int FOLDER_ROOT_ID_ELO = 1;
//
//    public final static FileUtils fileUtilsOS = new FileUtils(FileUtils.getOsRoot(), FileUtils.getOsFileSeparator());
//    public final static FileUtils fileUtilsElo = new FileUtils("ARCPATH:", String.valueOf((char) 182));
//    public final static FileUtils fileUtilsRegular = new FileUtils("/", "/");
//
//
//    private final static Integer ACTIVE_WORKFLOWS_MAX_NUMBER = Integer.MAX_VALUE;
//
//
//    //------------------------------------------------------------------------------------------------------------------
//    //--GENERIC METHODS-------------------------------------------------------------------------------------------------
//    //------------------------------------------------------------------------------------------------------------------
//    public boolean isArray(Object obj) {
//        return obj != null && obj.getClass().isArray();
//    }
//
//    public boolean isInteger(String s) {
//        try {
//            Integer.parseInt(s);
//        } catch (NumberFormatException e) {
//            return false;
//        }
//        // only got here if we didn't return false
//        return true;
//    }
//
//    public Date getCurrentDate() {
//        Calendar calendar = Calendar.getInstance();
//        Date currentDate = calendar.getTime();
//        return currentDate;
//    }
//
//    public Calendar convertIso2Calendar(IXConnection ixConnection, String isoDate) {
//        if (isoDate == null || isoDate.equals("")) {
//            return null;
//        }
//        try {
//            Calendar calendar = new GregorianCalendar();
//            calendar.setTime(ixConnection.isoToDate(isoDate));
//            return calendar;
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    public String convertCalendar2Iso(IXConnection ixConnection, GregorianCalendar date) {
//        return ixConnection.dateToIso(date.getTime());
//    }
//
//    //------------------------------------------------------------------------------------------------------------------
//    //--DMS METHODS-----------------------------------------------------------------------------------------------------
//    //------------------------------------------------------------------------------------------------------------------
//
//    public Sord createSord(IXConnection ixConnection, String parentPathNameOrId, String maskId, String name) throws de.elo.utils.net.RemoteException {
//        Sord sord = null;
//        sord = ixConnection.ix().createSord(parentPathNameOrId, maskId, SordC.mbAll);
//        sord.setName(name);
//        return sord;
//    }
//
//    public String createSord(IXConnection ixConnection, String fullPathName, String maskId, Map<String, Object> attributes) throws de.elo.utils.net.RemoteException {
//        String sordId;
//        Sord sord;
//        String sordDirectory = fileUtilsElo.getFolderName(fullPathName);
//        String sordName = fileUtilsElo.getParentFolderPathName(fullPathName);
//
//        String[] pathNames = sordDirectory.split("¶");
//        String pathName = null;
//        for (int i = 1; i < pathNames.length; i++) {
//            if (StringUtils.isNotEmpty(pathNames[i])) {
//                if (i == 1) {
//                    pathName = pathNames[0] + "¶" + pathNames[i];
//                    if (!existSord(ixConnection, pathName)) {
//                        Sord newSord = createSord(ixConnection, "1", maskId, pathNames[i]);
//                        saveSord(ixConnection, newSord, SordC.mbAll, LockC.YES);
//                    }
//                } else {
//                    if (existSord(ixConnection, pathName + "¶" + pathNames[i])) {
//                        pathName = pathName + "¶" + pathNames[i];
//                    } else {
//                        Sord newSord = createSord(ixConnection, pathName, maskId, pathNames[i]);
//                        saveSord(ixConnection, newSord, SordC.mbAll, LockC.YES);
//                        pathName = pathName + "¶" + pathNames[i];
//                    }
//                }
//            }
//        }
//        sord = createSord(ixConnection, fullPathName, maskId, sordName);
//        if (attributes != null) {
//            updateSordObjKeys(sord, attributes);
//        }
//        sordId = String.valueOf(saveSord(ixConnection, sord, SordC.mbAll, LockC.YES));
//        return sordId;
//    }
//
//    public Sord getSord(IXConnection ixConnection, String pathNameOrId, SordZ sordZ, LockZ lockZ) throws de.elo.utils.net.RemoteException {
//        Sord sord = null;
//        if (!isInteger(pathNameOrId)) {
//            pathNameOrId = fileUtilsRegular.convertPathName(pathNameOrId, fileUtilsElo);
//        }
//        try {
//            sord = ixConnection.ix().checkoutSord(pathNameOrId, sordZ, lockZ);
//        } catch (RemoteException e) {
//            IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
//            if (ixError.code != 5023) //cale incorecta
//            {
//                throw e;
//            } else {
//                // return null because the sord does not exists
//            }
//        }
//        return sord;
//    }
//
//    public boolean existSord(IXConnection ixConnection, String pathNameOrId) {
//        boolean existSord = false;
//        try {
//            ixConnection.ix().checkoutSord(pathNameOrId, SordC.mbAll, LockC.NO);
//            existSord = true;
//        } catch (de.elo.utils.net.RemoteException e) {
//            existSord = false;
//        }
//        return existSord;
//    }
//
//    public Map<Integer, ObjKey> getSordObjKeys(Sord sord) {
//        ObjKey[] objKeys = sord.getObjKeys();
//        // map properties to object
//        Map<Integer, ObjKey> objKeyMap = new HashMap<>();
//        for (ObjKey objKey : objKeys) {
//            objKeyMap.put(objKey.getId(), objKey);
//        }
//        return objKeyMap;
//    }
//
//    public void setObjKey(Map<Integer, ObjKey> objKeyMap, int id, String name, String[] data) {
//        if (!objKeyMap.containsKey(id)) {
//            objKeyMap.put(id, new ObjKey());
//        }
//        objKeyMap.get(id).setName(name);
//        objKeyMap.get(id).setData(data);
//    }
//
//    public void setObjKey(Map<Integer, ObjKey> objKeyMap, ObjKey objKey) {
//        if (objKeyMap.containsKey(objKey.getId())) {
//            objKeyMap.remove(objKey.getId());
//        }
//        objKeyMap.put(objKey.getId(), objKey);
//    }
//
//    public int getObjKeyIndex(Map<Integer, ObjKey> objKeyMap, String name) {
//        for (int index : objKeyMap.keySet()) {
//            if (objKeyMap.get(index).getName().equals(name)) {
//                return index;
//            }
//        }
//        return -1;
//    }
//
//    public ObjKey getObjKey(Map<Integer, ObjKey> objKeyMap, int id) {
//        return objKeyMap.get(id);
//    }
//
//    public ObjKey getObjKey(Map<Integer, ObjKey> objKeyMap, String name) {
//        return getObjKey(objKeyMap, getObjKeyIndex(objKeyMap, name));
//    }
//
//    public void setObjKey(Map<Integer, ObjKey> objKeyMap, Map<String, Object> attributes) {
//        if (attributes == null) {
//            return;
//        }
//        String name;
//        for (int index : objKeyMap.keySet()) {
//            name = objKeyMap.get(index).getName();
//            if (attributes.containsKey(name)) {
//                if (isArray(attributes.get(name))) {
//                    List<String> objKeyValue = new ArrayList<>();
//                    for (Object value : ((String[]) attributes.get(name))) {
//                        objKeyValue.add(String.valueOf(value));
//                    }
//                    objKeyMap.get(index).setData(objKeyValue.toArray(new String[objKeyValue.size()]));
//                } else {
//                    objKeyMap.get(index).setData(new String[]{attributes.get(name).toString()});
//                }
//            }
//        }
//    }
//
//    public void setSordObjKeyMap(Sord sord, Map<Integer, ObjKey> objKeyMap) {
//        ObjKey[] objKeys = (ObjKey[]) objKeyMap.values().toArray(new ObjKey[objKeyMap.size()]);
//        Arrays.sort(objKeys, new Comparator<ObjKey>() {
//            @Override
//            public int compare(ObjKey o1, ObjKey o2) {
//                if (o1 == null || o2 == null) {
//                    throw new NullPointerException("One of the compared objects is null.");
//                }
//                return (int) Math.signum(o1.getId() - o2.getId());
//            }
//        });
//        sord.setObjKeys(objKeys);
//    }
//
//    public void updateSordObjKeys(Sord sord, Map<String, Object> attributes) {
//        ObjKey[] objKeys = sord.getObjKeys();
//        for (int i = 0; i < objKeys.length; i++) {
//            if (attributes.containsKey(objKeys[i].getName())) {
//                if (isArray(attributes.get(objKeys[i].getName()))) {
//                    List<String> objKeyValue = new ArrayList<>();
//                    for (Object value : ((String[]) attributes.get(objKeys[i].getName()))) {
//                        objKeyValue.add(String.valueOf(value));
//                    }
//                    objKeys[i].setData(objKeyValue.toArray(new String[objKeyValue.size()]));
//                } else {
//                    objKeys[i].setData(new String[]{attributes.get(objKeys[i].getName()).toString()});
//                }
//            }
//        }
//        sord.setObjKeys(objKeys);
//    }
//
//    public boolean sordContainsAttribute(Sord sord, String attributeName) {
//        ObjKey[] objKeys = sord.getObjKeys();
//        for (int i = 0; i < objKeys.length; i++) {
//            if (objKeys[i].getName().equals(attributeName)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public int saveSord(IXConnection ixConnection, Sord sord, SordZ sordZ, LockZ unlockZ) throws de.elo.utils.net.RemoteException {
//        return ixConnection.ix().checkinSord(sord, sordZ, unlockZ);
//    }
//
//    public List<Sord> listFolderContent(IXConnection ixConnection, String pathNameOrId) throws de.elo.utils.net.RemoteException {
//        Sord sordParent;
//
//        sordParent = ixConnection.ix().checkoutSord(pathNameOrId, SordC.mbAll, LockC.NO);
//        if (!EloCmisUtils.getBaseTypeId(sordParent).equals(BaseTypeId.CMIS_FOLDER)) {
//            throw new de.elo.utils.net.RemoteException("Not a folder!");
//        }
//
//        // iterate through children
//        // Prepare findInfo object
//        FindInfo findInfo = new FindInfo();
//        findInfo.setFindChildren(new FindChildren());
//        findInfo.getFindChildren().setParentId(sordParent.getGuid());
//
//        return EloCmisUtils.findSords(findInfo, null, null, ixConnection);
//    }
//
//    //public String createDocument(org.apache.chemistry.opencmis.commons.data.Properties properties, String folderId, ContentStream contentStream, VersioningState versioningState, List<String> policies, Acl addAces, Acl removeAces) {
//    public String uploadDocument(IXConnection ixConnection, String pathNameOrId, String documentName, int maskId, String fileName, InputStream inputStream, int inputStreamLength, Map<String, Object> attributes, String version, String versionComment) {
//        Sord sord = null;
//        DocVersion docVersion = null;
//        int objectId = -1;
//        String eloPath = null;
//        IXServicePortC CONST = null;
//        EditInfo editInfo = null;
//        try {
//            CONST = ixConnection.getCONST();
//
//            //check if folderid is folder type
//            Sord parentSord = getSord(ixConnection, pathNameOrId, SordC.mbAll, LockC.NO);
//            if (parentSord == null || !isFolder(parentSord)) {
//                throw new de.elo.utils.net.RemoteException("Parent not found or is not a folder!");
//            }
//
//            eloPath = EloCmisUtils.getPathElo(parentSord, documentName);
//            try {
//                editInfo = ixConnection.ix().checkoutDoc(eloPath, null, EditInfoC.mbSordDocAtt, LockC.NO);
//            } catch (de.elo.utils.net.RemoteException e) {
//                IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
//                if (ixError.code == 5023) {
//                    try {
//                        editInfo = ixConnection.ix().createDoc(pathNameOrId, String.valueOf(maskId), null, EditInfoC.mbSordDocAtt);
//                    } catch (de.elo.utils.net.RemoteException e1) {
//                        throw new CmisRuntimeException(e1.getMessage(), e1);
//                    }
//                } else {
//                    throw new CmisRuntimeException(e.getMessage(), e);
//                }
//            }
//            sord = editInfo.getSord();
//            sord.setName(documentName);
//
//            Document document = null;
//            document = editInfo.getDocument();
//            if (document == null) {
//                document = new Document();
//            }
//
//            Map<Integer, ObjKey> objKeyMap = getSordObjKeys(sord);
//            setObjKey(objKeyMap, CONST.getDOC_MASK_LINE().getID_FILENAME(), CONST.getDOC_MASK_LINE().getNAME_FILENAME(), new String[]{fileName});
//            setObjKey(objKeyMap, attributes);
//            setSordObjKeyMap(sord,objKeyMap);
//
//            // Supply the file extension in a DocVersion object.
//            // Uses helper function from the IXClient.
//            String currentVersion = null;
//            if (document.getDocs() != null && document.getDocs().length > 0) {
//                currentVersion = document.getDocs()[0].getVersion();
//            }
//            DocVersion dv = new DocVersion();
//            dv.setExt(ixConnection.getFileExt(fileName));
//            dv.setPathId(sord.getPath());
//            document.setDocs(new DocVersion[]{dv});
//
//            // Step 2: Let IX create a upload URL
//            document = ixConnection.ix().checkinDocBegin(document);
//            dv = document.getDocs()[0];
//
//            // Step 3: Upload document. Use helper function of IXClient.
//            String uploadResult = null;
//            uploadResult = ixConnection.upload(dv.getUrl(), inputStream,  inputStreamLength, fileUtilsElo.getMimeType(fileName));
//
//            // assign response to uploadResult - contains document ID
//            dv.setUploadResult(uploadResult);
//
//            // Step 4: Commit document.
//            dv.setVersion(version);
//            dv.setComment(versionComment);
//            document = ixConnection.ix().checkinDocEnd(sord, SordC.mbAll, document, LockC.NO);
//
//            docVersion = document.getDocs()[0];
//            if (sord.getId() < 0) {
//                editInfo = ixConnection.ix().checkoutDoc(eloPath, null, EditInfoC.mbSord, LockC.NO);
//                sord = editInfo.getSord();
//            }
//        } catch (de.elo.utils.net.RemoteException e) {
//            throw new CmisRuntimeException(e.getMessage(), e);
//        }
//        return EloCmisUtils.calcObjectId(sord, docVersion);
//    }
//
//    public boolean isDocument(Sord sord) {
//        return (SordC.LBT_DOCUMENT <= sord.getType() && sord.getType() <= SordC.LBT_DOCUMENT_MAX);
//    }
//
//    public boolean isFolder(Sord sord) {
//        return ((sord.getType() == SordC.LBT_ARCHIVE) || (0 < sord.getType() && sord.getType() < SordC.LBT_DOCUMENT));
//    }
//
//    public boolean isRoot(Sord sord) {
//        return (sord.getType() == SordC.LBT_ARCHIVE);
//    }
//
//
//    //------------------------------------------------------------------------------------------------------------------
//    //--WORKFLOW METHODS------------------------------------------------------------------------------------------------
//    //------------------------------------------------------------------------------------------------------------------
//
//    public WFDiagram getWorkFlowTemplate(IXConnection ixConnection, String processDefinitionId, String versionId, WFDiagramZ wfDiagramZ, LockZ lockZ) throws de.elo.utils.net.RemoteException {
//        WFDiagram wfDiagram = null;
//        try {
//
//            wfDiagram = ixConnection.ix().checkoutWorkflowTemplate(processDefinitionId, null, WFDiagramC.mbAll, LockC.NO);
//        } catch (RemoteException e) {
//            IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
//            if (ixError.code != 5023) //cale incorecta
//            {
//                throw e;
//            } else {
//                return null;
//            }
//        } catch (NullPointerException e) {
//            throw e;
//        }
//        return wfDiagram;
//    }
//
//    public WFDiagram getWorkFlow(IXConnection ixConnection, String flowId, WFTypeZ wfTypeZ, WFDiagramZ wfDiagramZ, LockZ lockZ) throws de.elo.utils.net.RemoteException {
//        WFDiagram wfDiagram = null;
//        try {
//            wfDiagram = ixConnection.ix().checkoutWorkFlow(flowId, wfTypeZ, wfDiagramZ, lockZ);
//        } catch (RemoteException e) {
//            IXError ixError = IXError.parseException((de.elo.utils.net.RemoteException) e);
//            if (ixError.code != 5023) //cale incorecta
//            {
//                throw e;
//            } else {
//                return null;
//            }
//        } catch (NullPointerException e) {
//            throw e;
//        }
//        return wfDiagram;
//    }
//
//    public String startWorkFlow(IXConnection ixConnection, String templateId, String name, String sordId) throws de.elo.utils.net.RemoteException {
//        String workflowGUID = null;
//        int workflowId = ixConnection.ix().startWorkFlow(templateId, name, sordId);
//        WFDiagram wfDiagram = ixConnection.ix().checkoutWorkFlow(String.valueOf(workflowId), WFTypeC.ACTIVE, WFDiagramC.mbAll, LockC.NO);
//        workflowGUID = wfDiagram.getGuid();
//        // workflowGUID = workflowGUID.replace("(", "").replace(")", "");
//        return workflowGUID;
//    }
//
//    public WFNode getNode(IXConnection ixConnection, String workflowId, Integer nodeId) throws de.elo.utils.net.RemoteException {
//        for (WFNode wFNode : getWorkFlow(ixConnection, workflowId, WFTypeC.ACTIVE, WFDiagramC.mbAll, LockC.NO).getNodes()) {
//            if (wFNode.getId() == nodeId) {
//                return wFNode;
//            }
//        }
//        return null;
//    }
//
//    public String[] splitLoginUsers(String userIdentification) {
//        String[] userNames = userIdentification.split("@");
//        return userNames;
//    }
//
//    public Integer createUserGroup(IXConnection ixConnection, String groupName, String rightsAsUserId) {
//        try {
//            UserInfo group = ixConnection.ix().createUser(rightsAsUserId);
//            group.setType(UserInfoC.TYPE_GROUP);
//            group.setName(groupName);
//            int[] ints = ixConnection.ix().checkinUsers(new UserInfo[]{group}, CheckinUsersC.NEW_USER, LockC.YES);
//            return ints[0];
//        } catch (de.elo.utils.net.RemoteException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    public boolean checkIfGroupExist(IXConnection ixConnection, String groupName) {
//        try {
//            ixConnection.ix().checkoutUsers(new String[]{groupName}, CheckoutUsersC.ALL_GROUPS, LockC.NO);
//            return true;
//        } catch (de.elo.utils.net.RemoteException e) {
//            return false;
//        }
//    }
//
//    public List<WFNode> getCurrentNodesFromWFDiagram(WFDiagram wfDiagram) {
//        List<WFNodeAssoc> nodeAssocs = Arrays.asList(wfDiagram.getMatrix().getAssocs());
//        List<Integer> nodesFromNotDone = new ArrayList();
//        List<Integer> nodesToDone = new ArrayList();
//        for (WFNodeAssoc assoc : nodeAssocs) {
//            if (assoc.isDone()) {
//                nodesToDone.add(assoc.getNodeTo());
//            } else {
//                nodesFromNotDone.add(assoc.getNodeFrom());
//            }
//        }
//        nodesFromNotDone.retainAll(nodesToDone);
//        List<WFNode> allNodes = Arrays.asList(wfDiagram.getNodes());
//        List<WFNode> currentNodesList = new ArrayList<>();
//        for (Integer currentNodeId : nodesFromNotDone) {
//            for (WFNode wfNode : allNodes) {
//                if (currentNodeId.compareTo(wfNode.getId()) == 0) {
//                    currentNodesList.add(wfNode);
//                }
//            }
//        }
//        return currentNodesList;
//    }
//}
