/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.extension.github;

/**
 * Comments by bots and agents start collapsed. The app draws a comment from its HTML, so the HTML is wrapped in a
 * {@code details} element, which opens in place when tapped.
 */
@SuppressWarnings("unused")
public final class BotComments {
    private BotComments() {
    }

    /**
     * The HTML to draw for a comment.
     *
     * @param author The app's model of the author, whose toString names its fields.
     * @param html   The comment's HTML.
     * @param type   The app's kind of the comment.
     * @return The HTML, collapsed when a bot wrote the comment. Descriptions of issues and pull requests stay as they are.
     */
    public static String body(Object author, String html, Object type) {
        if (html == null || html.trim().isEmpty() || !isBot(String.valueOf(author))) return html;
        String kind = String.valueOf(type);
        if (kind.startsWith("EditIssueBody(") || kind.startsWith("EditPullRequestBody(")) return html;
        return "<details><summary>Show bot comment</summary>" + html + "</details>";
    }

    /** Whether an author is a bot: it has a bot's name or flags, or its page is that of a GitHub App. */
    private static boolean isBot(String author) {
        return author.startsWith("Author(")
                && (!author.contains(", botDisplayName=null, isCopilot=false, isAgent=false,")
                || author.contains(", url=https://github.com/apps/"));
    }
}
