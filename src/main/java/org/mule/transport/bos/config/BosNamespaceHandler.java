/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) Ricston Ltd  All rights reserved.  http://www.ricston.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.bos.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildListDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.bos.BosConnector;

/**
 * Registers a Bean Definition Parser for handling <code><bos:connector></code> elements
 * and supporting endpoint elements.
 */
public class BosNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(BosConnector.BOS, URIBuilder.PATH_ATTRIBUTES);

        registerConnectorDefinitionParser(BosConnector.class);
        registerBeanDefinitionParser("barFilePath", new ChildListEntryDefinitionParser("barFilePaths"));
    }
}
