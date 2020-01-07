package com.chromaclypse.market;

import java.util.Locale;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
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
		BlockData data = location.getBlockData();
		
		if(data instanceof Sign) {
			Sign s = (Sign) data;
			
			ShopData shop = s.getPersistentDataContainer().get(MARKET_DATA, marketSerializer);
			if(shop != null) {
				return shop;
			}
		}
		
		throw new IllegalArgumentException("No market at block");
	}
	
	
	
	public void removeCheckout(Block location) {
		BlockData data = location.getBlockData();
		
		if(data instanceof Sign) {
			Sign s = (Sign) data;
			
			if(s.getPersistentDataContainer().has(MARKET_DATA, marketSerializer)) {
				s.getPersistentDataContainer().remove(MARKET_DATA);
				s.update();
				return;
			}
		}
		
		throw new IllegalArgumentException("No market at block");
	}
	
	public void createCheckout(Block location, int stock) {
		BlockData data = location.getBlockData();
		
		if(data instanceof Sign) {
			Sign s = (Sign) data;
			
			if(!s.getPersistentDataContainer().has(MARKET_DATA, marketSerializer)) {
				ShopValue value = getShopValue(s);
				ShopData shop = new ShopData();
				shop.capacity = stock * 2;
				shop.stock = stock * 2;
				shop.initialBuy = (float) value.buy;
				shop.initialSell = (float) value.sell;
				
				s.getPersistentDataContainer().set(MARKET_DATA, marketSerializer, shop);
				s.update();
			}
			
			throw new IllegalArgumentException("A market already exists here!");
		}
		
		throw new IllegalArgumentException("That is not a sign!");
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
	
	public void updatePrice(Location location, ShopData checkout) {
		float percentFilled = checkout.stock / (float)checkout.capacity;
		float deviation = Math.abs(percentFilled * 2 - 1);
		
		if(deviation < .5f)
			return;
		
		BlockState state = location.getBlock().getState();
		
		if(state instanceof Sign) {
			Sign sign = (Sign)state;
			ShopValue val = getShopValue(sign);
			
			double scale = 1.1;
			
			if(checkout.stock * 2 > checkout.capacity) {
				scale = 1 / scale;
			}
			
			checkout.stock = checkout.capacity / 2;
			
			val.applyScale(scale);
			
			sign.setLine(2, "B " + val.buy + " : S " + val.sell);
			
			sign.update();
		}
	}
}
