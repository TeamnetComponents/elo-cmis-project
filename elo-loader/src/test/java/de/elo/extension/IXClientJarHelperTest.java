package de.elo.extension;

import de.elo.extension.ix.client.IXClientJarHelper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Lucian.Dragomir on 6/5/2015.
 */
public class IXClientJarHelperTest {

    @Test
    public void testELO() throws IOException, ClassNotFoundException {
        IXClientJarHelper IXClientJarHelper = new IXClientJarHelper(IXConnData.getURL(), IXConnData.getBaseDir());
        IXClientJarHelper.addJarsToContextClassLoader();

        Class clazzIXConnectionTest = Thread.currentThread().getContextClassLoader().loadClass("de.elo.ix.client.IXConnFactory");
    }
}
