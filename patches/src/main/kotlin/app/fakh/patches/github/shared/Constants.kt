/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.patches.github.shared

import app.fakh.patches.shared.Feature
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_GITHUB = Compatibility(
        name = "GitHub",
        packageName = "com.github.android",
        signatures = setOf("df08c9f2d809189d9d506497c15745a7395a41536efb433e3aee1aedbe11b261"),
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x1B1F23,
        targets = listOf(AppTarget(version = "1.285.0", versionCode = 956)),
    )

    // The rows of the Fakh Patches pages, a page per section and a header per group, in this order. A row is a switch,
    // except the ones the extension opens a sheet for: the navigation bar tabs, the landscape position, the username on
    // Home, the recent branches, the tab width and the download folder.
    val MATERIAL_YOU = Feature("fakh_material_you", "Material You", "Appearance")
    val PITCH_BLACK = Feature("fakh_amoled", "Pitch black", "Appearance", restart = false, default = false)
    val ICON_ONLY_BUTTONS = Feature(
        "fakh_icon_only_buttons", "Icon-only floating buttons", "Appearance", restart = false,
        description = "Floating buttons such as Comment show without their text.",
    )
    val HOME_TITLE = Feature("fakh_home_title", "Username on Home", "Appearance")

    val NAV_TABS = Feature("fakh_nav_tabs", "Navigation bar tabs", "Navigation bar")
    val HIDE_TAB_LABELS = Feature("fakh_hide_nav_labels", "Hide tab labels", "Navigation bar", default = false)
    val GLASS_NAVBAR = Feature("fakh_glass_navbar", "Liquid Glass navigation bar", "Navigation bar")
    val BACKGROUND_BLUR = Feature(
        "fakh_liquid_glass", "Background blur", "Navigation bar", dependsOn = GLASS_NAVBAR, default = false,
        description = "Blurs what's behind the glass. Uses more battery and might stutter.",
    )
    val NAV_RIGHT = Feature("fakh_nav_right", "Landscape navigation bar position", "Navigation bar", default = false)
    val SMALLER_NAVBAR = Feature("fakh_smaller_navbar", "Smaller navigation bar", "Navigation bar")

    val CONVENTIONAL_COMMITS = Feature(
        "fakh_conventional_commits", "Conventional commits", "Titles and lists",
        description = "Titles such as feat: add login show their type as a pill.",
        group = "General",
    )
    val EXACT_TIME = Feature(
        "fakh_exact_time", "Show exact time", "Titles and lists",
        description = "Dates and times instead of ages such as 20d.",
        group = "General",
    )
    val DIM_BOTS = Feature(
        "fakh_dim_bots", "Dim bots", "Titles and lists",
        description = "Fades commits, issues, pull requests and events by bots.",
        group = "General",
    )
    val HEADER_LABELS = Feature(
        "fakh_header_labels", "Labels in the header", "Titles and lists", restart = false,
        description = "Labels under the title of an issue or pull request.",
        group = "Issues and pull requests",
    )
    val RECENTLY_UPDATED = Feature(
        "fakh_recently_updated", "Recently updated first", "Titles and lists",
        description = "Issue and pull request lists open sorted by Recently updated.",
        group = "Issues and pull requests",
    )
    val PR_BASE_BRANCH = Feature(
        "fakh_pr_base_branch", "PR base branch", "Titles and lists", restart = false,
        description = "Shows the base branch in pull request lists when it is not the default branch.",
        group = "Issues and pull requests",
    )

    val REPOSITORY_AGE = Feature(
        "fakh_repository_age", "Repository age", "Repositories", restart = false,
        description = "When a repository was created, under its stars and forks.",
        group = "Overview",
    )
    val PINNED_BRANCHES = Feature(
        "fakh_pinned_branches", "Pinned branches", "Repositories", restart = false,
        description = "Lists develop, main, master and staging right after the default branch in the branch picker.",
        group = "Branches",
    )
    val RECENT_BRANCHES = Feature(
        "fakh_recent_branches", "Recent branches", "Repositories", restart = false, group = "Branches",
    )
    val COMMIT_TAGS = Feature(
        "fakh_commit_tags", "Tags on commits", "Repositories", restart = false,
        description = "Displays git tags next to matching commits in commit lists.",
        group = "Commits",
    )

    val FOLLOWS_YOU = Feature(
        "fakh_follows_you", "Follows you", "Profiles", restart = false,
        description = "Shows on user profiles when that user follows you.",
    )
    val LINKIFY_LOCATION = Feature(
        "fakh_linkify_location", "Linkify user location", "Profiles", restart = false,
        description = "Makes the location on a user profile open in Maps when tapped.",
    )

    val COLLAPSE_BOTS = Feature(
        "fakh_collapse_bots", "Collapse bot comments", "Comments and markdown", restart = false,
        description = "Comments by bots and agents start collapsed.",
        group = "Comments",
    )
    val HIDE_LOW_QUALITY = Feature(
        "fakh_hide_low_quality", "Hide low-quality comments", "Comments and markdown", restart = false,
        description = "Hides comments that only say +1, me too or ask for news.",
        group = "Comments",
    )
    val PREVIEW_HIDDEN = Feature(
        "fakh_preview_hidden", "Preview hidden comments", "Comments and markdown", restart = false,
        description = "The start of a hidden comment shows under its reason.",
        group = "Comments",
    )
    val HIDE_COPILOT_NOTE = Feature(
        "fakh_hide_copilot_note", "Hide Copilot note", "Comments and markdown", restart = false,
        description = "Removes the AI mistakes line under Copilot's comments.",
        group = "Comments",
    )
    val MENTION_AVATARS = Feature(
        "fakh_mention_avatars", "Mention avatars", "Comments and markdown", restart = false,
        description = "A small avatar before each @mention.",
        group = "Markdown",
    )
    val SHORTEN_LINKS = Feature(
        "fakh_shorten_links", "Shorten links", "Comments and markdown", restart = false,
        description = "Pasted links read as short references such as owner/repo#123.",
        group = "Markdown",
    )
    val COLLAPSE_CODE = Feature(
        "fakh_collapse_code", "Collapse long code blocks", "Comments and markdown", restart = false,
        description = "Code blocks longer than 25 lines start folded.",
        group = "Markdown",
    )
    val EDITOR_BUTTONS = Feature(
        "fakh_editor_buttons", "Section and table buttons", "Comments and markdown", restart = false,
        description = "Toolbar buttons that add a collapsible section or a table.",
        group = "Markdown",
    )
    val MERMAID = Feature(
        "fakh_mermaid", "Mermaid diagrams", "Comments and markdown", restart = false,
        description = "Draws Mermaid code blocks as diagrams.",
        group = "Markdown",
    )

    val TAB_WIDTH = Feature("fakh_tab_width", "Tab width", "Code and files")
    val WHITESPACE = Feature(
        "fakh_whitespace", "Show whitespace", "Code and files", restart = false,
        description = "Spaces and tabs show as dim marks in files and diffs.",
    )
    val HIDE_DIFF_SIGNS = Feature(
        "fakh_hide_diff_signs", "Hide diff signs", "Code and files", restart = false,
        description = "Hides + and - signs on diff lines.",
    )

    val LINKIFY_CODE = Feature(
        "fakh_linkify_code", "Linkify code", "Code and files", restart = false,
        description = "Addresses and owner/repo#123 in code open when tapped.",
    )
    val HTML_PREVIEW = Feature(
        "fakh_html_preview", "HTML preview", "Code and files", restart = false,
        description = "Preview in the menu of an HTML file.",
    )

    val DOWNLOAD_FILES = Feature(
        "fakh_download_files", "Download files", "Releases and downloads", restart = false,
        description = "Download in the menu of an open file.",
    )
    val DOWNLOAD_ASSETS = Feature(
        "fakh_download_assets", "Download assets", "Releases and downloads", restart = false,
        description = "Release files download in the app instead of the browser.",
    )
    val DOWNLOAD_FOLDER = Feature("fakh_download_folder", "Download location", "Releases and downloads")
    val DOWNLOAD_COUNT = Feature(
        "fakh_download_count", "Release download count", "Releases and downloads", restart = false,
        description = "How often each release file was downloaded.",
    )
}
