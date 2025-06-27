package com.sapereapi.model.energy;

import java.io.Serializable;
import java.util.Date;

import com.sapereapi.util.UtilDates;

public class EnergyWithdrawal implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Date date;
	private double energyWH;
	private String issuer;
	private String recipient;

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public double getEnergyWH() {
		return energyWH;
	}

	public void setEnergyWH(double energyWH) {
		this.energyWH = energyWH;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public EnergyWithdrawal(Date date, double energyWH, String issuer, String recipient) {
		super();
		this.date = date;
		this.energyWH = energyWH;
		this.issuer = issuer;
		this.recipient = recipient;
	}

	public EnergyWithdrawal clone() {
		return new EnergyWithdrawal(date, energyWH, issuer, recipient);
	}

	public void addEnergy(Date date, double toAddWH) {
		if (toAddWH > 0) {
			this.date = date;
			this.energyWH += toAddWH;
		}
	}

	public void reset(Date date) {
		this.date = date;
		energyWH = 0.0;
	}

	public String toStringShort() {
		StringBuffer result = new StringBuffer();
		String sTime = UtilDates.format_time.format(date);
		result.append(UtilDates.df3.format(energyWH)).append("@").append(sTime);
		return result.toString();

	}

	@Override
	public String toString() {
		return toStringShort();
	}
}
