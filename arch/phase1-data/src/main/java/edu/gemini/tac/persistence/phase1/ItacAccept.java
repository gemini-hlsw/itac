package edu.gemini.tac.persistence.phase1;

import javax.persistence.*;

@Embeddable
public class ItacAccept {
    @Column(name = "accept_program_id")
    protected String programId = "NO PROGRAM ID ASSIGNED";

    /**
     * Corresponds to gemini contact scientist.
     */
    @Column(name = "accept_contact") 
    protected String contact = "";

    /**
     * Corresponds to the ngo contact email.
     */
    @Column(name = "accept_email")
    protected String email = "No partner lead email assigned.";

    @Column(name = "accept_band")
    protected int band;

    @Embedded
    @AttributeOverrides( {
            @AttributeOverride(name="value", column = @Column(name="accept_award_value") ),
            @AttributeOverride(name="units", column = @Column(name="accept_award_units") )
    } )
    protected TimeAmount award = new TimeAmount(0.0d, TimeUnit.HR);

    @Column(name = "accept_rollover")
    protected Boolean rollover = Boolean.FALSE;

    public ItacAccept(){}

    public ItacAccept(TimeAmount timeAmount, int band, boolean isRollover, String contact, String email, String programId) {
        this.award = timeAmount;
        this.band = band;
        this.rollover = isRollover;
        this.contact = contact;
        this.email = email;
        this.programId = programId;
    }

    public ItacAccept(final edu.gemini.model.p1.mutable.ItacAccept mAccept) {
        this(new TimeAmount(mAccept.getAward()), mAccept.getBand(), mAccept.isRollover(),
                mAccept.getContact(), mAccept.getEmail(), mAccept.getProgramId());
    }

    public ItacAccept(final ItacAccept src) {
        if (src.getAward() != null)
            setAward(new TimeAmount(src.getAward()));
        setBand(src.getBand());
        setContact(src.getContact());
        setEmail(src.getEmail());
        setProgramId(src.getProgramId());
        setRollover(src.isRollover());
    }

    @Override
    public String toString() {
        return "ItacAccept{" +
                "programId='" + programId + '\'' +
                ", contact='" + contact + '\'' +
                ", email='" + email + '\'' +
                ", band=" + band +
                ", award=" + award +
                ", rollover=" + rollover +
                '}';
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String value) {
        this.programId = value;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String value) {
        this.contact = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    public int getBand() {
        return band;
    }

    public void setBand(int value) {
        this.band = value;
    }

    public TimeAmount getAward() {
        return award;
    }

    public void setAward(TimeAmount value) {
        this.award = value;
    }

    public boolean isRollover() {
        return rollover;
    }

    public void setRollover(boolean value) {
        this.rollover = value;
    }

    public String toDot(){
        String myDot = "ItacAccept";
        String dot = myDot + ";\n";
        return dot;
    }

    public edu.gemini.model.p1.mutable.ItacAccept toMutable() {
       final edu.gemini.model.p1.mutable.ItacAccept mItacAccept = new edu.gemini.model.p1.mutable.ItacAccept();
        mItacAccept.setAward(getAward().toMutable());
        mItacAccept.setEmail(getEmail());
        mItacAccept.setRollover(isRollover());
        mItacAccept.setBand(getBand());
        mItacAccept.setContact(getContact());
        mItacAccept.setProgramId(getProgramId());

        return mItacAccept;
    }
}
