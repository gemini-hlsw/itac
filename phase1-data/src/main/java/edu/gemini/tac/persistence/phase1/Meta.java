package edu.gemini.tac.persistence.phase1;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Meta {
    @Column(name = "attachment")
    protected String attachment;

    @Column(name = "band_3_option_chosen")
    protected Boolean band3optionChosen = Boolean.FALSE;

    public Meta() {}

    public Meta(final Meta copied) {
        if(copied != null){
            setAttachment(copied.getAttachment());
        }
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public Boolean getBand3optionChosen() {
        return band3optionChosen;
    }

    public void setBand3optionChosen(Boolean band3optionChosen) {
        this.band3optionChosen = band3optionChosen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Meta)) return false;

        Meta meta = (Meta) o;

        if (attachment != null ? !attachment.equals(meta.attachment) : meta.attachment != null) return false;
        if (band3optionChosen != null ? !band3optionChosen.equals(meta.band3optionChosen) : meta.band3optionChosen != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = attachment != null ? attachment.hashCode() : 0;
        result = 31 * result + (band3optionChosen != null ? band3optionChosen.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Meta{" +
                "attachment='" + attachment + '\'' +
                ", band3optionChosen=" + band3optionChosen +
                '}';
    }
}
