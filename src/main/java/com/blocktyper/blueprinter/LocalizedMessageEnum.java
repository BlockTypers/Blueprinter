package com.blocktyper.blueprinter;

public enum LocalizedMessageEnum {

	REQUIRED_MATERIALS("blueprinter-required-materials"),
	IMPROPER_ORIENTATION("blueprinter-improper-orientation"),
	MISSING_REQUIRED_MATERIALS("blueprinter-missing-required-materials"),
	RIGHT_CLICK_TO_VIEW_REQUIREMENTS("blueprinter-right-click-to-view-requirements"),
	UNDEFINED_MATERIAL("blueprinter-undefined-material"),
	NON_AIR_OR_STATIONARY_WATER_BLOCK_DETECTED("blueprinter-non-air-or-stationary-water-block-detected"),
	UNDEFINED_BLOCK_DETECTED("blueprinter-undefined-block-detected");

	private String key;

	private LocalizedMessageEnum(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

}
