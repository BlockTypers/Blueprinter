package com.blocktyper.blueprinter.listeners;

import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import com.blocktyper.blueprinter.BlueprinterPlugin;
import com.blocktyper.blueprinter.BuildException;
import com.blocktyper.blueprinter.Layout;
import com.blocktyper.blueprinter.LocalizedMessageEnum;

public abstract class LayoutBaseListener implements Listener{
	protected BlueprinterPlugin plugin;

	public LayoutBaseListener(BlueprinterPlugin plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	
	
	
	protected void validateMaterialAmounts(Layout layout, Player player) throws BuildException {

		if (!layout.requireMats()) {
			return;
		}
		
		for (Material material : layout.getRequirements().keySet()) {
			int amountRequired = layout.getRequirements().get(material);
			int amountFound = 0;
			if (layout.isRequireMatsInBag()) {
				amountFound = getAmountOfMaterialInBag(player, material);
			}else if (layout.isRequireMatsLoaded()) {
				amountFound = layout.getSupplies().get(material);
			}
			if (amountFound < amountRequired) {
				String missingRequiredMaterials = LocalizedMessageEnum.MISSING_REQUIRED_MATERIALS.getKey();
				String rightClickToViewRequirements = LocalizedMessageEnum.RIGHT_CLICK_TO_VIEW_REQUIREMENTS.getKey();
				throw new BuildException(Arrays.asList(missingRequiredMaterials, rightClickToViewRequirements));
			}
		}
	}
	
	protected int getAmountOfMaterialInBag(Player player, Material material){
		Integer currentAmountFound = 0;

		if (player.getInventory() != null && player.getInventory().getContents() != null) {
			for (ItemStack itemStack : player.getInventory().getContents()) {
				if (itemStack != null && itemStack.getType() == material && Layout.itemIsSuitableForLoading(itemStack)) {
					currentAmountFound += itemStack.getAmount();
				}
			}
		}
		return currentAmountFound;
	}
	
	
	protected void spendMaterialsInBag(Layout layout, Player player) {

		if (!layout.isRequireMatsInBag()) {
			return;
		}

		for (Material material : layout.getRequirements().keySet()) {
			int amountRequired = layout.getRequirements().get(material);

			if (player.getInventory() != null && player.getInventory().getContents() != null) {
				HashMap<Integer, ? extends ItemStack> materialsInBag = player.getInventory().all(material);
				if (materialsInBag != null) {
					for (ItemStack itemStack : player.getInventory().getContents()) {
						if(itemStack == null || itemStack.getType() != material){
							continue;
						}
						
						ItemStack itemOfCurrentType = itemStack;
						if (itemOfCurrentType.getItemMeta() == null
								|| (itemOfCurrentType.getItemMeta().getDisplayName() == null
										&& (itemOfCurrentType.getItemMeta().getLore() == null
												|| itemOfCurrentType.getItemMeta().getLore().isEmpty()))) {

							if (amountRequired >= itemOfCurrentType.getAmount()) {
								amountRequired -= itemOfCurrentType.getAmount();
								itemOfCurrentType.setAmount(0);
								player.getInventory().remove(itemOfCurrentType);
							} else {
								itemOfCurrentType.setAmount(itemOfCurrentType.getAmount() - amountRequired);
								amountRequired = 0;
							}
						}

						if (amountRequired < 1) {
							break;
						}
					}

				}
			}
		}
	}
}
