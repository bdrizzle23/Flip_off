package com.flippingplugin;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a completed flip transaction (buy + sell)
 */
@Data
@Builder
public class FlipTransaction
{
	private int itemId;
	private String itemName;
	private int quantity;
	private int buyPrice;
	private int sellPrice;
	private long buyTime;
	private long sellTime;
	private int tax;
	private int profit;
	private double roi;

	/**
	 * Calculate profit from a flip
	 */
	public static FlipTransaction fromTrades(Trade buyTrade, Trade sellTrade)
	{
		int quantity = Math.min(buyTrade.getQuantity(), sellTrade.getQuantity());
		int buyPrice = buyTrade.getPrice();
		int sellPrice = sellTrade.getPrice();

		// Calculate GE tax (2% with 5M cap per item)
		int tax = TaxCalculator.calculateTax(sellPrice) * quantity;

		// Calculate profit
		long revenue = (long) sellPrice * quantity;
		long cost = (long) buyPrice * quantity;
		int profit = (int) (revenue - cost - tax);

		// Calculate ROI
		double roi = cost > 0 ? (profit * 100.0 / cost) : 0;

		return FlipTransaction.builder()
			.itemId(buyTrade.getItemId())
			.itemName(buyTrade.getItemName())
			.quantity(quantity)
			.buyPrice(buyPrice)
			.sellPrice(sellPrice)
			.buyTime(buyTrade.getTimestamp())
			.sellTime(sellTrade.getTimestamp())
			.tax(tax)
			.profit(profit)
			.roi(roi)
			.build();
	}

	/**
	 * Get the duration of this flip in hours
	 */
	public double getFlipDurationHours()
	{
		return (sellTime - buyTime) / (1000.0 * 60 * 60);
	}

	/**
	 * Get profit per hour
	 */
	public double getProfitPerHour()
	{
		double hours = getFlipDurationHours();
		return hours > 0 ? profit / hours : 0;
	}
}
