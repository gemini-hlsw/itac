package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.InvestigatorStatus;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@NamedQueries({
        @NamedQuery(name = "Investigator.findPrincipalInvestigatorById",
                query = "from PrincipalInvestigator pi " +
                        "join fetch pi.phoneNumbers " +
                        "where pi.id = (:piId)"
        ),
        @NamedQuery(name = "Investigator.findCoinvestigatorsForIds",
                query = "from CoInvestigator coi " +
                        "where coi.id in (:coiIds)"
        ),
        @NamedQuery(name = "Investigator.findPrincipalInvestigator",
                query = "from PrincipalInvestigator pi " +
                        "left join fetch pi.phoneNumbers " +
                        "where pi = (:pi)"
        ),
        @NamedQuery(name = "Investigator.findCoinvestigators",
                query = "from CoInvestigator coi " +
                        "left join fetch coi.phoneNumbers " +
                        "where coi in (:cois" +
                        ")"
        ),
        @NamedQuery(name = "Investigator.findInvestigatorsByCommittee",
            query = "from Investigator i "
        ),
        @NamedQuery(name = "Investigator.findPhoneNumbers",
            query = "from Investigator i " +
                    "left join fetch i.phoneNumbers " +
                    "where i in (:investigators) "
        )
})
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@Table(name = "v2_investigators")
abstract public class Investigator {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Column(name = "first_name")
    protected String firstName;

    @Column(name = "last_name")
    protected String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "investigator_status")
    protected InvestigatorStatus status;

    @Column(name = "email")
    protected String email;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "v2_phone_numbers",
            joinColumns = @JoinColumn(name = "investigator_id"))
    @Column(name = "phone_number")
    @Fetch(FetchMode.SUBSELECT)
    protected Set<String> phoneNumbers = new HashSet<String>();

    public Investigator(){}

    public Investigator(String firstName, String lastName, InvestigatorStatus status, String email, Set<String> phoneNumbers){
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.email = email;
        this.phoneNumbers.addAll(phoneNumbers);
    }

    public Investigator updateFrom(final Investigator from) {
        setFirstName(from.getFirstName());
        setLastName(from.getLastName());
        setStatus(from.getStatus());
        setEmail(from.getEmail());
        phoneNumbers.clear();
        phoneNumbers.addAll(from.getPhoneNumbers());

        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Investigator)) return false;

        Investigator that = (Investigator) o;

        if (email != null ? !email.equals(that.email) : that.email != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return email != null ? email.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Investigator{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", status=" + status +
                ", email='" + email +
                '}';
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setStatus(InvestigatorStatus status) {
        this.status = status;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumbers(final Set<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public InvestigatorStatus getStatus() {
        return status;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Long getId() {
        return id;
    }

    //Separate field so that JointProposal's "Derived" PI can have a derived status ala "PhD, Grad Thesis"
    @Transient
    private String statusDisplayString = "";
    public String getStatusDisplayString(){
        if(statusDisplayString.equals("") && status != null){
            statusDisplayString = this.getStatus().value();
        }
        return statusDisplayString;
    }

    public void setStatusDisplayString(String s){
        statusDisplayString = s;
    }

    abstract public edu.gemini.model.p1.mutable.Investigator toMutable(String investigatorId);

    protected edu.gemini.model.p1.mutable.Investigator toMutable(edu.gemini.model.p1.mutable.Investigator mInvestigator, final String investigatorId)
    {
        mInvestigator.setEmail(getEmail());
        mInvestigator.setFirstName(getFirstName());
        mInvestigator.setId(investigatorId);
        mInvestigator.setLastName(getLastName());
        mInvestigator.setStatus(getStatus());
        mInvestigator.getPhone().addAll(getPhoneNumbers());

        return mInvestigator;
    }
}
