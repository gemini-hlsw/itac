package edu.gemini.tac.persistence.daterange;

import edu.gemini.shared.util.DateRange;
import org.hibernate.Session;
import org.hibernate.annotations.Columns;

import javax.persistence.*;

/**
 * Persists DateRange class.
 *
 * Author: lobrien
 * Date: 4/5/11
 */

@Entity
@org.hibernate.annotations.Entity(mutable = false)
@Table(name = "date_ranges")
public class DateRangePersister {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Columns(
            columns = {
                    @Column(name = "start_date"),
                    @Column(name = "end_date")
            }
    )
    @org.hibernate.annotations.Type(type = "edu.gemini.tac.persistence.daterange.DateRangeUserType")
    private  DateRange dateRange;

    protected  DateRangePersister(){

    }

    public DateRangePersister(DateRange dateRange){
        this.dateRange = dateRange;
    }

    public DateRangePersister(DateRangePersister that, Session session) {
        this.dateRange = new DateRange(that.getDateRange());
    }

    public DateRange getDateRange(){
        return dateRange;
    }

    public Long getId(){
        return id;
    }

    //For testing
    public void setDateRange(DateRange dr){
        this.dateRange = dr;
    }
}
