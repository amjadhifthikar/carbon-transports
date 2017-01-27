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
package org.wso2.carbon.transport.jms.sender;

import org.apache.commons.logging.Log;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.MessageProcessorException;
import org.wso2.carbon.messaging.TransportSender;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Map;

public class JMSSender implements TransportSender {

    @Override public boolean send(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws MessageProcessorException {

        try {

            // Extract Argument values.
            String connectionFactoryName =
                    (String) carbonMessage.getProperty(JMSConstants.ConnFactoryName);
            String destinationName =
                    (String) carbonMessage.getProperty(JMSConstants.destinationName);
            String destinationType =
                    (String) carbonMessage.getProperty(JMSConstants.destinationType);
            String messageType = (String) carbonMessage.getProperty(JMSConstants.msgType);

            Boolean isQueue = false;
            if (destinationType.equals("queue")) {
                isQueue = true;
            } else if (!destinationType.equals("topic")) {
                isQueue = false;
            } else {
                throw new Exception("Not a valid destination type.");
            }

            ConnectionFactory conFac;
            Connection connection = null;
            Destination destination;
            Queue myQueue;
            javax.naming.Context ctx;

            // Create the initial context.
            ctx = getInitialContext(
                    (String) carbonMessage.getProperty(JMSConstants.initialContextFactory),
                    (String) carbonMessage.getProperty(JMSConstants.jndiProviderUrl));
            // Lookup my connection factory from the admin object store.
            conFac = (javax.jms.ConnectionFactory) ctx.lookup(connectionFactoryName);

            //Create a connection.
            //todo support username and password
            String conUsername =
                    (String) carbonMessage.getProperty(JMSConstants.connectionUserName);
            String conPassword =
                    (String) carbonMessage.getProperty(JMSConstants.connectionPassword);

            if (conUsername != null && conPassword != null) {
                connection = conFac.createConnection(conUsername, conPassword);
            }
            if (connection == null) {
                connection = conFac.createConnection();
            }

            //Create a session within the connection.
            //todo let user set transacted and acknowledge mode
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Lookup my queue from the admin object store.
            destination = (Destination) ctx.lookup(destinationName);

            //Create a message producer.

            MessageProducer producer = session.createProducer(destination);

            Message message = null;

            //text message, map message
            if (messageType.equals("JMS_TEXT_MESSAGE")) {
                message = session.createTextMessage();
                TextMessage textMessage = (TextMessage) message;
                textMessage.setText(
                        JMSMessageUtils.getStringFromInputStream(carbonMessage.getInputStream()));
            } else if (messageType.equals("JMS_MAP_MESSAGE")) {
                message = session.createMapMessage();
                MapMessage mapMessage = (MapMessage) message;
                //todo set values to map message
            }

            //set transport headers

            Object transportHeaders = carbonMessage.getProperty(JMSConstants.transportHeaders);
            if (transportHeaders != null && transportHeaders instanceof Map) {
                JMSMessageUtils.setTransportHeaders(message, (Map<String, Object>) carbonMessage
                        .getProperty(JMSConstants.transportHeaders));
            }

            //Send a message to the destination(queue/topic).
            producer.send(message);

            //Close the session and connection resources.
            session.close();
            connection.close();

        } catch (Throwable t) {

        }

        return false;
    }

    protected javax.naming.Context getInitialContext(String initialContextFactory,
                                                     String jndiProviderUrl) {

        try {

            Hashtable env;

            env = new Hashtable();

            // Store the environment variable that tell JNDI which initial context
            // to use and where to find the provider.

            // For use with the File System JNDI Service Provider
            env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, initialContextFactory);
            env.put(javax.naming.Context.PROVIDER_URL, jndiProviderUrl);

            // Create the initial context.
            return new InitialContext(env);

        } catch (NamingException e) {

        }

        return null;
    }

    @Override public String getId() {
        return null;
    }

    //    private String getStringPayload(CarbonMessage message){
    //        String result;
    //        try {
    //            if (message.isAlreadyRead()) {
    //                result = message.bui;
    //            } else {
    //                String payload = MessageUtils.getStringFromInputStream(msg.value().getInputStream());
    //                result = new BString(payload);
    //                msg.setBuiltPayload(result);
    //                msg.setAlreadyRead(true);
    //            }
    //            if (log.isDebugEnabled()) {
    //                log.debug("Payload in String:" + result.stringValue());
    //            }
    //        } catch (Throwable e) {
    //            throw new BallerinaException("Error while retrieving string payload from message: " + e.getMessage());
    //        }
    //        return getBValues(result);
    //    }
}
