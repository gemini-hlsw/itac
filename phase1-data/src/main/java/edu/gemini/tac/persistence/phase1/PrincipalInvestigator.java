package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.InvestigatorStatus;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@NamedQueries({
        @NamedQuery(name = "principalInvestigator.findByEmail", query = "from PrincipalInvestigator where email = :email")
})
@Entity
@DiscriminatorValue("PrimaryInvestigator")
public class PrincipalInvestigator extends Investigator
{
    public PrincipalInvestigator() {}

    public PrincipalInvestigator(String firstName, String lastName, InvestigatorStatus status, String email, Set<String> phoneNumbers, InstitutionAddress institutionAddress){
        super(firstName, lastName, status, email, phoneNumbers);
        this.institutionAddress = institutionAddress;
    }

    public Investigator updateFrom(final PrincipalInvestigator from) {
        super.updateFrom(from);
        setInstitutionAddress(from.getInstitutionAddress());

        return this;
    }

    public PrincipalInvestigator(final edu.gemini.model.p1.mutable.PrincipalInvestigator mInvestigator) {
        this(mInvestigator.getFirstName(), mInvestigator.getLastName(),
                mInvestigator.getStatus(), mInvestigator.getEmail(),
                new HashSet<String>(mInvestigator.getPhone()),
                new InstitutionAddress(mInvestigator.getAddress()));
    }

    @Embedded
    protected InstitutionAddress institutionAddress = new InstitutionAddress();

    @Override
    public String toString() {
        return "PrincipalInvestigator{" +
                "institutionAddress=" + institutionAddress +
                "} " + super.toString();
    }

    @Override
    public edu.gemini.model.p1.mutable.Investigator toMutable(String investigatorId) {
        final edu.gemini.model.p1.mutable.PrincipalInvestigator mInvestigator = new edu.gemini.model.p1.mutable.PrincipalInvestigator();
        super.toMutable(mInvestigator, investigatorId);

        mInvestigator.setAddress(getInstitutionAddress().toMutable());

        return mInvestigator;
    }

    public InstitutionAddress getInstitutionAddress() {
        return institutionAddress;
    }

    public void setInstitutionAddress(InstitutionAddress value) {
        this.institutionAddress = value;
    }
}
