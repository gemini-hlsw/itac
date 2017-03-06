package edu.gemini.tac.persistence.phase1;

import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;

import javax.persistence.*;

@Entity
@DiscriminatorValue("TooTarget")
public class TooTarget extends Target {

    public TooTarget() {}

    public TooTarget(final Target that, final PhaseIProposal phaseIProposal, final String targetId) {
        super(that, phaseIProposal, targetId);
    }

    @Override
    public edu.gemini.model.p1.mutable.Target toMutable() {
        final edu.gemini.model.p1.mutable.TooTarget mt = new edu.gemini.model.p1.mutable.TooTarget();
        mt.setId(getTargetId());
        mt.setName(getName());
        return mt;
    }

    @Override
    public Target copy() {
        final TooTarget tooTarget = new TooTarget(this, phaseIProposal, targetId);
        tooTarget.setEpoch(getEpoch());
        tooTarget.setName(getName());
        tooTarget.setPhaseIProposal(getPhaseIProposal());
        if(getTargetId() != null){
            tooTarget.setTargetId(getTargetId());
        }else{
            tooTarget.setTargetId("");
        }

        return tooTarget;
    }

    @Override
    public int getRaBin() {
        return -1;
    }

    @Override
    public String getDisplayCoordinates() {
        return "-";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TooTarget)) return false;
        if (!super.equals(o)) return false;

        TooTarget that = (TooTarget) o;
        // no other attributes, no more checks for now
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        // no other attributes, no more values for hashcode
        return result;
    }

    @Override
    public String toString() {
        return "TooTarget{" +
                '}';
    }

    @Override
    public String toKml() {
        return "<LookAt>\n" +
        "      <altitude>0</altitude>\n" +
        "      <range>10000</range>\n" +
        "      <tilt>0</tilt>\n" +
        "      <heading>0</heading>\n" +
        "    </LookAt>\n" +
        "    <styleUrl>#ScienceTarget</styleUrl>\n";
    }

    @Override
    public String toDot() {
        String myDot = "SiderealTarget_" + (id == null ? "NULL" : id.toString()) + ";\n";
        return myDot;
    }

    @Override
    public String getPositionDisplay() {
        return "ToO";
    }

    @Override
    public String getBrightnessDisplay() {
        return "ToO";
    }

    @Override
    public void accept(TargetVisitor visitor){ visitor.visit(this); }

    public String getTargetClass() { return "Too";};
    public boolean isToo() { return true; }
}
