
package org.mule.transport.bos.runtime;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import javax.security.auth.login.LoginContext;

import junit.framework.TestCase;

import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BusinessArchiveFactory;
import org.ow2.bonita.util.SimpleCallbackHandler;

/*
 * NB. This test started off as the implementation of the tutorial on Bonita's Blog at:
 * http://www.bonitasoft.org/blog/tutorial/building-your-applications-with-bonita-runtime-part-1/
 * as a Unit Test
 */


public class BosRuntimeTestCase extends TestCase
{

    private static final String LOGIN = "admin";
    private static final String PASSWORD = "bpm";
    private static final String BAR_FILE_PATH = "src/test/resources/processes/example_1.0.bar";
    private static final String JAAS_FILE_PATH = "src/test/resources/bonita/jaas-standard.cfg";
    private static final String BOS_ENV = "src/test/resources/bonita/bonita-environment.xml";

    // get all used APIs
    final ManagementAPI managementAPI = AccessorUtil.getManagementAPI();
    final RuntimeAPI runtimeAPI = AccessorUtil.getRuntimeAPI();
    final QueryRuntimeAPI queryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI();

    static
    {
        System.setProperty(BonitaConstants.JAAS_PROPERTY, JAAS_FILE_PATH);
        System.setProperty(BonitaConstants.ENVIRONMENT_PROPERTY, BOS_ENV);
    }

    public void testStartRuntime() throws Exception
    {
        Collection<TaskInstance> tasks = null;

        // login
        LoginContext loginContext = new LoginContext("Bonita", new SimpleCallbackHandler(LOGIN, PASSWORD));
        loginContext.login();

        // clear everything, just in case there is any state left.
        managementAPI.deleteAllProcesses();

        // deploy the bar file
        final File barFile = new File(BAR_FILE_PATH);
        final BusinessArchive businessArchive = BusinessArchiveFactory.getBusinessArchive(barFile);
        final ProcessDefinition process = managementAPI.deploy(businessArchive);
        final ProcessDefinitionUUID processUUID = process.getUUID();

        // check process was deployed
        QueryDefinitionAPI queryDefinitionAPI = AccessorUtil.getQueryDefinitionAPI();
        Set<ProcessDefinition> processes = queryDefinitionAPI.getProcesses();
        assertEquals(1,processes.size());
        assertEquals(processUUID,processes.iterator().next().getUUID());
        
        // kick off process
        final ProcessInstanceUUID instanceUUID = runtimeAPI.instantiateProcess(processUUID);
        
        //Show activities to screen
        displayProcessActivities(instanceUUID);

        // get the task list and check it is not empty
        tasks = queryRuntimeAPI.getTaskList(ActivityState.READY);
        assertEquals(1,tasks.size());


        // get the first task in the list which must be "task1"
        final TaskInstance task1 = tasks.iterator().next();
        assertEquals("task1",task1.getActivityName());


        // execute task1 and assign it to me
        runtimeAPI.executeTask(task1.getUUID(), true);
        displayProcessActivities(instanceUUID);

        // check we have a new task in the task list
        tasks = queryRuntimeAPI.getTaskList(ActivityState.READY);
        assertEquals(1,tasks.size());

        // get the first task in the list which must be "task1"
        final TaskInstance task2 = tasks.iterator().next();
        assertEquals("task2",task2.getActivityName());

        // assign task2 to another user
        runtimeAPI.assignTask(task2.getUUID(), "john");

        // check my tasklist is empty
        tasks = queryRuntimeAPI.getTaskList(ActivityState.READY);
        assertEquals(0,tasks.size());

        // assign back task2 to admin and execute it
        runtimeAPI.assignTask(task2.getUUID(), LOGIN);
        runtimeAPI.executeTask(task2.getUUID(), true);

        displayProcessActivities(instanceUUID);

        // check process instance is finished
        final InstanceState instanceState = queryRuntimeAPI.getProcessInstance(instanceUUID)
            .getInstanceState();
        assertEquals(InstanceState.FINISHED,instanceState);

    }

    private void displayProcessActivities(ProcessInstanceUUID instanceUUID) throws Exception
    {
        ProcessInstance p = queryRuntimeAPI.getProcessInstance(instanceUUID);
        System.out.println(p);
        Set<ActivityInstance> activities = p.getActivities();
        System.out.println("Activities");
        for (ActivityInstance a : activities)
        {
            System.out.println(a);
        }
        System.out.println("Tasks");
        Set<TaskInstance> tasks = p.getTasks();
        for (TaskInstance t : tasks)
        {
            System.out.println(t);
        }
    }
}
