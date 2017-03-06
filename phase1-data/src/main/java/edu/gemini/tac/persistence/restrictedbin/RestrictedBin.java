package edu.gemini.tac.persistence.restrictedbin;

import edu.gemini.tac.persistence.queues.Queue;

import javax.persistence.*;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "restricted_bin_type", discriminatorType = DiscriminatorType.STRING) 
@Table(name = "restricted_bins")
@NamedQueries({
    @NamedQuery(name = "restrictedBin.findByIds", query = "FROM RestrictedBin rb WHERE rb.id IN (:ids)")
})
public abstract class RestrictedBin {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "units")
    private String units;

    @Column(name = "value")
    private Integer value;

    @ManyToMany(mappedBy = "restrictedBins")
    private Set<Queue> queues;

    public String getDescription() {
        return description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Integer getValue() {
        return value;
    }

    public String getUnits() {
        return units;
    }

    protected void setValue(Integer value) {
        this.value = value;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    protected void setUnits(String units) {
        this.units = units;
    }

    public abstract RestrictedBin copy(Integer value);
}
