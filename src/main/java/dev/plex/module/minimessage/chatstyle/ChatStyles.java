package dev.plex.module.minimessage.chatstyle;

import dev.plex.api.storage.ModuleStorage;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.logging.log4j.Logger;

public class ChatStyles
{
    public static final int MAX_LENGTH = 255;

    // Opt in to color tags only, so a future standard tag does not expand what a style can do.
    private static final MiniMessage COLORS = MiniMessage.builder().tags(TagResolver.resolver(
            StandardTags.color(),
            StandardTags.gradient(),
            StandardTags.rainbow(),
            StandardTags.transition(),
            StandardTags.pride())).build();

    private final ChatStyleRepository repository;
    private final Executor executor;
    private final Logger logger;
    // Chat and pre-login threads read this view. The SQL table stays authoritative.
    private final Map<UUID, String> styles = new ConcurrentHashMap<>();

    public ChatStyles(ModuleStorage storage, Executor executor, Logger logger)
    {
        this.repository = new ChatStyleRepository(storage);
        this.executor = executor;
        this.logger = logger;
    }

    // A style has tags only. A blocked, unknown, or escaped tag stays literal text and fails here.
    public boolean isValid(String input)
    {
        if (input.length() > MAX_LENGTH)
        {
            return false;
        }
        try
        {
            return PlainTextComponentSerializer.plainText().serialize(COLORS.deserialize(input)).isEmpty();
        }
        catch (ParsingException exception)
        {
            return false;
        }
    }

    public Component preview(String style)
    {
        return COLORS.deserialize(style + "<text>", Placeholder.component("text", Component.text(style)));
    }

    // The message is a component placeholder, so message text cannot close or reset the style.
    public Component apply(UUID player, Component message)
    {
        String style = styles.get(player);
        if (style == null)
        {
            return message;
        }
        return COLORS.deserialize(style + "<message>", Placeholder.component("message", message));
    }

    // Blocks. Call only from a thread that can block, such as the async pre-login thread.
    public void load(UUID player)
    {
        try
        {
            repository.find(player).ifPresentOrElse(style -> styles.put(player, style), () -> styles.remove(player));
        }
        catch (IllegalStateException exception)
        {
            logger.warn("Unable to load the chat style of {}", player, exception);
        }
    }

    public void loadAll(Collection<UUID> players)
    {
        players.forEach(player -> executor.execute(() -> load(player)));
    }

    public void forget(UUID player)
    {
        styles.remove(player);
    }

    public CompletableFuture<Void> set(UUID player, String style)
    {
        return CompletableFuture.runAsync(() -> repository.save(player, style), executor)
                .thenRun(() -> styles.put(player, style));
    }

    public CompletableFuture<Void> clear(UUID player)
    {
        return CompletableFuture.runAsync(() -> repository.delete(player), executor)
                .thenRun(() -> styles.remove(player));
    }
}
