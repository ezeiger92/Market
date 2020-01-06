package com.chromaclypse.market;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.chromaclypse.api.command.Context;

public class MarketCommand {
	
	private final Market handle;
	
	public MarketCommand(Market handle) {
		this.handle = handle;
	}
	
	public boolean create(Context context) {
		Player player = context.Player();
		int stock = context.GetArg(1, Integer::parseInt, "shop stock (int)");
		
		Block target = player.getTargetBlock(null, 8);
		handle.createCheckout(target.getLocation(), stock);
		return true;
	}
	
	public boolean remove(Context context) {
		Player player = context.Player();
		
		Block target = player.getTargetBlock(null, 8);
		handle.removeCheckout(target.getLocation());
		return true;
	}
	
	public boolean info(Context context) {
		Player player = context.Player();
		Block target = player.getTargetBlock(null, 8);
		Checkout found = handle.getCheckout(target.getLocation());
		
		if(found != null) {
			player.sendMessage("Checkout capacity: " + found.remaining + " / " + found.capacity); 
		}
		else {
			player.sendMessage("Not a checkout");
		}
		return true;
	}
	
	public boolean reload(Context context) {
		handle.getPlugin().onDisable();
		handle.getPlugin().onEnable();
		return true;
	}
}
