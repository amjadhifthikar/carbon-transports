/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.jms.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;

/**
 * This class tries to connect to JMS provider until the maximum re-try count meets.
 */
class JMSConnectionRetryHandler {
    private JMSServerConnector jmsServerConnector;
    private static final Log logger = LogFactory.getLog(JMSConnectionRetryHandler.class.getName());
    private long retryInterval;
    private int retryCount = 0;
    private int maxRetryCount;

    /**
     * Creates a jms connection retry handler.
     * @param jmsServerConnector JMS Server Connector
     * @param retryInterval Retry interval between
     * @param maxRetryCount Maximum retries
     */
    JMSConnectionRetryHandler(JMSServerConnector jmsServerConnector, long retryInterval, int maxRetryCount) {
        this.jmsServerConnector = jmsServerConnector;
        this.retryInterval = retryInterval;
        this.maxRetryCount = maxRetryCount;
    }

    /**
     * To start the retrying to connect to JMS provider.
     * @throws JMSConnectorException JMS Connector Exception
     */
    void start() throws JMSConnectorException {
        logger.error("Re-connection will be attempted after " + retryInterval + " milli-seconds.");
        try {
            Thread.sleep(retryInterval);
        } catch (InterruptedException e) {
            //Ignore the exception
        }
        while (retryCount < maxRetryCount) {
            try {
                retryCount++;
                jmsServerConnector.createMessageListener();
                logger.info("Connected to the message broker after retrying for " + retryCount + " time(s)");
                return;
            } catch (JMSConnectorException ex) {
                jmsServerConnector.closeAll();
                if (retryCount < maxRetryCount) {
                    logger.error("Retry connection attempt " + retryCount + " to JMS Provider failed. Retry will be "
                            + "attempted ");
                    retryInterval = retryInterval * 2;
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException e) {
                        // ignore the exception
                    }
                }
            }
        }
        throw new JMSConnectorException("Connection to the jms provider failed after retrying for " +
                retryCount + " times");
    }
}