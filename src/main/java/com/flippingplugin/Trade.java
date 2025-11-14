package com.flippingplugin;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a single Grand Exchange trade (buy or sell)
 */
@Data
@Builder
public class Trade
{
	private int itemId;
	private String itemName;
	private int quantity;
	private int price;
	private long timestamp;
	private TradeType type;
	private TradeState state;
	private int slot;
	private int currentQuantityInTrade;
	private int totalQuantityInTrade;

	public enum TradeType
	{
		BUY,
		SELL
	}

	public enum TradeState
	{
		EMPTY,
		PENDING,
		PARTIAL,
		COMPLETED,
		CANCELLED,
		ABORTED
	}

	/**
	 * Check if this trade is complete
	 */
	public boolean isComplete()
	{
		return state == TradeState.COMPLETED;
	}

	/**
	 * Check if this trade is active (pending or partial)
	 */
	public boolean isActive()
	{
		return state == TradeState.PENDING || state == TradeState.PARTIAL;
	}

	/**
	 * Get the total cost/revenue for this trade
	 */
	public long getTotalValue()
	{
		return (long) quantity * price;
	}
}
