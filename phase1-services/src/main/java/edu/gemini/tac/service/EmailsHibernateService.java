package edu.gemini.tac.service;

import edu.gemini.spModel.gemini.obscomp.ProgIdHash;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.queues.ScienceBand;
import edu.gemini.tac.persistence.emails.Email;
import edu.gemini.tac.persistence.emails.Template;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.submission.NgoSubmission;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionAccept;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.JointBanding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.ScienceBand;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/**
* Created by IntelliJ IDEA.
* User: fnussber
* Date: 2/23/11
* Time: 11:34 AM
* To change this template use File | Settings | File Templates.
*/
@Service(value="emailsService")
public class EmailsHibernateService implements IEmailsService {

    private static final Logger LOGGER = Logger.getLogger(EmailsHibernateService.class.getName());

    private String mailserverFromAddress;
    private String[] mailserverTestToAddress;
    private boolean mailserverSendForReal;

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Resource(name = "mailSender")
    private JavaMailSender mailSender;

    @Resource(name = "velocityEngine")
    private VelocityEngine velocityEngine;

    @Resource(name="committeeService")
    private ICommitteeService committeeService;

    @Resource(name="queueService")
    private IQueueService queueService;


    public EmailsHibernateService() {
    }

    @Override
    @Transactional(readOnly = true)
    public List<Template> getTemplates() {
        return sessionFactory.getCurrentSession().
                createQuery("from Template t order by t.id").
                list();
    }

    @Override
    @Transactional
    public void updateTemplate(Template template, String subjectText, String templateText) {
        Session session = sessionFactory.getCurrentSession();
        template.setSubject(subjectText);
        template.setTemplate(templateText);
        session.update(template);
    }

    @Override
    @Transactional
    public List<Email> createEmailsForQueue(Long queueId) {
        Map<Class, Template>templatesMap = getTemplatesMap();
        Set<Long> successfulIds = new HashSet<Long>();
        Set<Long> unsuccessfulIds = new HashSet<Long>();
        List<Email> createdEmails = new ArrayList<Email>();

        // Safeguards: there must be a finalized queue and there must be no emails yet
        // these conditions have to be verified in the gui / controller; here we just throw an exception
        Queue queue = queueService.getQueueWithoutData(queueId);
        Validate.notNull(queue);
        Validate.isTrue(queue.getFinalized());
        List<Email> emails = getEmailsForQueue(queueId);
        Validate.isTrue(emails.size() == 0);

        // create emails for all classical proposals (they have no banding and are returned by getClassicalProposals)
        Set<Proposal> classicalProposals = queue.getClassicalProposals();
        for (Proposal proposal : classicalProposals) {
            if (!proposal.isJointComponent()) {
                // send emails based on information in joint or simple proposals; skip component proposals
                Validate.isTrue(proposal.isClassical());
                createdEmails.addAll(createSuccessfulEmailsForClassical(queue, proposal, templatesMap));
                successfulIds.add(proposal.getId()); // collect ids of all proposals that made it into the queue
            }
        }
        //Validate.isTrue(createdEmails.size() > classicalProposals.size() * 2); GT because classical JPs will generate more emails

        // create emails for all queue proposals (they are linked to the queue through banding objects)
        Set<Banding> bandings = queue.getBandings();
        for (Banding banding : bandings) {
            Proposal proposal = banding.getProposal();
            // send emails based on information in joint or simple proposals; skip component proposals
            if (!proposal.isJointComponent()) {
                if(proposal.isClassical()){
                    LOGGER.error("Proposal " + proposal.getId() + " is classical but not a joint component");
                }
                Validate.isTrue(!proposal.isClassical());
                createdEmails.addAll(createSuccessfulEmailsForBanded(queue, banding, templatesMap));
                successfulIds.add(proposal.getId()); // collect ids of all proposals that made it into the queue
            }
        }

        // create emails for unsuccessful proposals of this committee that belong to the same site as the queue
        Set<Proposal> committeeProposals = queue.getCommittee().getProposals(); //committeeService.getAllProposalsForCommittee(queue.getCommittee().getId());
        for (Proposal proposal : committeeProposals) {
            if (proposal.belongsToSite(queue.getSite()) && !proposal.isJointComponent() && !successfulIds.contains(proposal.getId())) {
                // send emails based on information in joint or simple proposals; skip component proposals
                createdEmails.addAll(createUnsuccessfulEmails(queue, proposal, templatesMap));
                unsuccessfulIds.add(proposal.getId());
            }
        }
        return createdEmails;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean queueHasEmails(Long queueId) {
        Query query = sessionFactory.getCurrentSession().createQuery("select count(e) from Email e where e.queue.id = :queueId").setLong("queueId", queueId);
        Long count = (Long) query.uniqueResult();
        if (count > 0) {
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Email> getEmailsForQueue(Long queueId) {
        Queue queue = queueService.getQueueWithoutData(queueId);
        if (queue != null) {
            committeeService.getAllProposalsForCommittee(queue.getCommittee().getId());
            return sessionFactory.getCurrentSession().
                getNamedQuery("email.getEmailsForQueue").
                setLong("queueId", queue.getId()).
                list();
        } else {
            return new LinkedList<Email>();
        }
    }

    @Override
    @Transactional
    public void updateEmail(Long emailId, String address, String content) {
        Session session = sessionFactory.getCurrentSession();
        Email email = getEmailById(emailId);
        email.setAddress(address);
        email.setContent(content);
        session.update(email);
    }

    @Override
    @Transactional
    public void sendEmail(Long emailId, boolean resend) {
        Email email = getEmailById(emailId);
        Validate.notNull(email);
        sendEmail(email, resend);
    }


    @Override
    @Transactional
    public void deleteEmails(Long queueId) {
        sessionFactory.getCurrentSession().
                createQuery("delete from Email e where e.queue.id = :queueId").setLong("queueId", queueId).
                executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportEmails(Long queueId) {
        final List<Email> emails = getEmailsForQueue(queueId);
        final ByteArrayOutputStream byteWriter = new ByteArrayOutputStream(500*1024);

        try {
            final OutputStreamWriter writer = new OutputStreamWriter(byteWriter);
            int i = 0;
            for (Email email : emails) {
                if (i > 0) {
                    writer.write("========================================================================================" + "\n");
                }
                writer.write("TO           : " + email.getAddress() + "\n");
                writer.write("SUBJECT      : " + email.getSubject() + "\n");
                if (StringUtils.isNotBlank(email.getCc()))
                    writer.write("CC        : " + email.getCc() + "\n");
                if (email.getSentTimestamp() != null) {
                    writer.write("SEND STATE   : SENT " + email.getSentTimestamp() + "\n");
                } else if (email.getErrorTimestamp() != null) {
                    writer.write("SEND STATE   : FAILED " + email.getErrorTimestamp() + "\n");
                    writer.write("SEND ERROR   : " + email.getError() + "\n");
                } else {
                    writer.write("SEND STATE   : UNSENT\n");
                }
                writer.write("----------------------------------------------------------------------------------------" + "\n");
                writer.write("CONTENT:\n");
                writer.write("----------------------------------------------------------------------------------------" + "\n\n");
                writer.write(email.getContent());
                writer.write("\n\n");
                i++;
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("could not export emails", e);
        }

        return byteWriter.toByteArray();
    }

    /*
    Setter methods for Spring configuration
     */
    public void setMailserverFromAddress(String mailserverFromAddress) {
        this.mailserverFromAddress = mailserverFromAddress;
    }
    public void setMailserverTestToAddress(String mailserverTestToAddress) {
        this.mailserverTestToAddress = mailserverTestToAddress.split("; ");
    }
    public void setMailserverSendForReal(boolean mailserverSendForReal) {
        this.mailserverSendForReal = mailserverSendForReal;
    }

    private Email getEmailById(Long emailId) {
        return (Email) sessionFactory.getCurrentSession().
            getNamedQuery("email.findEmailById").
            setLong("emailId", emailId).
            uniqueResult();
    }

    private void sendEmail(Email email, boolean resend) {
        try {
            if (email.getSentTimestamp() == null || resend) {
                doSendEmail(email);
                email.setSentTimestamp(new Date());
                email.setErrorTimestamp(null);
                email.setError(null);
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            email.setSentTimestamp(null);
            email.setErrorTimestamp(new Date());
            email.setError(ex.getMessage());
        }
        sessionFactory.getCurrentSession().update(email);
        sessionFactory.getCurrentSession().flush();
    }

    private void doSendEmail(Email email) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(mailserverFromAddress);
        if (mailserverSendForReal && mailserverTestToAddress[0].isEmpty()) {
            // only try to send to addresses from database when "sendForReal" flag is set
            // and there is no test institutionAddress configured, we don't want to send emails to the outside
            // world when testing!!!
            String to = email.getAddress().replaceAll(" ", "").replaceAll(",",";"); // replace blanks and commas
            message.setTo(to.split(";"));
            if (StringUtils.isNotBlank(email.getCc())) {
                String cc = email.getCc().replaceAll(" ", "").replaceAll(",",";"); // replace blanks and commas
                message.setCc(cc.split(";"));
            }
        } else {
            // we are in "test mode", don't use addresses from database but a fake one
            message.setTo(mailserverTestToAddress);
        }

        message.setSubject(email.getSubject());
        message.setText(email.getContent());
        mailSender.send(message);
    }

    private List<Email> createSuccessfulEmailsForClassical(Queue queue, Proposal proposal, Map<Class, Template> templatesMap) {
        Validate.isTrue(proposal.isClassical());
        List<Email> createdEmails = new ArrayList<Email>();

        // for successful proposals the pi and all ngos are notified
        createdEmails.add(createPiEmail(queue, null, proposal, templatesMap.get(Template.PiSuccessful.class), true));

        if (proposal instanceof JointProposal) {
            for (Proposal component : proposal.getProposals()) {
               createdEmails.add(createNgoEmail(queue, null, proposal, component.getPartnerTacExtension(), templatesMap.get(Template.NgoClassicalJoint.class), true, component.getPartner().getNgoFeedbackEmail()));
            }
        } else {
            createdEmails.add(createNgoEmail(queue, null, proposal, proposal.getPartnerTacExtension(), templatesMap.get(Template.NgoClassical.class), true, proposal.getPartner().getNgoFeedbackEmail()));
        }
        return createdEmails;
    }

    private List<Email> createSuccessfulEmailsForBanded(Queue queue, Banding banding, Map<Class, Template> templatesMap) {
        Validate.isTrue(!banding.getProposal().isClassical());
        List<Email> createdEmails = new ArrayList<Email>();

        Proposal proposal = banding.getProposal();

        // for successful proposals the pi is notified
        createdEmails.add(createPiEmail(queue, banding, proposal, templatesMap.get(Template.PiSuccessful.class), true));

        // create ngo emails for joints
        if (banding instanceof JointBanding) {

            JointBanding jointBanding = (JointBanding) banding;
            // successful emails for accepted parts
            for (Proposal component : jointBanding.getAcceptedProposals()) {
                final String ngoFeedbackEmail = component.getPartner().getNgoFeedbackEmail();

                if (proposal.isExchange()) {
                        createdEmails.add(createNgoEmail(queue, banding, proposal, component.getPartnerTacExtension(), templatesMap.get(Template.NgoExchangeJoint.class), true, ngoFeedbackEmail));
                } else if (proposal.isPw() && banding.getBand().getRank() == ScienceBand.POOR_WEATHER.getRank()) {
                        createdEmails.add(createNgoEmail(queue, banding, proposal, component.getPartnerTacExtension(), templatesMap.get(Template.NgoPoorWeatherJoint.class), true, ngoFeedbackEmail));
                } else {
                        createdEmails.add(createNgoEmail(queue, banding, proposal, component.getPartnerTacExtension(), templatesMap.get(Template.NgoQueueJoint.class), true, ngoFeedbackEmail));
                }
            }
            // unsuccessful emails for rejected parts
            for (Proposal component : jointBanding.getRejectedProposals()) {
                createdEmails.add(createNgoEmail(queue, banding, proposal, component.getPartnerTacExtension(), templatesMap.get(Template.UnSuccessful.class), false, component.getPartner().getNgoFeedbackEmail()));
            }

        // create ngo emails for simple proposals
        } else {
            final String ngoFeedbackEmail = proposal.getPartner().getNgoFeedbackEmail();

            if (proposal.isExchange()) {
                createdEmails.add(createNgoEmail(queue, banding, proposal, proposal.getPartnerTacExtension(), templatesMap.get(Template.NgoExchange.class), true, ngoFeedbackEmail));
            } else if (proposal.isPw() && banding.getBand().getRank() == ScienceBand.POOR_WEATHER.getRank()) {
                createdEmails.add(createNgoEmail(queue, banding, proposal, proposal.getPartnerTacExtension(), templatesMap.get(Template.NgoPoorWeather.class), true, ngoFeedbackEmail));
            } else {
                createdEmails.add(createNgoEmail(queue, banding, proposal, proposal.getPartnerTacExtension(), templatesMap.get(Template.NgoQueue.class), true, ngoFeedbackEmail));
            }
        }
        return createdEmails;
    }

    private List<Email> createUnsuccessfulEmails(Queue queue, Proposal proposal, Map<Class, Template> templatesMap) {
        List<Email> createdEmails = new ArrayList<Email>();
        // for unsuccessful proposals only the ngos are notified
        for (Proposal component : proposal.getProposals()) {
            createdEmails.add(createNgoEmail(queue, null, proposal, component.getPartnerTacExtension(), templatesMap.get(Template.UnSuccessful.class), false, component.getPartner().getNgoFeedbackEmail()));
        }
        return createdEmails;
    }

    private Email createPiEmail(Queue queue, Banding banding, Proposal proposal, Template template, boolean successful) {
        // merging of proposals will merge PI emails to semi-colon separated list of emails
        final PhaseIProposal phaseIProposal = proposal.getPhaseIProposal();
        String recipients = phaseIProposal.getInvestigators().getPi().getEmail();
        Submission submission = phaseIProposal.getPrimary();
        VariableValues values = new VariableValues(proposal, banding, submission, successful);

        final List<String> ccRecipients = extractCcRecipientsFromProposal(proposal, phaseIProposal);

        return createEmail(queue, banding, proposal, template, values, recipients, StringUtils.join(ccRecipients,"; "));
    }

    private List<String> extractCcRecipientsFromProposal(Proposal proposal, PhaseIProposal phaseIProposal) {
        final List<Submission> submissions = phaseIProposal.getSubmissions();
        final List<String> ccRecipients = new ArrayList<String>();
        for (Submission s : submissions) {
            final SubmissionAccept accept = s.getAccept();
            if (accept != null) {
                final String email = accept.getEmail();
                if (StringUtils.isNotBlank(email)) {
                    final String[] emails = StringUtils.split(email, ",");
                    ccRecipients.add(StringUtils.join(emails,"; "));
                }
            }
        }
        final Itac itac = proposal.getItac();
        if (itac != null) {
            final ItacAccept itacAccept = itac.getAccept();
            if (itacAccept != null) {
                final String contact = itacAccept.getContact();
                if (StringUtils.isNotBlank(contact) && contact.contains("@"))
                    ccRecipients.add(contact);
            }
        }

        ccRecipients.remove("No partner lead email assigned.");

        return ccRecipients;
    }

    private Email createNgoEmail(Queue queue, Banding banding, Proposal proposal, Submission submission, Template template, boolean successful, String toEmailAddress) {
        VariableValues values = new VariableValues(proposal, banding, submission, successful);
        return createEmail(queue, banding, proposal, template, values, toEmailAddress);
    }

    private Email createEmail(Queue queue, Banding banding, Proposal proposal, Template template, VariableValues values, String address, final String ccRecipients) {
        Email email = new Email();
        email.setQueue(queue);
        email.setBanding(banding);
        email.setProposal(proposal);
        email.setAddress(address);
        email.setSubject(template.getSubject());
        email.setContent(fillTemplate(template, values));
        email.setDescription(template.getDescription());
        if (ccRecipients != null)
            email.setCc(ccRecipients);
        sessionFactory.getCurrentSession().save(email);
        return email;
    }

    private Email createEmail(Queue queue, Banding banding, Proposal proposal, Template template, VariableValues values, String address) {
        return createEmail(queue, banding, proposal, template, values, address, null);
    }

    private Map<Class, Template> getTemplatesMap() {
        List<Template> templates = sessionFactory.getCurrentSession().createQuery("from Template t").list();
        Map<Class, Template> templatesMap = new HashMap<Class, Template>();
        for (Template template : templates) {
            templatesMap.put(template.getClass(), template);
        }
        return templatesMap;
    }

    private String fillTemplate(Template template, VariableValues values) {
        Writer writer = new StringWriter(5000);
        Reader reader = new StringReader(template.getVelocityTemplate());

        VelocityContext context = new VelocityContext();
        context.put("values", values);

        try {
            velocityEngine.evaluate(context, writer, "tag", reader);
        } catch (IOException ex) {
            LOGGER.error("Could not fill template: " + ex.getMessage(), ex);
        }

        return writer.toString();
    }

    // simple java bean used to pass variable values to velocity engine
    public class VariableValues {
        public static final String N_A = "N/A";

        private String country = N_A;
        private String geminiComment = N_A;
        private String geminiContactEmail = N_A;
        private String geminiId = N_A;
        private String itacComments = N_A;
        private String jointInfo = N_A;
        private String jointTimeContribs = N_A;
        private String ntacComment = N_A;
        private String ntacRanking = N_A;
        private String ntacRecommendedTime = N_A;
        private String ntacRefNumber = N_A;
        private String ntacSupportEmail = N_A;
        private String piMail = N_A;
        private String piName = N_A;
        private String progId = N_A;
        private String progTitle = N_A;
        private String progKey = N_A;
        private String queueBand = N_A;
        private String progTime = N_A;
        private String partnerTime = N_A;
        private String timeAwarded = N_A;

        public VariableValues(Proposal proposal, Banding banding, Submission ntacExtension, boolean successful) {
            // proposals that made it that far must have an itac part, accepted ones must have an accept part
            Validate.notNull(proposal.getPhaseIProposal());
            Validate.notNull(proposal.getItac());

            // get some of the important objects
            PhaseIProposal doc = proposal.getPhaseIProposal();
            Itac itac = proposal.getItac();
            Investigator pi = doc.getInvestigators().getPi();
            Submission partnerSubmission = doc.getPrimary();

            this.geminiComment = itac.getGeminiComment() != null ? itac.getGeminiComment() : "";
            this.itacComments =  itac.getComment() != null ? itac.getComment() : "";
            if (itac.getRejected() || itac.getAccept() == null) {
                // either rejected or no accept part yet: set empty values
                this.progId = "";
                this.progKey = "";
                this.geminiContactEmail = "";
                this.timeAwarded = "";
            } else {
                this.progId = itac.getAccept().getProgramId();
                this.progKey = ProgIdHash.pass(this.progId);
                this.geminiContactEmail = itac.getAccept().getContact();
                this.timeAwarded = itac.getAccept().getAward().toPrettyString();
            }

            if (!successful) {
                // ITAC-70: use original partner time, the partner time might have been edited by ITAC to "optimize" queue
                this.timeAwarded = "0.0 " + ntacExtension.getRequest().getTime().getUnits();
            }

            if (proposal.isJoint()) {
                StringBuffer info = new StringBuffer();
                StringBuffer time = new StringBuffer();
                for(Submission submission : doc.getSubmissions()){
                    NgoSubmission ngoSubmission = (NgoSubmission) submission;
                    info.append(ngoSubmission.getPartner().getName());
                    info.append(" ");
                    info.append(ngoSubmission.getReceipt().getReceiptId());
                    info.append(" ");
                    info.append(ngoSubmission.getPartner().getNgoFeedbackEmail()); //TODO: Confirm -- not sure
                    info.append("\n");
                    time.append(ngoSubmission.getPartner().getName() + ": " + ngoSubmission.getAccept().getRecommend().toPrettyString());
                    time.append("\n");
                }
                this.jointInfo = info.toString();
                this.jointTimeContribs = time.toString();
            }

            // ITAC-70 & ITAC-583: use original recommended time, the awarded time might have been edited by ITAC to optimize queue
            TimeAmount time = ntacExtension.getAccept().getRecommend();
            this.country = ntacExtension.getPartner().getName();
            this.ntacComment = ntacExtension.getComment() != null ? ntacExtension.getComment() : "";
            this.ntacRanking = ntacExtension.getAccept().getRanking().toString();
            this.ntacRecommendedTime = time.toPrettyString();
            this.ntacRefNumber = ntacExtension.getReceipt().getReceiptId();
            this.ntacSupportEmail = ntacExtension.getAccept().getEmail();

            // We'll match the total time to the time awarded and scale
            // the program and partner time to fit

            // Find the correct set of observations. Note that they return the active observations already
            List<Observation> bandObservations = null;
            if (banding != null && banding.getBand().equals(ScienceBand.BAND_THREE)) {
              bandObservations = proposal.getPhaseIProposal().getBand3Observations();
            } else {
              bandObservations = proposal.getPhaseIProposal().getBand1Band2ActiveObservations();
            }
            if (successful) {
                TimeAmount progTime = new TimeAmount(0, TimeUnit.HR);
                TimeAmount partTime = new TimeAmount(0, TimeUnit.HR);
                for (Observation o : bandObservations) {
                    progTime = progTime.sum(o.getProgTime());
                    partTime = partTime.sum(o.getPartTime());
                }
                // Total time for program and partner
                TimeAmount sumTime = progTime.sum(partTime);
                // Scale factor with respect to the awarded time
                double ratio = progTime.getValueInHours().doubleValue() / sumTime.getValueInHours().doubleValue();
                // Scale the prog and program time
                this.progTime = TimeAmount.fromMillis(itac.getAccept().getAward().getDoubleValueInMillis() * ratio).toPrettyString();
                this.partnerTime = TimeAmount.fromMillis(itac.getAccept().getAward().getDoubleValueInMillis() * (1.0 - ratio)).toPrettyString();
            } else {
                this.partnerTime = "0.0 " + ntacExtension.getRequest().getTime().getUnits();
                this.progTime = "0.0 " + ntacExtension.getRequest().getTime().getUnits();
            }

            // Merging of PIs: first names and last names will be concatenated separated by '/',
            // emails will be concatenated to a list separated by semi-colons
            this.piMail = pi.getEmail();
            this.piName = pi.getFirstName() + " " + pi.getLastName();

            if (doc.getTitle() != null) {
                this.progTitle = doc.getTitle();
            }

            if (banding != null) {
                this.queueBand = banding.getBand().getDescription();
            } else if (proposal.isClassical()) {
                this.queueBand = "classical";
            } else {
                this.queueBand = N_A;
            }
        }

        public String getCountry() {
            return country;
        }

        public String getGeminiComment() {
            return geminiComment;
        }

        public String getGeminiContactEmail() {
            return geminiContactEmail;
        }

        public String getGeminiId() {
            return geminiId;
        }

        public String getItacComments() {
            return itacComments;
        }

        public String getJointInfo() {
            return jointInfo;
        }

        public String getJointTimeContribs() {
            return jointTimeContribs;
        }

        public String getNtacComment() {
            return ntacComment;
        }

        public String getNtacRanking() {
            return ntacRanking;
        }

        public String getNtacRecommendedTime() {
            return ntacRecommendedTime;
        }

        public String getNtacRefNumber() {
            return ntacRefNumber;
        }

        public String getNtacSupportEmail() {
            return ntacSupportEmail;
        }

        public String getPiMail() {
            return piMail;
        }

        public String getPiName() {
            return piName;
        }

        public String getProgId() {
            return progId;
        }

        public String getProgTitle() {
            return progTitle;
        }

        public String getProgKey() {
            return progKey;
        }

        public String getQueueBand() {
            return queueBand;
        }

        public String getTimeAwarded() {
            return timeAwarded;
        }

        public String getProgramTime() {
            return progTime;
        }

        public String getPartnerTime() {
            return partnerTime;
        }
    }
}
