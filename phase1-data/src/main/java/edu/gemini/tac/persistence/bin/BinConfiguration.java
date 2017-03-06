package edu.gemini.tac.persistence.bin;

import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.*;

/**
 * Allows some small amount of customization around the discrete sizes
 * of resources that we are attempting to allocate.  Tied to site and
 * semester.
 *
 * @author ddawson
 *
 */
@Entity
@Table(name = "bin_configurations")
@NamedQueries({
    @NamedQuery(name = "binConfiguration.findById", query = "from BinConfiguration bc where bc.id = :id"),
    @NamedQuery(name = "binConfiguration.findByName", query = "from BinConfiguration bc where bc.name= :name")
})
public class BinConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ra_bin_size_id")
    private RABinSize raBinSize;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dec_bin_size_id")
    private DecBinSize decBinSize;

    @Column(name = "smoothing_length")
    private Integer smoothingLength;

    @Column(name = "editable")
    private Boolean editable;

    public final String getName() {
        return name;
    }
    public final void setName(String name) {
        this.name = name;
    }
    public final RABinSize getRaBinSize() {
        return raBinSize;
    }
    public final void setRaBinSize(RABinSize raBinSize) {
        this.raBinSize = raBinSize;
    }
    public final DecBinSize getDecBinSize() {
        return decBinSize;
    }
    public final void setDecBinSize(DecBinSize decBinSize) {
        this.decBinSize = decBinSize;
    }
    public final Integer getSmoothingLength() {
        return smoothingLength;
    }
    public final void setSmoothingLength(Integer smoothingLength) {
        this.smoothingLength = smoothingLength;
    }
    public final Boolean getEditable() {
        return editable;
    }
    public final void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public String toString() {
        return new ToStringBuilder(this).
            append("id", id).
            append("name", name).
            append("raBinSize", raBinSize).
            append("decBinSize", decBinSize).
            append("smoothingLength", smoothingLength).
            append("editable", editable).
            toString();
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
}
