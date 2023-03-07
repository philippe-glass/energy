package com.sapereapi.model.referential;

import java.util.ArrayList;
import java.util.List;

import com.sapereapi.model.energy.OptionItem;

public enum DeviceCategory {
	WATER_HEATING("Water Heating", false)
	,HEATING("Heating", false)
	,COOKING("Cooking", false)
	,SHOWERS("Showers", false)
	,WASHING_DRYING("Washing/drying", false)
	,LIGHTING("Lighting", false)
	,AUDIOVISUAL("Audiovisual", false)
	,COLD_APPLIANCES("Cold applicances", false)
	,ICT("ICT", false)
	,OTHER("Other", false)
	,UNKNOWN("Unknown", false)
	,ELECTRICAL_PANEL("Electrical panel", false)
	// Producer category
	,EXTERNAL_ENG("External supply", true)
	,WIND_ENG("Wind", true)
	,SOLOR_ENG("Solar", true)
	,BIOMASS_ENG("Biomass", true)
	,HYDRO_ENG("Hydro", true)
	;

	private String label;
	private boolean isProducer;

	DeviceCategory(String _label, boolean _isProducer) {
		this.label = _label;
		this.isProducer = _isProducer;
	}

	public String getLabel() {
		return label;
	}

	public boolean isProducer() {
		return isProducer;
	}

	public OptionItem getOptionItem() {
		return new OptionItem(this.name(), label);
	}

	public static DeviceCategory getByLabel(String label) {
		String label2 = (label == null) ? "" : label;
		for (DeviceCategory pLevel : DeviceCategory.values()) {
			if (pLevel.getLabel().equals(label2)) {
				return pLevel;
			}
		}
		return DeviceCategory.UNKNOWN;
	}

	public static DeviceCategory getByName(String name) {
		String name2 = (name == null) ? "" : name;
		for (DeviceCategory deviceCategory : DeviceCategory.values()) {
			if (deviceCategory.name().equals(name2)) {
				return deviceCategory;
			}
		}
		return DeviceCategory.UNKNOWN;
	}

	public static List<String> getLabels() {
		List<String> result = new ArrayList<String>();
		for (DeviceCategory pLevel : DeviceCategory.values()) {
			result.add(pLevel.getLabel());
		}
		return result;
	}

	public static List<DeviceCategory> getList() {
		List<DeviceCategory> result = new ArrayList<DeviceCategory>();
		for (DeviceCategory item : DeviceCategory.values()) {
			result.add(item);
		}
		return result;
	}

	public static List<OptionItem> getOptionList(boolean isProducer) {
		List<OptionItem> result = new ArrayList<OptionItem>();
		for (DeviceCategory item : DeviceCategory.values()) {
			if(item.isProducer == isProducer) {
				result.add(item.getOptionItem());
			}
		}
		return result;
	}
}
