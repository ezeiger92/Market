package com.chromaclypse.market;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;

import com.chromaclypse.api.Log;
import com.chromaclypse.market.Storage.Register;

public class Market {
	
	private Plugin handle;
	private Storage data = new Storage();
	private final HashMap<Location, Checkout> checkouts = new HashMap<>();
	
	public Market(Plugin handle) {
		this.handle = handle;
		data.init(handle);
		
		for(Register r : data.registers) {
			checkouts.put(new Location(handle.getServer().getWorld(r.world), r.x, r.y, r.z), new Checkout(r.capacity));
		}
	}
	
	public Plugin getPlugin() {
		return handle;
	}
	
	public Map<Location, Checkout> getCheckouts() {
		return checkouts;
	}
	
	public Checkout getCheckout(Location location) {
		return checkouts.get(location);
	}
	
	public void removeCheckout(Location location) {
		Checkout found = checkouts.remove(location);
		
		if(found != null) {
			for(Register r : data.registers) {
				if(r.x == location.getBlockX() && r.z == location.getBlockZ() && r.y == location.getBlockY()
						&& r.world.equalsIgnoreCase(location.getWorld().getName())) {
					int size = data.registers.size();
					
					if(size == 1) {
						data.registers.clear();
					}
					else {
						Register last = data.registers.get(size - 1);
						r.x = last.x;
						r.y = last.y;
						r.z = last.z;
						r.world = last.world;
						data.registers.remove(size - 1);
					}
				}
			}
			data.save(handle);
		}
	}
	
	public void save() {
		data.save(handle);
	}
	
	public void createCheckout(Location location, int stock) {
		Checkout found = checkouts.get(location);
		
		if(found == null) {
			Register r = new Register();
			r.x = location.getBlockX();
			r.y = location.getBlockY();
			r.z = location.getBlockZ();
			r.world = location.getWorld().getName();
			r.capacity = stock * 2;
			
			data.registers.add(r);
			data.save(handle);
			
			// avoid nasty after-the-fact location editing and fractional differences in one go!
			checkouts.put(new Location(location.getWorld(), r.x, r.y, r.z), new Checkout(r.capacity));
		}
	}
	
	private static class ShopValue {
		public double buy = 0;
		public double sell = 0;
		
		public void applyScale(double scale) {
			int parts = buy > 0 && sell > 0 ? 2 : 1;
			double total = buy + sell;
			
			double diff = Math.round(total / parts * (scale - 1) * 100) / 100.0;
			double newBuy = buy;
			double newSell = sell;
			
			if(buy > 0) {
				newBuy += diff;
				Log.info("newBuy: " + newBuy);
				if(newBuy <= 0) {
					return;
				}
			}
			
			if(sell > 0) {
				newSell += diff;
				Log.info("newSell: " + newSell);
				if(newSell <= 0) {
					return;
				}
			}
			
			buy = newBuy;
			sell = newSell;
		}
	}
	
	private ShopValue getShopValue(Sign sign) {
		String[] parts = sign.getLine(2).toLowerCase(Locale.ENGLISH).split(":", 2);
		ShopValue val = new ShopValue();
		
		try {
			val.buy = Double.parseDouble(parts[0].replaceAll("[^0-9.]", ""));
			if(parts.length > 1) {
				val.sell = Double.parseDouble(parts[1].replaceAll("[^0-9.]", ""));
			}
		}
		catch(NumberFormatException e) {
			return val;
		}

		// sell-first sign
		if(parts[0].contains("s")) {
			double temp = val.buy;
			val.buy = val.sell;
			val.sell = temp;
		}
		
		return val;
	}
	
	public void updatePrice(Location location, Checkout checkout) {
		float percentFilled = checkout.remaining / (float)checkout.capacity;
		float deviation = Math.abs(percentFilled * 2 - 1);
		
		Log.info("location: " + location);
		Log.info("deviation: " + deviation);
		if(deviation < .5f)
			return;
		
		BlockState state = location.getBlock().getState();
		
		if(state instanceof Sign) {
			Sign sign = (Sign)state;
			ShopValue val = getShopValue(sign);
			
			Log.info("buy | sell: " + val.buy + " | " + val.sell);
			double scale = 1.1;
			
			if(checkout.remaining * 2 > checkout.capacity) {
				scale = 1 / scale;
			}
			
			checkout.remaining = checkout.capacity / 2;
			
			val.applyScale(scale);
			Log.info("buy | sell: " + val.buy + " | " + val.sell);
			
			sign.setLine(2, "B " + val.buy + " : S " + val.sell);
			
			sign.update();
		}
	}
}
