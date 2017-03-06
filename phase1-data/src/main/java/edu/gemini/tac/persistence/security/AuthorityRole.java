package edu.gemini.tac.persistence.security;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

/**
 * A security role a la "ROLE_ADMIN" "ROLE_USER" etc. A specific role has some set of privileges in the system. At the
 * Web level, these privileges are used, e.g.,
 *     <security:authorize access="isInRole('ROLE_ADMIN')> <p>Some privileged data</p> </security:authorize>
 * and are also used in the context configuration files, e.g.,
 * 		<security:intercept-url pattern="/tac/committees/**" access="isInRole('ROLE_COMMITTEE')"/>
 *
 * @author lobrien
 */


@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name="authority_roles")
public class AuthorityRole {
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_COMMITTEEE_MEMBER = "ROLE_COMMITTEE_MEMBER";
    public static final String ROLE_SECRETARY = "ROLE_SECRETARY";
    public static final String ROLE_JOINT_PROPOSAL_ADMIN = "ROLE_JOINT_PROPOSAL_ADMIN";
    
    @Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(name = "rolename")
	private String rolename;

    public Long getId() {
           return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRolename() {
        return rolename;
    }

    public void setRolename(String rolename) {
        this.rolename = rolename;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this).
			append("id", id).
			append("rolename", rolename).
			toString();
    }
}
