package edu.gemini.tac.persistence.phase1;

import edu.gemini.model.p1.mutable.NgoPartner;
import org.apache.commons.lang.StringUtils;

import javax.persistence.*;

@Entity
@Table(name = "v2_itacs")
public class Itac {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Embedded
    protected ItacAccept accept;

    @Column(name = "rejected")
    protected Boolean rejected = Boolean.FALSE;

    @Column(name = "comment")
    protected String comment = "";

    /**
     * TODO: This is not currently part of the schema.  Need to get this in-place for next year.
     */
    @Column(name = "gemini_comment")
    protected String geminiComment = "None";

    @Column(name = "ngo_authority", nullable = true)
    @Enumerated(EnumType.STRING)
    protected NgoPartner ngoAuthority;

    @Override
    public String toString() {
        return "Itac{" +
                "id=" + id +
                ", accept=" + ((accept != null) ? accept.toString() : "null") +
                ", rejected=" + rejected +
                ", comment='" + comment + '\'' +
                ", ngoAuthority='" + ngoAuthority + '\'' +
                ", geminiComment='" + geminiComment + '\'' +
                '}';
    }

    public Itac(){}

    public Itac(final Itac src) {
        this(src.getAccept(), src.getRejected(), src.getComment(), src.getNgoAuthority());
    }

    public Itac(ItacAccept accept, Boolean isRejected, String comment, NgoPartner ngoAuthority){
        this.accept = accept;
        this.rejected = isRejected;
        this.comment = comment;
        this.ngoAuthority = ngoAuthority;
    }

    public Itac(edu.gemini.model.p1.mutable.Itac mItac) {
        this((mItac.getAccept() != null) ?
                new ItacAccept(mItac.getAccept()) : null,
                mItac.getReject() != null,
                StringUtils.trim(mItac.getComment()),
                mItac.getNgoauthority());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public ItacAccept getAccept() {
        return accept;
    }

    public void setAccept(ItacAccept value) {
        this.rejected = false; // if accept part is set this must not be flagged as rejected!
        this.accept = value;
    }

    public Boolean getRejected() {
        return rejected;
    }

    public void setRejected(Boolean value) {
        this.rejected = value;
        if (this.rejected) this.accept = null; // if this is flagged as rejected it must not have an accept part
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String value) {
        this.comment = value;
    }

    public String toDot(){
        String myDot = "Itac_" + (id == null ? "NULL" : id.toString());
        String dot = myDot + ";\n";
        if(accept != null){
            dot += myDot + "->" + accept.toDot();
        }
        return dot;
    }

    public String getGeminiComment() {
        return geminiComment;
    }

    public void setGeminiComment(String geminiComment) {
        this.geminiComment = geminiComment;
    }

    public edu.gemini.model.p1.mutable.Itac toMutable() {
        final edu.gemini.model.p1.mutable.Itac mItac = new edu.gemini.model.p1.mutable.Itac();

        // TODO: Temporary combination until schema can be modified in order to support a gemini comment.
        mItac.setComment("ITAC Comment\n" + getComment() + "\nGemini Comment\n" + getGeminiComment());
        if (rejected)
            mItac.setReject(new edu.gemini.model.p1.mutable.ItacReject());

        if (getAccept() != null)
            mItac.setAccept(getAccept().toMutable());

        if (getNgoAuthority() != null)
            mItac.setNgoauthority(getNgoAuthority());

        return mItac;
    }

    public NgoPartner getNgoAuthority() {
        return ngoAuthority;
    }

    public void setNgoAuthority(NgoPartner ngoAuthority) {
        this.ngoAuthority = ngoAuthority;
    }
}
