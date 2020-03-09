package edu.gemini.tac.persistence;

import edu.gemini.tac.persistence.security.AuthorityRole;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * A user in the itac application.  Someone with an account that has some level
 * of privileges in the system.  There is a lot of work remaining to be done involving
 * bringing the concept of privileges and roles into the application.  Some of that will
 * happen in phase one, other parts will be delayed.
 * 
 * This is a RDBMS backed class for persistence.
 * 
 * @author ddawson
 *
 */
@NamedQueries({
        // find user by name
        @NamedQuery(name = "person.findByName",
                query = "from Person p " +
                        "join fetch p.partner " +
                        "where p.name = :name"
        ),
        @NamedQuery(name = "person.findWithCommitteesByName",
                query = "from Person p " +
                        "join fetch p.partner " +
                        "join fetch p.committees " +
                        "where p.name = :name"
        ),
        @NamedQuery(name = "person.getAllPeople",
                query = "from Person p ",
                hints = { @QueryHint(name = "org.hibernate.cacheable", value = "true") }
        )
})

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "people")
public class Person {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@Column(name = "name")
	private String name;
	
	// Someday in the future we'll do this right with salts and hashes.  Right now, we're
	// not even officially delivering this feature so I'm going to go quick and dirty.
	@Column(name = "password")
	private String password;

    @Column(name="enabled")
    private boolean enabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Partner partner;

    @ManyToMany(targetEntity=AuthorityRole.class,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.EAGER )
    @JoinTable(name="authorities",
            joinColumns=@JoinColumn(name="person_id"),
            inverseJoinColumns=@JoinColumn(name="role_id") )
    @Fetch(FetchMode.SUBSELECT)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<AuthorityRole> authorities = new HashSet<AuthorityRole>();

    @ManyToMany(targetEntity=Committee.class)
    @JoinTable(name="memberships",
            joinColumns=@JoinColumn(name="person_id"),
            inverseJoinColumns=@JoinColumn(name="committee_id"))
    private Set<Committee> committees = new HashSet<Committee>();

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getPassword() {
		return password;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}
    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }
    public boolean isEnabled() {
        return enabled;
    }

    public Set<AuthorityRole> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<AuthorityRole> authorities) {
        this.authorities = authorities;
    }

    public Set<Committee> getCommittees() {
        return committees;
    }

    public void setCommittees(Set<Committee> committees) {
        this.committees = committees;
    }

    public void setPartner(Partner partner) {
        this.partner = partner;
    }

    public Partner getPartner() {
        return partner;
    }
    
    public boolean isAdmin() {
        for (AuthorityRole ar : authorities) {
            if (ar.getRolename().equals(AuthorityRole.ROLE_ADMIN))
                return true;
        }
        
        return false;
    }

    public boolean isInRole(final String rolename) {
        for (AuthorityRole ar : authorities) {
            if (ar.getRolename().equals(rolename))
                return true;
        }

        return false;
    }

    public String toString() {
		return new ToStringBuilder(this).
			append("id", id).
			append("name", name).
			append("password", password).
            append("enabled", enabled).
			toString();
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;

        Person person = (Person) o;

        if (name != null ? !name.equals(person.name) : person.name != null) return false;
        if (password != null ? !password.equals(person.password) : person.password != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }
}
