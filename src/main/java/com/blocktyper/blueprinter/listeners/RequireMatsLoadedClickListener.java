package com.blocktyper.blueprinter.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.blocktyper.blueprinter.BlueprinterPlugin;
import com.blocktyper.blueprinter.Layout;

public class RequireMatsLoadedClickListener implements Listener {

	private BlueprinterPlugin plugin;

	public RequireMatsLoadedClickListener(BlueprinterPlugin plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	/*
	 * ON INVENTORY CLICK
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onInventoryClickEvent(InventoryClickEvent event) {

		plugin.debugInfo("onInventoryClickEvent: " + event.getClick().name());

		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}

		if (event.getClickedInventory() == null) {
			plugin.debugInfo("clicked inventory was null");
			return;
		}

		Player player = ((Player) event.getWhoClicked());

		ItemStack item = event.getCurrentItem();

		if (item == null || item.getAmount() > 1) {
			return;
		}

		Layout layout = plugin.getLayout(item);

		if (layout != null) {

			if (!layout.isRequireMatsLoaded()) {
				return;
			}

			if (event.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)) {
				ItemStack cursor = event.getCurrentItem();
				if (cursor == null) {
					return;
				} else if (!Layout.itemIsSuitableForLoading(cursor)) {
					return;
				}

				if (!layout.getRequirements().containsKey(cursor.getType())) {
					return;
				}

				int requiredAmount = layout.getRequirements().get(cursor.getType());

				if (requiredAmount < 1) {
					return;
				}

				Integer amountLoaded = layout.getSupplies().get(cursor.getType());
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
				}

				layout.getSupplies().put(cursor.getType(), amountLoaded);

				item = plugin.setLayout(item, layout);

				event.getClickedInventory().setItem(event.getSlot(), item);
				event.setCancelled(true);
			} else if (event.getClick().equals(ClickType.RIGHT)) {
				List<ItemStack> requiredItems = new ArrayList<>();
				for (Material material : layout.getRequirements().keySet()) {
					int requiredAmount = layout.getRequirements().get(material);
					Integer amountLoaded = layout.getSupplies().get(material);
					amountLoaded = amountLoaded != null ? amountLoaded : 0;
					ItemStack requiredItem = new ItemStack(material);
					ItemMeta itemMeta = requiredItem.getItemMeta();
					List<String> lore = Arrays.asList(amountLoaded + "/" + requiredAmount);
					itemMeta.setLore(lore);
					requiredItem.setItemMeta(itemMeta);
					if (requiredAmount > amountLoaded) {
						requiredItem.addUnsafeEnchantment(Enchantment.LUCK, 1);
					}
					requiredItems.add(requiredItem);
				}

				int rows = (requiredItems.size() / 9) + (requiredItems.size() % 9 > 0 ? 1 : 0);

				Inventory requiredMaterialsInventory = Bukkit.createInventory(null, rows * 9, "Required Materials");

				for (ItemStack requiredItem : requiredItems) {
					requiredMaterialsInventory.addItem(requiredItem);
				}
				event.setCancelled(true);
			}
		}
	}

}
