package app.fakh.extension.smartlife;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Makes the app see its original signing certificate instead of the one a patched APK is resigned with.
 * Smart Life's native security library (libthing_security) reads the app's signature and kills the process
 * when it does not match. The system IPackageManager held in ActivityThread is wrapped with a dynamic proxy
 * that rewrites the certificate in every PackageInfo returned for this package, so GET_SIGNATURES and
 * GET_SIGNING_CERTIFICATES reads yield the original. Must run before the app reads its own signature.
 */
@SuppressWarnings("unused")
public final class SignatureSpoof {
    private static final String TAG = "FakhSmartLife";

    /** This app's package; the proxy only rewrites PackageInfo for it. */
    private static final String PACKAGE_NAME = "com.tuya.smartlife";

    /** Smart Life's original signing certificate (DER, Base64). SHA-1 94dbfb934543ca1477b440add47566d757fd25ef. */
    private static final String ORIGINAL_CERT =
        "MIIDgzCCAmugAwIBAgIEe7R4yTANBgkqhkiG9w0BAQsFADBxMQswCQYDVQQGEwJDTjERMA8GA1UECBMIWmhlSmlhbmcxETAPBgNVBAcTCEhhbmd6aG91MQ4wDAYDVQQKEwV0bGlmZTEVMBMGA1UECxMMd3d3LnR1eWEuY29tMRUwEwYDVQQDEwx3d3cudHV5YS5jb20wIBcNMTYwNTE3MDM0OTA5WhgPMjA3MTAyMTgwMzQ5MDlaMHExCzAJBgNVBAYTAkNOMREwDwYDVQQIEwhaaGVKaWFuZzERMA8GA1UEBxMISGFuZ3pob3UxDjAMBgNVBAoTBXRsaWZlMRUwEwYDVQQLEwx3d3cudHV5YS5jb20xFTATBgNVBAMTDHd3dy50dXlhLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIqNaWHwN3GHIIXhxgKC7EPRaHYvkkW+CoCqID7Yd1ExIuLZGuywT7Pgyr05ktE4idxWhvP5gemzEuwSYLqPvX8VjMikLVWO/wW52n7nbdISJpOSV3fpSR0cD9JnPd7vM8sxmYYgelkNhFIdnGEnH1imYyyjgM3n19d6XNWF5wMC2k4/bCUrunUPq3IB8bKf6gNxMuefWzCTiOYKjMcIvojx6+cb76oipW2d3J1U/aeFNQ4SD72acDW3fs5p2YakiXOxZ5CIMulezWNFW5lSOptMDaKXzq4gGgcOkNOgn5MDGEhGu1ac++azlhq9y1FHJWjHF9l5Pj6bVigWcw1x5XECAwEAAaMhMB8wHQYDVR0OBBYEFFTyzFsc+0lRy8qrsbgncBD70yK5MA0GCSqGSIb3DQEBCwUAA4IBAQCIqGDKz9hormN5JS7viJeSWgG57fiAz7AF8YO2aOUAXg4mnato7WzgrZJydsZFTVjDEbRJUmMdBnHA1QsHkW77L0KSlxhaIbMHcWjjZgCkwnkMybVr7EMbbhZ0OO+9NVcuE2+Ids/ircpsxRqb8hIFNNV+jfHMOqR3ssPUHLrdDAUUgCI+05Bant4WWHg+Lm2KkiL39Q+7oK2oZRzB/nmuhrvJZxHPM5PJoV/NpNzFelwXuT4wf7SQnwzCyzFsvkaIOuhlVSnqQICVWkAUCrDQP6mxyBohop1FPhASkA3BJDJ+tSWHua0/+ZROMLq/d7VF6GQ47bojstlh/p/dfbK4";

    private static Signature originalSignature;
    private static boolean installed;

    private SignatureSpoof() {
    }

    /** Installs the proxy once. Safe to call more than once and never throws into the caller. */
    public static synchronized void install() {
        if (installed) return;
        installed = true;
        try {
            originalSignature = new Signature(Base64.decode(ORIGINAL_CERT, Base64.DEFAULT));

            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Field pmField = activityThread.getDeclaredField("sPackageManager");
            pmField.setAccessible(true);
            Object realPm = pmField.get(null);
            if (realPm == null) return;

            Class<?> ipmInterface = Class.forName("android.content.pm.IPackageManager");
            Object proxy = Proxy.newProxyInstance(
                ipmInterface.getClassLoader(),
                new Class<?>[]{ipmInterface},
                new PmHandler(realPm));
            pmField.set(null, proxy);

            // Any ApplicationPackageManager the app creates after this reads sPackageManager, so it picks up
            // the proxy; install() runs before the app first touches its PackageManager.
            Log.w(TAG, "signature spoof installed");
        } catch (Throwable t) {
            Log.w(TAG, "signature spoof failed", t);
        }
    }

    /** Rewrites the certificate of a PackageInfo for this package in place. */
    private static Object patch(Object result) {
        if (!(result instanceof PackageInfo)) return result;
        PackageInfo info = (PackageInfo) result;
        if (!PACKAGE_NAME.equals(info.packageName)) return result;
        if (info.signatures != null && info.signatures.length > 0) {
            info.signatures[0] = originalSignature;
        }
        if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) {
            Signature[] signers = info.signingInfo.getApkContentsSigners();
            if (signers != null && signers.length > 0) {
                signers[0] = originalSignature;
            }
        }
        return result;
    }

    /** Forwards every IPackageManager call to the real one, rewriting getPackageInfo results. */
    private static final class PmHandler implements InvocationHandler {
        private final Object real;

        PmHandler(Object real) {
            this.real = real;
        }

        @Override
        public Object invoke(Object proxyObj, Method method, Object[] args) throws Throwable {
            Object result;
            try {
                result = method.invoke(real, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause() != null ? e.getCause() : e;
            }
            if ("getPackageInfo".equals(method.getName())) {
                return patch(result);
            }
            return result;
        }
    }
}
