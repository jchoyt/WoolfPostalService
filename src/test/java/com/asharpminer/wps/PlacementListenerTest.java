package com.asharpminer.wps;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.Directional;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers PlacementListener's two event handlers. Both are exercised through the listener that
 * WoolfPostalService.onEnable() already registers on TestableWoolfPostalService - real events are
 * fired at the mock server and PlacementListener's reaction (player messages, WPS channel
 * notifications) is observed from there, rather than calling the handler methods directly.
 */
class PlacementListenerTest {

    private ServerMock server;
    private TestableWoolfPostalService plugin;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.loadWith(TestableWoolfPostalService.class, "plugin.yml");
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("placing an empty shulker box at a nicknamed mailbox notifies WPS and the customer")
    void placingEmptyShulkerAtNicknamedMailboxNotifies() {
        Block box = world.getBlockAt(5, 70, -12);
        plugin.setMailbox("Front Porch", box);
        PlayerMock customer = server.addPlayer("Alice");

        server.getPluginManager().callEvent(placeShulkerEvent(box, customer));

        assertEquals(1, plugin.getNotifications().size());
        assertEquals(
            "Alice placed a package for pickup at Front Porch (5 70 -12 in world). It's empty. No rush.",
            plugin.getNotifications().get(0));
        assertEquals(
            "WPS has been notified of your request and someone will be by shortly to pick up your package.",
            customer.nextMessage());
    }

    @Test
    @DisplayName("placing a shulker box at a mailbox with no nickname falls back to raw coordinates")
    void placingShulkerAtUnnamedMailboxUsesCoordinates() {
        Block box = world.getBlockAt(1, 65, 1);
        plugin.setMailbox(null, box);
        PlayerMock customer = server.addPlayer("Bob");

        server.getPluginManager().callEvent(placeShulkerEvent(box, customer));

        assertEquals(1, plugin.getNotifications().size());
        assertEquals(
            "Bob placed a package for pickup at 1 65 1 in world. It's empty. No rush.",
            plugin.getNotifications().get(0));
    }

    @Test
    @DisplayName("mail staff placing their own shulker box gets a joke instead of triggering a notification")
    void mailStaffPlacingBoxIsNotNotified() {
        Block box = world.getBlockAt(2, 64, 2);
        plugin.setMailbox("Back Door", box);
        PlayerMock mailman = server.addPlayer("Carol");
        mailman.addAttachment(plugin, "wps.mailman", true);

        server.getPluginManager().callEvent(placeShulkerEvent(box, mailman));

        assertTrue(plugin.getNotifications().isEmpty());
        assertEquals(
            "Good job! Entomo's a lazy sod and hasn't done the rest of Issue 4 yet.",
            mailman.nextMessage());
    }

    @Test
    @DisplayName("placing a shulker box somewhere that isn't a tracked mailbox is ignored")
    void placingShulkerOutsideMailboxIsIgnored() {
        Block box = world.getBlockAt(40, 64, 40);
        PlayerMock customer = server.addPlayer("Dana");

        server.getPluginManager().callEvent(placeShulkerEvent(box, customer));

        assertTrue(plugin.getNotifications().isEmpty());
        assertEquals(null, customer.nextMessage());
    }

    @Test
    @DisplayName("a dispenser ejecting a shulker box into a tracked mailbox notifies WPS")
    void dispensingShulkerIntoMailboxNotifies() {
        Block dispenser = world.getBlockAt(3, 64, 3);
        dispenser.setType(Material.DISPENSER);
        Directional facing = (Directional) dispenser.getBlockData();
        facing.setFacing(BlockFace.NORTH);
        dispenser.setBlockData(facing);
        Block box = dispenser.getRelative(BlockFace.NORTH);
        plugin.setMailbox("Loading Dock", box);

        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        server.getPluginManager().callEvent(
            new BlockDispenseEvent(dispenser, shulker, new Vector(0, 0, 0)));

        assertEquals(1, plugin.getNotifications().size());
        assertEquals("The magic of WPS moved a package to Loading Dock", plugin.getNotifications().get(0));
    }

    @Test
    @DisplayName("a dispenser ejecting a shulker box away from any tracked mailbox is ignored")
    void dispensingShulkerOutsideMailboxIsIgnored() {
        Block dispenser = world.getBlockAt(9, 64, 9);
        dispenser.setType(Material.DISPENSER);
        Directional facing = (Directional) dispenser.getBlockData();
        facing.setFacing(BlockFace.SOUTH);
        dispenser.setBlockData(facing);

        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        server.getPluginManager().callEvent(
            new BlockDispenseEvent(dispenser, shulker, new Vector(0, 0, 0)));

        assertTrue(plugin.getNotifications().isEmpty());
    }

    @Test
    @DisplayName("a dispenser ejecting a non-shulker item is ignored even at a tracked mailbox")
    void dispensingNonShulkerItemIsIgnored() {
        Block dispenser = world.getBlockAt(6, 64, 6);
        dispenser.setType(Material.DISPENSER);
        Directional facing = (Directional) dispenser.getBlockData();
        facing.setFacing(BlockFace.EAST);
        dispenser.setBlockData(facing);
        Block box = dispenser.getRelative(BlockFace.EAST);
        plugin.setMailbox("Side Door", box);

        server.getPluginManager().callEvent(
            new BlockDispenseEvent(dispenser, new ItemStack(Material.ARROW), new Vector(0, 0, 0)));

        assertTrue(plugin.getNotifications().isEmpty());
    }

    private BlockPlaceEvent placeShulkerEvent(Block box, PlayerMock player) {
        box.setType(Material.SHULKER_BOX);
        return new BlockPlaceEvent(
            box,
            box.getState(),
            box.getRelative(BlockFace.DOWN),
            new ItemStack(Material.SHULKER_BOX),
            player,
            true,
            EquipmentSlot.HAND);
    }
}
