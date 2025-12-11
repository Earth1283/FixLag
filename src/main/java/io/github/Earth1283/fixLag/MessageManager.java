package io.github.Earth1283.fixLag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MessageManager {

    private final JavaPlugin plugin;
    private FileConfiguration messagesConfig;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacySection();
        loadMessages();
    }

    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Update messages with missing keys
        try {
            java.io.InputStream defConfigStream = plugin.getResource("messages.yml");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defConfigStream));
                messagesConfig.setDefaults(defConfig);
                messagesConfig.options().copyDefaults(true);
                messagesConfig.save(messagesFile);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not update messages.yml: " + e.getMessage());
        }
    }

    public String getMessage(String key, boolean includePrefix, String... replacements) {
        String raw = messagesConfig.getString(key, "Error: Message key '" + key + "' not found in messages.yml");
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                raw = raw.replace(replacements[i], replacements[i + 1]);
            }
        }
        if (includePrefix) {
            String prefixRaw = messagesConfig.getString("prefix", "<gray>[<green>FixLag<gray>] <reset>");
            raw = prefixRaw + raw;
        }
        Component comp = miniMessage.deserialize(raw);
        return legacySerializer.serialize(comp);
    }

    public String getMessage(String key, String... replacements) {
        return getMessage(key, true, replacements);
    }

    public String getLogMessage(String key, String... replacements) {
        String raw = messagesConfig.getString(key, "Error: Log message key '" + key + "' not found in messages.yml");
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                raw = raw.replace(replacements[i], replacements[i + 1]);
            }
        }
        Component comp = miniMessage.deserialize(raw);
        return legacySerializer.serialize(comp);
    }

    public String getRawMessage(String key) {
        return messagesConfig.getString(key, "");
    }
}
