/*
 * Copyright (c) Fiorano Software and affiliates. All rights reserved. http://www.fiorano.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package com.fiorano.bc.bcgateway.ccp;

import com.fiorano.bc.bcgateway.ESBComponent;
import com.fiorano.esb.util.CommandLineParameters;
import com.fiorano.microservice.common.ccp.AbstractCCPEventHandler;
import com.fiorano.microservice.common.ccp.Bundle;
import com.fiorano.microservice.common.ccp.ICCPEventGenerator;
import com.fiorano.microservice.common.ccp.ServiceStateListener;
import com.fiorano.microservice.common.log.LoggerUtil;
import com.fiorano.openesb.microservice.ccp.event.ControlEvent;
import com.fiorano.openesb.microservice.ccp.event.common.DataEvent;
import com.fiorano.openesb.microservice.ccp.event.common.DataRequestEvent;
import com.fiorano.openesb.microservice.ccp.event.common.LogLevelRequestEvent;
import com.fiorano.openesb.microservice.ccp.event.common.data.Data;
import com.fiorano.openesb.microservice.ccp.event.common.data.LogLevel;
import com.fiorano.openesb.microservice.ccp.event.component.StatusEvent;
import com.fiorano.openesb.microservice.ccp.event.peer.CommandEvent;
import com.fiorano.services.common.util.RBUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The CCPEventHandler listens to the messages that are published on the CCP_PEER_TO_COMPONENT_TRANSPORT,
 * converts them into control events and responds to the events.
 * <p/>
 * Date: Feb 28, 2010
 * Time: 7:23:47 PM
 */
public class CCPEventHandler extends AbstractCCPEventHandler {

    private final ESBComponent service;
    private ServiceStateListener serviceStateListener;

    public CCPEventHandler(ESBComponent service, ICCPEventGenerator ccpEventGenerator, Logger logger) {
        super(ccpEventGenerator, logger);
        this.service = service;
        service.getServiceState().addPropertyChangeListener(serviceStateListener = new ServiceStateListener(ccpEventGenerator, logger));
    }

    protected void handleCommand(CommandEvent event) {
        switch (event.getCommand()) {
            case INITIATE_SHUTDOWN:
                try{
                synchronized (service) {
                    service.notifyAll();
                }
                }catch (Exception e){}
                logger.log(Level.INFO, RBUtil.getMessage(Bundle.class, Bundle.RECEIVED_SHUTDOWN_COMMAND,
                        new Object[]{event.getCorrelationID()}));
                service.shutdown("CCP");
                break;

            case REPORT_STATE:
                StatusEvent serviceStatus = serviceStateListener.getServiceStatus();
                serviceStatus.setCorrelationID(event.getEventId());
                if (ccpEventGenerator != null) {
                    ccpEventGenerator.sendEvent(serviceStatus);
                }
                //Reset Correlation ID
                serviceStatus.setCorrelationID(0);
                break;
            case SET_LOGLEVEL:
                for (Map.Entry<String, String> logLevelEntry : event.getArguments().entrySet()) {
                    CommandLineParameters commandLineParameters = service.getCommandLineParams();
                    Logger logger = LoggerUtil.getServiceLogger(logLevelEntry.getKey(), commandLineParameters);
                    LoggerUtil.setLevel(logger, Level.parse(logLevelEntry.getValue()));
                }
                break;
            case CLEAR_OUT_LOGS:
                service.clearOutLogs();
                break;
            case CLEAR_ERR_LOGS:
                service.clearErrLogs();
                break;
            default:
                super.handleCommand(event);
                break;
        }
    }

    @Override
    protected String getComponentID() {
        return service.getCommandLineParams().getConnectionFactory();
    }

    @Override
    protected void handleLogLevelEvent(DataRequestEvent event, DataEvent dataEvent) {

        LogLevel level = (LogLevel) Data.getDataObject(Data.DataType.LOG_LEVEL);
        HashMap<String, Level> logLevels = new HashMap<>();
        if (event instanceof LogLevelRequestEvent) {
            for (String loggerName : ((LogLevelRequestEvent) event).getLoggerNames()) {
                CommandLineParameters commandLineParameters = service.getCommandLineParams();
                Logger logger = LoggerUtil.getServiceLogger(loggerName, commandLineParameters);
                logLevels.put(loggerName, LoggerUtil.getLevel(logger));
            }
        } else {
            logLevels.put(service.getLogger().getName(), LoggerUtil.getLevel(service.getLogger()));
        }
        level.setLoggerLevels(logLevels);
        dataEvent.getData().put(DataRequestEvent.DataIdentifier.LOG_LEVELS, level);
    }

    /**
     * Handles the event sent from the peer server to component.
     *
     * @param event Event sent from the peer server.
     */
    public void handleEvent(ControlEvent event) {
        logger.log(Level.INFO, RBUtil.getMessage(Bundle.class, Bundle.RECEIVED_EVENT, new Object[]{event}));
        switch (event.getEventType()) {
            case DATA:
               service.updateConfiguration(((DataEvent) event).getData());
                synchronized (service) {
                    service.notifyAll();
                }
                break;
            default:
                super.handleEvent(event);
                break;
        }
    }
}
