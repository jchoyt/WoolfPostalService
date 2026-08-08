package com.asharpminer.wps;

import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers WoolfPostalService's own mailbox bookkeeping: tracking/untracking a location in memory,
 * and persisting that list to mailboxes.yml so it survives a restart. Discord connectivity is
 * skipped entirely - see TestableWoolfPostalService.
 */
class WoolfPostalServiceMailboxTest {

    private TestableWoolfPostalService plugin;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        // loadWith(), not load(): plugin.yml's main: points at WoolfPostalService itself, and
        // MockBukkit.load() insists the loaded class match that exactly. loadWith() takes the
        // description file as given instead, so it's happy to enable our test subclass under it.
        plugin = MockBukkit.loadWith(TestableWoolfPostalService.class, "plugin.yml");
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("a block is not a mailbox until it's registered")
    void notAMailboxByDefault() {
        Block block = world.getBlockAt(0, 64, 0);

        assertFalse(plugin.isMailbox(block));
        assertNull(plugin.getNickname(block));
    }

    @Test
    @DisplayName("setMailbox tracks the block under its nickname")
    void setMailboxTracksBlock() {
        Block block = world.getBlockAt(5, 70, -12);

        plugin.setMailbox("Front Porch", block);

        assertTrue(plugin.isMailbox(block));
        assertEquals("Front Porch", plugin.getNickname(block));
    }

    @Test
    @DisplayName("deleteMailbox stops tracking a known block and reports success")
    void deleteMailboxRemovesBlock() {
        Block block = world.getBlockAt(1, 65, 1);
        plugin.setMailbox("Back Door", block);

        assertTrue(plugin.deleteMailbox(block));

        assertFalse(plugin.isMailbox(block));
        assertNull(plugin.getNickname(block));
    }

    @Test
    @DisplayName("deleteMailbox on an untracked block reports failure")
    void deleteMailboxOnUnknownBlockFails() {
        Block block = world.getBlockAt(99, 64, 99);

        assertFalse(plugin.deleteMailbox(block));
    }

    @Test
    @DisplayName("saving writes the world:x:y:z:nickname format readMailboxes() expects")
    void saveMailboxesWritesExpectedFormat() throws Exception {
        Block block = world.getBlockAt(10, 65, -3);

        plugin.setMailbox("Front Porch", block);

        assertEquals(List.of("world:10:65:-3:Front Porch"), readSavedMailboxLines(plugin));
    }

    @Test
    @DisplayName("mailboxes saved to disk are picked back up on the next load")
    void mailboxesRoundTripThroughDisk() throws Exception {
        Block first = world.getBlockAt(10, 65, -3);
        Block second = world.getBlockAt(-4, 71, 8);
        plugin.setMailbox("Front Porch", first);
        plugin.setMailbox("Back Door", second);

        // Simulate a server restart: forget everything held in memory, then reload from the
        // mailboxes.yml that setMailbox() above already wrote to disk.
        mailboxMap(plugin).clear();
        assertFalse(plugin.isMailbox(first));

        invokeReadMailboxes(plugin);

        assertTrue(plugin.isMailbox(first));
        assertEquals("Front Porch", plugin.getNickname(first));
        assertTrue(plugin.isMailbox(second));
        assertEquals("Back Door", plugin.getNickname(second));
    }

    // -- reflection helpers for the persistence internals, which are private on purpose ----

    private static List<?> readSavedMailboxLines(WoolfPostalService plugin) throws Exception {
        File configFile = (File) getField(plugin, "configFile");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        return config.getList("mailboxes");
    }

    @SuppressWarnings("unchecked")
    private static Map<Block, String> mailboxMap(WoolfPostalService plugin) throws Exception {
        return (Map<Block, String>) getField(plugin, "mailboxes");
    }

    private static Object getField(WoolfPostalService plugin, String name) throws Exception {
        Field field = WoolfPostalService.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(plugin);
    }

    private static void invokeReadMailboxes(WoolfPostalService plugin) throws Exception {
        Method method = WoolfPostalService.class.getDeclaredMethod("readMailboxes");
        method.setAccessible(true);
        method.invoke(plugin);
    }
}
