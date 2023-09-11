package com.sapereapi.db;

public class DBConfig {
	private String driverClassName = "";
	private String url = "";// "jdbc:mariadb://localhost/energy";
	private String user = "";
	private String password = "";

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public DBConfig(String driverClassName, String url, String user, String password) {
		super();
		this.driverClassName = driverClassName;
		this.url = url;
		this.user = user;
		this.password = password;
	}
}
