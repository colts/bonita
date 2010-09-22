/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) Ricston Ltd  All rights reserved.  http://www.ricston.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.bos;

import org.mule.tck.FunctionalTestCase;

/**
 * Simple test to check that the Connector configuration works as expected
 */
public class BosNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "mule/bos-namespace-config.xml";
    }

    public void testBosConfig() throws Exception
    {
        BosConnector c = (BosConnector) muleContext.getRegistry().lookupConnector("bosConnector");
        assertNotNull(c);
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());

        //Asserting that specific properties are configured correctly
        assertEquals("admin",c.getLogin());
        assertEquals("bpm",c.getPassword());
        assertEquals("src/test/resources/bonita/jaas-standard.cfg",c.getJassFilePath());
        assertEquals(1,c.getBarFilePaths().size());
        assertEquals("src/test/resources/processes/example_1.0.bar",c.getBarFilePaths().get(0));

    }
}
