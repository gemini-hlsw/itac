package edu.gemini.tac.persistence.emails;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.validation.Errors;

import javax.persistence.*;

@Entity
@Table(name = "email_templates")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class Template {

    // MAGIC translation table from ancient Gemini speak to hardcore Velocity mumble talk.
    // This is the key to all the secrets on this planet and gives infinite wisdom to it's owner.
    // Handle with care.
    static private String[][] translationTable;
    static {
        translationTable = new String[][] {
                { "@COUNTRY@",              "$values.country"},
                { "@GEMINI_COMMENT@",       "$values.geminiComment"},
                { "@GEMINI_CONTACT_EMAIL@", "$values.geminiContactEmail"},
                { "@GEMINI_ID@",            "$values.geminiId"}, //TODO: remove: appears to be redundant.  See UX-1574
                { "@ITAC_COMMENTS@",        "$values.itacComments"},
                { "@JOINT_INFO@",           "$values.jointInfo"},
                { "@JOINT_TIME_CONTRIBS@",  "$values.jointTimeContribs"},
                { "@NTAC_COMMENT@",         "$values.ntacComment"},
                { "@NTAC_RANKING@",         "$values.ntacRanking"},
                { "@NTAC_RECOMMENDED_TIME@","$values.ntacRecommendedTime"},
                { "@NTAC_REF_NUMBER@",      "$values.ntacRefNumber"},
                { "@NTAC_SUPPORT_EMAIL@",   "$values.ntacSupportEmail"},
                { "@PI_MAIL@",              "$values.piMail"},
                { "@PI_NAME@",              "$values.piName"},
                { "@PROG_ID@",              "$values.progId"},
                { "@PROG_TITLE@",           "$values.progTitle"},
                { "@PROG_KEY@",             "$values.progKey"},
                { "@QUEUE_BAND@",           "$values.queueBand"},
                { "@PROG_TIME@",            "$values.programTime"},
                { "@PARTNER_TIME@",         "$values.partnerTime"},
                { "@TIME_AWARDED@",         "$values.timeAwarded"}
        };
    }

    /**
     * This is only public for testing purposed, do not use this directly in the code.
     * @return
     */
    public static String[][] getPlaceholderTranslationTable() {
        return translationTable;
    }

    @Entity(name="PiSuccessful")
    @DiscriminatorValue("PiSuccessful")
    public static class PiSuccessful extends Template {}

    @Entity
    @DiscriminatorValue("UnSuccessful")
    public static class UnSuccessful extends Template {}

    @Entity
    @DiscriminatorValue("NgoClassical")
    public static class NgoClassical extends Template {}

    @Entity
    @DiscriminatorValue("NgoClassicalJoint")
    public static class NgoClassicalJoint extends Template {}

    @Entity
    @DiscriminatorValue("NgoExchange")
    public static class NgoExchange extends Template {}

    @Entity
    @DiscriminatorValue("NgoExchangeJoint")
    public static class NgoExchangeJoint extends Template {}

    @Entity
    @DiscriminatorValue("NgoPoorWeather")
    public static class NgoPoorWeather extends Template {}

    @Entity
    @DiscriminatorValue("NgoPoorWeatherJoint")
    public static class NgoPoorWeatherJoint extends Template {}

    @Entity
    @DiscriminatorValue("NgoQueue")
    public static class NgoQueue extends Template {}

    @Entity
    @DiscriminatorValue("NgoQueueJoint")
    public static class NgoQueueJoint extends Template {}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

    @Column(name="description")
    private String description;

    @Column(name="subject")
    private String subject;

    @Column(name="template")
    private String geminiTemplate = null;

    @Transient
    private String velocityTemplate = null;

	@SuppressWarnings("unused")
	protected Template() { }

    public Long getId() {
        return id;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    public void setTemplate(String template) {
        this.geminiTemplate = template;
    }

    public String getTemplate() {
        return geminiTemplate;
    }

    public String getVelocityTemplate() {
        if (velocityTemplate == null) {
            velocityTemplate = translateGeminiSpeakToVelocityTalk(geminiTemplate);
        }
        return velocityTemplate;
    }

    public String getDescription() {
        return description;
    }

	public String toString() {
		return new ToStringBuilder(this).
			append("id", id).
            append("description", description).
			toString();
	}

    /**
     * Placeholders in the templates use the notation @<variableName>@, we replace all of these
     * placeholders with velocity style ones $<variableName> before filling the template using
     * the velocity engine.
     * @param template
     * @return
     */
    private String translateGeminiSpeakToVelocityTalk(String template) {
        // is there a more efficient way to do this?
        for (String[] variableMapping : translationTable) {
            template = template.replace(variableMapping[0], variableMapping[1]);
        }
        return template;
    }

    /**
     * Validator for email templates.
     */
    public static class Validator implements org.springframework.validation.Validator {

        @Override
        public boolean supports(Class<?> aClass) {
            return Template.class.equals(aClass);
        }

        @Override
        public void validate(Object o, Errors errors) {
            Template template = (Template) o;

            // TODO: validate that template variables are valid, hook everything up to the GUI
            //ValidationUtils.rejectIfEmptyOrWhitespace(errors, "model", "field.required", "Required field");
        }
    }

}
