package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.emails.Email;
import edu.gemini.tac.persistence.emails.Template;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.queues.Queue;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class EmailsServiceTest extends FastHibernateFixture.WithQueues {
	@Resource(name = "emailsService")
	private IEmailsService emailsService;

    @Resource(name = "committeeService")
    private ICommitteeService committeeService;

    @Resource(name = "queueService")
    private IQueueService queueService;

    @Resource(name = "mailSender")
    private JavaMailSender mailSender;


    @Test(expected=java.lang.IllegalArgumentException.class)
    public void doesNotCreateEmailsOnUnFinalized() {
        // calling this on an unfinalized queue will throw an exception!
        emailsService.createEmailsForQueue(getQueue(0).getId());
    }

	@Test
	public void createsEmailsOnFinalized() {
        // load some data
        Long queueId = getQueue(0).getId();
        Queue queue = queueService.getQueue(queueId);
        List<Proposal> committeeProposals = committeeService.getAllProposalsForCommittee(queue.getCommittee().getId());
        int proposalsCnt = 0;
        for (Proposal p : committeeProposals) {
            if (!p.isJointComponent() && p.belongsToSite(queue.getSite())) {
                proposalsCnt++;
            }
        }

        // finalize queue and create emails
        queueService.finalizeQueue(queueId);
        emailsService.createEmailsForQueue(queueId);

        // read emails and check number of emails created
        List<Email> createdEmails = emailsService.getEmailsForQueue(queueId);
        // for all successful proposals (i.e. banded or classical in queue) two emails should be created (PI + NTAC)
        // for all other proposals only one email is sent to the NTAC; therefore we expect
        // [(banded + classical) * 2 + unsuccessful] emails
        int successfulClassical = queue.getClassicalProposals().size();
        int successfulBanded = queue.getBandings().size();
        int unsuccessfulCnt = proposalsCnt - successfulClassical - successfulBanded;
        int expectedEmailsCnt = unsuccessfulCnt + (successfulClassical + successfulBanded) * 2;
        Assert.assertEquals(expectedEmailsCnt, createdEmails.size());

        // check that all placeholders in emails have been replaced
        String[][] translationTable = Template.getPlaceholderTranslationTable();
        for (Email email : createdEmails) {
            for (String[] translation : translationTable) {
                // make sure Gemini placeholders have been replaced
                Assert.assertTrue(!email.getContent().contains(translation[0]));
                // make sure Velocity placeholders have been replaced
                if (email.getContent().contains(translation[1])) {
                    System.out.println("non-replaced: " + translation[1]);
                }
                Assert.assertTrue(!email.getContent().contains(translation[1]));
            }
        }
    }

    @Test
    public void canDeleteEmails() {
        Long queueId = getQueue(0).getId();

        queueService.finalizeQueue(queueId);

        Assert.assertTrue(emailsService.getEmailsForQueue(queueId).size() == 0);

        emailsService.createEmailsForQueue(queueId);
        Assert.assertTrue(emailsService.getEmailsForQueue(queueId).size() > 0);

        emailsService.deleteEmails(queueId);
        Assert.assertTrue(emailsService.getEmailsForQueue(queueId).size() == 0);
    }

    // -- special test case to test the return-path behavior for emails with invalid email adresses
    // -- not meant to be run during normal tests...
    @Test
    @Ignore
    public void doesProduceBounces() throws MessagingException {
        MimeMessage m = mailSender.createMimeMessage();
        MimeMailMessage message = new MimeMailMessage(m);
        message.getMimeMessage().addHeader("Return-Path", "announcements@gemini.edu");
        message.setFrom("announcements@gemini.edu");
        message.setTo("test-bouncing-emails-for-broken-adress@gmail.com; florian.nussberger@gmail.com".split("; "));
        message.setSubject("This is a test");
        message.setText("This is a test");
        mailSender.send(message.getMimeMessage());
    }
}
