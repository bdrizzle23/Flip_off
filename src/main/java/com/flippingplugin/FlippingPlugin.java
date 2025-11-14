package com.flippingplugin;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "Flipping Optimizer",
	description = "Advanced flipping plugin with pattern recognition, opportunity scoring, and smart alerts",
	tags = {"flipping", "grand exchange", "ge", "trading", "money making"}
)
public class FlippingPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private FlippingConfig config;

	@Inject
	private PriceDataService priceDataService;

	@Inject
	private PatternDetector patternDetector;

	@Inject
	private OpportunityScorer opportunityScorer;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Notifier notifier;

	@Inject
	private DiscordNotifier discordNotifier;

	@Inject
	private ScheduledExecutorService executorService;

	private FlippingPanel panel;
	private NavigationButton navButton;
	private ScheduledFuture<?> updateTask;
	private final List<FlippingOpportunity> currentOpportunities = new ArrayList<>();
	private final Map<Integer, Long> discordCooldowns = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		log.info("Flipping Optimizer started!");

		// Create and add panel
		panel = injector.getInstance(FlippingPanel.class);
		panel.setRefreshAction(this::refreshOpportunities);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		if (icon != null)
		{
			navButton = NavigationButton.builder()
				.tooltip("Flipping Optimizer")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

			if (config.enablePanel())
			{
				clientToolbar.addNavigation(navButton);
			}
		}

		// Start auto-refresh if enabled
		if (config.autoRefresh())
		{
			scheduleUpdates();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Flipping Optimizer stopped!");

		// Cancel scheduled updates
		if (updateTask != null && !updateTask.isCancelled())
		{
			updateTask.cancel(false);
		}

		// Remove panel
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}

		// Clear data
		currentOpportunities.clear();
		discordCooldowns.clear();
		priceDataService.clearCache();
		patternDetector.clearHistory();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			log.debug("Player logged in, fetching initial price data");
			executorService.schedule(this::refreshOpportunities, 5, TimeUnit.SECONDS);
		}
	}

	/**
	 * Schedules periodic updates based on config
	 */
	private void scheduleUpdates()
	{
		if (updateTask != null && !updateTask.isCancelled())
		{
			updateTask.cancel(false);
		}

		int intervalSeconds = config.updateInterval();
		updateTask = executorService.scheduleAtFixedRate(
			this::refreshOpportunities,
			0,
			intervalSeconds,
			TimeUnit.SECONDS
		);

		log.debug("Scheduled updates every {} seconds", intervalSeconds);
	}

	/**
	 * Refreshes flipping opportunities from OSRS Wiki API
	 */
	private void refreshOpportunities()
	{
		try
		{
			log.debug("Refreshing flipping opportunities...");

			// Fetch latest prices
			Map<Integer, ItemPriceData> priceData = priceDataService.fetchLatestPrices();
			if (priceData.isEmpty())
			{
				log.warn("No price data available");
				return;
			}

			List<FlippingOpportunity> opportunities = new ArrayList<>();

			for (Map.Entry<Integer, ItemPriceData> entry : priceData.entrySet())
			{
				int itemId = entry.getKey();
				ItemPriceData data = entry.getValue();

				// Filter by volume threshold
				if (!priceDataService.meetsVolumeThreshold(data, config.minVolume()))
				{
					continue;
				}

				// Filter by valid prices
				if (data.getLowPrice() <= 0 || data.getHighPrice() <= 0)
				{
					continue;
				}

				// Update pattern history
				patternDetector.updateHistory(data);

				// Detect pattern
				MarketPattern pattern = patternDetector.detectPattern(itemId, data);

				// Get item name
				String itemName = getItemName(itemId);

				// Create opportunity
				FlippingOpportunity opportunity = opportunityScorer.createOpportunity(
					itemId,
					itemName,
					data,
					pattern
				);

				// Filter by minimum ROI
				if (opportunity.getRoiPercentage() < config.minROI())
				{
					continue;
				}

				// Filter by minimum profit
				if (opportunity.getProfitAfterTax() < config.minProfit())
				{
					continue;
				}

				// Filter by grade
				if (opportunity.getGrade().getMinScore() < config.minGrade().getMinScore())
				{
					continue;
				}

				opportunities.add(opportunity);
			}

			// Sort by score (highest first)
			opportunities.sort(Comparator.comparingInt(FlippingOpportunity::getScore).reversed());

			// Limit to max opportunities
			int maxOpportunities = config.maxOpportunities();
			if (opportunities.size() > maxOpportunities)
			{
				opportunities = opportunities.subList(0, maxOpportunities);
			}

			// Update current opportunities
			synchronized (currentOpportunities)
			{
				currentOpportunities.clear();
				currentOpportunities.addAll(opportunities);
			}

			// Update panel
			if (panel != null)
			{
				panel.updateOpportunities(opportunities);
			}

			// Send notifications for high-quality opportunities
			if (config.enableNotifications())
			{
				sendNotifications(opportunities);
			}

			// Send Discord notifications if enabled
			if (config.enableDiscord())
			{
				sendDiscordNotifications(opportunities);
			}

			log.debug("Found {} flipping opportunities", opportunities.size());
		}
		catch (Exception e)
		{
			log.error("Error refreshing opportunities", e);
		}
	}

	/**
	 * Sends notifications for high-quality opportunities
	 */
	private void sendNotifications(List<FlippingOpportunity> opportunities)
	{
		OpportunityGrade notifyGrade = config.notifyGrade();

		for (FlippingOpportunity opp : opportunities)
		{
			// Flash crash always notifies if enabled
			if (config.flashCrashAlert() && opp.getPattern() == MarketPattern.FLASH_CRASH)
			{
				notifier.notify("Flash Crash Detected: " + opp.getItemName() +
					" - " + opp.getProfitAfterTax() + " gp profit");
				continue;
			}

			// Notify on pattern detection
			if (config.notifyPatterns() && opp.getPattern() != MarketPattern.NONE &&
				opp.getGrade().getMinScore() >= notifyGrade.getMinScore())
			{
				notifier.notify(opp.getPattern().getDisplayName() + ": " + opp.getItemName() +
					" [" + opp.getGrade().getDisplayName() + "]");
				continue;
			}

			// Notify on high grade
			if (opp.getGrade().getMinScore() >= notifyGrade.getMinScore())
			{
				notifier.notify("Opportunity: " + opp.getItemName() +
					" [" + opp.getGrade().getDisplayName() + "] - " +
					opp.getProfitAfterTax() + " gp");
			}
		}
	}

	/**
	 * Gets item name from item ID
	 */
	private String getItemName(int itemId)
	{
		ItemComposition itemComp = itemManager.getItemComposition(itemId);
		if (itemComp != null && itemComp.getName() != null)
		{
			return itemComp.getName();
		}
		return "Item #" + itemId;
	}

	/**
	 * Gets current opportunities (for external access)
	 */
	public List<FlippingOpportunity> getCurrentOpportunities()
	{
		synchronized (currentOpportunities)
		{
			return new ArrayList<>(currentOpportunities);
		}
	}

	/**
	 * Sends Discord notifications for high-quality opportunities
	 */
	private void sendDiscordNotifications(List<FlippingOpportunity> opportunities)
	{
		String webhookUrl = config.discordWebhookUrl();
		if (webhookUrl == null || webhookUrl.trim().isEmpty())
		{
			log.debug("Discord webhook URL not configured");
			return;
		}

		OpportunityGrade minGrade = config.discordMinGrade();
		long cooldownMillis = config.discordCooldown() * 60 * 1000L;
		long currentTime = System.currentTimeMillis();

		// Filter opportunities by grade and cooldown
		List<FlippingOpportunity> discordOpportunities = opportunities.stream()
			.filter(opp -> opp.getGrade().getMinScore() >= minGrade.getMinScore())
			.filter(opp -> {
				Long lastNotified = discordCooldowns.get(opp.getItemId());
				if (lastNotified == null || (currentTime - lastNotified) >= cooldownMillis)
				{
					discordCooldowns.put(opp.getItemId(), currentTime);
					return true;
				}
				return false;
			})
			.collect(Collectors.toList());

		if (discordOpportunities.isEmpty())
		{
			log.debug("No opportunities meet Discord notification criteria");
			return;
		}

		// Send notifications
		if (config.discordBatchNotifications())
		{
			// Send as batch summary
			discordNotifier.sendOpportunitySummary(webhookUrl, discordOpportunities, discordOpportunities.size());
		}
		else
		{
			// Send individual notifications
			for (FlippingOpportunity opp : discordOpportunities)
			{
				discordNotifier.sendOpportunity(webhookUrl, opp, opp.getItemName());
			}
		}

		log.debug("Sent {} Discord notification(s)", discordOpportunities.size());
	}

	@Provides
	FlippingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FlippingConfig.class);
	}
}
