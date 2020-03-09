package edu.gemini.tac.persistence.daterange;

import edu.gemini.tac.persistence.phase1.Instrument;
import org.hibernate.Session;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * Previously linked a DateRange with a Resource, now with an Instrument.  (Should probably be able to handle
 * a site as well.)
 *
 * @author lobrien
 * @author ddawson
 * Date: 4/5/11
 */

@Entity
@Table(name="blackouts")
@org.hibernate.annotations.Entity(mutable = false)
public class Blackout {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name ="date_range_id")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    DateRangePersister dateRangePersister;

    @Enumerated(EnumType.STRING)
    @Column(name = "instrument")
    Instrument instrument;

    public Blackout(final Blackout that, final Session session) {
        dateRangePersister = new DateRangePersister(that.getDateRangePersister(), session);
        instrument = that.getInstrument();
    }

    public Blackout() {
    }


    public void setDateRangePersister(DateRangePersister drp){
        this.dateRangePersister = drp;
    }

    public DateRangePersister getDateRangePersister(){
        return dateRangePersister;
    }

    public Long getId(){
        return id;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }
}
