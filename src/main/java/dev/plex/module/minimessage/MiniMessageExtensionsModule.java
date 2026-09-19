package dev.plex.module.minimessage;

import dev.plex.api.storage.ModuleStorage;
import dev.plex.module.PlexModule;
import dev.plex.module.minimessage.chatstyle.ChatStyleListener;
import dev.plex.module.minimessage.chatstyle.ChatStyles;
import dev.plex.module.minimessage.command.ChatStyleCommand;
import dev.plex.module.minimessage.command.NickMMCommand;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MiniMessageExtensionsModule extends PlexModule
{
    private ExecutorService executor;
    private ChatStyles chatStyles;

    @Override
    public void load()
    {
        loadMessages("messages.yml");
        registerCommand(new NickMMCommand());
        registerCommand(new ChatStyleCommand(this));
    }

    @Override
    public void enable()
    {
        if (!Bukkit.getPluginManager().isPluginEnabled("Essentials"))
        {
            getLogger().warn("EssentialsX is not enabled; /nickmm is unavailable");
        }

        ModuleStorage storage = api().storage().forModule(this);
        try
        {
            storage.migrations().run();
        }
        catch (SQLException exception)
        {
            throw new IllegalStateException("Unable to initialize chat style storage", exception);
        }

        executor = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("Plex-MiniMessageExtensions").daemon(true).factory());
        chatStyles = new ChatStyles(storage, executor, getLogger());
        registerListener(new ChatStyleListener(chatStyles));
        chatStyles.loadAll(Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList());
    }

    @Override
    public void disable()
    {
        if (executor != null)
        {
            executor.shutdown();
            executor = null;
        }
        chatStyles = null;
    }

    public ChatStyles chatStyles()
    {
        if (chatStyles == null)
        {
            throw new IllegalStateException("Chat styles are not available before the module is enabled");
        }
        return chatStyles;
    }
}
