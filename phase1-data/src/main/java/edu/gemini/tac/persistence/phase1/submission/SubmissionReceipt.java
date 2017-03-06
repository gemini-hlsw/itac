package edu.gemini.tac.persistence.phase1.submission;

import edu.gemini.tac.persistence.util.Conversion;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Embeddable
public class SubmissionReceipt {
    @Column(name = "receipt_timestamp")
    protected Date timestamp;

    @Column(name = "receipt_id")
    protected String receiptId;

    public SubmissionReceipt() {}

    /**
     * @param copiedReceipt
     */
    public SubmissionReceipt(final SubmissionReceipt copiedReceipt) {
        setTimestamp(copiedReceipt.getTimestamp());
        setReceiptId(copiedReceipt.getReceiptId());
    }

    public SubmissionReceipt(final edu.gemini.model.p1.mutable.SubmissionReceipt mReceipt) {
        this(mReceipt.getTimestamp().toGregorianCalendar().getTime(), mReceipt.getId());
    }

    public SubmissionReceipt(final Date timestamp, final String receiptId) {
        this.timestamp = timestamp;
        this.receiptId = receiptId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmissionReceipt)) return false;

        SubmissionReceipt that = (SubmissionReceipt) o;

        if (receiptId != null ? !receiptId.equals(that.receiptId) : that.receiptId != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = timestamp != null ? timestamp.hashCode() : 0;
        result = 31 * result + (receiptId != null ? receiptId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SubmissionReceipt{" +
                "timestamp=" + timestamp +
                ", receiptId='" + receiptId + '\'' +
                '}';
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public edu.gemini.model.p1.mutable.SubmissionReceipt toMutable() {
        final edu.gemini.model.p1.mutable.SubmissionReceipt mReceipt = new edu.gemini.model.p1.mutable.SubmissionReceipt();

        mReceipt.setTimestamp(Conversion.dateToXmlGregorian(getTimestamp()));
        mReceipt.setId(getReceiptId());

        return mReceipt;
    }
}
