package com.chromaclypse.market;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType;

public class TransactionListener implements Listener {
	
	private Market handle;
	
	public TransactionListener(Market handle) {
		this.handle = handle;
	}
	
	@EventHandler
	public void onTransaction(TransactionEvent transaction) {
		Checkout checkout = handle.getCheckout(transaction.getSign().getLocation());
		
		if(checkout == null)
			return;
		
		if(transaction.getTransactionType() == TransactionType.BUY) {
			checkout.salesToPlayer += 1;
		}
		else {
			checkout.purchasesFromPlayer += 1;
		}
	}
}
