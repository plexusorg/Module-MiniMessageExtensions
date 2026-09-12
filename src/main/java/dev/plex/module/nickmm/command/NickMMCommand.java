package dev.plex.module.nickmm.command;

import com.earth2me.essentials.I18n;
import com.earth2me.essentials.User;
import com.earth2me.essentials.adventure.AdventureFacet;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.SimplePlexCommand;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.module.nickmm.NickMiniMessageModule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NickMMCommand extends SimplePlexCommand
{
    private final NickMiniMessageModule module;
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
    private final LegacyComponentSerializer legacyComponent = LegacyComponentSerializer.builder()
            .character('\u00a7').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private final MiniMessage miniMessage = MiniMessage.builder().tags(new NicknameTagResolver()).build();

    public NickMMCommand(NickMiniMessageModule module)
    {
        super(command("nickmm")
                .description("Change your nickname using MiniMessage formatting!")
                .usage("/<command> <nick>")
                .aliases("nickminimessage")
                .permission("plex.nickmm")
                .source(RequiredCommandSource.IN_GAME)
                .build());
        this.module = module;
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

        final Component nick = miniMessage.deserialize(input).clickEvent(null).hoverEvent(null);
        final String plain = plainText.serialize(nick);
        AdventureFacet adventure = module.getEssentials().getAdventureFacet();

        if (plain.length() > module.getEssentials().getSettings().getMaxNickLength()
                && !commandSender.hasPermission("plex.nickmm.ignore_length_limit"))
        {
            adventure.send(commandSender, adventure.deserializeMiniMessage(I18n.tlLiteral("nickTooLong")));
            return null;
        }

        if (!commandSender.hasPermission("plex.nickmm.ignore_matching"))
        {
            for (final User user : module.getEssentials().getOnlineUsers())
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
        User essentialsUser = module.getEssentials().getUser(player);
        essentialsUser.setNickname(legacy);
        essentialsUser.setDisplayNick();

        adventure.send(commandSender, adventure.deserializeMiniMessage(I18n.tlLiteral("nickSet", legacy)));
        return null;
    }

    private static class NicknameTagResolver implements TagResolver
    {
        private static final TagResolver STANDARD_RESOLVER = TagResolver.standard();
        private static final List<String> IGNORED_TAGS = Arrays.asList(
                "click",
                "hover",
                "insertion",
                "insert",
                "obfuscated",
                "obf",
                "br",
                "newline",
                "lang",
                "key",
                "translate");

        @Override
        public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException
        {
            return IGNORED_TAGS.contains(name.toLowerCase()) ? null : STANDARD_RESOLVER.resolve(name, arguments, ctx);
        }

        @Override
        public boolean has(@NotNull String name)
        {
            return STANDARD_RESOLVER.has(name);
        }
    }
}
