package com.flippingplugin;

import lombok.Data;

@Data
public class ItemPriceData
{
	private int itemId;
	private int highPrice;
	private long highPriceVolume;
	private int lowPrice;
	private long lowPriceVolume;
	private long timestamp;
}
