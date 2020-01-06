package com.chromaclypse.market;

public class Checkout {
	public Checkout(int capacity) {
		this.capacity = capacity;
		remaining = capacity / 2;
	}
	public int remaining;
	public final int capacity;
}
