package com.blocktyper.blueprinter.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.blocktyper.blueprinter.Layout;
import com.blocktyper.blueprinter.LocalizedMessageEnum;
import com.blocktyper.v1_2_6.helpers.ComplexMaterial;
import com.blocktyper.v1_2_6.helpers.InvisHelper;

public class RequireMatsClickListener extends LayoutBaseListener {

	public static String REQUIRED_MATS_INVIS_PREFIX = "#BLUEPRINTER_REQUIRED_MATS";

	/*
	 * ON INVENTORY CLICK
	 */
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onInventoryClickEvent(InventoryClickEvent event) {

		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}

		if (event.getClickedInventory() == null) {
			return;
		}

		if (event.getInventory() != null && event.getInventory().getName() != null) {
			String inventoryName = event.getInventory().getName();
			inventoryName = InvisHelper.convertToVisibleString(inventoryName);
			if (inventoryName.startsWith(REQUIRED_MATS_INVIS_PREFIX)) {
				event.setCancelled(true);
				return;
			}
		}

		Player player = ((Player) event.getWhoClicked());

		ItemStack item = event.getCurrentItem();

		if (item == null || item.getAmount() > 1) {
			return;
		}

		Layout layout = plugin.getLayout(item);

		if (layout != null) {

			if (!layout.requireMats()) {
				return;
			}

			if (layout.isRequireMatsLoaded() && event.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)) {
				ItemStack cursor = event.getCursor();

				if (cursor == null) {
					return;
				} else if (!Layout.itemIsSuitableForLoading(cursor)) {
					return;
				}

				Byte cursorData = cursor.getData() != null ? cursor.getData().getData() : null;
				ComplexMaterial complexMaterial = new ComplexMaterial(cursor.getType(), cursorData);

				String matKey = complexMaterial.toString();

				if (!layout.getRequirements().containsKey(matKey)) {
					return;
				}

				int requiredAmount = layout.getRequirements().get(matKey);

				if (requiredAmount < 1) {
					return;
				}

				if (layout.getSupplies() == null) {
					layout.setSupplies(new HashMap<>());
				}

				if (!layout.getSupplies().containsKey(matKey)) {
					layout.getSupplies().put(matKey, 0);
				}

				Integer amountLoaded = layout.getSupplies().get(matKey);
				amountLoaded = amountLoaded != null ? amountLoaded : 0;

				int amountLeft = requiredAmount - amountLoaded;

				if (amountLeft < 1) {
					return;
				}

				if (amountLeft >= cursor.getAmount()) {
					amountLoaded = amountLoaded + cursor.getAmount();
					cursor.setAmount(0);
					player.setItemOnCursor(null);
				} else {
					amountLoaded = amountLoaded + amountLeft;
					cursor.setAmount(cursor.getAmount() - amountLeft);
					player.setItemOnCursor(cursor);
				}

				layout.getSupplies().put(matKey, amountLoaded);

				item = plugin.setLayout(item, layout);

				event.getClickedInventory().setItem(event.getSlot(), item);
				event.setCancelled(true);
				return;
			} else if (event.getClick().equals(ClickType.RIGHT)) {
				List<ItemStack> requiredItems = new ArrayList<>();
				for (String complexMaterialString : layout.getRequirements().keySet()) {
					ComplexMaterial complexMaterial = ComplexMaterial.fromString(complexMaterialString);
					int requiredAmount = layout.getRequirements().get(complexMaterialString);

					Integer amountObtained = getAmountObtained(player, layout, complexMaterial);

					amountObtained = amountObtained != null ? amountObtained : 0;
					short damage = 1;
					ItemStack requiredItem = new ItemStack(complexMaterial.getMaterial(), 1, damage,
							complexMaterial.getData());
					ItemMeta itemMeta = requiredItem.getItemMeta();

					if (itemMeta != null) {
						List<String> lore = Arrays.asList(amountObtained + "/" + requiredAmount);
						itemMeta.setLore(lore);
						requiredItem.setItemMeta(itemMeta);
					}

					if (requiredAmount <= amountObtained) {
						requiredItem.addUnsafeEnchantment(Enchantment.LUCK, 1);
						requiredItem.setItemMeta(itemMeta);
					}
					requiredItems.add(requiredItem);
				}

				int rows = (requiredItems.size() / 9) + (requiredItems.size() % 9 > 0 ? 1 : 0);

				String inventoryName = getLocalizedMessage(LocalizedMessageEnum.REQUIRED_MATERIALS.getKey(),
						event.getWhoClicked());
				inventoryName = InvisHelper.convertToInvisibleString(REQUIRED_MATS_INVIS_PREFIX)
						+ ChatColor.RESET + inventoryName;

				Inventory requiredMaterialsInventory = Bukkit.createInventory(null, rows * 9, inventoryName);

				for (ItemStack requiredItem : requiredItems) {
					requiredMaterialsInventory.addItem(requiredItem);
				}

				new BukkitRunnable() {
					@Override
					public void run() {
						player.closeInventory();
						player.openInventory(requiredMaterialsInventory);

					}
				}.runTaskLater(plugin, 1L);

				event.setCancelled(true);
				return;
			}
		}
	}

	private Integer getAmountObtained(Player player, Layout layout, ComplexMaterial complexMaterial) {
		Integer amountObtained = null;

		String matKey = complexMaterial.toString();

		if (layout.isRequireMatsLoaded()) {
			amountObtained = layout.getSupplies() != null && layout.getSupplies().get(matKey) != null
					? layout.getSupplies().get(matKey) : 0;
		} else {
			amountObtained = getPlayerHelper().getAmountOfMaterialInBag(player, complexMaterial, false);
		}

		return amountObtained;
	}

}
