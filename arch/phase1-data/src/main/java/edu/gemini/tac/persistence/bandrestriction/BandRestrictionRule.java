package edu.gemini.tac.persistence.bandrestriction;

import edu.gemini.tac.persistence.queues.Queue;

import javax.persistence.*;
import java.util.Set;

/**
 * A rule that may restrict us from accepting a proposal that fulfills some
 * set of conditions if we are currently assigning within a certain band.  For
 * example, we will not consider further proposals that require LGS if we
 * have already exhausted the band 1 resource.
 * 
 * @author ddawson
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "rule_type", discriminatorType = DiscriminatorType.STRING) 
@Table(name = "band_restriction_rules")
@NamedQueries({
    @NamedQuery(name = "bandRestriction.findByIds", query = "from BandRestrictionRule brr where brr.id in (:ids)")
})
public abstract class BandRestrictionRule {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToMany(mappedBy = "bandRestrictionRules")
    private Set<Queue> queues;

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public Set<Queue> getQueues() {
        return queues;
    }
}
