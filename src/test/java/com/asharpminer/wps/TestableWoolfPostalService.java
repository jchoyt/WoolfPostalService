package com.asharpminer.wps;

import java.util.ArrayList;
import java.util.List;

/**
 * Test double used only by this project's unit tests. WoolfPostalService normally logs its bot
 * into Discord as part of onEnable() (see WoolfPostalService#connectDiscord); this subclass skips
 * that so tests never touch the network, and records outbound notifications instead of only
 * logging or sending them, so tests can assert on what would have gone out.
 */
public class TestableWoolfPostalService extends WoolfPostalService {

    private final List<String> notifications = new ArrayList<>();

    @Override
    protected void connectDiscord() {
        // no-op: never connect to Discord in tests
    }

    @Override
    public void notifyMailChannel(String msg) {
        notifications.add(msg);
    }

    public List<String> getNotifications() {
        return notifications;
    }
}
