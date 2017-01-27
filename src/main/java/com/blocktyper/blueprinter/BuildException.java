package com.blocktyper.blueprinter;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;

public class BuildException extends Exception {
	private static final long serialVersionUID = 1L;

	List<String> messagesKeys;
	Map<String, Object[]> messagesParams;

	public BuildException(String messageKey) {
		super(messageKey);
		messagesKeys = Arrays.asList(messageKey);
	}

	public BuildException(String messageKey, Object[] params) {
		super(messageKey);
		messagesKeys = Arrays.asList(messageKey);
		messagesParams = new HashMap<>();
		messagesParams.put(messageKey, params);
	}

	public BuildException(List<String> messagesKeys) {
		super(messagesKeys != null && !messagesKeys.isEmpty() ? messagesKeys.get(0) : null);
		this.messagesKeys = messagesKeys;
	}

	public BuildException(List<String> messagesKeys, Map<String, Object[]> messagesParams) {
		super(messagesKeys != null && !messagesKeys.isEmpty() ? messagesKeys.get(0) : null);
		this.messagesKeys = messagesKeys;
		this.messagesParams = messagesParams;
	}

	public List<String> getMessageKeys() {
		return messagesKeys;
	}

	public void sendMessages(HumanEntity player, BlueprinterPlugin plugin) {
		if (messagesKeys != null && !messagesKeys.isEmpty()) {
			for (String messageKey : messagesKeys) {
				String message = plugin.getLocalizedMessage(messageKey, player);

				if (messagesParams != null && messagesParams.containsKey(messageKey)
						&& messagesParams.get(messageKey) != null && messagesParams.get(messageKey).length > 0) {
					message = new MessageFormat(message).format(messagesParams.get(messageKey));
				}

				player.sendMessage(message);
			}
		} else {
			player.sendMessage(ChatColor.RED + ":(");
		}
	}
}
