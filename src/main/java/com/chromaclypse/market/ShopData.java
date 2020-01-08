package com.chromaclypse.market;

import java.nio.ByteBuffer;

import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

public class ShopData {
	public float initialBuy;
	public float initialSell;
	public int capacity;
	public int stock;
	
	private int task = 0;
	
	public void touch(Market market, Location location) {
		if(task > 0) {
			market.getPlugin().getServer().getScheduler().cancelTask(task);
		}
		
		task = market.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(market.getPlugin(), () -> {
			market.updatePrice(location, this);
			task = 0;
		}, 5 * 20);
	}

	public static class Serializer implements PersistentDataType<byte[], ShopData> {

		@Override
		public Class<ShopData> getComplexType() {
			return ShopData.class;
		}

		@Override
		public Class<byte[]> getPrimitiveType() {
			return byte[].class;
		}

		@Override
		public ShopData fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
			ByteBuffer bb = ByteBuffer.wrap(primitive);
			ShopData data = new ShopData();
			
			data.initialBuy = bb.getFloat();
			data.initialSell = bb.getFloat();
			data.capacity = bb.getInt();
			data.stock = bb.getInt();
			
			return data;
		}

		@Override
		public byte[] toPrimitive(ShopData complex, PersistentDataAdapterContext context) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[32]);

			bb.putFloat(complex.initialBuy);
			bb.putFloat(complex.initialSell);
			bb.putInt(complex.capacity);
			bb.putInt(complex.stock);
			
			return bb.array();
		}
	}
}
