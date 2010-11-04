/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) Ricston Ltd  All rights reserved.  http://www.ricston.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.bos.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.bos.BosBpms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HelloWorldFuntionalTestCase extends FunctionalTestCase
{

    public void testSimple() throws Exception
    {
        doTestSimple();
        //Wait for Bonita to Persist anything it needs...
        Thread.sleep(500);
    }
    
    public void stestMultipleSimple() throws Exception
    {
        for(int i=0;i<3;i++)
        {
            doTestSimple();
        }
        //Wait for Bonita to Persist anything it needs...
        Thread.sleep(500);
    }
    
    public void stestParallelCalls() throws Exception
    {
        int requestCount=20;
        MuleClient client=new MuleClient(muleContext);
        ArrayList<String> results=new ArrayList<String>();
        
        //Set the destination name variable
        Map props=new HashMap();
        props.put(BosBpms.BOS_PROPERTY_DESTINATION_VARIABLE, "name");
        
        //Kick of the processes
        for(int i=0;i<requestCount;i++)
        {            
            client.dispatch("bos://HelloWorld", "Stephen Fenech"+i,props);
        }
        Thread.sleep(3000);
        MuleMessage result;
        
        //Collect the results
        for(int i=0;i<requestCount;i++)
        {
            result=client.request("vm://result", 10000);
            assertNotNull(result);
            results.add(result.getPayloadAsString());
        }
        
        //Check that ALL have arrived
        assertEquals(requestCount,results.size());
        for(int i=0;i<requestCount;i++)
        {
            assertTrue(results.contains("Stephen Fenech"+i+", Mule says Hello!"));
        }
        
        //Wait for Bonita to Persist anything it needs...
        Thread.sleep(500);
        
    }
    
    public void doTestSimple() throws Exception
    {
        MuleClient client=new MuleClient(muleContext);
        
        //Set the destination name variable
        Map props=new HashMap();
        props.put(BosBpms.BOS_PROPERTY_DESTINATION_VARIABLE, "name");
        
        //Get Result
        MuleMessage msg=client.send("bos://HelloWorld", "Stephen Fenech",props);
        MuleMessage result=client.request("vm://result", 10000);
        assertNotNull(result);
        assertEquals("Stephen Fenech, Mule says Hello!",result.getPayloadAsString());
    }
    
    @Override
    protected String getConfigResources()
    {
        return "mule/hello-world-mule-config.xml";
    }

}


