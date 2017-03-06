package edu.gemini.tac.service;

import edu.gemini.tac.persistence.emails.Email;
import edu.gemini.tac.persistence.emails.Template;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fnussber
 * Date: 2/23/11
 * Time: 11:34 AM
 * To change this template use File | Settings | File Templates.
 */
public interface IEmailsService {

    public List<Template> getTemplates();

    public void updateTemplate(Template template, String subjectText, String templateText);

    public boolean queueHasEmails(Long queueId);

    public List<Email> getEmailsForQueue(Long queueId);

    public List<Email> createEmailsForQueue(Long queueId);

    public void sendEmail(Long emailId, boolean resend);

    public void updateEmail(Long emailId, String address, String content);

    public void deleteEmails(Long queueId);

    public byte[] exportEmails(Long queueId);

}
