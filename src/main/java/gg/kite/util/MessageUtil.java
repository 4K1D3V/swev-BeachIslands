package gg.kite.util;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;

public class MessageUtil {
    private final JavaPlugin plugin;
    private final FileConfiguration messagesConfig;
    private final MiniMessage miniMessage;

    @Inject
    public MessageUtil(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.defaults())
                        .build())
                .build();
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public Component getMessage(String key) {
        String message = messagesConfig.getString("messages." + key, "<red>Message not found: " + key);
        return miniMessage.deserialize(message);
    }

    public Component getMessage(String key, @NotNull Map<String, String> placeholders) {
        String message = messagesConfig.getString("messages." + key, "<red>Message not found: " + key);
        TagResolver.Builder resolverBuilder = TagResolver.builder();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolverBuilder.resolver(TagResolver.resolver(entry.getKey(), Tag.preProcessParsed(entry.getValue())));
        }
        return miniMessage.deserialize(message, resolverBuilder.build());
    }
}