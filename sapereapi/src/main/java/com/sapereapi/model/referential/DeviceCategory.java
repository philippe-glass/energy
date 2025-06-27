package com.sapereapi.model.referential;

import java.util.ArrayList;
import java.util.List;

import com.sapereapi.model.OptionItem;

public enum DeviceCategory {
	 // Consumer categories
	 WATER_HEATING("Water Heating", false, true)
	,HEATING("Heating", false, true)
	,COOKING("Cooking", false, true)
	,SHOWERS("Showers", false, true)
	,WASHING_DRYING("Washing/drying", false, true)
	,LIGHTING("Lighting", false, true)
	,AUDIOVISUAL("Audiovisual", false, true)
	,COLD_APPLIANCES("Cold applicances", false, true)
	,ICT("ICT", false, true)
	,OTHER("Other", false, true)
	,UNKNOWN("Unknown", false, true)
	,ELECTRICAL_PANEL("Electrical panel", false, true)
	// Producer categories
	,EXTERNAL_ENG("External supply", true, false)
	,WIND_ENG("Wind", true, false)
	,SOLOR_ENG("Solar", true, false)
	,BIOMASS_ENG("Biomass", true, false)
	,HYDRO_ENG("Hydro", true, false)
	,BATTERY_ENG("Battery", true, false)
	// Hybrid categories
	,HYBRID("Hybrid", true, true)
	;

	private String label;
	private boolean isProducer;
	private boolean isConsumer;

	DeviceCategory(String _label, boolean _isProducer, boolean _isConsumer) {
		this.label = _label;
		this.isProducer = _isProducer;
		this.isConsumer = _isConsumer;
	}

	public String getLabel() {
		return label;
	}

	public boolean isProducer() {
		return isProducer;
	}

	public boolean isConsumer() {
		return isConsumer;
	}

	public OptionItem getOptionItem() {
		return new OptionItem(this.name(), label);
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

	public static List<OptionItem> getOptionList(boolean isProducer, boolean isConsumer) {
		List<OptionItem> result = new ArrayList<OptionItem>();
		for (DeviceCategory item : DeviceCategory.values()) {
			if(item.isProducer() == isProducer && item.isConsumer() == isConsumer) {
				result.add(item.getOptionItem());
			}
		}
		return result;
	}
}
