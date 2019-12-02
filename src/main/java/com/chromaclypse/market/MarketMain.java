package com.chromaclypse.market;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class MarketMain extends JavaPlugin {
	private Market impl;
	private int task_id;

	@Override
	public void onEnable() {
		impl = new Market(this);
		new TransactionListener(impl);
		MarketCommand commandImpl = new MarketCommand(impl);
		
		PluginCommand marketCommand = getCommand("market");
		marketCommand.setExecutor(commandImpl);
		marketCommand.setTabCompleter(commandImpl);
		
		long hour = 20 * 60 * 60;
		task_id = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
			
			for(Map.Entry<Location, Checkout> entry : impl.getCheckouts().entrySet()) {
				impl.updatePrice(entry.getKey(), entry.getValue());
			}
			impl.save();
		}, hour, hour);
	}
	
	@Override
	public void onDisable() {
		HandlerList.unregisterAll(this);
		getServer().getScheduler().cancelTask(task_id);
		impl = null;
	}
}
