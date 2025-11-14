package com.flippingplugin;

public class TaxCalculator
{
	private static final double TAX_RATE = 0.02; // 2%
	private static final int TAX_CAP = 5_000_000; // 5M cap

	/**
	 * Calculates the GE tax for a given sell price
	 * @param sellPrice The price the item is being sold for
	 * @return The tax amount (capped at 5M)
	 */
	public static int calculateTax(int sellPrice)
	{
		int tax = (int) Math.ceil(sellPrice * TAX_RATE);
		return Math.min(tax, TAX_CAP);
	}

	/**
	 * Calculates profit after tax
	 * @param buyPrice The purchase price
	 * @param sellPrice The selling price
	 * @return Profit after deducting GE tax
	 */
	public static int calculateProfitAfterTax(int buyPrice, int sellPrice)
	{
		int rawProfit = sellPrice - buyPrice;
		int tax = calculateTax(sellPrice);
		return rawProfit - tax;
	}

	/**
	 * Calculates ROI percentage after tax
	 * @param buyPrice The purchase price
	 * @param sellPrice The selling price
	 * @return ROI as a percentage
	 */
	public static double calculateROI(int buyPrice, int sellPrice)
	{
		if (buyPrice <= 0)
		{
			return 0;
		}
		int profitAfterTax = calculateProfitAfterTax(buyPrice, sellPrice);
		return (profitAfterTax / (double) buyPrice) * 100.0;
	}
}
