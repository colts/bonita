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

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.ConnectorException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.bpm.BPMS;
import org.mule.transport.bpm.ProcessConnector;

import com.ricston.bonitasoft.connectors.mule.MuleManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BusinessArchiveFactory;
import org.ow2.bonita.util.SimpleCallbackHandler;

/**
 * <code>BosConnector</code> TODO document
 */
public class BosConnector extends ProcessConnector
{
    public static final String BOS = "bos";
    
    ManagementAPI managementAPI;
	RuntimeAPI runtimeAPI;
	QueryRuntimeAPI queryRuntimeAPI;
	QueryDefinitionAPI queryDefinitionAPI;
	
	String login = "admin";
	String password = "bpm";
	String jaasFilePath;
	String bonitaEnvironmentPath=null;
	ArrayList<String> barFilePaths=new ArrayList<String>();
	boolean undeployUnlistedProcesses=true;
	boolean redeployProcesses=false;
	
	BosBpms bosBpms;
	
	public BosConnector(MuleContext context)
    {
        super(context);
    }
	
	/* Initializes the Jass property retrieved from the file specified in the file path. 
	 * The same is done for the Bonita Environment property which is retrieved through the file 
	 * specified in the file path. Finally the bonita property representing of the API type is set. */
	public void doInitialise() throws InitialisationException
    {
	    logger.debug("jassFilePath is "+jaasFilePath);
	    logger.debug("bonitaEnvironmentPath is "+bonitaEnvironmentPath);
	    logger.debug("System Property "+BonitaConstants.JAAS_PROPERTY+" before being set is "+System.getProperty(BonitaConstants.JAAS_PROPERTY));
	    logger.debug("System Property "+BonitaConstants.ENVIRONMENT_PROPERTY+" before being set is "+System.getProperty(BonitaConstants.ENVIRONMENT_PROPERTY));
    	if(jaasFilePath!=null)
		{
			System.setProperty(BonitaConstants.JAAS_PROPERTY, jaasFilePath);			
		}
		if(bonitaEnvironmentPath!=null)
		{
		    System.setProperty(BonitaConstants.ENVIRONMENT_PROPERTY, bonitaEnvironmentPath);
		}
		System.setProperty(BonitaConstants.API_TYPE_PROPERTY, "Standard");
    }

	/* Retreival of APIs */
    public void doConnect() throws Exception
    {
    	managementAPI = AccessorUtil.getManagementAPI();
		runtimeAPI = AccessorUtil.getRuntimeAPI();
		queryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI();
		queryDefinitionAPI = AccessorUtil.getQueryDefinitionAPI();
		bosBpms=new BosBpms(managementAPI,runtimeAPI,queryRuntimeAPI,login, password);
    }

    public void doDisconnect() throws Exception
    {
        //Nothing to do...
    }

    /* Mule logs in the bonita engine using the login in the BosBpms class. 
     * The bar files are deployed and the process UUIDs are stored in an 
     * array list and the context is registered. 
     * If the connector is set to undeploy unlisted process, it runs 
     * through the current list of processes and undeploys the unlisted processes. 
     * A Mule exception is thrown in the case that an exception is thrown while executing the method.
     */
    public void doStart() throws MuleException
    {
        ArrayList<ProcessDefinitionUUID> pUUIDs=new ArrayList<ProcessDefinitionUUID>();
		try {
			login();
			for (String s : barFilePaths) {
				logger.info("Starting Deployment of "+s);
				ProcessDefinitionUUID puuid=deployBarFile(s);
				pUUIDs.add(puuid);
				registerContext(puuid.toString());				
			}
			//Check if should undeploy any processes
			if(undeployUnlistedProcesses)
			{
			    Set<ProcessDefinition> processes=queryDefinitionAPI.getProcesses();
			    ArrayList<ProcessDefinition> toUndeploy=new ArrayList<ProcessDefinition>();
			    for(ProcessDefinition p:processes)
			    {
			        if(!pUUIDs.contains(p.getUUID()))
			        {
			            toUndeploy.add(p);
			        }
			    }
			    for(ProcessDefinition p:toUndeploy)
			    {
			        logger.info("Undeploying "+p.getUUID()+" since not in Connector list and undeployUnlistedProcesses is true");
			        managementAPI.deleteProcess(p.getUUID());
			    }
			}
		} catch (Exception e) {
			throw new ConnectorException(MessageFactory.createStaticMessage("Error while starting Connector"),this,e);
		}
    }

    /* Mule performs a logout from the Bonita engine using the BosBpms class. */
    public void doStop() throws MuleException
    {
    	try {
    	    bosBpms.logout();
		} catch (LoginException e) {
			throw new DefaultMuleException(e);
		}
    }

    public void doDispose()
    {
        //Nothing to do...
    }
    
    /* Logs in using the BosBpms class */
    private void login() throws LoginException {
        bosBpms.login();
	}
    
    /* Registration of the Mule Context */
    private void registerContext(String s) throws Exception{
        MuleManager.getInstance().registerContext(s, getMuleContext());
    }
    
    /* Retrieves the file specified in the string and a business archive 
     * is created as an object from the file retrieved. 
     * If the file is found and redeploying is enabled in the connector, 
     * it will delete the process. Finally the process is deployed. */
	private ProcessDefinitionUUID deployBarFile(String s) throws Exception{
		final File barFile = new File(s);
		final BusinessArchive businessArchive = BusinessArchiveFactory
				.getBusinessArchive(barFile);
		
		//Check if process is already deployed
		Set<ProcessDefinition> alreadyDeployed=queryDefinitionAPI.getProcesses();
		ProcessDefinition processDefn=businessArchive.getProcessDefinition();
		if(alreadyDeployed.contains(processDefn))
	    {
		    ProcessDefinition p=queryDefinitionAPI.getProcess(processDefn.getUUID());
		    logger.info("Process "+p.getUUID()+" is already deployed.");
		    if(redeployProcesses)
		    {
		        logger.info("Deleting "+p.getUUID()+" for Redeployment");
		        managementAPI.deleteProcess(p.getUUID());
		    }else
		    {
		        logger.info("Not Deploying "+p.getUUID()+" since already deployed and redeployProcesses is false");
		        return p.getUUID();
		    }
	    }

		 final ProcessDefinition process = managementAPI.deploy(businessArchive);
		 logger.info("Deployed "+process.getUUID());
         return process.getUUID();
	}

    public String getProtocol()
    {
        return BOS;
    }
    
	@Override
	public BPMS getBpms() {
		return bosBpms;
	}
	
	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getJaasFilePath() {
		return jaasFilePath;
	}

	public void setJaasFilePath(String jassFilePath) {
		this.jaasFilePath = jassFilePath;
	}

	public ArrayList<String> getBarFilePaths() {
		return barFilePaths;
	}

	public void setBarFilePaths(ArrayList<String> barFilePaths) {
		this.barFilePaths = barFilePaths;
	}

    public boolean isUndeployUnlistedProcesses()
    {
        return undeployUnlistedProcesses;
    }

    public void setUndeployUnlistedProcesses(boolean undeployUnlistedProcesses)
    {
        this.undeployUnlistedProcesses = undeployUnlistedProcesses;
    }

    public boolean isRedeployProcesses()
    {
        return redeployProcesses;
    }

    public void setRedeployProcesses(boolean redeployProcesses)
    {
        this.redeployProcesses = redeployProcesses;
    }

    public String getBonitaEnvironmentPath()
    {
        return bonitaEnvironmentPath;
    }

    public void setBonitaEnvironmentPath(String bonitaEnvironmentPath)
    {
        this.bonitaEnvironmentPath = bonitaEnvironmentPath;
    }
	
	

}
