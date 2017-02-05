package com.blocktyper.blueprinter;

public enum LocalizedMessageEnum {

	REQUIRED_MATERIALS("blueprinter-required-materials"),
	CONSTRUCTION_RECEIPT("blueprinter-construction-receipt"),
	IMPROPER_ORIENTATION("blueprinter-improper-orientation"),
	MISSING_REQUIRED_MATERIALS("blueprinter-missing-required-materials"),
	RIGHT_CLICK_TO_VIEW_REQUIREMENTS("blueprinter-right-click-to-view-requirements"),
	UNDEFINED_MATERIAL("blueprinter-undefined-material"),
	NON_AIR_OR_STATIONARY_WATER_BLOCK_DETECTED("blueprinter-non-air-or-stationary-water-block-detected"),
	UNDEFINED_BLOCK_DETECTED("blueprinter-undefined-block-detected"),
	MATERIAL_SWAP("blueprinter-material-swap"),
	RETURN("blueprinter-return"),
	TELEPORT("blueprinter-teleport"),
	HIDE("blueprinter-hide"),
	SHOW("blueprinter-show"),
	MISSING_WORLD("blueprinter-missing-world"),
	REQUIRE_MATS_IN_BAG("blueprinter-require-mats-in-bag"),
	REQUIRE_MATS_LOADED("blueprinter-require-mats-loaded"),
	BUILD_DOWN("blueprinter-build-down"),
	ALLOW_REPLACEMENT("blueprinter-allow-replacement"),
	HEIGHT("blueprinter-height");

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
