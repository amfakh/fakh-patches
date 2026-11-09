package app.fakh.extension.linkedin;

import app.fakh.extension.shared.GmsCoreCheck;

/** The message LinkedIn shows when Google sign-in is started without GmsCore installed. */
public final class GmsCore {
    private GmsCore() {
    }

    /** Called where the app starts the Google sign-in. */
    public static void require() {
        GmsCoreCheck.require("\"Continue with Google\" needs microG RE. Install it and sign in to a Google account, then try again.");
    }
}
