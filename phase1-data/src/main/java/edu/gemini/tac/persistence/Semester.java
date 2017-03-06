package edu.gemini.tac.persistence;

import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A semester is a temporal unit that is the focus of a TAC process.
 *
 * @author ddawson
 *
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "semesters")
public class Semester implements Comparable<Semester> {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(name = "name")
	private String name;

    @Column(name = "display_year")
    private Integer year;

	public String getDisplayName() {
        return ((year == null) || (name == null)) ? null : getYear() + getName();
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getName() {
        return name;
    }

    public void setName(String semesterName) {
        this.name = semesterName;
    }

    @Override
    public int compareTo(Semester semester) {
        final int yearDifference = getYear().compareTo(semester.getYear());
        if (yearDifference != 0)
            return yearDifference;

        final int nameDifference = getName().compareTo(semester.getName());

        return nameDifference;
    }
}
