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
 * Covers /deletebox. Like MailboxCommandExecutorTest, tests grant wps.mailman to whichever
 * player should be allowed to run the command - Bukkit itself enforces that permission (declared
 * in plugin.yml) before DeleteMailboxCommandExecutor ever runs.
 */
class DeleteMailboxCommandExecutorTest {

    private ServerMock server;
    private TestableWoolfPostalService plugin;
    private WorldMock world;
    private PluginCommand deletebox;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.loadWith(TestableWoolfPostalService.class, "plugin.yml");
        world = server.addSimpleWorld("world");
        deletebox = plugin.getCommand("deletebox");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("/deletebox stops tracking the player's current block")
    void stopsTrackingCurrentBlock() {
        PlayerMock player = server.addPlayer("Alice");
        player.setLocation(world.getBlockAt(5, 70, -12).getLocation());
        player.addAttachment(plugin, "wps.mailman", true);
        plugin.setMailbox("Front Porch", world.getBlockAt(5, 70, -12));

        boolean handled = deletebox.execute(player, "deletebox", new String[0]);

        assertTrue(handled);
        assertFalse(plugin.isMailbox(world.getBlockAt(5, 70, -12)));
        assertEquals("This location is no longer being monitored", player.nextMessage());
    }

    @Test
    @DisplayName("/deletebox on a block that isn't a mailbox changes nothing")
    void leavesUntrackedBlockAlone() {
        PlayerMock player = server.addPlayer("Bob");
        player.setLocation(world.getBlockAt(1, 65, 1).getLocation());
        player.addAttachment(plugin, "wps.mailman", true);

        boolean handled = deletebox.execute(player, "deletebox", new String[0]);

        assertTrue(handled);
        assertEquals("This isn't an WPS location. No change made.", player.nextMessage());
    }

    @Test
    @DisplayName("/deletebox from the console is rejected - it requires a player")
    void rejectsNonPlayerSenders() {
        boolean handled = deletebox.execute(server.getConsoleSender(), "deletebox", new String[0]);

        assertFalse(handled);
        assertEquals("You must be a player!", server.getConsoleSender().nextMessage());
    }
}
