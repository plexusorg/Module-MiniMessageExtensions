package dev.plex.module.minimessage.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.SimplePlexCommand;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.module.minimessage.MiniMessageExtensionsModule;
import dev.plex.module.minimessage.chatstyle.ChatStyles;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

public class ChatStyleCommand extends SimplePlexCommand
{
    private final MiniMessageExtensionsModule module;

    public ChatStyleCommand(MiniMessageExtensionsModule module)
    {
        super(command("chatstyle")
                .description("Set a color style for your chat messages")
                .usage("/<command> <style | off>")
                .permission("plex.chatstyle")
                .source(RequiredCommandSource.IN_GAME)
                .build());
        this.module = module;
    }

    @Override
    protected void configureCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, (sender, player) -> usage()));
        command.then(greedyString("style")
                .executes(context -> executeCommand(context,
                        (sender, player) -> setStyle(player, string(context, "style")))));
    }

    private Component setStyle(Player player, String input)
    {
        ChatStyles styles = module.chatStyles();
        if (input.equalsIgnoreCase("off"))
        {
            report(player, styles.clear(player.getUniqueId()), messageComponent("chatStyleCleared"));
            return null;
        }
        if (!styles.isValid(input))
        {
            return messageComponent("chatStyleInvalid");
        }
        report(player, styles.set(player.getUniqueId(), input),
                messageComponent("chatStyleSet", Placeholder.component("style", styles.preview(input))));
        return null;
    }

    private void report(Player player, CompletableFuture<Void> persisted, Component success)
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
        });
    }
}
