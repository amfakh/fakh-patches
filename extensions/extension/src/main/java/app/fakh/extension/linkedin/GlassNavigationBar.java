package app.fakh.extension.linkedin;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

/** Draws LinkedIn's bottom bar as a floating, translucent capsule, in the style of Morphe's glass navigation bar. */
final class GlassNavigationBar {
    private static final float FILL_ALPHA = 0.85f;
    private static final int SIDE_MARGIN_DP = 12;
    private static final int INNER_PADDING_DP = 14;
    /** The widest a tab gets, so the capsule stays compact on a wide screen. */
    private static final int MAX_TAB_DP = 96;

    private GlassNavigationBar() {
    }

    /** Replaces the background of the bar with the capsule, and removes what the app draws apart from it. */
    static void install(View bar, ViewGroup tabs) {
        Drawable background = tabs.getBackground();
        int color = background instanceof ColorDrawable ? ((ColorDrawable) background).getColor() : Color.DKGRAY;
        tabs.setBackground(null);
        // The app sets the visibility and alpha of these itself when the tab changes.
        for (String name : new String[]{"bb_slim_bottom_bar_shadow", "bottom_nav_indicator_view"}) {
            View view = NavigationBar.child(bar, name);
            if (view != null) view.setScaleY(0);
        }
        Activity activity = activity(bar.getContext());
        if (activity != null) {
            activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
            activity.getWindow().setNavigationBarContrastEnforced(false);
        }
        bar.setElevation(0);
        Capsule capsule = new Capsule(tabs, color, bar.getResources().getDisplayMetrics().density);
        bar.setBackground(capsule);
        // On a light theme the app paints both backgrounds again after the bar is shown, so the capsule goes back.
        bar.getViewTreeObserver().addOnPreDrawListener(() -> {
            if (bar.getBackground() != capsule) bar.setBackground(capsule);
            if (tabs.getBackground() != null) tabs.setBackground(null);
            // Selecting a tab redraws the tab, not the bar, so the pill behind the selected tab is refreshed here.
            capsule.refreshSelection();
            return true;
        });
    }

    /** Gives the tabs equal shares of the capsule, and a ripple that stays inside the tab. */
    static void arrangeTabs(ViewGroup tabs, int barWidth) {
        int count = tabs.getChildCount();
        if (count == 0 || barWidth <= 0) return;
        float density = tabs.getResources().getDisplayMetrics().density;
        int width = Math.min((barWidth - 2 * Math.round((SIDE_MARGIN_DP + INNER_PADDING_DP) * density)) / count,
                Math.round(MAX_TAB_DP * density));
        for (int i = 0; i < count; i++) {
            View tab = tabs.getChildAt(i);
            ViewGroup.LayoutParams params = tab.getLayoutParams();
            if (params.width != width) {
                params.width = width;
                tab.setLayoutParams(params);
            }
            if (!(tab.getBackground() instanceof TabRipple)) tab.setBackground(new TabRipple());
        }
    }

    private static Activity activity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /** A ripple that stays inside its tab, as a pill, where LinkedIn's own one spreads past the capsule. */
    private static final class TabRipple extends RippleDrawable {
        TabRipple() {
            super(ColorStateList.valueOf(0x33808080), null, mask());
        }

        private static Drawable mask() {
            GradientDrawable mask = new GradientDrawable();
            mask.setColor(Color.WHITE);
            mask.setCornerRadius(Float.MAX_VALUE);
            return mask;
        }
    }

    /** The capsule, around the tabs, which the row centers, with the bar's own color as a translucent fill. */
    private static final class Capsule extends Drawable {
        private final ViewGroup tabs;
        private final float density;
        private final RectF rect = new RectF();
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int selected = -1;
        /** Where the pill was drawn last (in the tabs' coordinates), and how far it is on its way to the selected tab. */
        private float lastLeft;
        private float lastRight;
        private float fromLeft;
        private float fromRight;
        private float progress = 1;
        private ValueAnimator slide;
        /** A light bar has no dark backdrop to stand out from, so it gets a dark edge and a soft shadow instead. */
        private final boolean light;

        Capsule(ViewGroup tabs, int color, float density) {
            this.tabs = tabs;
            this.density = density;
            this.light = Color.red(color) + Color.green(color) + Color.blue(color) > 3 * 128;
            fill.setColor(Color.argb(Math.round(255 * FILL_ALPHA), Color.red(color), Color.green(color), Color.blue(color)));
            fill.setShadowLayer(10 * density, 0, 3 * density, 0x50000000);
            pill.setColor(light ? 0x1F000000 : 0x2EFFFFFF);
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(density);
        }

        /** Redraws the capsule when another tab became the selected one. */
        void refreshSelection() {
            int now = -1;
            for (int i = 0; i < tabs.getChildCount() && now < 0; i++) {
                if (tabs.getChildAt(i).isSelected()) now = i;
            }
            if (now == selected) return;
            boolean moving = selected >= 0 && now >= 0 && lastRight > lastLeft;
            selected = now;
            if (slide != null) slide.cancel();
            if (moving) {
                // The pill slides from where it is to the new tab.
                fromLeft = lastLeft;
                fromRight = lastRight;
                progress = 0;
                slide = ValueAnimator.ofFloat(0, 1).setDuration(260);
                slide.setInterpolator(new DecelerateInterpolator());
                slide.addUpdateListener(a -> {
                    progress = (float) a.getAnimatedValue();
                    invalidateSelf();
                });
                slide.start();
            } else {
                progress = 1;
            }
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            int count = tabs.getChildCount();
            if (count == 0) return;
            View row = (View) tabs.getParent();
            float top = row.getTop() + tabs.getTop();
            float left = row.getLeft() + tabs.getLeft();
            float padding = INNER_PADDING_DP * density;
            rect.set(left + tabs.getChildAt(0).getLeft() - padding, top,
                    left + tabs.getChildAt(count - 1).getRight() + padding, top + tabs.getHeight());
            // Dark bars get a light sheen at the top; on a light bar that would vanish, so it shades the bottom instead.
            sheen.setShader(light
                    ? new LinearGradient(0, rect.top, 0, rect.bottom, Color.TRANSPARENT, 0x26000000, Shader.TileMode.CLAMP)
                    : new LinearGradient(0, rect.top, 0, rect.bottom, 0x1FFFFFFF, Color.TRANSPARENT, Shader.TileMode.CLAMP));
            edge.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, light ? 0x40000000 : 0x55FFFFFF,
                    light ? 0x14000000 : 0x12FFFFFF, Shader.TileMode.CLAMP));
            float radius = rect.height() / 2;
            if (light) {
                // Hardware canvases ignore a shadow layer on shapes, so the shadow is a few widening translucent rings.
                for (int i = 4; i >= 1; i--) {
                    float grow = i * 2 * density;
                    shadow.setColor(Color.argb(10, 0, 0, 0));
                    canvas.drawRoundRect(rect.left - grow, rect.top - grow / 2, rect.right + grow,
                            rect.bottom + grow * 1.5f, radius + grow, radius + grow, shadow);
                }
            }
            canvas.drawRoundRect(rect, radius, radius, fill);
            canvas.drawRoundRect(rect, radius, radius, sheen);
            if (selected >= 0 && selected < count) {
                // A pill behind the selected tab, inset from the capsule so a thin rim of it stays around the pill.
                View tab = tabs.getChildAt(selected);
                float inset = 5 * density;
                lastLeft = fromLeft + (tab.getLeft() - fromLeft) * progress;
                lastRight = fromRight + (tab.getRight() - fromRight) * progress;
                if (progress >= 1) {
                    lastLeft = tab.getLeft();
                    lastRight = tab.getRight();
                }
                canvas.drawRoundRect(left + lastLeft + inset, rect.top + inset, left + lastRight - inset,
                        rect.bottom - inset, radius, radius, pill);
            }
            canvas.drawRoundRect(rect, radius, radius, edge);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
