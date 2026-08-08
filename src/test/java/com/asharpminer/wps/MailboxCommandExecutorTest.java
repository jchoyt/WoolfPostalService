package com.asharpminer.wps;

import org.bukkit.command.PluginCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers /wpsbox. Bukkit gates the command on the wps.mailman permission declared in plugin.yml
 * before MailboxCommandExecutor ever sees it (PluginCommand.execute() itself checks it, and
 * refuses to run the executor at all without it) - so tests grant it to players who should be
 * able to run the command, same as a server operator would via a permissions plugin.
 */
class MailboxCommandExecutorTest {

    private ServerMock server;
    private TestableWoolfPostalService plugin;
    private WorldMock world;
    private PluginCommand wpsbox;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.loadWith(TestableWoolfPostalService.class, "plugin.yml");
        world = server.addSimpleWorld("world");
        wpsbox = plugin.getCommand("wpsbox");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("/wpsbox <nickname> tracks the player's current block under that nickname")
    void tracksCurrentBlockUnderGivenNickname() {
        PlayerMock player = server.addPlayer("Alice");
        player.setLocation(world.getBlockAt(5, 70, -12).getLocation());
        player.addAttachment(plugin, "wps.mailman", true);

        // MailboxCommandExecutor only ever reads args[0]; it doesn't join the remaining args.
        boolean handled = wpsbox.execute(player, "wpsbox", new String[] { "FrontPorch" });

        assertTrue(handled);
        assertTrue(plugin.isMailbox(world.getBlockAt(5, 70, -12)));
        assertEquals("FrontPorch", plugin.getNickname(world.getBlockAt(5, 70, -12)));
        assertEquals("WPS is monitoring this location", player.nextMessage());
    }

    @Test
    @DisplayName("/wpsbox with no argument defaults the nickname to \"nn\"")
    void defaultsNicknameWhenNoArgumentGiven() {
        PlayerMock player = server.addPlayer("Bob");
        player.setLocation(world.getBlockAt(1, 65, 1).getLocation());
        player.addAttachment(plugin, "wps.mailman", true);

        wpsbox.execute(player, "wpsbox", new String[0]);

        assertEquals("nn", plugin.getNickname(world.getBlockAt(1, 65, 1)));
    }

    @Test
    @DisplayName("/wpsbox from the console is rejected - it requires a player")
    void rejectsNonPlayerSenders() {
        boolean handled = wpsbox.execute(server.getConsoleSender(), "wpsbox", new String[] { "Front" });

        assertFalse(handled);
        assertEquals("You must be a player!", server.getConsoleSender().nextMessage());
    }
}
