package gg.kite.model;

import java.time.Instant;
import java.util.UUID;

public record Invite(UUID inviter, UUID invitee, Instant expiration) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiration);
    }
}