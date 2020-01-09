package com.chromaclypse.market;

import java.util.HashMap;
import java.util.Locale;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;

public class Market {
	private final Plugin handle;
	private final NamespacedKey MARKET_DATA;
	private final ShopData.Serializer marketSerializer = new ShopData.Serializer();
	
	public Market(Plugin handle) {
		this.handle = handle;
		MARKET_DATA = new NamespacedKey(handle, "market_data");
	}
	
	public Plugin getPlugin() {
		return handle;
	}
	
	public ShopData getCheckout(Block location) {
		BlockState state = location.getState();
		
		if(state instanceof Sign) {
			Sign s = (Sign) state;
			
			ShopData shop = s.getPersistentDataContainer().get(MARKET_DATA, marketSerializer);
			if(shop != null) {
				return shop;
			}
		}
		
		return null;
	}
	
	public ShopData unsafeGetCheckout(Block location) {
		ShopData shop = getCheckout(location);
		
		if(shop != null) {
			return shop;
		}
		
		throw new IllegalArgumentException("No market at block");
	}
	
	public void removeCheckout(Block location) {
		BlockState state = location.getState();
		
		if(state instanceof Sign) {
			Sign s = (Sign) state;
			
			if(s.getPersistentDataContainer().has(MARKET_DATA, marketSerializer)) {
				s.getPersistentDataContainer().remove(MARKET_DATA);
				s.update();
				return;
			}
		}
		
		throw new IllegalArgumentException("No market at block");
	}
	
	public void createCheckout(Block location, int stock) {
		BlockState state = location.getState();
		
		if(state instanceof Sign) {
			Sign s = (Sign) state;
			
			if(!s.getPersistentDataContainer().has(MARKET_DATA, marketSerializer)) {
				ShopValue value = getShopValue(s);
				ShopData shop = new ShopData();
				shop.capacity = stock * 2;
				shop.stock = stock;
				shop.initialBuy = (float) value.buy;
				shop.initialSell = (float) value.sell;
				
				s.getPersistentDataContainer().set(MARKET_DATA, marketSerializer, shop);
				s.update();
				return;
			}
			
			throw new IllegalArgumentException("A market already exists here!");
		}
		
		throw new IllegalArgumentException("That is not a sign!");
	}
	
	private static int hundreds(double val) {
		return (int)Math.round(val * 100);
	}
	
	private static class ShopValue {
		public double buy = 0;
		public double sell = 0;
		
		public void applyScale(double scale) {
			int parts = buy > 0 && sell > 0 ? 2 : 1;
			double total = buy + sell;
			
			double diff = hundreds(total / parts * (scale - 1)) / 100.0;
			double newBuy = buy;
			double newSell = sell;
			
			if(buy > 0) {
				newBuy += diff;
				if(newBuy <= 0) {
					return;
				}
			}
			
			if(sell > 0) {
				newSell += diff;
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
	
	private HashMap<Location, Integer> tasks = new HashMap<>();
	
	public void updated(Location location, ShopData checkout) {
		BlockState state = location.getBlock().getState();
		
		Integer task = tasks.get(location);
		if(task != null) {
			handle.getServer().getScheduler().cancelTask(task);
		}
		
		if(state instanceof Sign) {
			tasks.put(location, handle.getServer().getScheduler().scheduleSyncDelayedTask(handle, () -> {
				if(state instanceof Sign) {
					updatePrice((Sign)state, checkout);
				}
				tasks.remove(location);
			}, 2 * 60 * 20));
			
			((Sign)state).getPersistentDataContainer().set(MARKET_DATA, marketSerializer, checkout);
			((Sign)state).update();
		}
	}
	
	private void updatePrice(Sign sign, ShopData checkout) {
		float percentFilled = checkout.stock / (float)checkout.capacity;
		float deviation = Math.abs(percentFilled * 2 - 1);
		
		if(deviation < .5f)
			return;
	
		ShopValue val = getShopValue(sign);
		
		double scale = 1.1;
		
		if(checkout.stock * 2 > checkout.capacity) {
			scale = 1 / scale;
		}
		
		checkout.stock = checkout.capacity / 2;
		
		val.applyScale(scale);

		int buy = hundreds(val.buy);
		int sell = hundreds(val.sell);
		
		
		int b1 = buy / 100;
		int b2 = buy % 100;
		int s1 = sell / 100;
		int s2 = sell % 100;
		
		String buyline = "";
		
		if(buy > 0) {
			buyline += "B " + b1;
			
			if(b2 > 0) {
				buyline += "." + b2 / 10;
				if(b2 % 10 > 0) {
					buyline += b2 % 10;
				}
			}
			if(sell > 0) {
				buyline += " : ";
			}
		}
		
		if(sell > 0) {
			buyline += "S " + s1;
			
			if(s2 > 0) {
				buyline += "." + s2 / 10;
				if(s2 % 10 > 0) {
					buyline += s2 % 10;
				}
			}
		}
		
		sign.setLine(2, buyline);
		
		sign.getPersistentDataContainer().set(MARKET_DATA, marketSerializer, checkout);
		sign.update();
	}
}
