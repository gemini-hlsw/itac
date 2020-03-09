package edu.gemini.tac.persistence.bin;

import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.*;

/**
 *  DD:MM:SS.SS where DD is degrees -90 to 90.
 * 
 * @author ddawson
 *
 */
@Entity
@Table(name = "dec_bin_sizes")
public class DecBinSize {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@Column(name = "degrees")
	private Integer degrees;
	
	public String getDisplay() {
		return String.format("%02d", degrees);
	}
	
	public void setDegrees(Integer degrees) {
		this.degrees = degrees;
	}
	public Integer getDegrees() {
		return degrees;
	}
	
	public String toString() {
		return new ToStringBuilder(this).
			append("degrees", degrees).
			toString();
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}
}
