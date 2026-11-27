/*
 * Copyright 2026 amfakh
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.fakh.patches.github

import app.fakh.patches.github.shared.Constants.COMPATIBILITY_GITHUB
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import java.io.File

/**
 * Appends [text] to one of the files of the WebView that renders READMEs, issue and pull request bodies and comments.
 *
 * @param file `assets/webview/markdown.css` or `assets/webview/markdown.js`.
 * @param text The CSS rules or the script to add after the app's own.
 */
private fun appendToMarkdownView(file: File, text: String) {
    if (!file.exists()) throw PatchException("GitHub: ${file.name} not found in assets/webview")
    file.appendText("\n/* Fakh Patches */\n${text.trimIndent()}\n")
}

/**
 * Runs [body] after the WebView loads each piece of content, by wrapping `GitHub.prototype._didLoad`, which
 * `markdown.js` calls once the HTML is in the page and before it reports the height to the app.
 */
private fun afterLoad(body: String) = """
    ;(() => {
      const didLoad = GitHub.prototype._didLoad
      GitHub.prototype._didLoad = function () {
        try {
    ${body.trimIndent().prependIndent("        ")}
        } catch (e) {}
        didLoad.call(this)
      }
    })()
"""

@Suppress("unused")
val tabWidthPatch = patch(
    name = "Tab width",
    description = "Shows tabs in code blocks as 2, 4 or 8 spaces wide. The app uses 8.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GITHUB)
    category("Feature")

    val width = intOption(
        key = "width",
        default = 4,
        values = listOf(2, 4, 8).associateBy { "$it spaces" },
        title = "Width",
        description = "How many spaces a tab takes in code blocks.",
        required = true,
    ) { value -> value in listOf(2, 4, 8) }

    execute {
        appendToMarkdownView(
            resourceFile("assets/webview/markdown.css"),
            """
                .markdown-body pre, .markdown-body code { tab-size: ${width.value}; -moz-tab-size: ${width.value}; }
            """,
        )
    }
}

@Suppress("unused")
val collapseLongCodeBlocksPatch = patch(
    name = "Collapse long code blocks",
    description = "Folds code blocks longer than 25 lines to their first 20, with a button that shows all of them.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GITHUB)
    category("Feature")

    execute {
        // The WebView is as tall as its content and sits in a scrolling list, so a code block with its own vertical
        // scroll never gets the swipe. Folding it keeps the list short and a tap opens it in place.
        appendToMarkdownView(
            resourceFile("assets/webview/markdown.css"),
            """
                .markdown-body pre.fakh-folded {
                  max-height: calc(20 * 1.45em + 32px);
                  overflow-y: hidden;
                  -webkit-mask-image: linear-gradient(to bottom, #000 70%, transparent);
                  mask-image: linear-gradient(to bottom, #000 70%, transparent);
                }
                .markdown-body .fakh-unfold {
                  display: block;
                  box-sizing: border-box;
                  width: 100%;
                  text-align: center;
                  margin: -12px 0 16px;
                  padding: 8px;
                  font: inherit;
                  font-size: 85%;
                  color: var(--link);
                  background: transparent;
                  border: 1px solid var(--border);
                  border-radius: 6px;
                }
            """,
        )
        appendToMarkdownView(
            resourceFile("assets/webview/markdown.js"),
            afterLoad(
                """
                    for (const pre of document.querySelectorAll('#content pre')) {
                      const lines = pre.textContent.replace(/\n$/, '').split('\n').length
                      if (lines <= 25 || pre.classList.contains('fakh-folded')) continue
                      pre.classList.add('fakh-folded')
                      // Not a <button>: removing a focused element makes the app scroll back to the top of the body.
                      const button = document.createElement('div')
                      button.className = 'fakh-unfold'
                      button.setAttribute('role', 'button')
                      button.textContent = 'Show all ' + lines + ' lines'
                      button.addEventListener('click', () => {
                        pre.classList.remove('fakh-folded')
                        button.remove()
                      })
                      pre.after(button)
                    }
                """,
            ),
        )
    }
}

@Suppress("unused")
val shortenLinksPatch = patch(
    name = "Shorten links",
    description = "Shows pasted links in comments and READMEs as short references, such as owner/repo#123.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GITHUB)
    category("Feature")

    execute {
        // Only links whose text is their own address are rewritten; a link with its own text stays as written.
        // The address itself is not touched, so tapping it opens the same page.
        appendToMarkdownView(
            resourceFile("assets/webview/markdown.js"),
            afterLoad(
                """
                    const reserved = new Set(['about', 'apps', 'collections', 'explore', 'features', 'marketplace',
                      'new', 'notifications', 'orgs', 'organizations', 'pricing', 'search', 'settings', 'sponsors',
                      'topics', 'trending', 'users'])
                    const sha = (ref) => /^[0-9a-f]{40}$/.test(ref) ? ref.slice(0, 7) : ref
                    const shorten = (href) => {
                      const url = new URL(href)
                      if (url.protocol !== 'https:' && url.protocol !== 'http:') return null
                      const host = url.hostname.replace(/^www\./, '')
                      const path = decodeURIComponent(url.pathname).replace(/\/$/, '')
                      const plain = host + path + url.search + url.hash
                      if (host !== 'github.com') return plain
                      const [owner, repo, type, ...rest] = path.split('/').slice(1)
                      if (!owner || reserved.has(owner)) return plain
                      if (!repo) return url.search || url.hash ? plain : '@' + owner
                      const slug = owner + '/' + repo
                      if (!type) return url.search || url.hash ? plain : slug
                      const comment = url.hash ? ' (comment)' : ''
                      if (['issues', 'pull', 'discussions'].includes(type) && /^\d+$/.test(rest[0] || '')) {
                        const tab = rest.length > 1 ? ' (' + rest.slice(1).join('/') + ')' : ''
                        return slug + '#' + rest[0] + (tab || comment)
                      }
                      if (type === 'commit' && rest.length === 1) return slug + '@' + sha(rest[0]) + comment
                      if (type === 'releases' && rest[0] === 'tag' && rest.length === 2) return slug + '@' + rest[1]
                      if (type === 'compare' && rest.length === 1) return slug + '@' + rest[0]
                      if ((type === 'blob' || type === 'tree') && rest.length >= 1) {
                        const file = rest.slice(1).join('/')
                        return slug + (file ? '/' + file : '') + '@' + sha(rest[0]) + (url.hash.startsWith('#L') ? url.hash : '')
                      }
                      return plain
                    }
                    for (const link of document.querySelectorAll('#content a[href]')) {
                      if (link.childElementCount > 0) continue
                      const text = link.textContent.trim()
                      if (text !== link.href && text !== link.getAttribute('href') && text + '/' !== link.href) continue
                      try {
                        const short = shorten(link.href)
                        if (short) link.textContent = short
                      } catch (e) {}
                    }
                """,
            ),
        )
    }
}
