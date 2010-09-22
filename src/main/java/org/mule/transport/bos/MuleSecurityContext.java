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

import org.ow2.bonita.facade.BonitaSecurityContext;

public class MuleSecurityContext implements BonitaSecurityContext
{

    public String getUser()
    {
        return "Mule Server";
    }

}


