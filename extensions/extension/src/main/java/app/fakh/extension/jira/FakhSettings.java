package app.fakh.extension.jira;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import app.fakh.extension.shared.GlassTabs;

/**
 * The Fakh Patches category of Jira's Settings: a card of switch rows between the Language/Theme card and the Quick
 * Lock card, built from the app's own composables by reflection so the rows look like its own. Changing a switch
 * saves it and offers a restart in a snackbar, since the palettes and the bar are built once.
 */
public final class FakhSettings {
    /** Keys of the rows' content items, which only have to differ from each other. */
    private static final int ROW_KEY = 0x46414B30;
    private static final int CARD_KEY = 0x46414B3F;
    /** Number of the first switch that belongs to a tab, and the number of the row that opens the tab sheet. */
    private static final int FIRST_TAB = 4;
    private static final int TABS_ROW = 4;
    private static final String SHEET_TAG = "fakh_tabs_sheet";

    private static WeakReference<Activity> current = new WeakReference<>(null);
    /** A Compose state that counts the switches flipped, so the rows are drawn again when one is. */
    private static Object version;

    private FakhSettings() {
    }

    /** Keeps the activity that is being created, for the snackbar. */
    static void remember(Activity activity) {
        current = new WeakReference<>(activity);
    }

    /**
     * Injected after the Language/Theme card of the main Settings list: draws the Fakh Patches card with the app's own
     * card composable.
     *
     * @param composer the Compose composer of the screen, which is only read
     */
    public static void addCard(Object composer) {
        try {
            ClassLoader loader = composer.getClass().getClassLoader();
            Class<?> items = Class.forName("com.atlassian.jira.infrastructure.compose.ui.SpacedItemsKt", true, loader);
            Class<?> function1 = Class.forName("kotlin.jvm.functions.Function1", true, loader);
            Method card = null;
            for (Method method : items.getDeclaredMethods()) {
                if (method.getName().equals("a") && method.getParameterTypes().length == 7) card = method;
            }
            if (card == null) return;
            version = remember(composer, loader);
            Object builder = Proxy.newProxyInstance(loader, new Class<?>[] {function1}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    if (args == null || args.length != 1) return MaterialYou.objectMethod(proxy, method, args);
                    addRows(args[0]);
                    return null;
                }
            });
            // Modifier, gap and shape are the card's own defaults.
            card.invoke(null, null, 0f, null, builder, composer, 0, 1 | 2 | 4);
        } catch (Throwable error) {
            android.util.Log.w("FakhPatches", "Settings card", error);
        }
    }

    /** A Compose state kept across recompositions of the screen, made through the composer's remember slots. */
    private static Object remember(Object composer, ClassLoader loader) throws ReflectiveOperationException {
        Class<?> composerType = composer.getClass();
        Method read = null;
        Method write = null;
        for (Method method : composerType.getMethods()) {
            if (method.getName().equals("m0") && method.getParameterTypes().length == 0) read = method;
            if (method.getName().equals("P0") && method.getParameterTypes().length == 1) write = method;
        }
        if (read == null || write == null) return null;
        Object empty = Class.forName("oy3", true, loader).getField("b").get(null);
        Object cached = read.invoke(composer);
        if (cached != empty) return cached;
        Object policy = Class.forName("wzd", true, loader).getField("e").get(null);
        Object state = null;
        for (Method make : Class.forName("apn", true, loader).getDeclaredMethods()) {
            Class<?>[] types = make.getParameterTypes();
            if (make.getName().equals("a") && types.length == 2 && types[0] == Object.class && types[1].isInstance(policy)) {
                state = make.invoke(null, 0, policy);
                break;
            }
        }
        if (state == null) throw new NoSuchMethodException("mutableStateOf");
        write.invoke(composer, state);
        return state;
    }

    /** Adds every Fakh Patches row to the card's list scope. */
    private static void addRows(Object scope) {
        try {
            ClassLoader loader = scope.getClass().getClassLoader();
            Class<?> items = Class.forName("com.atlassian.jira.infrastructure.compose.ui.SpacedItemsKt", true, loader);
            Class<?> wrapper = Class.forName("ta4", true, loader);
            Class<?> rowType = Class.forName("com.atlassian.jira.feature.settings.impl.SettingsKt", true, loader);
            Class<?> lambdaType = Class.forName("kotlin.jvm.functions.Function2", true, loader);
            Method add = null;
            for (Method method : items.getDeclaredMethods()) {
                if (method.getName().equals("b") && method.getParameterTypes().length == 5) add = method;
            }
            Constructor<?> create = wrapper.getConstructor(int.class, boolean.class, Object.class);
            if (add == null) return;
            for (int which = 0; which <= TABS_ROW; which++) {
                Object content = create.newInstance(ROW_KEY + which, true, row(lambdaType, rowType, which));
                // Scope, modifier (default), click (none), content, defaults.
                add.invoke(null, scope, null, null, content, 7);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    /** The composable lambda of one row, as an instance of the app's lambda interface. */
    private static Object row(Class<?> lambdaType, final Class<?> rowType, final int which) {
        return Proxy.newProxyInstance(lambdaType.getClassLoader(), new Class<?>[] {lambdaType}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (args == null || args.length != 2) return MaterialYou.objectMethod(proxy, method, args);
                for (Method row : rowType.getDeclaredMethods()) {
                    Class<?>[] types = row.getParameterTypes();
                    if (!row.getName().equals("e") || types.length != 8 || types[0] != String.class || types[1] != boolean.class) continue;
                    Context context = MaterialYou.application();
                    if (context == null) return null;
                    // Reading the state makes this row draw again when a switch is flipped.
                    readVersion();
                    if (which == TABS_ROW) {
                        tabsRow(args[0]);
                        break;
                    }
                    boolean selected = MaterialYou.prefs(context).getBoolean(MaterialYou.KEYS[which], MaterialYou.DEFAULTS[which]);
                    // Label, checked, onChange, subtitle (none), enabled (default), composer, changed, defaults.
                    row.invoke(null, MaterialYou.LABELS[which], selected, toggle(types[2], which), null, false, args[0], 0, 24);
                    break;
                }
                return null;
            }
        });
    }

    private static void readVersion() {
        try {
            if (version != null) version.getClass().getMethod("getValue").invoke(version);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void bumpVersion() {
        try {
            if (version == null) return;
            Object now = version.getClass().getMethod("getValue").invoke(version);
            version.getClass().getMethod("setValue", Object.class).invoke(version, ((Integer) now) + 1);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    /** The change callback of a row: flips its switch, saves it, redraws the rows and offers a restart. */
    private static Object toggle(Class<?> type, final int which) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if (args == null || args.length != 1 || !(args[0] instanceof Boolean)) return MaterialYou.objectMethod(proxy, method, args);
                Context context = MaterialYou.application();
                if (context == null) return null;
                SharedPreferences prefs = MaterialYou.prefs(context);
                prefs.edit().putBoolean(MaterialYou.KEYS[which], !prefs.getBoolean(MaterialYou.KEYS[which], MaterialYou.DEFAULTS[which])).commit();
                bumpVersion();
                showSnackbar();
                return null;
            }
        });
    }

    /** The row that opens the tab sheet: the app's selectable row, with a summary of the tabs shown. */
    private static void tabsRow(Object composer) throws ReflectiveOperationException {
        Context context = MaterialYou.application();
        if (context == null) return;
        ClassLoader loader = composer.getClass().getClassLoader();
        Class<?> rowType = Class.forName("com.atlassian.jira.feature.settings.impl.SelectableRowKt", true, loader);
        for (Method row : rowType.getDeclaredMethods()) {
            Class<?>[] types = row.getParameterTypes();
            if (!row.getName().equals("a") || types.length != 8 || types[0] != String.class || types[1] != boolean.class) continue;
            int shown = 1;
            for (int i = FIRST_TAB; i < MaterialYou.KEYS.length; i++) {
                if (MaterialYou.prefs(context).getBoolean(MaterialYou.KEYS[i], MaterialYou.DEFAULTS[i])) shown++;
            }
            Object click = Proxy.newProxyInstance(loader, new Class<?>[] {types[2]}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    // Compose also calls hashCode, equals and toString on a lambda.
                    if (method.getDeclaringClass() == Object.class) return MaterialYou.objectMethod(proxy, method, args);
                    showSheet();
                    return null;
                }
            });
            String summary = GlassTabs.summary(shown, MaterialYou.KEYS.length - FIRST_TAB + 1);
            // Label, selected (no check), click, modifier (default), subtitle, composer, changed, defaults.
            row.invoke(null, "Navigation tabs", false, click, null, summary, composer, 0, 8);
            return;
        }
    }

    /** A bottom sheet with one check row per bottom bar tab; closing it offers the restart when a tab was changed. */
    private static void showSheet() {
        final Activity activity = current.get();
        if (activity == null) return;
        final ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null || root.findViewWithTag(SHEET_TAG) != null) return;
        final float density = root.getResources().getDisplayMetrics().density;
        boolean night = (root.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        boolean dynamic = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S;
        int surface = dynamic ? activity.getColor(night ? android.R.color.system_neutral1_900 : android.R.color.system_neutral2_50)
                : (night ? 0xFF2B2930 : 0xFFF7F2FA);
        final int accent = dynamic ? activity.getColor(night ? android.R.color.system_accent1_200 : android.R.color.system_accent1_600)
                : (night ? 0xFFD0BCFF : 0xFF6750A4);
        final int text = night ? 0xFFE6E0E9 : 0xFF1D1B20;

        final FrameLayout scrim = new FrameLayout(activity);
        scrim.setTag(SHEET_TAG);
        scrim.setBackgroundColor(0x66000000);
        scrim.setClickable(true);
        scrim.setElevation(24 * density);
        final boolean[] changed = {false};
        final android.window.OnBackInvokedCallback[] back = {null};
        final Runnable close = new Runnable() {
            @Override
            public void run() {
                if (scrim.getParent() == null) return;
                ((ViewGroup) scrim.getParent()).removeView(scrim);
                if (back[0] != null) activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(back[0]);
                if (changed[0]) showSnackbar();
            }
        };
        // Back closes the sheet: through the platform dispatcher on Android 13+, and as a key on the sheet before that.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            back[0] = new android.window.OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    close.run();
                }
            };
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY, back[0]);
        }
        scrim.setFocusableInTouchMode(true);
        scrim.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, android.view.KeyEvent event) {
                if (keyCode != android.view.KeyEvent.KEYCODE_BACK) return false;
                if (event.getAction() == android.view.KeyEvent.ACTION_UP) close.run();
                return true;
            }
        });
        scrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                close.run();
            }
        });

        LinearLayout sheet = new LinearLayout(activity);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setClickable(true);
        int pad = Math.round(24 * density);
        sheet.setPadding(pad, Math.round(12 * density), pad, Math.round(24 * density));
        GradientDrawable background = new GradientDrawable();
        background.setColor(surface);
        float radius = 28 * density;
        background.setCornerRadii(new float[] {radius, radius, radius, radius, 0, 0, 0, 0});
        sheet.setBackground(background);

        View handle = new View(activity);
        GradientDrawable handleShape = new GradientDrawable();
        handleShape.setColor((text & 0xFFFFFF) | 0x66000000);
        handleShape.setCornerRadius(2 * density);
        handle.setBackground(handleShape);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(Math.round(32 * density), Math.round(4 * density));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = Math.round(16 * density);
        sheet.addView(handle, handleParams);

        TextView title = new TextView(activity);
        title.setText("Navigation tabs");
        title.setTextColor(text);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, Math.round(8 * density));
        sheet.addView(title);

        final Context context = MaterialYou.application();
        // Home is always shown: its row is checked and dimmed, and does nothing.
        for (int i = FIRST_TAB - 1; i < MaterialYou.KEYS.length; i++) {
            final int which = i;
            final boolean fixed = i < FIRST_TAB;
            LinearLayout line = new LinearLayout(activity);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            int vertical = Math.round(14 * density);
            line.setPadding(0, vertical, 0, vertical);
            TextView label = new TextView(activity);
            label.setText(fixed ? "Home" : MaterialYou.LABELS[i].replace("Show ", "").replace(" tab", ""));
            label.setTextColor(text);
            label.setTextSize(16);
            line.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            final TextView check = new TextView(activity);
            check.setText("\u2713");
            check.setTextColor(accent);
            check.setTextSize(20);
            check.setVisibility(fixed || MaterialYou.prefs(context).getBoolean(MaterialYou.KEYS[i], MaterialYou.DEFAULTS[i]) ? View.VISIBLE : View.INVISIBLE);
            line.addView(check);
            if (fixed) {
                line.setAlpha(0.38f);
                sheet.addView(line);
                continue;
            }
            line.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SharedPreferences prefs = MaterialYou.prefs(context);
                    boolean on = !prefs.getBoolean(MaterialYou.KEYS[which], MaterialYou.DEFAULTS[which]);
                    prefs.edit().putBoolean(MaterialYou.KEYS[which], on).commit();
                    check.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
                    changed[0] = true;
                    bumpVersion();
                }
            });
            sheet.addView(line);
        }
        scrim.addView(sheet, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        root.addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrim.requestFocus();
        sheet.setTranslationY(400 * density);
        sheet.animate().translationY(0).setDuration(220).start();
    }

    /** A snackbar at the bottom of the screen that offers to restart the app, which the changes need. */
    private static void showSnackbar() {
        final Activity activity = current.get();
        if (activity == null) return;
        final ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        root.post(new Runnable() {
            @Override
            public void run() {
                final String tag = "fakh_restart_snackbar";
                if (root.findViewWithTag(tag) != null) return;
                float density = root.getResources().getDisplayMetrics().density;
                final LinearLayout bar = new LinearLayout(activity);
                bar.setTag(tag);
                bar.setOrientation(LinearLayout.HORIZONTAL);
                bar.setGravity(Gravity.CENTER_VERTICAL);
                bar.setPadding(Math.round(16 * density), Math.round(4 * density), Math.round(8 * density), Math.round(4 * density));
                GradientDrawable background = new GradientDrawable();
                background.setColor(0xFF2F3033);
                background.setCornerRadius(4 * density);
                bar.setBackground(background);
                bar.setElevation(6 * density);
                TextView message = new TextView(activity);
                message.setText("Restart to apply the change");
                message.setTextColor(0xFFF1F0F4);
                message.setTextSize(14);
                bar.addView(message, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                TextView action = new TextView(activity);
                action.setText("Restart");
                action.setTextColor(0xFFD0BCFF);
                action.setTextSize(14);
                action.setAllCaps(true);
                action.setPadding(Math.round(12 * density), Math.round(12 * density), Math.round(12 * density), Math.round(12 * density));
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MaterialYou.restart(activity);
                    }
                });
                bar.addView(action);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
                int side = Math.round(12 * density);
                params.setMargins(side, 0, side, Math.round(72 * density));
                root.addView(bar, params);
                bar.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (bar.getParent() != null) ((ViewGroup) bar.getParent()).removeView(bar);
                    }
                }, 10000);
            }
        });
    }
}
