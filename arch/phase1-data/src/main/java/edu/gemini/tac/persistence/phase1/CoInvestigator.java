package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.InvestigatorGender;
import edu.gemini.model.p1.mutable.InvestigatorStatus;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@NamedQueries({
        @NamedQuery(name = "coInvestigator.findByEmail", query = "from CoInvestigator where email = :email")
})
@Entity
@DiscriminatorValue("CoInvestigator")
public class CoInvestigator extends Investigator
{
    @Column(name = "institution")
    protected String institution;

    public Investigator updateFrom(final CoInvestigator from) {
        super.updateFrom(from);
        setInstitution(from.getInstitution());

        return this;
    }

    @Override
    public String toString() {
        return "CoInvestigator{" +
                "institution='" + institution + '\'' +
                "} " + super.toString();
    }

    @Override
    public edu.gemini.model.p1.mutable.Investigator toMutable(String investigatorId) {
        final edu.gemini.model.p1.mutable.CoInvestigator mInvestigator = new edu.gemini.model.p1.mutable.CoInvestigator();
        super.toMutable(mInvestigator, investigatorId);

        mInvestigator.setInstitution(getInstitution());

        return mInvestigator;
    }

    public CoInvestigator(){}

    public CoInvestigator(String firstName, String lastName, InvestigatorGender gender, InvestigatorStatus status, String email, Set<String> phoneNumbers, String institution){
        super(firstName, lastName, gender, status, email, phoneNumbers);
        this.institution = institution;
    }

    public CoInvestigator(final edu.gemini.model.p1.mutable.CoInvestigator mInvestigator) {
        this(mInvestigator.getFirstName(), mInvestigator.getLastName(),
                mInvestigator.getGender(),
                mInvestigator.getStatus(), mInvestigator.getEmail(),
                new HashSet<String>(mInvestigator.getPhone()), mInvestigator.getInstitution());
    }


    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }
}
