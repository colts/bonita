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

import java.util.HashMap;

import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;

public class HelloWorldTutorialV3TestCase extends FunctionalTestCase {

    @Override
    protected String getConfigResources()
    {
        return "mule/hello-world-tutorial-v3-config.xml";
    }
    
    public void testSimple() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        //start a new Process
        MuleMessage msg=client.send("bos://HelloWorldTutorialV3", "",null);
        ProcessInstanceUUID processInstanceUUID=(ProcessInstanceUUID) msg.getPayload();
        
        //do the "human" work by supplying a new
        HashMap props=new HashMap();
        props.put(BosBpms.BOS_PROPERTY_DESTINATION_VARIABLE, "name");
        client.dispatch("bos://HelloWorldTutorialV3/"+processInstanceUUID.getValue(), "Stephen",props);
        
        //bonita will send the name to mule who will do the work to get the greetings and the execute the task
        
        //finally mule gets the result, and technically, the "human" will get the result as well.
        Thread.sleep(3000);
        MuleMessage result = client.request("vm://result", 10000);
        assertNotNull(result);
        assertEquals("Hello Stephen!",result.getPayloadAsString());
    }

	
}
