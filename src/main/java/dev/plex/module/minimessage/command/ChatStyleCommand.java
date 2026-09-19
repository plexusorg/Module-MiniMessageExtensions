package dev.plex.module.minimessage.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.SimplePlexCommand;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.module.minimessage.MiniMessageExtensionsModule;
import dev.plex.module.minimessage.chatstyle.ChatStyles;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ChatStyleCommand extends SimplePlexCommand
{
    private static final String SAMPLE_MESSAGE = "The quick brown fox jumps over the lazy dog";
    private static final List<String> STYLE_SUGGESTIONS = List.of("off", "<rainbow>", "<gradient:red:blue>", "<pride>", "<#ff8800>");

    private final MiniMessageExtensionsModule module;

    public ChatStyleCommand(MiniMessageExtensionsModule module)
    {
        super(command("chatstyle")
                .description("Set a color style for your chat messages")
                .usage("/<command> <tag | off>")
                .permission("plex.chatstyle")
                .source(RequiredCommandSource.IN_GAME)
                .build());
        this.module = module;
    }

    @Override
    protected void configureCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, (sender, player) -> noStyleGiven()));
        command.then(greedyString("style")
                .suggests((context, builder) -> suggestMatching(builder, STYLE_SUGGESTIONS))
                .executes(context -> executeCommand(context,
                        (sender, player) -> setStyle(player, string(context, "style")))));
    }

    private Component noStyleGiven()
    {
        return usage().append(Component.newline()).append(messageComponent("chatStyleExample"));
    }

    private Component setStyle(Player player, String input)
    {
        ChatStyles styles = module.chatStyles();
        if (input.equalsIgnoreCase("off"))
        {
            Component testLine = testLine(player, Component.text(SAMPLE_MESSAGE));
            report(player, styles.clear(player.getUniqueId()), messageComponent("chatStyleCleared"), testLine);
            return null;
        }
        if (!styles.isValid(input))
        {
            return messageComponent("chatStyleInvalid");
        }
        Component testLine = testLine(player, styles.style(input, Component.text(SAMPLE_MESSAGE)));
        report(player, styles.set(player.getUniqueId(), input), messageComponent("chatStyleSet"), testLine);
        return null;
    }

    // Reads the player's chat line format, so it must run on the command thread before persistence starts.
    private Component testLine(Player player, Component body)
    {
        boolean chatEnabled = api().configuration().mainConfig().getBoolean("chat.enabled", true);
        return chatEnabled ? api().messages().chatLine(player, body) : body;
    }

    private void report(Player player, CompletableFuture<Void> persisted, Component success, Component testLine)
    {
        persisted.whenComplete((ignored, throwable) ->
        {
            if (throwable != null)
            {
                module.getLogger().warn("Unable to save the chat style of {}", player.getName(), throwable);
                player.sendMessage(messageComponent("chatStyleFailed"));
                return;
            }
            player.sendMessage(success);
            player.sendMessage(testLine);
        });
    }
}
