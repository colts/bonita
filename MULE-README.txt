








WELCOME
=======
Congratulations you have just created a new Mule transport!

This wizard created a number of new classes and resources useful for Mule transports.  Each of the created files
contains documentation and _todo_ items where necessary.  You'll need to look at each of the classes and other files and
address the _todo_ items in the files. Here is an overview of what was created.

./pom.xml:
A maven project descriptor that describes how to build this project.  If you enabled this project for the
MuleForge, this file will contain additional information about the project on MuleForge.

./assembly.xml:
A maven assembly descriptor that defines how this project will be packaged when you make a release.

./LICENSE.txt:
The open source license text for this project.

-----------------------------------------------------------------
./src/main/java/org/mule/transport/bos/i18n/BosMessages.java:

The BosMessages java class contains methods for access i18n messages embedded in your java code.

-----------------------------------------------------------------
./src/main/resources/META-INF/services/org/mule/i18n/bos-messages.properties

These message properties contain i18n strings used by BosMessages.java.


-----------------------------------------------------------------
./src/main/java/org/mule/transport/bos/BosConnector.java

The connector for this transport. This is used for configuing common properties on endpoints for this transport
and initialising shared resources.

-----------------------------------------------------------------
./src/main/java/org/mule/transport/bos/BosEndpointURIBuilder.java

The class responsible for parsing custom endpoints for this transport.

-----------------------------------------------------------------
./src/main/java/org/mule/transport/bos/BosInboundTransformer.java

This transformer should convert the inbound message into a type consumable by Mule.  For example, in the case of JMS this
class would would convert a JMSMessage to a String, object, Map, etc depending on the time of message.  If your transport
does not have a specific message type you do not need this class (see BosMessageAdapter).

-----------------------------------------------------------------
./src/main/java/org/mule/transport/bos/BosOutboundTransformer.java

This transformer should convert the otbound message into a type supported by the underlying technology.  For example,
in the case of JMS this class would would convert a MuleMessage to a JMSMessage.  If your transport
does not have a specific message type you do not need this class.




-----------------------------------------------------------------
./src/main/resources/META-INF/mule-bos.xsd

The configuration schema file for this module. All configuration elements should be defined in this schema.

-----------------------------------------------------------------
./src/main/resources/META-INF/spring.schemas

Contains a mapping of the Namespace URI for this projects schema.

-----------------------------------------------------------------
./src/main/resources/META-INF/spring.handlers

Contains a mapping of the namespace handler to use for the schema in this project.

-----------------------------------------------------------------
./src/main/java/org/mule/transport/bos/config/BosNamespaceHandler.java

The implmentation of the namespace handler used to parse elements defined in mule-bos.xsd.

TESTING
=======

This  project also contains test classes that can be run as part of a test suite.
-----------------------------------------------------------------
./src/test/java/org/mule/transport/bos/BosTestCase.java

This is an example functional test case.  The test will work as is, but you need to configure it to actually test your
code.  For more information about testing see: http://www.mulesource.org/display/MULE2USER/Functional+Testing.

-----------------------------------------------------------------
./src/test/resources/bos-functional-test-config.xml

Defines the Mule configuration for the BosTestCase.java.

-----------------------------------------------------------------
./src/test/java/org/mule/transport/bos/BosConnectorTestCase.java

The unit test case for testing the connecotr object for this transport.

-----------------------------------------------------------------
./src/test/java/org/mule/transport/bos/BosEndpointTestCase.java

The unit test case for testing the endpoint builder object for this transport.


-----------------------------------------------------------------
./src/test/java/org/mule/transport/bos/BosNamespaceHandlerTestCase.java

A test case that is used to test each of the configuration elements inside your mule-bos.xsd schema file.

-----------------------------------------------------------------
./src/test/resources/bos-namespace-config.xml

The configuration file for the BosNamespaceHandlerTestCase.java testcase.


ADDITIONAL RESOURCES
====================
Everything you need to know about getting started with Mule can be found here:
http://www.mulesource.org/display/MULE2INTRO/Home

There further useful information about extending Mule here:
http://mule.mulesource.org/display/MULE2USER/Introduction+to+Extending+Mule

We recommend you read the page on writing Mule transports if you have not done so already:
http://mule.mulesource.org/display/MULE2USER/Creating+Transports

There is also detailed information about creating Mule configuration schemas here:
http://mule.mulesource.org/display/MULE2USER/Creating+a+Custom+XML+Namespace

For information about working with Mule inside and IDE with maven can be found here:
http://www.mulesource.org/display/MULE2INTRO/Setting+Up+Eclipse

Remember if you get stuck you can try getting help on the Mule user list:
http://www.mulesource.org/display/MULE/Mailing+Lists

Also, MuleSource, the company behind Mule, offers 24x7 support options:
http://www.mulesource.com/services/subscriptions.php

Enjoy your Mule ride!

The Mule Team

--------------------------------------------------------------------
This project was auto-generated by the mule-transport-archetype.

artifactId=mule-transport-bos
description=BOS bpm
muleVersion=2.2.1
hasCustomSchema=y
hasReceiver=n
hasDispatcher=n
hasRequestor=n
hasCustomMessageAdapter=n
hasBootstrap=n
hasTransactions=n
hasCustomTransactions=n
inboundTransformer=n
outboundTransformer=n
ModuleType=Transport
forgeProject=y
transports=vm,bpm,stdio
modules=client

version=1.0-SNAPSHOT
groupId=org.mule.transports
basedir=F:\Ricston\other-svn\ricston\ricston-dev\Projects\bos-mule-connector\mule-bos-transport-2.2.1
--------------------------------------------------------------------