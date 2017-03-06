package edu.gemini.tac.persistence.phase1;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class InstitutionAddress {
    @Column(name = "institution")
    protected String institution;

    @Column(name = "institution_address")
    protected String address;

    @Column(name = "country")
    protected String country;

    public InstitutionAddress(){}

    public InstitutionAddress(String institution, String address, String country){
        this.institution = institution;
        this.address = address;
        this.country = country;
    }

    public InstitutionAddress(final edu.gemini.model.p1.mutable.InstitutionAddress mAddress) {
        this(mAddress.getInstitution(), mAddress.getAddress(), mAddress.getCountry());
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public edu.gemini.model.p1.mutable.InstitutionAddress toMutable() {
        final edu.gemini.model.p1.mutable.InstitutionAddress mInstitutionAddress = new edu.gemini.model.p1.mutable.InstitutionAddress();

        mInstitutionAddress.setAddress(getAddress());
        mInstitutionAddress.setCountry(getCountry());
        mInstitutionAddress.setInstitution(getInstitution());

        return mInstitutionAddress;
    }
}
