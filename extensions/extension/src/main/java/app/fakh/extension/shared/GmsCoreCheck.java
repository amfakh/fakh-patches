package app.fakh.extension.shared;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Tells the user that signing in with Google needs GmsCore (microG RE) when it is not installed. Each app has a small
 * class of its own that calls {@link #require} with its own message, and the GmsCore patch of the app calls that class
 * where the app starts the Google sign-in.
 */
public final class GmsCoreCheck {
    private static final String GMS_CORE = "app.revanced.android.gms";
    private static boolean warned;

    private GmsCoreCheck() {
    }

    /**
     * Shows [message] once per process when GmsCore is missing; does nothing when it is installed.
     *
     * @param message what to tell the user, in the app's own words
     */
    public static void require(String message) {
        Application app = AppRuntime.application();
        if (app == null || warned) return;
        try {
            app.getPackageManager().getPackageInfo(GMS_CORE, 0);
        } catch (PackageManager.NameNotFoundException missing) {
            warned = true;
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(app, message, Toast.LENGTH_LONG).show());
        }
    }
}
