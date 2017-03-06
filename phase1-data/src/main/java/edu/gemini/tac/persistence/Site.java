package edu.gemini.tac.persistence;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The actual site for which we are generating a queue.  Gemini North?  Gemini South?
 *
 * @author ddawson
 *
 */
@Entity
@Table(name = "sites")
@NamedQueries({
	@NamedQuery(name = "site.findSiteByDisplayName", query = "from Site where display_name = :displayName")
})
public class Site {

    public final static Site NORTH = new Site("North");
    public final static Site SOUTH = new Site("South");

    public final static Site[] sites = { NORTH, SOUTH};

    public static final Set<Site> NORTH_SET = Collections.singleton(NORTH);
    public static final Set<Site> SOUTH_SET = Collections.singleton(SOUTH);
    public static final Set<Site> ALL_SET;

    static {
        final Set<Site> tmp = new HashSet<Site>();
        for (Site s : sites) tmp.add(s);
        ALL_SET = Collections.unmodifiableSet(tmp);
    }

    @Id
   	@GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Column(name = "display_name")
	private String displayName;

    public Site() {
    }

    public Site(String displayName) {
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

	public String getDisplayName() {
		return displayName;
	}

    @Override
    public int hashCode() {
        return new HashCodeBuilder(117236111, 2141713121).
                append(this.displayName).
                toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Site))
            return false;

        final Site that = (Site) o;

        return new EqualsBuilder().
                append(this.displayName, that.displayName).
                isEquals();
    }

    public Site otherSite(){
        return this.equals(Site.NORTH) ? Site.SOUTH : Site.NORTH;
    }
}
