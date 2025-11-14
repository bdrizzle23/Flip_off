package com.flippingplugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Grand Exchange buy limits for items
 * Data sourced from OSRS Wiki
 */
public class BuyLimitData
{
	private static final Map<Integer, Integer> BUY_LIMITS = new HashMap<>();

	static
	{
		// Popular flipping items with their buy limits
		// You can expand this list as needed

		// Runes
		BUY_LIMITS.put(556, 25000); // Air rune
		BUY_LIMITS.put(557, 25000); // Water rune
		BUY_LIMITS.put(558, 25000); // Earth rune
		BUY_LIMITS.put(559, 25000); // Fire rune
		BUY_LIMITS.put(560, 25000); // Body rune
		BUY_LIMITS.put(561, 25000); // Cosmic rune
		BUY_LIMITS.put(562, 25000); // Chaos rune
		BUY_LIMITS.put(563, 25000); // Nature rune
		BUY_LIMITS.put(564, 25000); // Law rune
		BUY_LIMITS.put(565, 25000); // Death rune
		BUY_LIMITS.put(566, 25000); // Blood rune
		BUY_LIMITS.put(9075, 25000); // Astral rune

		// Dragon items
		BUY_LIMITS.put(1215, 8); // Dragon dagger
		BUY_LIMITS.put(1305, 8); // Dragon longsword
		BUY_LIMITS.put(3204, 8); // Dragon halberd
		BUY_LIMITS.put(11235, 8); // Dark bow
		BUY_LIMITS.put(4087, 8); // Dragon platelegs
		BUY_LIMITS.put(4585, 8); // Dragon plateskirt

		// Barrows items
		BUY_LIMITS.put(4708, 8); // Ahrim's robetop
		BUY_LIMITS.put(4710, 8); // Ahrim's robeskirt
		BUY_LIMITS.put(4716, 8); // Dharok's platebody
		BUY_LIMITS.put(4718, 8); // Dharok's platelegs
		BUY_LIMITS.put(4720, 8); // Dharok's greataxe
		BUY_LIMITS.put(4722, 8); // Guthan's platebody
		BUY_LIMITS.put(4724, 8); // Guthan's chainskirt
		BUY_LIMITS.put(4726, 8); // Guthan's warspear
		BUY_LIMITS.put(4728, 8); // Karil's leathertop
		BUY_LIMITS.put(4730, 8); // Karil's leatherskirt
		BUY_LIMITS.put(4732, 8); // Karil's crossbow
		BUY_LIMITS.put(4734, 8); // Torag's platebody
		BUY_LIMITS.put(4736, 8); // Torag's platelegs
		BUY_LIMITS.put(4738, 8); // Torag's hammers
		BUY_LIMITS.put(4745, 8); // Verac's brassard
		BUY_LIMITS.put(4747, 8); // Verac's plateskirt
		BUY_LIMITS.put(4749, 8); // Verac's flail

		// God Wars items
		BUY_LIMITS.put(11694, 8); // Armadyl godsword
		BUY_LIMITS.put(11696, 8); // Bandos godsword
		BUY_LIMITS.put(11698, 8); // Saradomin godsword
		BUY_LIMITS.put(11700, 8); // Zamorak godsword
		BUY_LIMITS.put(11826, 8); // Armadyl helmet
		BUY_LIMITS.put(11828, 8); // Armadyl chestplate
		BUY_LIMITS.put(11830, 8); // Armadyl chainskirt
		BUY_LIMITS.put(11832, 8); // Bandos chestplate
		BUY_LIMITS.put(11834, 8); // Bandos tassets

		// Whips and other popular items
		BUY_LIMITS.put(4151, 8); // Abyssal whip
		BUY_LIMITS.put(13652, 8); // Dragon claws
		BUY_LIMITS.put(11785, 8); // Armadyl crossbow
		BUY_LIMITS.put(20997, 8); // Twisted bow
		BUY_LIMITS.put(22804, 8); // Scythe of vitur
		BUY_LIMITS.put(21021, 8); // Elder maul

		// Commonly flipped resources
		BUY_LIMITS.put(2364, 10000); // Runite bar
		BUY_LIMITS.put(454, 10000); // Coal
		BUY_LIMITS.put(440, 10000); // Iron ore
		BUY_LIMITS.put(1623, 5000); // Uncut sapphire
		BUY_LIMITS.put(1621, 5000); // Uncut emerald
		BUY_LIMITS.put(1619, 5000); // Uncut ruby
		BUY_LIMITS.put(1617, 5000); // Uncut diamond
		BUY_LIMITS.put(6571, 5000); // Uncut dragonstone

		// Potions (4 dose)
		BUY_LIMITS.put(2436, 1000); // Super attack(4)
		BUY_LIMITS.put(2440, 1000); // Super strength(4)
		BUY_LIMITS.put(2444, 1000); // Ranging potion(4)
		BUY_LIMITS.put(3024, 1000); // Super restore(4)
		BUY_LIMITS.put(2448, 1000); // Magic potion(4)
		BUY_LIMITS.put(2452, 1000); // Super defence(4)

		// Food
		BUY_LIMITS.put(385, 13000); // Shark
		BUY_LIMITS.put(7946, 13000); // Monkfish
		BUY_LIMITS.put(379, 13000); // Lobster

		// Logs
		BUY_LIMITS.put(1511, 25000); // Logs
		BUY_LIMITS.put(1521, 25000); // Oak logs
		BUY_LIMITS.put(1519, 25000); // Willow logs
		BUY_LIMITS.put(1517, 25000); // Maple logs
		BUY_LIMITS.put(1513, 25000); // Magic logs
		BUY_LIMITS.put(1515, 25000); // Yew logs
	}

	/**
	 * Get the GE buy limit for an item
	 * @param itemId The item ID
	 * @return The buy limit, or -1 if unknown
	 */
	public static int getBuyLimit(int itemId)
	{
		return BUY_LIMITS.getOrDefault(itemId, -1);
	}

	/**
	 * Check if we have buy limit data for an item
	 */
	public static boolean hasBuyLimit(int itemId)
	{
		return BUY_LIMITS.containsKey(itemId);
	}
}
