package edu.gemini.tac.persistence.condition;

import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Container for various constraints on an queue.  Has a name and an associated set
 * of water vapor, image quality, sky background, cloud cover and available percentages.
 *
 * @author ddawson
 *
 */
@Entity
@Table(name = "condition_sets")
@NamedQueries({
    @NamedQuery(name = "condition.findById", query = "from ConditionSet cs where cs.id = :id"),
    @NamedQuery(name = "condition.findByName", query = "from ConditionSet cs where cs.name = :name")
})
public class ConditionSet {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "condition_set_id")
    @OrderBy("availablePercentage asc, imageQuality, skyBackground")
    private Set<ConditionBucket> conditions = new HashSet<ConditionBucket>();

    public final String getName() {
        return name;
    }
    public final void setName(final String name) {
        this.name = name;
    }
    public final Long getId() {
        return id;
    }
    public final void setId(Long id) {
        this.id = id;
    }
    public final Set<ConditionBucket> getConditions() {
        return Collections.unmodifiableSet(conditions);
    }
    public final void setConditions(final Set<ConditionBucket> conditions) {
        this.conditions = conditions;
    }
    public final void addCondition(final ConditionBucket condition) {
        conditions.add(condition);
    }

    public String toString() {
        return new ToStringBuilder(this).
            append("name", name).
            append("id", id).
            append("conditions", conditions).
            toString();
    }
}
