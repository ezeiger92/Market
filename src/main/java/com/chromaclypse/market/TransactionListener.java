package com.chromaclypse.market;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent.TransactionOutcome;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType;

public class TransactionListener implements Listener {
	
	private Market handle;
	
	public TransactionListener(Market handle) {
		this.handle = handle;
	}
	
	@EventHandler
	public void onTransaction(PreTransactionEvent transaction) {
		ShopData checkout = handle.getCheckout(transaction.getSign().getBlock());
		
		if(checkout == null)
			return;
		
		int amount = 0;
		for(ItemStack stack : transaction.getStock()) {
			amount += stack.getAmount();
		}
		
		if(transaction.getTransactionType() == TransactionType.BUY) {
			if(checkout.stock - amount < 0) {
				transaction.setCancelled(TransactionOutcome.NOT_ENOUGH_STOCK_IN_CHEST);
			}
		}
		else {
			if(checkout.stock + amount > checkout.capacity) {
				transaction.setCancelled(TransactionOutcome.NOT_ENOUGH_SPACE_IN_CHEST);
			}
		}
	}
	
	@EventHandler
	public void onTransaction(TransactionEvent transaction) {
		ShopData checkout = handle.getCheckout(transaction.getSign().getBlock());
		
		if(checkout == null)
			return;
		
		int amount = 0;
		for(ItemStack stack : transaction.getStock()) {
			amount += stack.getAmount();
		}
		
		if(transaction.getTransactionType() == TransactionType.BUY) {
			checkout.stock -= amount;
		}
		else {
			checkout.stock += amount;
		}
		
		handle.updated(transaction.getSign().getBlock().getLocation(), checkout);
	}
}
