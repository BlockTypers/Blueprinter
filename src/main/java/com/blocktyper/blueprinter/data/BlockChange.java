package com.blocktyper.blueprinter.data;

import com.blocktyper.v1_1_8.helpers.ComplexMaterial;
import com.blocktyper.v1_1_8.helpers.Coord;

public class BlockChange {
	private Coord coord;
	private ComplexMaterial from;
	private ComplexMaterial to;
	private Character symbol;
	
	public Coord getCoord() {
		return coord;
	}
	public void setCoord(Coord coord) {
		this.coord = coord;
	}
	public ComplexMaterial getFrom() {
		return from;
	}
	public void setFrom(ComplexMaterial from) {
		this.from = from;
	}
	public ComplexMaterial getTo() {
		return to;
	}
	public void setTo(ComplexMaterial to) {
		this.to = to;
	}
	public Character getSymbol() {
		return symbol;
	}
	public void setSymbol(Character symbol) {
		this.symbol = symbol;
	}
}
