package app.fakh.extension.shared;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The runtime switches of the patches in this app. The features patch rewrites {@link #registry()} at patch time with
 * one line per feature of the patches that were applied, so a feature whose patch was left out is never listed and
 * never on. A switch keeps its default until the user changes it, and a feature whose dependency is off is off too.
 */
public final class Features {
    /** One switch, as the patch that implements it registered it. */
    public static final class Feature {
        public final String id;
        /** The English title, translated when shown. */
        public final String title;
        public final String section;
        /** The id of the feature this one needs on, or an empty string. */
        public final String dependsOn;
        /** Whether a change applies only after the app restarts. */
        public final boolean restart;
        /** Whether the switch is on until the user changes it. */
        public final boolean defaultOn;
        /** What the switch does in English, or an empty string when the title says it. */
        public final String description;

        Feature(String[] fields) {
            id = fields[0];
            title = fields[1];
            section = fields[2];
            dependsOn = fields[3];
            restart = Boolean.parseBoolean(fields[4]);
            defaultOn = Boolean.parseBoolean(fields[5]);
            description = fields[6];
        }
    }

    private static final String PREFS = "fakh_patches";

    private static List<Feature> all;
    private static Map<String, Feature> byId;
    private static SharedPreferences prefs;

    private Features() {
    }

    /**
     * Rewritten at patch time: one line per feature, its fields separated by tabs (id, title, section, dependsOn,
     * restart, default, description).
     */
    static String registry() {
        return "";
    }

    /** The features of the applied patches, in the order the settings list them. */
    public static synchronized List<Feature> all() {
        if (all == null) {
            List<Feature> list = new ArrayList<>();
            Map<String, Feature> ids = new HashMap<>();
            for (String line : registry().split("\n")) {
                String[] fields = line.split("\t", -1);
                if (fields.length != 7) continue;
                Feature feature = new Feature(fields);
                list.add(feature);
                ids.put(feature.id, feature);
            }
            all = Collections.unmodifiableList(list);
            byId = ids;
        }
        return all;
    }

    /**
     * Whether the patch of a feature was applied, whatever its switch says.
     *
     * @param id the id the patch registered the feature with
     */
    public static boolean has(String id) {
        all();
        return byId.containsKey(id);
    }

    /**
     * Whether a feature acts: its patch was applied, its switch is on and so is the feature it depends on. Hooks call
     * this, so any failure answers false and the app keeps its own behavior.
     *
     * @param id the id the patch registered the feature with
     */
    public static boolean on(String id) {
        try {
            all();
            Feature feature = byId.get(id);
            return feature != null && saved(id) && (feature.dependsOn.isEmpty() || on(feature.dependsOn));
        } catch (RuntimeException failure) {
            return false;
        }
    }

    /** Whether a switch is on, whatever its dependency says; its default until the user changes it. */
    public static boolean saved(String id) {
        all();
        Feature feature = byId.get(id);
        boolean defaultOn = feature == null || feature.defaultOn;
        SharedPreferences store = prefs();
        return store == null ? defaultOn : store.getBoolean(id, defaultOn);
    }

    /** Saves a switch; committed at once, since a restart may follow right after. */
    public static void set(String id, boolean on) {
        SharedPreferences store = prefs();
        if (store != null) store.edit().putBoolean(id, on).commit();
    }

    private static SharedPreferences prefs() {
        if (prefs == null) {
            Context context = AppRuntime.application();
            if (context != null) prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
        return prefs;
    }
}
