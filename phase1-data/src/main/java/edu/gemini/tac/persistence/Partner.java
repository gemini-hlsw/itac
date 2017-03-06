package edu.gemini.tac.persistence;

import edu.gemini.model.p1.mutable.ExchangePartner;
import edu.gemini.model.p1.mutable.NgoPartner;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import javax.persistence.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a real-life entity that has a stake and role in the time allocation
 * committee.  Typically, a partner will have sorted through a larger set of
 * proposals, decided which wants they want to promote to the telescope, and ranked
 * those proposals (all of this before they enter into the ITAC process).
 * <p/>
 * Currently, partners are countries and Gemini Staff.
 *
 * @author ddawson
 */
@Entity
@Table(name = "partners")
@NamedQueries({
        @NamedQuery(name = "partner.findAllPartners", query = "from Partner p"),
        @NamedQuery(name = "partner.findPartnerById", query = "from Partner p where p.id = :id"),
        @NamedQuery(name = "partner.findPartnersByListOfIds", query = "from Partner p where p.id in (:ids)"),
        @NamedQuery(name = "partner.findAllPartnerCountries", query = "from Partner p where p.isCountry = true"),
        @NamedQuery(name = "partner.findByKey", query = "from Partner p where upper(p.partnerCountryKey) = :key")
})
public class Partner implements Comparable<Partner> {
    private static final Logger LOGGER = Logger.getLogger(Partner.class.toString());

    public static Partner forCountryKey(String countryKey, Session session) {
        Query q = session.createQuery("from Partner where partnerCountryKey = '" + countryKey + "'");
        Object o = q.uniqueResult();
        return (Partner) o;
    }

    public static Partner forNgoPartner(final NgoPartner ngoPartner, final Session session) {
        Query q = session.createQuery("from Partner where abbreviation = '" + ngoPartner.name() + "'");
        Object o = q.uniqueResult();
        return (Partner) o;
    }

    public static Partner forExchangePartner(final ExchangePartner exchangePartner, final Session session) {
        Query q = session.createQuery("from Partner where partnerCountryKey = '" + exchangePartner.name() + "'");
        Object o = q.uniqueResult();
        return (Partner) o;
    }

    public static Partner forExchangePartner(final ExchangePartner exchangePartner, final Collection<Partner> allPartners) {
        for (Partner p : allPartners) {
            if (p.getPartnerCountryKey().equals(exchangePartner.name()))
                return p;
        }

        return null;
    }

    public enum SiteShare {
        SOUTH,
        NORTH,
        BOTH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "abbreviation")
    private String abbreviation;

    @Column(name = "partner_country_key")
    private String partnerCountryKey;

    @Column(name = "is_country")
    private boolean isCountry;

    @Column(name = "percentage_share")
    private double percentageShare;

    @Enumerated(EnumType.STRING)
    @Column(name = "site")
    private SiteShare siteShare;

    @Column(name = "ngo_feedback_email")
    private String ngoFeedbackEmail;


    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartnerCountryKey() {
        return partnerCountryKey;
    }

    public boolean isCountry() {
        return isCountry;
    }

    public boolean isChargeable() {
        return isCountry() || (getPercentageShare() > 0);
    }

    public String getNgoFeedbackEmail() {
        return ngoFeedbackEmail;
    }

    public void setNgoFeedbackEmail(String email) {
        ngoFeedbackEmail = email;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("id", id).
                append("name", name).
                append("abbreviation", abbreviation).
                append("partnerCountryKey", partnerCountryKey).
                append("isCountry", isCountry).
                append("percentageShare", percentageShare).
                append("siteShare", siteShare).
                append("ngoFeedbackEmail", ngoFeedbackEmail).
                toString();
    }

    @Override
    public int hashCode() {
        if(getAbbreviation() == null){
            return super.hashCode();
        }
        return getAbbreviation().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Partner))
            return false;

        final Partner other = (Partner) o;
        if (this.getId() != null && this.getId().equals(other.getId())) {
            // if they both have an id and they're equal, then they're indeed identical
            return true;
        }
        // if ids are not identical then check contents, assuming abbreviation is unique
        if (this.getAbbreviation().equals(other.getAbbreviation())) {
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(Partner o) {
        if(o.abbreviation == null || this.abbreviation == null){
            o.abbreviation = "";
        }
        return abbreviation.compareTo(o.abbreviation);
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPartnerCountryKey(String partnerCountryKey) {
        this.partnerCountryKey = partnerCountryKey;
    }

    public void setIsCountry(boolean country) {
        isCountry = country;
    }

    public double getPercentageShare() {
        return percentageShare;
    }

    public double getPercentageShare(Site site) {
        return getSites().contains(site) ? percentageShare : 0.0;
    }

    public void setPercentageShare(double percentageShare){
        this.percentageShare = percentageShare;
    }

    public boolean isNorth() {
        return (siteShare == SiteShare.NORTH || siteShare == SiteShare.BOTH);
    }

    public boolean isSouth() {
        return (siteShare == SiteShare.SOUTH || siteShare == SiteShare.BOTH);
    }

    public void setSiteShare(SiteShare ss){
        this.siteShare = ss;
    }

    public Set<Site> getSites() {
        switch (siteShare) {
            case NORTH: return Site.NORTH_SET;
            case SOUTH: return Site.SOUTH_SET;
            default:    return Site.ALL_SET;
        }
    }

    public boolean isNgo() {
        try{
        for (NgoPartner v: NgoPartner.values()) {
            if (v.name().equals(getPartnerCountryKey()))
                return true;
        }
        }catch(Exception x){
            LOGGER.log(Level.WARN, "Exception " + x + " caught in Partner");
        }
        return false;
    }

    public boolean isExchange() {
        for (ExchangePartner v: ExchangePartner.values()) {
            if (v.name().equals(partnerCountryKey))
                return true;
        }
        return false;
    }

    public boolean isLargeProgram() {
        return partnerCountryKey.equals("LP");
    }

    //These are hard-coded, because of expectation that exchange proposals have similarly hard-coded keys
    public static String KECK_KEY = "KECK";
    public static String SUBARU_KEY = "SUBARU";
}
