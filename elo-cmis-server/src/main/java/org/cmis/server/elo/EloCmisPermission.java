package org.cmis.server.elo;

import org.apache.chemistry.opencmis.commons.definitions.PermissionDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PermissionDefinitionDataImpl;
import org.cmis.base.BasePermission;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lucian.Dragomir on 6/5/2014.
 */
public enum EloCmisPermission implements BasePermission<EloCmisPermission> {
    CMIS_READ("cmis:read", null, true, "CMIS read permission."),
    CMIS_WRITE("cmis:write", null, true, "CMIS write permission"),
    CMIS_ALL("cmis:all", null, true, "CMIS all permission");

    private String value;
    private EloCmisPermission cmisStandardPermission;
    private boolean cmisPermissionExact;
    private String description;

    private PermissionDefinition permissionDefinition;

    EloCmisPermission(String value, EloCmisPermission cmisStandardPermission, boolean cmisPermissionExact, String description) {
        this.value = value;
        this.description = description;
        this.cmisStandardPermission = cmisStandardPermission;
        this.cmisPermissionExact = cmisPermissionExact;
        //create permission definition object
        this.permissionDefinition = createPermissionDefinition();
    }

    public static List<PermissionDefinition> getPermissionDefinitionList(boolean cmisStandardPermissionOnly) {
        List<PermissionDefinition> permissionDefinitionList = new ArrayList<>();
        for (EloCmisPermission eloCmisPermission : EloCmisPermission.values()) {
            if (!cmisStandardPermissionOnly || (cmisStandardPermissionOnly && eloCmisPermission.isCmisStandardPermission())) {
                permissionDefinitionList.add(eloCmisPermission.getPermissionDefinition());
            }
        }
        return permissionDefinitionList;
    }

    @Override
    public String getValue() {
        return value;
    }

    public String getValue(boolean standard) {
        return (standard) ? this.getCmisStandardPermission().getValue() : this.getValue();
    }

    @Override
    public EloCmisPermission getCmisStandardPermission() {
        return (cmisStandardPermission == null) ? this : cmisStandardPermission;
    }

    @Override
    public boolean isCmisPermissionExact() {
        return cmisPermissionExact;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public boolean isCmisStandardPermission() {
        return this.equals(this.getCmisStandardPermission());
    }

    public PermissionDefinition getPermissionDefinition() {
        return permissionDefinition;
    }

    private PermissionDefinition createPermissionDefinition() {
        PermissionDefinitionDataImpl permissionDefinition = new PermissionDefinitionDataImpl();
        permissionDefinition.setId(getValue());
        permissionDefinition.setDescription(getDescription());
        return permissionDefinition;
    }
}
