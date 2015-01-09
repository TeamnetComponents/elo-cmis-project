package org.cmis.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Lucian.Dragomir on 6/6/2014.
 */
public interface BasePermission<T extends BasePermission> {

    public String getValue();

    public T getCmisStandardPermission();

    public boolean isCmisPermissionExact();

    public String getDescription();
}
