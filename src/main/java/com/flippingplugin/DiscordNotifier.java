package com.flippingplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Handles sending flipping opportunity notifications to Discord via webhooks.
 */
@Slf4j
@Singleton
public class DiscordNotifier {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);

    private final OkHttpClient httpClient;

    @Inject
    public DiscordNotifier(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Send a single flipping opportunity to Discord
     */
    public void sendOpportunity(String webhookUrl, FlippingOpportunity opportunity, String itemName) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.debug("Discord webhook URL not configured, skipping notification");
            return;
        }

        JsonObject payload = new JsonObject();

        // Create embed
        JsonObject embed = createEmbed(opportunity, itemName);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);

        sendWebhook(webhookUrl, payload);
    }

    /**
     * Send multiple flipping opportunities as a summary
     */
    public void sendOpportunitySummary(String webhookUrl, List<FlippingOpportunity> opportunities, int totalCount) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.debug("Discord webhook URL not configured, skipping notification");
            return;
        }

        JsonObject payload = new JsonObject();

        // Create main embed
        JsonObject mainEmbed = new JsonObject();
        mainEmbed.addProperty("title", "🔔 Flipping Opportunities Update");
        mainEmbed.addProperty("description", String.format("Found %d opportunities matching your criteria", totalCount));
        mainEmbed.addProperty("color", 0x00FF00); // Green
        mainEmbed.addProperty("timestamp", Instant.now().toString());

        JsonArray embeds = new JsonArray();
        embeds.add(mainEmbed);

        // Add up to 5 top opportunities as separate embeds
        int count = Math.min(5, opportunities.size());
        for (int i = 0; i < count; i++) {
            FlippingOpportunity opp = opportunities.get(i);
            String itemName = "Item #" + opp.getItemId(); // Will be replaced with actual name
            embeds.add(createEmbed(opp, itemName));
        }

        payload.add("embeds", embeds);
        sendWebhook(webhookUrl, payload);
    }

    /**
     * Create a Discord embed for a flipping opportunity
     */
    private JsonObject createEmbed(FlippingOpportunity opportunity, String itemName) {
        JsonObject embed = new JsonObject();

        // Set title and color based on grade
        embed.addProperty("title", String.format("Grade %s: %s", opportunity.getGrade(), itemName));
        embed.addProperty("color", getColorForGrade(opportunity.getGrade()));
        embed.addProperty("timestamp", Instant.now().toString());

        // Add fields
        JsonArray fields = new JsonArray();

        // Buy/Sell prices
        JsonObject priceField = new JsonObject();
        priceField.addProperty("name", "💰 Prices");
        priceField.addProperty("value", String.format("Buy: %s gp\nSell: %s gp",
            formatNumber(opportunity.getBuyPrice()),
            formatNumber(opportunity.getSellPrice())));
        priceField.addProperty("inline", true);
        fields.add(priceField);

        // Profit and ROI
        JsonObject profitField = new JsonObject();
        profitField.addProperty("name", "📈 Profit");
        profitField.addProperty("value", String.format("**%s gp**\nROI: %.2f%%",
            formatNumber(opportunity.getProfitAfterTax()),
            opportunity.getRoiPercentage()));
        profitField.addProperty("inline", true);
        fields.add(profitField);

        // Pattern
        JsonObject patternField = new JsonObject();
        patternField.addProperty("name", "📊 Pattern");
        patternField.addProperty("value", getPatternEmoji(opportunity.getPattern()) + " " +
            opportunity.getPattern().toString().replace("_", " "));
        patternField.addProperty("inline", true);
        fields.add(patternField);

        // Volume
        JsonObject volumeField = new JsonObject();
        volumeField.addProperty("name", "🔄 Volume");
        volumeField.addProperty("value", formatNumber(opportunity.getDailyVolume()) + " trades/day");
        volumeField.addProperty("inline", true);
        fields.add(volumeField);

        // Score
        JsonObject scoreField = new JsonObject();
        scoreField.addProperty("name", "⭐ Score");
        scoreField.addProperty("value", String.format("%d/100", opportunity.getScore()));
        scoreField.addProperty("inline", true);
        fields.add(scoreField);

        embed.add("fields", fields);

        // Footer
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Flipping Optimizer • RuneLite");
        embed.add("footer", footer);

        return embed;
    }

    /**
     * Get Discord embed color for opportunity grade
     */
    private int getColorForGrade(OpportunityGrade grade) {
        switch (grade) {
            case A_PLUS:
                return 0x00FF00; // Bright green
            case A:
                return 0x32CD32; // Lime green
            case B_PLUS:
                return 0x1E90FF; // Dodger blue
            case B:
                return 0x4169E1; // Royal blue
            case C_PLUS:
                return 0xFFD700; // Gold
            case C:
                return 0xFFA500; // Orange
            case D:
                return 0xFF8C00; // Dark orange
            case F:
                return 0xFF4500; // Red-orange
            default:
                return 0x808080; // Gray
        }
    }

    /**
     * Get emoji for market pattern
     */
    private String getPatternEmoji(MarketPattern pattern) {
        switch (pattern) {
            case FLASH_CRASH:
                return "⚡";
            case MANIPULATION:
                return "🎯";
            case DUMP:
                return "📉";
            case RECOVERY:
                return "📈";
            case VOLATILITY_SPIKE:
                return "🌊";
            case TREND_REVERSAL:
                return "🔄";
            case STABLE_SPREAD:
                return "➡️";
            default:
                return "📊";
        }
    }

    /**
     * Format number with commas
     */
    private String formatNumber(int number) {
        return NUMBER_FORMAT.format(number);
    }

    private String formatNumber(long number) {
        return NUMBER_FORMAT.format(number);
    }

    /**
     * Send a completed flip notification to Discord
     */
    public void sendFlipCompletion(String webhookUrl, FlipTransaction flip) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.debug("Discord webhook URL not configured, skipping flip notification");
            return;
        }

        JsonObject payload = new JsonObject();

        // Create embed for completed flip
        JsonObject embed = new JsonObject();
        embed.addProperty("title", "✅ Flip Completed: " + flip.getItemName());
        embed.addProperty("timestamp", Instant.now().toString());

        // Color based on profit
        if (flip.getProfit() > 0) {
            embed.addProperty("color", 0x00FF00); // Green for profit
        } else {
            embed.addProperty("color", 0xFF0000); // Red for loss
        }

        // Add fields
        JsonArray fields = new JsonArray();

        // Quantity
        JsonObject qtyField = new JsonObject();
        qtyField.addProperty("name", "📦 Quantity");
        qtyField.addProperty("value", formatNumber(flip.getQuantity()));
        qtyField.addProperty("inline", true);
        fields.add(qtyField);

        // Buy Price
        JsonObject buyField = new JsonObject();
        buyField.addProperty("name", "💰 Buy Price");
        buyField.addProperty("value", formatNumber(flip.getBuyPrice()) + " gp");
        buyField.addProperty("inline", true);
        fields.add(buyField);

        // Sell Price
        JsonObject sellField = new JsonObject();
        sellField.addProperty("name", "💵 Sell Price");
        sellField.addProperty("value", formatNumber(flip.getSellPrice()) + " gp");
        sellField.addProperty("inline", true);
        fields.add(sellField);

        // Total Profit
        JsonObject profitField = new JsonObject();
        profitField.addProperty("name", "📈 Total Profit");
        String profitText = (flip.getProfit() >= 0 ? "+" : "") + formatNumber(flip.getProfit()) + " gp";
        profitField.addProperty("value", profitText);
        profitField.addProperty("inline", true);
        fields.add(profitField);

        // ROI
        JsonObject roiField = new JsonObject();
        roiField.addProperty("name", "📊 ROI");
        roiField.addProperty("value", String.format("%.2f%%", flip.getRoi()));
        roiField.addProperty("inline", true);
        fields.add(roiField);

        // Tax Paid
        JsonObject taxField = new JsonObject();
        taxField.addProperty("name", "💸 GE Tax");
        taxField.addProperty("value", formatNumber(flip.getTax()) + " gp");
        taxField.addProperty("inline", true);
        fields.add(taxField);

        // Flip Duration
        JsonObject durationField = new JsonObject();
        durationField.addProperty("name", "⏱️ Duration");
        double hours = flip.getFlipDurationHours();
        String durationText;
        if (hours < 1) {
            durationText = String.format("%.0f minutes", hours * 60);
        } else if (hours < 24) {
            durationText = String.format("%.1f hours", hours);
        } else {
            durationText = String.format("%.1f days", hours / 24);
        }
        durationField.addProperty("value", durationText);
        durationField.addProperty("inline", true);
        fields.add(durationField);

        // Profit per hour
        JsonObject profitPerHourField = new JsonObject();
        profitPerHourField.addProperty("name", "💹 Profit/Hour");
        profitPerHourField.addProperty("value", formatNumber((int) flip.getProfitPerHour()) + " gp");
        profitPerHourField.addProperty("inline", true);
        fields.add(profitPerHourField);

        embed.add("fields", fields);

        // Footer
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Flipping Optimizer • Personal Trade");
        embed.add("footer", footer);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);

        sendWebhook(webhookUrl, payload);
    }

    /**
     * Send the webhook request to Discord
     */
    private void sendWebhook(String webhookUrl, JsonObject payload) {
        RequestBody body = RequestBody.create(JSON, payload.toString());
        Request request = new Request.Builder()
            .url(webhookUrl)
            .post(body)
            .build();

        try {
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.warn("Discord webhook failed with status {}: {}", response.code(), response.message());
            } else {
                log.debug("Discord notification sent successfully");
            }
            response.close();
        } catch (IOException e) {
            log.error("Failed to send Discord webhook", e);
        }
    }
}
