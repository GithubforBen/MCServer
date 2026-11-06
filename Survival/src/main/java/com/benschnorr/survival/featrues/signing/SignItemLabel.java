package com.benschnorr.survival.featrues.signing;

/**
 * Decides whether a chat line names {@code /signItem} in any letter case and what it is rewritten to.
 * No Bukkit types, so it can be checked without a server.
 */
public final class SignItemLabel {
    private static final String LABEL = "/signitem";

    private SignItemLabel() {
    }

    /** The message with the label lowercased and the arguments kept, or null if nothing needs rewriting. */
    public static String rewrite(String message) {
        if (message == null || !message.regionMatches(true, 0, LABEL, 0, LABEL.length())) return null;
        if (message.length() > LABEL.length() && !Character.isWhitespace(message.charAt(LABEL.length()))) return null;
        String rewritten = LABEL + message.substring(LABEL.length());
        return rewritten.equals(message) ? null : rewritten;
    }
}
