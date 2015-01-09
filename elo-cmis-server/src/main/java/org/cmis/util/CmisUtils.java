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
package org.cmis.util;

import de.elo.ix.client.Sord;
import de.elo.ix.client.SordC;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.*;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class CmisUtils {
    private static final Logger LOG = LoggerFactory.getLogger(CmisUtils.class);


    private CmisUtils() {
    }

    /**
     * Returns the boolean value of the given value or the default value if the
     * given value is <code>null</code>.
     */
    public static boolean getBooleanParameter(Boolean value, boolean def) {
        if (value == null) {
            return def;
        }

        return value.booleanValue();
    }

    public static GregorianCalendar getCurrentDateTime() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        return calendar;
    }

    /**
     * Converts milliseconds into a {@link java.util.GregorianCalendar} object, setting
     * the timezone to GMT and cutting milliseconds off.
     */

    public static GregorianCalendar DateIsoToCalendar(String datetime) {
        GregorianCalendar calendar = new GregorianCalendar();
        //Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
        try {
            calendar.setTime(sdf.parse("Mon Mar 14 16:02:37 GMT 2011"));// all done
        } catch (ParseException e) {
            throw new CmisRuntimeException("Unable to parse date format.", e);
        }

        return calendar;
    }

    public static GregorianCalendar millisToCalendar(long millis) {
        GregorianCalendar result = new GregorianCalendar();
        result.setTimeZone(TimeZone.getTimeZone("GMT"));
        result.setTimeInMillis((long) (Math.ceil((double) millis / 1000) * 1000));

        return result;
    }

    /**
     * Splits a filter statement into a collection of properties. If
     * <code>filter</code> is <code>null</code>, empty or one of the properties
     * is '*' , an empty collection will be returned.
     */
    public static Set<String> splitFilter(String filter) {
        if (filter == null) {
            return null;
        }

        if (filter.trim().length() == 0) {
            return null;
        }

        Set<String> result = new HashSet<String>();
        for (String s : filter.split(",")) {
            s = s.trim();
            if (s.equals("*")) {
                return null;
            } else if (s.length() > 0) {
                result.add(s);
            }
        }

        // set a few base properties
        // query name == id (for base type properties)
        result.add(PropertyIds.OBJECT_ID);
        result.add(PropertyIds.OBJECT_TYPE_ID);
        result.add(PropertyIds.BASE_TYPE_ID);

        return result;
    }

    /**
     * Gets the type id from a set of properties.
     */
    public static String getObjectTypeId(Properties properties) {
        PropertyData<?> typeProperty = properties.getProperties().get(PropertyIds.OBJECT_TYPE_ID);
        if (!(typeProperty instanceof PropertyId)) {
            throw new CmisInvalidArgumentException("Type Id must be set!");
        }

        String typeId = ((PropertyId) typeProperty).getFirstValue();
        if (typeId == null) {
            throw new CmisInvalidArgumentException("Type Id must be set!");
        }

        return typeId;
    }




}
