package org.cmis.util;

import de.elo.ix.client.IXConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Lucian.Dragomir on 6/8/2014.
 */
public class DateUtil {
    private static final Logger LOG = LoggerFactory.getLogger(DateUtil.class);


    private static final String ELOPATTERN = "yyyyMMddHHmmss";
    private static final String DB2PATTERN = "yyyyMMddHHmm";

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

//    public static String convertCalendar2Iso(Date date) {
//        SimpleDateFormat formatter = new SimpleDateFormat(ELOPATTERN);
//        return formatter.format(date);
//    }
}
