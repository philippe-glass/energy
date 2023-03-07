package com.sapereapi.model;

import eu.sapere.middleware.agent.SapereAgent;

public class Service {
	
	private String name;
	private String[] input ;
	private String[] output;
	private String url;
	private String appid;

	public Service(String name, String[] input, String[] output, String url,String appid) {
		super();
		this.name = name;
		this.input = input;
		this.output = output;
		this.url = url;
		this.appid = appid;
	}

	public Service() {
		super();
	}

	public Service(SapereAgent service) {
		super();
		this.name = service.getAgentName();
		this.input = service.getInput();
		this.output = service.getOutput();
		this.url = service.getUrl();

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getInput() {
		return input;
	}

	public void setInput(String[] input) {
		this.input = input;
	}

	public String[] getOutput() {
		return output;
	}

	public void setOutput(String[] output) {
		this.output = output;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getAppid() {
		return appid;
	}

	public void setAppid(String appid) {
		this.appid = appid;
	}
	
}
