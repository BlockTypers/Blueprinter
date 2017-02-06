package com.blocktyper.blueprinter.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blocktyper.blueprinter.Layout;
import com.blocktyper.v1_1_8.helpers.ComplexMaterial;

public class ConstructionReceipt {
	Layout layout;
	int x;
	int y;
	int z;
	int playerX;
	int playerY;
	int playerZ;
	String world;
	String uuid;
	boolean showing = true;

	private ComplexMaterial replacedComplexMaterial;
	private Map<Character, ComplexMaterial> symbolMap = new HashMap<>();

	private List<BlockChange> changes;

	public Layout getLayout() {
		return layout;
	}

	public void setLayout(Layout layout) {
		this.layout = layout;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public int getPlayerX() {
		return playerX;
	}

	public void setPlayerX(int playerX) {
		this.playerX = playerX;
	}

	public int getPlayerY() {
		return playerY;
	}

	public void setPlayerY(int playerY) {
		this.playerY = playerY;
	}

	public int getPlayerZ() {
		return playerZ;
	}

	public void setPlayerZ(int playerZ) {
		this.playerZ = playerZ;
	}

	public String getWorld() {
		return world;
	}

	public void setWorld(String world) {
		this.world = world;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public ComplexMaterial getReplacedComplexMaterial() {
		return replacedComplexMaterial;
	}

	public void setReplacedComplexMaterial(ComplexMaterial replacedComplexMaterial) {
		this.replacedComplexMaterial = replacedComplexMaterial;
	}

	public Map<Character, ComplexMaterial> getSymbolMap() {
		return symbolMap;
	}

	public void setSymbolMap(Map<Character, ComplexMaterial> symbolMap) {
		this.symbolMap = symbolMap;
	}

	public void addChange(BlockChange change) {
		if (changes == null) {
			changes = new ArrayList<>();
		}
		changes.add(change);
	}

	public List<BlockChange> getChanges() {
		return changes;
	}

	public void setChanges(List<BlockChange> changes) {
		this.changes = changes;
	}

	public boolean isShowing() {
		return showing;
	}

	public void setShowing(boolean showing) {
		this.showing = showing;
	}
}
