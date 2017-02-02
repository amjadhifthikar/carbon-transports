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
package org.wso2.carbon.transport.jms.receiver;

import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.MessageProcessorException;
import org.wso2.carbon.transport.jms.factory.JMSConnectionFactory;
import org.wso2.carbon.transport.jms.utils.JMSConstants;
import org.wso2.carbon.transport.jms.utils.StorableMessage;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

/**
 * JMS receiver to get one message.
 */

public class JMSReceiver {

    public CarbonMessage receive(Map<String, Object> parameters)
            throws MessageProcessorException {

        try {
            Properties properties = new Properties();
            Set<Map.Entry<String, Object>> set = parameters.entrySet();
            for (Map.Entry<String, Object> entry : set) {
                String mappedParameter = JMSConstants.MAPPING_PARAMETERS.get(entry.getKey());
                if (mappedParameter != null) {
                    properties.put(mappedParameter, entry.getValue());
                }
            }
            JMSConnectionFactory jmsConnectionFactory = new JMSConnectionFactory(properties);

            String conUsername =
                    (String) parameters.get(JMSConstants.CONNECTION_USERNAME);
            String conPassword =
                    (String) parameters.get(JMSConstants.CONNECTION_PASSWORD);

            Connection connection = null;
            if (conUsername != null && conPassword != null) {
                connection = jmsConnectionFactory.createConnection(conUsername, conPassword);
            }
            if (connection == null) {
                connection = jmsConnectionFactory.getConnection();
            }

            Session session = jmsConnectionFactory.getSession(connection);
            Destination destination = jmsConnectionFactory.getDestination(session);




            MessageConsumer messageConsumer =
                    jmsConnectionFactory.createMessageConsumer(session, destination);

            Message message = null;


            //Send a message to the destination(queue/topic).
            message = messageConsumer.receive();

            //Retreive the contents of the message.
            if (!(message instanceof ObjectMessage)) {
                return null;
            }
            ObjectMessage objMsg = (ObjectMessage) message;
            if (!(objMsg.getObject() instanceof StorableMessage)) {
                return null;
            }
            StorableMessage storableMessage = (StorableMessage) objMsg.getObject();

            //Close the session and connection resources.
            session.close();
            connection.close();

            return storableMessage;

        } catch (JMSException e) {
            throw new RuntimeException("Exception occurred while receiving the message.");
        }
    }

}
