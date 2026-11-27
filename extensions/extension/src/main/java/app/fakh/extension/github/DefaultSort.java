/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.extension.github;

/**
 * The sort of a saved issue or pull request filter, as the app reads it back from its database. The app saves the
 * sort of every list it opens, Newest included, so without this a list opened before the patch keeps Newest.
 */
@SuppressWarnings("unused")
public final class DefaultSort {
    private DefaultSort() {
    }

    /**
     * Turns a saved Newest sort into Recently updated, the patched default. Any other saved sort stays.
     *
     * @param sort The saved constant of the app's sort enum.
     * @return Recently updated for Newest, otherwise {@code sort} itself.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object saved(Object sort) {
        if (sort instanceof Enum && "CreatedDescending".equals(((Enum<?>) sort).name())) {
            return Enum.valueOf((Class) ((Enum<?>) sort).getDeclaringClass(), "UpdatedDescending");
        }
        return sort;
    }
}
