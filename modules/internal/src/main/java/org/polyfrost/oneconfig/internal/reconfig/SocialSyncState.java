package org.polyfrost.oneconfig.internal.reconfig;

/** Notification cursors are independent from conversation history cursors. */
public final class SocialSyncState {
    private long messages, requests, deletions, invitations;
    private boolean ready;

    public void reset() { messages = requests = deletions = invitations = 0; ready = false; }
    public boolean isReady() { return ready; }
    public long getMessages() { return messages; }
    public long getRequests() { return requests; }
    public long getDeletions() { return deletions; }
    public long getInvitations() { return invitations; }
    public String path() {
        return ready ? "/v2/sync?after=" + messages + "&afterRequest=" + requests
                + "&afterDeletion=" + deletions + "&afterInvitation=" + invitations
                : "/v2/sync?bootstrap=1";
    }
    public void bootstrap(long message, long request, long deletion, long invitation) {
        messages = Math.max(0, message); requests = Math.max(0, request);
        deletions = Math.max(0, deletion); invitations = Math.max(0, invitation); ready = true;
    }
    public void advance(long message, long request, long deletion, long invitation) {
        if (!ready) throw new IllegalStateException("Notification baseline is required");
        messages = Math.max(messages, message); requests = Math.max(requests, request);
        deletions = Math.max(deletions, deletion); invitations = Math.max(invitations, invitation);
    }
    public static boolean validIdentity(String name, String uuid) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}")
                && uuid != null && uuid.matches("[a-f0-9]{32}");
    }
    public static boolean sentByMe(String self, String peer, String sender, String recipient) {
        return self.equals(sender) && peer.equals(recipient);
    }
    public static boolean inConversation(String self, String peer, String sender, String recipient) {
        return sentByMe(self, peer, sender, recipient) || (self.equals(recipient) && peer.equals(sender));
    }
}
