/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package org.wso2.carbon.transport.jms.jndi.sender;

import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.ClientConnector;
import org.wso2.carbon.messaging.TextCarbonMessage;
import org.wso2.carbon.messaging.exceptions.ClientConnectorException;
import org.wso2.carbon.transport.jms.jndi.exception.JMSServerConnectorException;
import org.wso2.carbon.transport.jms.jndi.factory.JMSConnectionFactory;
import org.wso2.carbon.transport.jms.jndi.utils.JMSConstants;
import org.wso2.carbon.transport.jms.jndi.utils.SerializableCarbonMessage;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * JMS sender implementation.
 */

public class JMSClientConnector implements ClientConnector {

    private MessageProducer messageProducer;
    private Session session;
    private Connection connection;
    private JMSConnectionFactory jmsConnectionFactory;

    @Override public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws ClientConnectorException {
        return false;
    }

    @Override public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback,
                                  Map<String, String> propertyMap) throws ClientConnectorException {
        try {
            try {
                Set<Map.Entry<String, String>> propertySet = propertyMap.entrySet();
                this.createConnection(propertySet);
            } catch (JMSServerConnectorException e) {
                throw new ClientConnectorException(e.getMessage(), e);
            }

            Message message = null;
            String messageType = propertyMap.get(JMSConstants.JMS_MESSAGE_TYPE);

            if (carbonMessage instanceof TextCarbonMessage) {
                String textData = ((TextCarbonMessage) carbonMessage).getText();
                if (messageType.equals(JMSConstants.TEXT_MESSAGE_TYPE)) {
                    message = session.createTextMessage();
                    TextMessage textMessage = (TextMessage) message;
                    textMessage.setText(textData);
                } else if (messageType.equals(JMSConstants.BYTES_MESSAGE_TYPE)) {
                    message = session.createBytesMessage();
                    BytesMessage bytesMessage = (BytesMessage) message;
                    bytesMessage.writeBytes(textData.getBytes(Charset.defaultCharset()));
                }
            } else if (messageType.equals(JMSConstants.OBJECT_MESSAGE_TYPE) &&
                       carbonMessage instanceof SerializableCarbonMessage) {
                message = session.createObjectMessage((SerializableCarbonMessage) carbonMessage);
            }

            if (carbonMessage.getProperty(JMSConstants.PERSISTENCE) != null &&
                carbonMessage.getProperty(JMSConstants.PERSISTENCE).equals(false)) {
                messageProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            }

            if (message != null) {
                messageProducer.send(message);
            }

        } catch (JMSException e) {
            throw new ClientConnectorException("Exception occurred while sending the message", e);
        } finally {
            try {
                jmsConnectionFactory.closeMessageProducer(messageProducer);
                jmsConnectionFactory.closeSession(session);
                jmsConnectionFactory.closeConnection(connection);
            } catch (JMSServerConnectorException e) {
                throw new ClientConnectorException(e.getMessage(), e);
            }
        }
        return false;
    }

    private void createConnection(Set<Map.Entry<String, String>> propertySet)
            throws JMSServerConnectorException, JMSException {
        Properties properties = new Properties();
        for (Map.Entry<String, String> entry : propertySet) {
            String mappedParameter = JMSConstants.MAPPING_PARAMETERS.get(entry.getKey());
            if (mappedParameter != null) {
                properties.put(mappedParameter, entry.getValue());
            } else {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        JMSConnectionFactory jmsConnectionFactory = new JMSConnectionFactory(properties);
        this.jmsConnectionFactory = jmsConnectionFactory;

        String conUsername = properties.getProperty(JMSConstants.CONNECTION_USERNAME);
        String conPassword = properties.getProperty(JMSConstants.CONNECTION_PASSWORD);

        Connection connection;
        if (conUsername != null && conPassword != null) {
            connection = jmsConnectionFactory.createConnection(conUsername, conPassword);
        } else {
            connection = jmsConnectionFactory.createConnection();
        }

        this.connection = connection;

        Session session = jmsConnectionFactory.createSession(connection);
        this.session = session;

        Destination destination = jmsConnectionFactory.getDestination(session);
        MessageProducer messageProducer =
                jmsConnectionFactory.createMessageProducer(session, destination);
        this.messageProducer = messageProducer;
    }

    @Override public String getProtocol() {
        return "jms";
    }
}
