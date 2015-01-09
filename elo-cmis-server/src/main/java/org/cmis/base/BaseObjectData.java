package org.cmis.base;

import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectDataImpl;
import org.apache.chemistry.opencmis.commons.impl.server.ObjectInfoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Lucian.Dragomir on 6/9/2014.
 */
public class BaseObjectData {
    private static final Logger LOG = LoggerFactory.getLogger(BaseObjectData.class);

    private final ObjectDataImpl objectData;
    private final ObjectInfoImpl objectInfo;

    public BaseObjectData() {
        super();
        this.objectData = new ObjectDataImpl();
        this.objectInfo = new ObjectInfoImpl();
    }

    public ObjectDataImpl getObjectData() {
        return objectData;
    }

    public ObjectInfoImpl getObjectInfo() {
        return objectInfo;
    }
}
