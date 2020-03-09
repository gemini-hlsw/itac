package edu.gemini.tac.persistence.condition;

import edu.gemini.tac.persistence.phase1.sitequality.CloudCover;
import edu.gemini.tac.persistence.phase1.sitequality.ImageQuality;
import edu.gemini.tac.persistence.phase1.sitequality.SkyBackground;
import edu.gemini.tac.persistence.phase1.sitequality.WaterVapor;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.*;

/**
 * A grouping of various conditions that we need to able to control
 * the allocation of in order to ensure that we don't accept 100 proposals
 * that require, for example, perfect image quality when that condition
 * only exists long enough to execute 3 proposals.
 *
 * This looks crazy similar to Condition.  I wonder what the difference was meant to be.
 * 
 * @author ddawson
 *
 */
@Entity
@Table(name = "conditions")
public class ConditionBucket {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@Column(name = "name")
	private String name;
	
	@Enumerated( EnumType.STRING )
	@Column(name = "image_quality")
	private ImageQuality imageQuality;

	@Enumerated( EnumType.STRING )
	@Column(name = "sky_background")
	private SkyBackground skyBackground;

	@Enumerated( EnumType.STRING )
	@Column(name = "cloud_cover")
	private CloudCover cloudCover;

	@Enumerated( EnumType.STRING )
	@Column(name = "water_vapor")
	private WaterVapor waterVapor;

	@Column(name = "available_percentage")
	private Integer availablePercentage;
	
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public ImageQuality getImageQuality() {
		return imageQuality;
	}
	public void setImageQuality(final ImageQuality imageQuality) {
		this.imageQuality = imageQuality;
	}
	public SkyBackground getSkyBackground() {
		return skyBackground;
	}
	public void setSkyBackground(final SkyBackground skyBackground) {
		this.skyBackground = skyBackground;
	}
	public CloudCover getCloudCover() {
		return cloudCover;
	}
	public void setCloudCover(final CloudCover cloudCover) {
		this.cloudCover = cloudCover;
	}
	public WaterVapor getWaterVapor() {
		return waterVapor;
	}
	public void setWaterVapor(final WaterVapor waterVapor) {
		this.waterVapor = waterVapor;
	}
	public Integer getAvailablePercentage() {
		return availablePercentage;
	}
	public void setAvailablePercentage(final Integer availablePercentage) {
		this.availablePercentage = availablePercentage;
	}
	
	public String toString() {
		return new ToStringBuilder(this).
			append("name", name).
			append("imageQuality", imageQuality).
			append("skyBackground", skyBackground).
			append("cloudCover", cloudCover).
			append("waterVapor", waterVapor).
			append("availablePercentage", availablePercentage).
			toString();
	}
	public void setId(final Long id) {
		this.id = id;
	}
	public Long getId() {
		return id;
	}	
}
