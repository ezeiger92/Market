package com.chromaclypse.market;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;

import com.chromaclypse.market.Storage.Register;

public class Market {
	
	private Plugin handle;
	private Random random;
	private Storage data = new Storage();
	private final HashMap<Location, Checkout> checkouts = new HashMap<>();
	
	public Market(Plugin handle) {
		this.handle = handle;
		data.init(handle);
		
		random = new Random(System.currentTimeMillis());
		
		for(Register r : data.registers) {
			checkouts.put(new Location(handle.getServer().getWorld(r.world), r.x, r.y, r.z), new Checkout());
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
		}
	}
	
	public void save() {
		data.save(handle);
	}
	
	public void createCheckout(Location location) {
		Checkout found = checkouts.get(location);
		
		if(found == null) {
			Register r = new Register();
			r.x = location.getBlockX();
			r.y = location.getBlockY();
			r.z = location.getBlockZ();
			r.world = location.getWorld().getName();
			
			data.registers.add(r);
			
			// avoid nasty after-the-fact location editing and fractional differences in one go!
			checkouts.put(new Location(location.getWorld(), r.x, r.y, r.z), new Checkout());
		}
	}
	
	public void updatePrice(Location location, Checkout checkout) {
		float saleFactor = (checkout.salesToPlayer - checkout.purchasesFromPlayer)
				/ Math.max(checkout.salesToPlayer + checkout.purchasesFromPlayer, 1.0f);
		
		if(Math.abs(saleFactor) < 0.05f)
			return;
		
		BlockState state = location.getBlock().getState();
		
		if(state instanceof Sign) {
			Sign sign = (Sign)state;
			
			String[] parts = sign.getLine(2).toLowerCase(Locale.ENGLISH).split(":", 2);
			
			double buy = 0.0;
			double sell = 0.0;
			
			try {
				buy = Double.parseDouble(parts[0].replaceFirst(".*\\d+(\\.\\d+)?.*", "$1"));
				if(parts.length > 1) {
					sell = Double.parseDouble(parts[1].replaceFirst(".*\\d+(\\.\\d+)?.*", "$1"));
				}
			}
			catch(NumberFormatException e) {
				return;
			}
			
			// sell-first sign
			if(parts[0].indexOf('s') > -1) {
				double temp = buy;
				buy = sell;
				sell = temp;
			}
			
			int delta = (int)(random.nextFloat() * 5) + 1;
			
			if(saleFactor < 0) {
				delta = -delta;
			}
			
			if(delta + sell <= 0) {
				return;
			}
			
			sign.setLine(2, "B " + buy + " : S " + sell);
		}
	}
}
