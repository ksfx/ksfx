/**
 *
 * Copyright (C) 2011-2017 KSFX. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ksfx.services.mail;

import ch.ksfx.services.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Date;
import java.util.Properties;

@Service
public class MailSender
{
    public static Logger logger = LoggerFactory.getLogger(MailSender.class);

    private SystemEnvironment systemEnvironment;

    public MailSender(SystemEnvironment systemEnvironment)
    {
        this.systemEnvironment = systemEnvironment;
    }

    public void sendMail(String subject, String message, String... toEmail)
    {
        try {
            String username = systemEnvironment.getMainConfiguration().getString("mail.username");
            String password = systemEnvironment.getMainConfiguration().getString("mail.password");
            String smtphost = systemEnvironment.getMainConfiguration().getString("mail.smtphost");

            ch.ksfx.services.mail.MailAuthenticator auth = new ch.ksfx.services.mail.MailAuthenticator(username, password);

            Properties properties = new Properties();

            properties.put("mail.smtp.host", smtphost);
            properties.put("mail.smtp.auth", "true");
            properties.put("useJMTA", "false");

            Session session = Session.getInstance(properties, auth);

            Message msg = new MimeMessage(session);

            //Set Recipients and Sender
            msg.setFrom(new InternetAddress("info@ksfx.de"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(
                toEmail[0], false));

            if (toEmail.length > 1) {
                for (Integer iI = 1; iI < toEmail.length; iI++) {
                    msg.addRecipient(Message.RecipientType.CC, new InternetAddress(toEmail[iI]));
                }
            }

            //Set Subject and Text
            msg.setSubject("[KSFX (" + systemEnvironment.getMainConfiguration().getString("instanceName") + ")] " + subject);
            msg.setSentDate(new Date());

            // create the message part
            MimeBodyPart messageBodyPart = new MimeBodyPart();

            //fill message
            messageBodyPart.setText(message);

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            // Put parts in message
            msg.setContent(multipart);

            Transport.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Could not send mail", e);
        }
    }
}
