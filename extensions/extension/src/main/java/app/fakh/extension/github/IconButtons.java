/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.extension.github;

import android.app.Application;
import android.content.res.Resources;

import app.fakh.extension.shared.AppRuntime;

/**
 * The floating buttons that show only their icon. A button is known by its label, in the app's language, and keeps that
 * label for screen readers.
 */
@SuppressWarnings("unused")
public final class IconButtons {
    // The labels of the floating buttons: issues and pull requests, discussions and the home screen.
    private static final String[] LABELS = {
            "issue_pr_comment",
            "label_locked_badge",
            "discussions_write_comment",
            "discussions_write_comment_locked",
            "discussions_suggest_answer",
            "home_bar_of_actions_new_session_label",
    };

    private IconButtons() {
    }

    /**
     * The space after the icon of a button.
     *
     * @param label The button's label.
     * @return None for a button without text, otherwise the app's 8dp.
     */
    public static float gap(String label) {
        return isFloating(label) ? 0 : 8;
    }

    /**
     * What a screen reader says for the icon of a button.
     *
     * @param label The button's label.
     * @return The label for a button without text, otherwise null as in the app, whose text is read instead.
     */
    public static String description(String label) {
        return isFloating(label) ? label : null;
    }

    /**
     * The text a button with an icon shows.
     *
     * @param label The button's label.
     * @return Nothing for a floating button, otherwise the label.
     */
    public static String text(String label) {
        return isFloating(label) ? "" : label;
    }

    /**
     * What a screen reader says for the icon of the review button.
     *
     * @return The text the button no longer shows.
     */
    public static String reviewDescription() {
        return string("issue_pr_review_review_finish");
    }

    /** Whether this is the label of one of the floating buttons. */
    private static boolean isFloating(String label) {
        for (String name : LABELS) {
            if (label.equals(string(name))) return true;
        }
        return false;
    }

    /** A string of the app by its resource name, or null when it has none by that name. */
    @SuppressWarnings("DiscouragedApi")
    private static String string(String name) {
        Application app = AppRuntime.application();
        if (app == null) return null;
        Resources resources = app.getResources();
        int id = resources.getIdentifier(name, "string", app.getPackageName());
        return id == 0 ? null : resources.getString(id);
    }
}
