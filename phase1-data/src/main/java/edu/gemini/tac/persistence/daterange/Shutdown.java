package edu.gemini.tac.persistence.daterange;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Site;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Date;

/**
 * Persistence object representing a site-wide unavailability
 */
@Entity
@Table(name = "shutdowns")
@NamedQueries({
        @NamedQuery(name = "shutdown.forCommittee",
                query = "from Shutdown s where s.committee.id = :committeeId"),
        @NamedQuery(name = "shutdown.forId",
                query = "from Shutdown s where s.id = :shutdownId")
})
public class Shutdown {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  Long id;


  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "date_range_id")
  @Cascade(org.hibernate.annotations.CascadeType.ALL)
  DateRangePersister dateRangePersister;


  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "site_id")
  Site site;

  @OneToOne(fetch = FetchType.LAZY)
  Committee committee;


    public Shutdown(){}

    public Shutdown(DateRangePersister drp, Site site, Committee committee){
        this.dateRangePersister = drp;
        this.site = site;
        this.committee = committee;
        committee.addShutdown(this);
    }

    public DateRangePersister getDateRangePersister(){
        return this.dateRangePersister;
    }

    public void setDateRangePersister(DateRangePersister drp){
        this.dateRangePersister = drp;
    }

    public Date getStart() {
        return dateRangePersister.getDateRange().getStartDate();
    }

    public Date getEnd() {
        return dateRangePersister.getDateRange().getEndDate();
    }

    public Long getId(){
        return this.id;
    }

    public void setId(Long id){
        this.id = id;
    }

    public Site getSite(){
        return this.site;
    }

    public void setSite(Site s){
        this.site = s;
    }

    public Committee getCommittee(){
        return this.committee;
    }

    public void setCommittee(Committee committee){
        this.committee = committee;
    }
}
