

package edu.gemini.tac.persistence.phase1;


import edu.gemini.model.p1.mutable.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Embeddable
public class Investigators {
    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "principal_investigator_id")
    @Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
    protected PrincipalInvestigator pi = new PrincipalInvestigator();

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name="v2_proposals_co_investigators",
            joinColumns=@JoinColumn(name="v2_phase_i_proposal_id"),
            inverseJoinColumns=@JoinColumn(name="investigator_id") )
    @Fetch(FetchMode.SUBSELECT)
    @Cascade({ org.hibernate.annotations.CascadeType.SAVE_UPDATE })
    protected Set<CoInvestigator> coi = new HashSet<CoInvestigator>();

    public Investigators() {}

    public Investigators(final Investigators src) {
        setPi(src.getPi());
        coi.addAll(src.getCoi());
    }

    public PrincipalInvestigator getPi() {
        return pi;
    }

    public Set<CoInvestigator> getCoi() {
        return coi;
    }

    public void setPi(PrincipalInvestigator pi) {
        this.pi = pi;
    }

    public void setCoi(final Set<CoInvestigator> cois){
        this.coi = cois;
    }

    public edu.gemini.model.p1.mutable.Investigators toMutable(final HashMap<Investigator, String> hibernateInvestigatorToIdMap) {
        final edu.gemini.model.p1.mutable.Investigators mInvestigators = new edu.gemini.model.p1.mutable.Investigators();

        final edu.gemini.model.p1.mutable.PrincipalInvestigator principalInvestigator =
                (edu.gemini.model.p1.mutable.PrincipalInvestigator) getPi().toMutable(hibernateInvestigatorToIdMap.get(getPi()));
        mInvestigators.setPi(principalInvestigator);

        final List<edu.gemini.model.p1.mutable.CoInvestigator> mCoi = mInvestigators.getCoi();
        for (edu.gemini.tac.persistence.phase1.CoInvestigator c : getCoi()) {
            mCoi.add((edu.gemini.model.p1.mutable.CoInvestigator) c.toMutable(hibernateInvestigatorToIdMap.get(c)));
        }

        return mInvestigators;
    }
}
