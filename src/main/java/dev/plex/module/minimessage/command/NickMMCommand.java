package dev.plex.module.minimessage.command;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.I18n;
import com.earth2me.essentials.User;
import com.earth2me.essentials.adventure.AdventureFacet;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.SimplePlexCommand;
import dev.plex.command.source.RequiredCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

public class NickMMCommand extends SimplePlexCommand
{
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
    private final LegacyComponentSerializer legacyComponent = LegacyComponentSerializer.builder()
            .character('\u00a7').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public NickMMCommand()
    {
        super(command("nickmm")
                .description("Change your nickname using MiniMessage formatting!")
                .usage("/<command> <nick>")
                .aliases("nickminimessage")
                .permission("plex.nickmm")
                .source(RequiredCommandSource.IN_GAME)
                .build());
    }

    @Override
    protected void configureCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, (sender, player) -> executeTyped(sender, player, null)));
        command.then(greedyString("nick")
                .executes(context -> executeCommand(context,
                        (sender, player) -> executeTyped(sender, player, string(context, "nick").split(" ", 2)[0]))));
    }

    private Component executeTyped(CommandSender commandSender, Player player, @Nullable String input)
    {
        if (input == null)
        {
            return usage();
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("Essentials"))
        {
            return messageComponent("nickUnavailable");
        }
        Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");

        final Component nick = api().messages().playerText(input);
        final String plain = plainText.serialize(nick);
        AdventureFacet adventure = essentials.getAdventureFacet();

        if (plain.length() > essentials.getSettings().getMaxNickLength()
                && !commandSender.hasPermission("plex.nickmm.ignore_length_limit"))
        {
            adventure.send(commandSender, adventure.deserializeMiniMessage(I18n.tlLiteral("nickTooLong")));
            return null;
        }

        if (!commandSender.hasPermission("plex.nickmm.ignore_matching"))
        {
            for (final User user : essentials.getOnlineUsers())
            {
                final String name = user.getNickname() != null ? plainText.serialize(legacyComponent.deserialize(user.getNickname())) : user.getName();

                if (name.equalsIgnoreCase(plain) && !user.getUUID().equals(player.getUniqueId()))
                {
                    adventure.send(commandSender, adventure.deserializeMiniMessage(I18n.tlLiteral("nickInUse")));
                    return null;
                }
            }
        }

        final String legacy = legacyComponent.serialize(nick);
        User essentialsUser = essentials.getUser(player);
        essentialsUser.setNickname(legacy);
        essentialsUser.setDisplayNick();

        adventure.send(commandSender, adventure.deserializeMiniMessage(I18n.tlLiteral("nickSet", legacy)));
        return null;
    }
}
