package edu.gemini.tac.persistence.rollover;

import javax.persistence.*;

/**
 * Adds a "name" field to help the user remember them
 *
 * Author: lobrien
 * Date: 3/14/11
 */

@Entity
@DiscriminatorValue("class edu.gemini.tac.persistence.rollover.RolloverSet")
@NamedQueries({
        @NamedQuery(name="RolloverSet.findBySite",
        query="from RolloverSet r left outer join fetch r.observations where r.site.displayName = :siteName"),
        @NamedQuery(name="RolloverSet.getById",
        query="from RolloverSet r " +
              "left outer join fetch r.observations os " +
              "left outer join fetch os.condition "  +
              "left join fetch os.target t " +
              "where r.id = :id")
})
public class RolloverSet extends RolloverReport {
    @Column
    private String set_name;
    public String getName() { return set_name; }
    public void setName(String name) { this.set_name = name; }


}
