/*
 * Copyright (c) Fiorano Software and affiliates. All rights reserved. http://www.fiorano.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.fiorano.edbc.framework.service.internal.engine;

import com.fiorano.edbc.framework.service.internal.IModule;


/**
 * Created by IntelliJ IDEA.
 * User: Venkat
 * Date: Nov 18, 2010
 * Time: 4:55:30 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IRequestProcessFactory {
    IRequestProcessor createRequestProcessor(IModule parent, String type);
}
