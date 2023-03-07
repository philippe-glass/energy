package com.sapereapi.model.energy;

public class MissingRequest {
	protected String issuer;
	protected Double power; // electric power in watts
	protected Boolean hasWarning;
	protected Integer warningDurationSec;

	public String getIssuer() {
		return issuer;
	}
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}
	public Double getPower() {
		return power;
	}
	public void setPower(Double power) {
		this.power = power;
	}
	public Boolean getHasWarning() {
		return hasWarning;
	}
	public void setHasWarning(Boolean hasWarning) {
		this.hasWarning = hasWarning;
	}
	public Integer getWarningDurationSec() {
		return warningDurationSec;
	}
	public void setWarningDurationSec(Integer warningDurationSec) {
		this.warningDurationSec = warningDurationSec;
	}
	public MissingRequest(String issuer, Double power, Boolean hasWarning, Integer warningDurationSec) {
		super();
		this.issuer = issuer;
		this.power = power;
		this.hasWarning = hasWarning;
		this.warningDurationSec = warningDurationSec;
	}
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(issuer).append(" ").append(power).append("W");
		if(warningDurationSec > 0) {
			result.append(", warningDurationSec : ").append(warningDurationSec);
		}
		return result.toString();
	}
}
