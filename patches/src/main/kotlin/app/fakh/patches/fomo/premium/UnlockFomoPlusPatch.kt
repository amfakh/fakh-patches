package app.fakh.patches.fomo.premium

import app.fakh.patches.fomo.shared.Constants.COMPATIBILITY_FOMO
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch

// ── Architecture ─────────────────────────────────────────────────────────────
//
// FOMO Indonesia (id.anon.android) is a React Native app whose entire UI runs
// from a single Hermes bundle:
//
//   assets/index.android.bundle
//
// The subscription state comes from the server's `GET user/me` response and is
// stored wholesale on the global store (`handleMeObject` assigns
// GlobalCore.user). The user object carries:
//
//   user.inner.subscription          'NONE' | 'PLUS' | 'PRO'
//   user.subscriptionExpirationDate  ISO date or null
//
// Every premium gate funnels through one exported predicate (module 2580):
//
//   hasFomoPlusSubscription():
//     sub = GlobalCore.user?.inner?.subscription
//     return sub != null && sub != 'NONE'
//
// Its callers all gate in the same direction — when it returns true the
// paywall UI is skipped and the premium path runs:
//
//   - Salary insight summary: the "Mau liat summary?" popup and the role
//     level dropdown (onSelect falls back to FomoSubscriptionScreen).
//   - Post reaction styles: _onSelectReactionStyle redirects to
//     FomoSubscriptionScreen for subscriber-only styles.
//   - ReactionStyleSegmentedPicker: locks subscriber styles.
//   - Profile: subscription status card colors and the FOMO+ label.
//
// Direct `user.inner.subscription` reads exist but are display-only (the
// settings screen label, avatar border colors). RevenueCat purchase state
// feeds the backend, not the JS gates, so faking entitlements natively is
// pointless here — the single predicate is the unlock.
//
// ── Patch strategy ───────────────────────────────────────────────────────────
//
// One in-place bytecode rewrite inside the Hermes bundle (version 98). The
// predicate compiles to function #16603, 81 bytes, frame size 6:
//
//   00 GetParentEnvironment r3, 0
//   03 LoadFromEnvironment r3, r3, 0
//   07 ... body reads user/inner/subscription, compares to NONE ...
//   4c Not r0, r0
//   4f Ret r0
//
// The patch overwrites the first four bytes with the same-length sequence
//
//   95 00 76 00   →   LoadConstTrue r0; Ret r0
//
// so the function returns true immediately; the remaining 77 bytes become
// unreachable dead code (the function has no exception handler, and Hermes
// branches never cross function boundaries, so nothing jumps into the body).
// Offsets never move, so the bundle needs no repacking.
//
// The search pattern is the full 81-byte body. It must occur exactly once in
// the bundle; a miss or an ambiguous match aborts the patch rather than
// rewriting the wrong function.
//
// ── Checksum trailer ─────────────────────────────────────────────────────────
//
// The bundle ends with a 20-byte SHA-1 of everything before it. The patch
// verifies that layout holds before touching anything, then recomputes the
// trailer after the rewrite so the file stays self-consistent.

import java.security.MessageDigest

/**
 * The compiled body of `hasFomoPlusSubscription`, byte for byte.
 *
 * Verified unique in the 9.0.64 bundle (single occurrence at offset
 * 0x0060aff8). Derived with hermes-dec (hbc-disassembler) against HBC
 * bytecode version 98.
 */
private val FUNCTION_BODY = ubyteArrayOf(
    0x34u, 0x03u, 0x00u, // 00 GetParentEnvironment r3, 0
    0x3Bu, 0x03u, 0x03u, 0x00u, // 03 LoadFromEnvironment r3, r3, 0
    0x44u, 0x04u, 0x03u, 0x00u, 0xF5u, // 07 GetByIdShort r4, r3, 0, 'user'
    0x44u, 0x04u, 0x04u, 0x01u, 0x6Bu, // 0C GetByIdShort r4, r4, 1, 'inner'
    0x94u, 0x01u, // 11 LoadConstNull r1
    0x16u, 0x00u, 0x04u, 0x01u, // 13 Eq r0, r4, r1
    0x93u, 0x02u, // 17 LoadConstUndefined r2
    0x93u, 0x05u, // 19 LoadConstUndefined r5
    0xB0u, 0x09u, 0x00u, // 1B JmpTrue 9, r0
    0x45u, 0x05u, 0x04u, 0x02u, 0x8Au, 0x9Du, // 1E GetById r5, r4, 2, 'subscription'
    0x90u, 0x04u, 0x59u, 0x89u, // 24 LoadConstString r4, 'NONE'
    0x16u, 0x00u, 0x05u, 0x04u, // 28 Eq r0, r5, r4
    0xB0u, 0x20u, 0x00u, // 2C JmpTrue 32, r0
    0x44u, 0x03u, 0x03u, 0x00u, 0xF5u, // 2F GetByIdShort r3, r3, 0, 'user'
    0x44u, 0x04u, 0x03u, 0x01u, 0x6Bu, // 34 GetByIdShort r4, r3, 1, 'inner'
    0x16u, 0x01u, 0x04u, 0x01u, // 39 Eq r1, r4, r1
    0x93u, 0x03u, // 3D LoadConstUndefined r3
    0xB0u, 0x09u, 0x01u, // 3F JmpTrue 9, r1
    0x45u, 0x03u, 0x04u, 0x02u, 0x8Au, 0x9Du, // 42 GetById r3, r4, 2, 'subscription'
    0x16u, 0x00u, 0x03u, 0x02u, // 48 Eq r0, r3, r2
    0x13u, 0x00u, 0x00u, // 4C Not r0, r0
    0x76u, 0x00u, // 4F Ret r0
).toByteArray()

/**
 * `LoadConstTrue r0; Ret r0` — the replacement written over the body's first
 * four bytes. Opcode values are HBC version 98's.
 */
private val UNLOCK_PREFIX = ubyteArrayOf(0x95u, 0x00u, 0x76u, 0x00u).toByteArray()

/** Byte length of the SHA-1 trailer appended to the Hermes bundle. */
private const val SHA1_TRAILER_SIZE = 20

/**
 * One in-place Hermes bytecode rewrite: an anchor that must occur exactly once
 * and the replacement bytes (same length as the anchor).
 */
private class HbPatch(val label: String, anchor: ByteArray, replacement: ByteArray) {
    /** Bytecode to find, byte for byte. */
    val anchor: ByteArray = anchor

    /** Replacement, same length as [anchor]. */
    val replacement: ByteArray = replacement
}

// ── Promos in the feed ───────────────────────────────────────────────────────
//
// The home feed renders every server item through `_renderFeedSelector`
// (function #15312 @ 0x005e480e). Promotional items carry one of four
// `item.inner.type` values: CLIENT_PROMO, INTERNAL_PROMO (FOMO's own cards and
// banners, e.g. "LANGGANAN F+ AJA~") and BANNER_PROMO / PROMO (sponsored
// posts). The function already has a path that returns undefined (item is not
// rendered); the original code takes it only for a CLIENT_PROMO that repeats
// the previous item's type.
//
// The 82 bytes after the `index > 0` guard are rewritten, same length, into:
//
//   type = item.inner.type
//   if (type === 'CLIENT_PROMO' || 'INTERNAL_PROMO' || 'BANNER_PROMO' || 'PROMO')
//       jump to the skip exit (0x21e)
//   jump to the render path (0x73)
//
// Each LoadConstString keeps its 16-bit string id operand, so the ids are the
// bundle's own (2381, 11650, 12558, 12561). An item at index 0 still renders,
// which the feed does not produce for promos (they land mid-feed).
//
// The anchor is the original 82 bytes, verified unique in the 9.0.64 bundle.

private val FEED_SELECTOR_ANCHOR = ubyteArrayOf(
    0x44u, 0x06u, 0x0Fu, 0x00u, 0xA6u, // 21 GetByIdShort item
    0x44u, 0x06u, 0x06u, 0x01u, 0x6Bu, // 26 GetByIdShort inner
    0x44u, 0x07u, 0x06u, 0x02u, 0xCDu, // 2B GetByIdShort type
    0x90u, 0x06u, 0x4Du, 0x09u, // 30 LoadConstString r6, 'CLIENT_PROMO' (id 2381)
    0xD5u, 0x3Fu, 0x07u, 0x06u, // 34 JStrictNotEqual -> +0x73
    0x44u, 0x06u, 0x0Fu, 0x00u, 0xA6u, // 38 GetByIdShort item
    0x44u, 0x06u, 0x06u, 0x01u, 0x6Bu, // 3D GetByIdShort inner
    0x44u, 0x07u, 0x06u, 0x02u, 0xCDu, // 42 GetByIdShort type
    0x44u, 0x06u, 0x0Eu, 0x03u, 0xE1u, // 47 GetByIdShort state
    0x45u, 0x06u, 0x06u, 0x04u, 0x47u, 0xDCu, // 4C GetById dataSource
    0x8Bu, 0x00u, 0x01u, 0x26u, 0x00u, 0x05u, 0x00u, // 52 LoadConstUInt8/Sub/GetByVal
    0x5Du, 0x06u, 0x06u, 0x00u, // 59 GetByVal
    0x44u, 0x06u, 0x06u, 0x00u, 0xA6u, // 5D GetByIdShort item
    0x44u, 0x06u, 0x06u, 0x01u, 0x6Bu, // 62 GetByIdShort inner
    0x44u, 0x06u, 0x06u, 0x02u, 0xCDu, // 67 GetByIdShort type
    0xD4u, 0xB2u, 0x01u, 0x00u, 0x00u, 0x07u, 0x06u, // 6C JStrictEqualLong r7, r6 -> +0x21e (skip)
).toByteArray()

private val FEED_SELECTOR_REPLACEMENT = ubyteArrayOf(
    0x44u, 0x06u, 0x0Fu, 0x00u, 0xA6u, // 21 GetByIdShort item
    0x44u, 0x06u, 0x06u, 0x01u, 0x6Bu, // 26 GetByIdShort inner
    0x44u, 0x07u, 0x06u, 0x02u, 0xCDu, // 2B GetByIdShort type
    0x90u, 0x06u, 0x4Du, 0x09u, // 30 LoadConstString r6, 'CLIENT_PROMO' (id 2381)
    0xD4u, 0xEAu, 0x01u, 0x00u, 0x00u, 0x07u, 0x06u, // 34 JStrictEqualLong r7, r6 -> +0x21e (skip)
    0x90u, 0x06u, 0x82u, 0x2Du, // 3B LoadConstString r6, 'INTERNAL_PROMO' (id 11650)
    0xD4u, 0xDFu, 0x01u, 0x00u, 0x00u, 0x07u, 0x06u, // 3F JStrictEqualLong r7, r6 -> +0x21e (skip)
    0x90u, 0x06u, 0x0Eu, 0x31u, // 46 LoadConstString r6, 'BANNER_PROMO' (id 12558)
    0xD4u, 0xD4u, 0x01u, 0x00u, 0x00u, 0x07u, 0x06u, // 4A JStrictEqualLong r7, r6 -> +0x21e (skip)
    0x90u, 0x06u, 0x11u, 0x31u, // 51 LoadConstString r6, 'PROMO' (id 12561)
    0xD4u, 0xC9u, 0x01u, 0x00u, 0x00u, 0x07u, 0x06u, // 55 JStrictEqualLong r7, r6 -> +0x21e (skip)
    0xAEu, 0x17u, // 5C Jmp +0x17 -> 0x73 (render the item)
    0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, // 5E dead padding up to 0x72
).toByteArray()

// ── "Gunakan logo default" 403 alert ─────────────────────────────────────────
//
// Resetting the avatar POSTs `user/me/reset-avatar`; the server answers 403
// for non-subscribers and the shared fetch layer (`asyncFetch`, function
// #39298 @ 0x0079677d) shows the "Anda tidak bisa mengakses konten ini"
// alert. The status dispatch compares `response.status === 403` and jumps to
// the alert branch; the rewrite retargets that jump to the layer's quiet exit
// (the same tail every non-alerting status arm shares), so a 403 neither
// alerts nor blocks the loading promise.
//
// Derived from the dispatch at +0x193 in the 9.0.64 bundle; the context is
// verified unique.

private val FORBIDDEN_DISPATCH_ANCHOR = ubyteArrayOf(
    0x44u, 0x23u, 0x23u, 0x01u, 0x29u, // 18E GetByIdShort status
    0xD4u, 0x00u, 0x02u, 0x00u, 0x00u, 0x23u, // 193 JStrictEqualLong -> +0x393, r35, r13
).toByteArray()

private val FORBIDDEN_DISPATCH_REPLACEMENT = ubyteArrayOf(
    0x44u, 0x23u, 0x23u, 0x01u, 0x29u,
    0xD4u, 0x8Au, 0x03u, 0x00u, 0x00u, 0x23u, // JStrictEqualLong -> +0x523 (quiet exit)
).toByteArray()

private val BUNDLE_PATCHES = listOf(
    HbPatch("promo feed skip", FEED_SELECTOR_ANCHOR, FEED_SELECTOR_REPLACEMENT),
    HbPatch("403 alert quiet exit", FORBIDDEN_DISPATCH_ANCHOR, FORBIDDEN_DISPATCH_REPLACEMENT),
)

@Suppress("unused")
val fomoUnlockPlusPatch = rawResourcePatch(
    name = "Unlock FOMO+",
    description = "Unlocks FOMO+ on your phone: salary insight summary and role levels, " +
        "subscriber reaction styles, and the premium profile perks. The paywall screens " +
        "no longer block these features. Also hides promo cards, banners and sponsored " +
        "posts in the feed. Salary content the server withholds from non-subscribers " +
        "stays out of reach.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_FOMO)
    category("Premium")

    execute {
        val bundleFile = get("assets/index.android.bundle")
        if (!bundleFile.exists()) {
            throw PatchException(
                "FOMO: assets/index.android.bundle not found. The bundle path may have changed.",
            )
        }

        val bundle = bundleFile.readBytes()
        if (bundle.isEmpty()) throw PatchException("FOMO: index.android.bundle is empty.")
        if (bundle.size <= SHA1_TRAILER_SIZE) {
            throw PatchException("FOMO: index.android.bundle is too small to be a Hermes bundle.")
        }

        // Verify the SHA-1 trailer before rewriting, so we never corrupt a
        // bundle whose trailing layout differs from what we assume.
        val storedSha1 = bundle.copyOfRange(bundle.size - SHA1_TRAILER_SIZE, bundle.size)
        val computedSha1 = MessageDigest
            .getInstance("SHA-1")
            .digest(bundle.copyOfRange(0, bundle.size - SHA1_TRAILER_SIZE))
        if (!storedSha1.contentEquals(computedSha1)) {
            throw PatchException(
                "FOMO: index.android.bundle does not end with a SHA-1 of its own bytes. " +
                    "The checksum trailer layout may have changed.",
            )
        }

        fun occurrencesOf(pattern: ByteArray): List<Int> {
            val idxs = mutableListOf<Int>()
            var i = 0
            while (i <= bundle.size - pattern.size) {
                if (bundle.copyOfRange(i, i + pattern.size).contentEquals(pattern)) {
                    idxs.add(i)
                    i += pattern.size
                } else {
                    i++
                }
            }
            return idxs
        }

        // Detect a double apply first: the patched body keeps its 77-byte tail
        // after the four rewritten bytes, so it matches body-with-new-prefix.
        val patched = UNLOCK_PREFIX + FUNCTION_BODY.copyOfRange(4, FUNCTION_BODY.size)
        if (occurrencesOf(patched).isNotEmpty()) {
            throw PatchException(
                "FOMO: hasFomoPlusSubscription is already patched. " +
                    "Check the patch pipeline for a duplicate apply.",
            )
        }

        val matches = occurrencesOf(FUNCTION_BODY)
        if (matches.isEmpty()) {
            throw PatchException(
                "FOMO: hasFomoPlusSubscription body not found in index.android.bundle. " +
                    "The bundle may have changed — re-derive the bytecode pattern.",
            )
        }
        if (matches.size > 1) {
            throw PatchException(
                "FOMO: hasFomoPlusSubscription body found ${matches.size} times in " +
                    "index.android.bundle; expected exactly 1. Refusing an ambiguous rewrite.",
            )
        }

        UNLOCK_PREFIX.copyInto(bundle, matches[0])

        for (patch in BUNDLE_PATCHES) {
            val idx = occurrencesOf(patch.anchor)
            if (idx.isEmpty()) {
                throw PatchException(
                    "FOMO: '${patch.label}' anchor not found in index.android.bundle. " +
                        "The bundle may have changed — re-derive the bytecode pattern.",
                )
            }
            if (idx.size > 1) {
                throw PatchException(
                    "FOMO: '${patch.label}' anchor found ${idx.size} times in " +
                        "index.android.bundle; expected exactly 1. Refusing an ambiguous rewrite.",
                )
            }
            patch.replacement.copyInto(bundle, idx[0])
        }

        // Keep the trailer a valid SHA-1 of the patched bytes.
        MessageDigest
            .getInstance("SHA-1")
            .digest(bundle.copyOfRange(0, bundle.size - SHA1_TRAILER_SIZE))
            .copyInto(bundle, bundle.size - SHA1_TRAILER_SIZE)

        bundleFile.writeBytes(bundle)
    }
}
