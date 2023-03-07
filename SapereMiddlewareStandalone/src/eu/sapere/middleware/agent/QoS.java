package eu.sapere.middleware.agent;

import java.io.Serializable;

public class QoS implements Serializable{
	private static final long serialVersionUID = 201L;
	private double time; // second
	private double cost; // unit
	private double availability; // percentage/100

	public QoS() {
		time = 1;
		cost = 1;
		availability = 1;
	}

	public QoS(double time, double cost, double availability) {
		this.time = time;
		this.cost = cost;
		this.availability = availability;
	}

	public double getScore() {
		return (1 / time) + (1 / cost) + availability;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public double getAvailability() {
		return availability;
	}

	public void setAvailability(double availability) {
		this.availability = availability;
	}

}
