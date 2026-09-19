package dev.plex.module.minimessage.chatstyle;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class ChatStyleListener implements Listener
{
    private final ChatStyles styles;

    public ChatStyleListener(ChatStyles styles)
    {
        this.styles = styles;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED)
        {
            styles.load(event.getUniqueId());
        }
    }

    // Also fires for a login that a later stage denies, so the view cannot keep that entry.
    @EventHandler
    public void onConnectionClose(PlayerConnectionCloseEvent event)
    {
        styles.forget(event.getPlayerUniqueId());
    }

    // Runs after Plex parses the message and after staff chat and guild chat cancel the event.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event)
    {
        event.message(styles.apply(event.getPlayer().getUniqueId(), event.message()));
    }
}
