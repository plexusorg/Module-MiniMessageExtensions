package dev.plex.module.minimessage.chatstyle;

import dev.plex.api.storage.ModuleStorage;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

class ChatStyleRepository
{
    private final Jdbi jdbi;
    private final String table;

    ChatStyleRepository(ModuleStorage storage)
    {
        this.jdbi = storage.jdbi();
        this.table = storage.table("chat_style");
    }

    Optional<String> find(UUID player)
    {
        try
        {
            return jdbi.withHandle(handle -> handle.createQuery("SELECT style FROM " + table + " WHERE uuid = :uuid")
                    .bind("uuid", player.toString())
                    .mapTo(String.class)
                    .findFirst());
        }
        catch (RuntimeException exception)
        {
            throw new IllegalStateException("Failed to load chat style", exception);
        }
    }

    void save(UUID player, String style)
    {
        try
        {
            jdbi.useTransaction(handle ->
            {
                handle.createUpdate("DELETE FROM " + table + " WHERE uuid = :uuid")
                        .bind("uuid", player.toString())
                        .execute();
                handle.createUpdate("INSERT INTO " + table + " (uuid, style) VALUES (:uuid, :style)")
                        .bind("uuid", player.toString())
                        .bind("style", style)
                        .execute();
            });
        }
        catch (RuntimeException exception)
        {
            throw new IllegalStateException("Failed to save chat style", exception);
        }
    }

    void delete(UUID player)
    {
        try
        {
            jdbi.useHandle(handle -> handle.createUpdate("DELETE FROM " + table + " WHERE uuid = :uuid")
                    .bind("uuid", player.toString())
                    .execute());
        }
        catch (RuntimeException exception)
        {
            throw new IllegalStateException("Failed to delete chat style", exception);
        }
    }
}
