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

import java.util.ArrayList;
import java.util.LinkedList;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class ParallelTasksProcessTestCase extends FunctionalTestCase
{

    public void testSimple() throws Exception
    {
        doTestSimple();
        //Wait for Bonita to persist whatever it needs.
        Thread.sleep(1000);
    }
    
    public void testParallelCalls() throws Exception
    {
        int requestCount=20;
        MuleClient client=new MuleClient(muleContext);
        ArrayList<String> results=new ArrayList<String>();
        
        //Kick Off processes
        LinkedList<String> list;
        for(int i=0;i<requestCount;i++)
        {
            list= new LinkedList<String>();
            list.add("Stephen"+i);
            list.add("Fenech"+i);
            client.dispatch("bos://ParallelTasksProcess", list,null);
        }
        Thread.sleep(3000);
        
        //Get back results
        for(int i=0;i<requestCount;i++)
        {
            MuleMessage result=client.request("vm://result", 10000);
            assertNotNull(result);
            results.add(result.getPayloadAsString());
        }
        assertEquals(requestCount,results.size());
        for(int i=0;i<requestCount;i++)
        {
            assertTrue(results.contains("Stephen"+i+" is your name... and Fenech"+i+" is your surname..."));
        }
        
    }
    
    public void dtestSequentialCalls() throws Exception
    {
        for(int i=0;i<8;i++)
        {
            testSimple();
        }
        //Wait for Bonita to persist whatever it needs.
        Thread.sleep(1000);
    }
    
    protected void doTestSimple() throws Exception
    {
        MuleClient client=new MuleClient(muleContext);
        LinkedList<String> list = new LinkedList<String>();
        list.add("Stephen");
        list.add("Fenech");
        
        //Kick of the process
        client.dispatch("bos://ParallelTasksProcess", list,null);
        
        //Get back the results
        MuleMessage result=client.request("vm://result", 10000);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof String);
        assertEquals("Stephen is your name... and Fenech is your surname...",result.getPayloadAsString());
        
    }
    

    
    @Override
    protected String getConfigResources()
    {
        return "mule/parallel-tasks-process-mule-config.xml";
    }

}


