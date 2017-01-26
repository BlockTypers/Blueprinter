package com.blocktyper.blueprinter;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;

public class Layout {
	private List<String> floorNumbers;
	private Map<String, List<String>> rowsNumberPerFloor;
	private Map<String, Map<String, String>> floorNumberRowNumberRowMap;
	private Map<String, Material> matMap;
	
	public List<String> getFloorNumbers() {
		return floorNumbers;
	}
	public void setFloorNumbers(List<String> floorNumbers) {
		this.floorNumbers = floorNumbers;
	}
	public Map<String, List<String>> getRowsNumberPerFloor() {
		return rowsNumberPerFloor;
	}
	public void setRowsNumberPerFloor(Map<String, List<String>> rowsNumberPerFloor) {
		this.rowsNumberPerFloor = rowsNumberPerFloor;
	}
	public Map<String, Map<String, String>> getFloorNumberRowNumberRowMap() {
		return floorNumberRowNumberRowMap;
	}
	public void setFloorNumberRowNumberRowMap(Map<String, Map<String, String>> floorNumberRowNumberRowMap) {
		this.floorNumberRowNumberRowMap = floorNumberRowNumberRowMap;
	}
	public Map<String, Material> getMatMap() {
		return matMap;
	}
	public void setMatMap(Map<String, Material> matMap) {
		this.matMap = matMap;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((floorNumberRowNumberRowMap == null) ? 0 : floorNumberRowNumberRowMap.hashCode());
		result = prime * result + ((floorNumbers == null) ? 0 : floorNumbers.hashCode());
		result = prime * result + ((matMap == null) ? 0 : matMap.hashCode());
		result = prime * result + ((rowsNumberPerFloor == null) ? 0 : rowsNumberPerFloor.hashCode());
		return result;
	}
	
}
