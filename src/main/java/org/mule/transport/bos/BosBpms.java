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

import org.mule.api.config.MuleProperties;
import org.mule.transport.bpm.BPMS;
import org.mule.transport.bpm.MessageService;
import org.mule.transport.bpm.ProcessConnector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.ActivityDefNotFoundException;
import org.ow2.bonita.facade.exception.BonitaInternalException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.exception.TaskNotFoundException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.SimpleCallbackHandler;
import org.slf4j.LoggerFactory;

public class BosBpms implements BPMS
{

    ManagementAPI managementAPI;
    RuntimeAPI runtimeAPI;
    QueryRuntimeAPI queryRuntimeAPI;
    QueryDefinitionAPI queryDefinitionAPI;
    String login;
    String password;
    LoginContext loginContext=null;

    public final static String BOS_PROPERTY_PREFIX="BOS_";
    public final static String BOS_PROPERTY_ACTIVITY= MuleProperties.PROPERTY_PREFIX + BOS_PROPERTY_PREFIX + "ACTIVITY";
    public final static String BOS_PROPERTY_ACTIVITY_NAME=BOS_PROPERTY_ACTIVITY +"_NAME";
    public final static String BOS_PROPERTY_DESTINATION_VARIABLE= MuleProperties.PROPERTY_PREFIX + BOS_PROPERTY_PREFIX + "RESULT_VARIABLE";
    
    public final static String PROCESS_ID = "processId";
    public final static String ACTIVITY_ID = "activityId";

    protected static final org.slf4j.Logger log = LoggerFactory.getLogger(BosBpms.class);

    /* Constructor with parameters to initialize properties */
    public BosBpms(ManagementAPI managementAPI,
                   RuntimeAPI runtimeAPI,
                   QueryRuntimeAPI queryRuntimeAPI,
                   String login,
                   String password)
    {
        this.managementAPI = managementAPI;
        this.runtimeAPI = runtimeAPI;
        this.queryRuntimeAPI = queryRuntimeAPI;
        this.queryDefinitionAPI = AccessorUtil.getQueryDefinitionAPI();
        this.login = login;
        this.password = password;
    }

    /* Takes care of retrieving the last process through the query API and then starting the process. */
    public Object startProcess(Object processType, Object transition, Map processVariables) throws Exception
    {
        login();
        
        // TODO use versioning
        log.info("Starting Process " + processType);
        ProcessDefinition processDef = queryDefinitionAPI.getLastProcess((String) processType);
        
        massageProcessVariables(processVariables);
        
        ProcessInstanceUUID instanceUUID;
        try
        {
            instanceUUID = runtimeAPI.instantiateProcess(processDef.getUUID(),processVariables);
        }catch(BonitaInternalException x)
        {
            //This happens VERY RARELY...
            x.printStackTrace();
            log.warn("When attempting to instanciate process "+processDef.getUUID()+" the following exception was received:"+x.getMessage());
            log.info("Retrying to instanciate process");                    
            instanceUUID = runtimeAPI.instantiateProcess(processDef.getUUID(),processVariables);
        }
       
        log.info("Started Process " + instanceUUID);
        return instanceUUID;
    }
    /* Cancels the process */
    public void abortProcess(Object processId) throws Exception
    {
        login();
        runtimeAPI.cancelProcessInstance(new ProcessInstanceUUID((String) processId));
    }

    /* Initially, the Bos property activity is set and the massageProcessVariables method is called. 
     * If the transition is not null, the activity instance and process instance are set and the task is executed, 
     * finally returning the processInstanceUUID. Otherwise, if null, the process instance is retrieved 
     * and the tasks are cycled through to get their state. 
     * If not null, each task is executed and finally the process is returned. 
     */
    public Object advanceProcess(Object processId, Object transition, Map processVariables) throws Exception
    {
        login();
        log.debug("advanceProcess called with processId "+processId+" and transition "+transition);
        
        //Has Bos activity property been set?
        if(processVariables.containsKey(BOS_PROPERTY_ACTIVITY))
        {
            //if the Activity property is set, use that instead of whatever the BPM transport put as a transition
            transition=processVariables.get(BOS_PROPERTY_ACTIVITY);
        }
        String activityName=null;
        if(processVariables.containsKey(BOS_PROPERTY_ACTIVITY_NAME))
        {
            activityName=(String) processVariables.get(BOS_PROPERTY_ACTIVITY_NAME);
        }
        massageProcessVariables(processVariables);
        
        ActivityInstanceUUID activityInstanceUUID;
        if (transition != null)
        {
            activityInstanceUUID=(transition instanceof ActivityInstance)?
                                         ((ActivityInstance) transition).getUUID():
                                         new ActivityInstanceUUID(transition.toString());
            ProcessInstanceUUID processInstanceUUID=(processId instanceof ProcessInstance)?
                                                           ((ProcessInstance) processId).getUUID():
                                                           new ProcessInstanceUUID((String) processId);
            log.info("Advancing process " + processInstanceUUID + " by executing task "
                     + activityInstanceUUID);
            
            executeTask(activityInstanceUUID,restrictVariables(processInstanceUUID,activityInstanceUUID,processVariables));
            return processInstanceUUID;
        }
        else
        {
            ProcessInstance p=(processId instanceof ProcessInstance)?(ProcessInstance) processId:
                                                  queryRuntimeAPI.getProcessInstance(new ProcessInstanceUUID((String) processId));
            
            Set<TaskInstance> tasks;
            if(activityName==null)
            {
                tasks=p.getTasks();
            }else
            {
                Set<ActivityInstance> activitySet=p.getActivities(activityName);
                tasks=new HashSet<TaskInstance>();
                for(ActivityInstance a:activitySet)
                {
                    if(a.isTask())
                    {
                        tasks.add((TaskInstance)a);
                    }
                }
            }
            TaskInstance nextTask = null;
            
            for (TaskInstance t : tasks)
            {
                if (ActivityState.READY.equals(t.getState()))
                {
                    nextTask = t;
                    break;
                }
            }

            // advance activity
            if (nextTask != null)
            {
                log.info("Advancing process " + p.getUUID() + " by executing the first ready task "
                         + nextTask.getUUID());
                executeTask(nextTask.getUUID(),restrictVariables(p.getUUID(),nextTask.getUUID(),processVariables));
            }
            else
            {
                throw new Exception("Could not Advance Process!");
            }

            return p.getProcessInstanceUUID();
        }
    }

    /* Using the process instance and the activity instance passed through the parameters, 
     * two maps are retrieved containing the relative datafields. 
     * The activity fields Map is appended the process fields map. 
     * For each field in the activity fields map, the values are put in a separate hash map, 
     * only if the values are found in the process fields map. Finally the new hash map is returned. 
     */
    private Map restrictVariables(ProcessInstanceUUID processInstanceUUID,ActivityInstanceUUID activityInstanceUUID,Map processVariables) throws ActivityDefNotFoundException, ProcessNotFoundException
    {
        HashMap restricted=new HashMap();
        Set<DataFieldDefinition> activityFields=queryDefinitionAPI.getActivityDataFields(activityInstanceUUID.getActivityDefinitionUUID());
        Set<DataFieldDefinition> processFields=queryDefinitionAPI.getProcessDataFields(processInstanceUUID.getProcessDefinitionUUID());
        activityFields.addAll(processFields);
        
        for(DataFieldDefinition f:activityFields)
        {
            if(processVariables.containsKey(f.getName()))
            {
                restricted.put(f.getName(), processVariables.get(f.getName()));
            }
        }
        return restricted;
    }
    
    /* Given the ActivityInstanceUUID and the Map containing the ProcessVariables, 
     * the process is started using the runtimeAPI. If it fails, it waits and tries again. 
     * Then the runtimeAPI is set with every variable present in the map processVariables. 
     * Finally the runtimeAPI is used to finish the task. */
    private synchronized void executeTask(ActivityInstanceUUID activityInstanceUUID, Map processVariables)
        throws Exception
    {

        try
        {
            runtimeAPI.startTask(activityInstanceUUID, true);
        }
        catch (TaskNotFoundException e)
        {
            // Sometimes, you need to wait for a few seconds before the task is
            // available...
            log.warn("Task was not Found, sleeping once before retrying");
            Thread.sleep(500);
            try
            {                
                runtimeAPI.startTask(activityInstanceUUID, true);
            }catch(TaskNotFoundException e2)
            {
                log.warn("Task was not Found, sleeping second and last time before retrying");
                Thread.sleep(1500);
                runtimeAPI.startTask(activityInstanceUUID, true);
            }
        }
        for (Object key : processVariables.keySet())
        {
            try
            {
                try
                {
                    runtimeAPI.setVariable(activityInstanceUUID, key.toString(), processVariables.get(key));
                }catch(BonitaInternalException x)
                {
                    //This happens VERY RARELY...
                    x.printStackTrace();
                    log.warn("When attempting to set variable "+key.toString()+" to value "+processVariables.get(key)+" the following exception was received:"+x.getMessage());
                    log.info("Retrying to set variable");                    
                    runtimeAPI.setVariable(activityInstanceUUID, key.toString(), processVariables.get(key));
                }
            }
            catch (VariableNotFoundException e)
            {
                // This should NOT happen anymore!
                log.warn("Property " + key.toString() + " with value " + processVariables.get(key)
                         + " has not been added since Variable was not found in process");
            }
        }
        runtimeAPI.finishTask(activityInstanceUUID, true);
    }

    /* Return the process */
    public Object getId(Object process) throws Exception
    {

        return process;
    }
    
    /* Login and return the state of the Process Instance. */
    public Object getState(Object process) throws Exception
    {
        login();
        return queryRuntimeAPI.getProcessInstance((ProcessInstanceUUID) process).getInstanceState();
    }

    /* Checks if process has ended - is in Finished state. */
    public boolean hasEnded(Object process) throws Exception
    {

        return InstanceState.FINISHED.equals(getState(process));
    }

    /* Checks if an object is an instance of the ProcessInstanceUUID class. */
    public boolean isProcess(Object obj) throws Exception
    {
        if (obj instanceof ProcessInstanceUUID)
        {
            return true;
        }
        return false;
    }

    public Object lookupProcess(Object processId) throws Exception
    {

        return null;
    }

    public void setMessageService(MessageService msgService)
    {
        //This is not applicable, since technically we can use the Bonita Mule Connector to communicate with Mule
    }

    public Object updateProcess(Object processId, Map processVariables) throws Exception
    {
        //In Bonita, it does not really make sense to update a process. 
        //Once a process is executed, it should not be changed, unless the flow requires it to change
        throw new UnsupportedOperationException("Cannot Update a Bonita Process");
    }
    
    /* Checks if the BOS property destination variable has been set. If yes, then process them accordingly. */
    protected void massageProcessVariables(Map processVariables)
    {
        //Has the Bos destination variable property been set?
        if(processVariables.containsKey(BOS_PROPERTY_DESTINATION_VARIABLE))
        {
            processVariables.put(processVariables.get(BOS_PROPERTY_DESTINATION_VARIABLE), processVariables.get(ProcessConnector.PROCESS_VARIABLE_INCOMING));
            log.info("Mapped payload with value \""+processVariables.get(ProcessConnector.PROCESS_VARIABLE_INCOMING)+"\" to \""+processVariables.get(BOS_PROPERTY_DESTINATION_VARIABLE)+"\"");
            processVariables.remove(BOS_PROPERTY_DESTINATION_VARIABLE);
            processVariables.remove(ProcessConnector.PROCESS_VARIABLE_INCOMING);            
        }
    }

    /* If the login context is null, a new login context is created from the supplied username and password and a login is performed. */
    protected void login() throws LoginException
    {
        // TODO make the possibility for Mule to log in as Multiple users
    	//Log in every time
        LoginContext ctx = new LoginContext("Bonita", new SimpleCallbackHandler(login, password));        
        ctx.login();
        log.info("Logged in as " + managementAPI.getLoggedUser());        
    }
    
    protected void logout() throws LoginException
    {
        //This is not necessary since Bonita is Embedded
        //loginContext.logout();
    }

}
