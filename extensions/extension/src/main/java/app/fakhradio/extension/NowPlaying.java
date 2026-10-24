package app.fakhradio.extension;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Rewrites the media session metadata the app publishes, so the system media
 * card and notification show the song instead of the station.
 *
 * <p>The app's {@code MediaMetadataCompat} and {@code MediaSessionCompat} keep
 * their class names but R8 renames their methods, so this class only touches
 * them through {@link Parcel} (the metadata parcels as a single {@link Bundle})
 * and through a reflective lookup by method signature.
 */
@SuppressWarnings("unused")
public final class NowPlaying {
    private static final String TITLE = "android.media.metadata.TITLE";
    private static final String ARTIST = "android.media.metadata.ARTIST";
    private static final String ALBUM = "android.media.metadata.ALBUM";
    private static final String ART = "android.media.metadata.ART";
    private static final String ART_URI = "android.media.metadata.ART_URI";
    private static final String DISPLAY_TITLE = "android.media.metadata.DISPLAY_TITLE";
    private static final String DISPLAY_SUBTITLE = "android.media.metadata.DISPLAY_SUBTITLE";
    private static final String DISPLAY_ICON_URI = "android.media.metadata.DISPLAY_ICON_URI";
    private static final int MAX_ART_SIZE = 512;

    private static final ExecutorService loader = Executors.newSingleThreadExecutor();
    private static final Handler main = new Handler(Looper.getMainLooper());

    private static String artUrl;
    private static Bitmap art;
    private static Object lastSession;
    private static Parcelable lastMetadata;

    private NowPlaying() {
    }

    /**
     * Called by the patched {@code MediaService2.Z()} right before it hands
     * {@code metadata} to {@code session}. Returns the metadata to publish instead.
     *
     * @param session  the service's {@code MediaSessionCompat}
     * @param metadata the {@code MediaMetadataCompat} the app built, or null
     * @return the rewritten metadata, or {@code metadata} unchanged on any failure
     */
    public static synchronized Parcelable decorate(Object session, Parcelable metadata) {
        lastSession = session;
        lastMetadata = metadata;
        if (metadata == null) {
            return null;
        }
        try {
            Bundle bundle = toBundle(metadata);
            String url = firstNonEmpty(bundle.getString(ART_URI), bundle.getString(DISPLAY_ICON_URI));
            CharSequence song = bundle.getString(TITLE);
            if (!TextUtils.isEmpty(song)) {
                // The station moves to the album line, where players that show
                // three lines still list it.
                bundle.putCharSequence(ALBUM, bundle.getCharSequence(DISPLAY_TITLE));
                bundle.putCharSequence(DISPLAY_TITLE, song);
                bundle.putCharSequence(DISPLAY_SUBTITLE, bundle.getString(ARTIST));
            }
            if (url != null && url.equals(artUrl) && art != null) {
                bundle.putParcelable(ART, art);
            } else if (url != null && !url.equals(artUrl)) {
                load(url);
            }
            return fromBundle(metadata, bundle);
        } catch (Throwable t) {
            return metadata;
        }
    }

    /**
     * Downloads {@code url} off the main thread, then publishes the last metadata
     * again so it picks up the bitmap.
     */
    private static void load(String url) {
        artUrl = url;
        art = null;
        loader.execute(() -> {
            Bitmap bitmap = download(url);
            synchronized (NowPlaying.class) {
                if (bitmap == null || !url.equals(artUrl)) {
                    return;
                }
                art = bitmap;
            }
            main.post(NowPlaying::republish);
        });
    }

    /** Sets the last metadata on the last session again, now with the bitmap attached. */
    private static void republish() {
        Object session;
        Parcelable metadata;
        synchronized (NowPlaying.class) {
            session = lastSession;
            metadata = lastMetadata;
        }
        if (session == null || metadata == null) {
            return;
        }
        try {
            Parcelable decorated = decorate(session, metadata);
            for (Method method : session.getClass().getMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (method.getReturnType() == void.class && parameters.length == 1
                        && parameters[0] == metadata.getClass()) {
                    method.invoke(session, decorated);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // The next metadata update from the app attaches the cached bitmap anyway.
        }
    }

    /** Fetches and decodes the image at {@code url}, scaled down to about {@code MAX_ART_SIZE} pixels. */
    private static Bitmap download(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            byte[] data;
            try (InputStream in = connection.getInputStream()) {
                data = readAll(in);
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (options.inSampleSize * 2) >= MAX_ART_SIZE) {
                options.inSampleSize *= 2;
            }
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (Throwable t) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Reads {@code in} to the end. */
    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /** Unpacks the metadata's backing bundle; MediaMetadataCompat parcels as exactly that. */
    private static Bundle toBundle(Parcelable metadata) {
        Parcel parcel = Parcel.obtain();
        try {
            metadata.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            Bundle bundle = parcel.readBundle(metadata.getClass().getClassLoader());
            return bundle != null ? bundle : new Bundle();
        } finally {
            parcel.recycle();
        }
    }

    /** Builds a new metadata of the same class as {@code template} around {@code bundle}, through its CREATOR. */
    private static Parcelable fromBundle(Parcelable template, Bundle bundle) throws ReflectiveOperationException {
        Parcelable.Creator<?> creator =
                (Parcelable.Creator<?>) template.getClass().getField("CREATOR").get(null);
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(bundle);
            parcel.setDataPosition(0);
            return (Parcelable) creator.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }

    /** Returns {@code first} unless it is null or empty, else {@code second} unless empty, else null. */
    private static String firstNonEmpty(String first, String second) {
        if (!TextUtils.isEmpty(first)) {
            return first;
        }
        return TextUtils.isEmpty(second) ? null : second;
    }
}
