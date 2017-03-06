package edu.gemini.tac.persistence.queues.partnerCharges;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.queues.Queue;

import javax.persistence.*;

/**
 * This class connects the charges incurred by partners for participating in the exchange 
 * program.
 * 
 * @author ddawson
 *
 */
@Entity
@DiscriminatorColumn(name = "charge_type")
@Table(name = "partner_charges")
public abstract class PartnerCharge {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "queue_id")
	private Queue queue;

	@ManyToOne
	@JoinColumn(name = "partner_id")
	private Partner partner;

    @Embedded
       @AttributeOverrides( {
               @AttributeOverride(name="value", column = @Column(name="charge_value") ),
               @AttributeOverride(name="units", column = @Column(name="charge_units") )
       } )
    private TimeAmount charge;

//	@Column(name = "charge")
//	private Double charge;

	protected PartnerCharge() {};

	public PartnerCharge(final Queue queue, final Partner partner, final TimeAmount charge) {
		this.queue = queue;
		this.partner = partner;
		this.charge = charge;
	}

	public Long getId() {
		return id;
	}

	public Queue getQueue() {
		return queue;
	}

	public Partner getPartner() {
		return partner;
	}

	public TimeAmount getCharge() {
		return charge;
	}

    @Override
    public String toString() {
        return "PartnerCharge{" +
                "id=" + id +
                ", partner=" + partner +
                ", charge=" + charge +
                '}';
    }
}
