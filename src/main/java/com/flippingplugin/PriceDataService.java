package com.flippingplugin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class PriceDataService
{
	private static final String OSRS_WIKI_API = "https://prices.runescape.wiki/api/v1/osrs";
	private static final String LATEST_PRICES_ENDPOINT = "/latest";
	private static final String TIMESERIES_ENDPOINT = "/timeseries";
	private static final String USER_AGENT = "RuneLite Flipping Plugin";

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final Map<Integer, ItemPriceData> priceCache;
	private final Map<Integer, Long> lastUpdateTime;
	private static final long CACHE_DURATION_MS = 60_000; // 1 minute cache

	@Inject
	public PriceDataService(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.priceCache = new ConcurrentHashMap<>();
		this.lastUpdateTime = new ConcurrentHashMap<>();
	}

	/**
	 * Fetches latest price data for all items from OSRS Wiki API
	 * @return Map of item ID to price data
	 */
	public Map<Integer, ItemPriceData> fetchLatestPrices()
	{
		HttpUrl url = HttpUrl.parse(OSRS_WIKI_API + LATEST_PRICES_ENDPOINT);
		if (url == null)
		{
			log.error("Invalid URL for OSRS Wiki API");
			return new HashMap<>();
		}

		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				log.error("Failed to fetch prices: HTTP {}", response.code());
				return new HashMap<>();
			}

			String responseBody = response.body().string();
			log.info("API response body length: {} bytes", responseBody.length());

			JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
			if (jsonObject == null)
			{
				log.error("Failed to parse JSON response");
				return new HashMap<>();
			}

			JsonObject data = jsonObject.getAsJsonObject("data");
			if (data == null)
			{
				log.error("No 'data' field in API response. Response keys: {}", jsonObject.keySet());
				return new HashMap<>();
			}
			log.info("API data object has {} items", data.size());

			Map<Integer, ItemPriceData> prices = new HashMap<>();
			int itemsWithVolume = 0;
			boolean loggedSample = false;

			for (Map.Entry<String, JsonElement> entry : data.entrySet())
			{
				try
				{
					int itemId = Integer.parseInt(entry.getKey());
					JsonObject itemData = entry.getValue().getAsJsonObject();

					// Log a sample item to see what fields are available
					if (!loggedSample)
					{
						log.info("Sample API response for item {}: {}", itemId, itemData.toString());
						loggedSample = true;
					}

					ItemPriceData priceData = new ItemPriceData();
					priceData.setItemId(itemId);

					if (itemData.has("high"))
					{
						priceData.setHighPrice(itemData.get("high").getAsInt());
					}
					if (itemData.has("highPriceVolume"))
					{
						priceData.setHighPriceVolume(itemData.get("highPriceVolume").getAsLong());
						if (itemData.get("highPriceVolume").getAsLong() > 0)
						{
							itemsWithVolume++;
						}
					}
					if (itemData.has("low"))
					{
						priceData.setLowPrice(itemData.get("low").getAsInt());
					}
					if (itemData.has("lowPriceVolume"))
					{
						priceData.setLowPriceVolume(itemData.get("lowPriceVolume").getAsLong());
					}

					priceData.setTimestamp(System.currentTimeMillis());
					prices.put(itemId, priceData);

					// Update cache
					priceCache.put(itemId, priceData);
					lastUpdateTime.put(itemId, System.currentTimeMillis());
				}
				catch (Exception e)
				{
					log.debug("Error parsing item data: {}", e.getMessage());
				}
			}

			log.info("Fetched price data for {} items ({} with volume data)", prices.size(), itemsWithVolume);
			return prices;
		}
		catch (IOException e)
		{
			log.error("Error fetching price data", e);
			return new HashMap<>();
		}
	}

	/**
	 * Gets price data for a specific item, using cache if available
	 * @param itemId The item ID
	 * @return Price data or null if not available
	 */
	public ItemPriceData getPriceData(int itemId)
	{
		Long lastUpdate = lastUpdateTime.get(itemId);
		if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < CACHE_DURATION_MS)
		{
			return priceCache.get(itemId);
		}

		// Cache expired or not present, refresh all prices
		fetchLatestPrices();
		return priceCache.get(itemId);
	}

	/**
	 * Checks if an item meets the minimum volume threshold
	 * @param priceData The price data to check
	 * @param minVolume Minimum daily volume required
	 * @return true if volume threshold is met
	 */
	public boolean meetsVolumeThreshold(ItemPriceData priceData, long minVolume)
	{
		if (priceData == null)
		{
			return false;
		}

		long totalVolume = priceData.getHighPriceVolume() + priceData.getLowPriceVolume();

		// Log first few items to debug volume data
		if (priceData.getItemId() % 1000 == 0) // Sample logging
		{
			log.debug("Item {} volume: high={}, low={}, total={}, threshold={}",
				priceData.getItemId(),
				priceData.getHighPriceVolume(),
				priceData.getLowPriceVolume(),
				totalVolume,
				minVolume);
		}

		return totalVolume >= minVolume;
	}

	/**
	 * Clears the price cache
	 */
	public void clearCache()
	{
		priceCache.clear();
		lastUpdateTime.clear();
	}
}
