package app.fakh.extension.linkedin;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Material icons drawn from SVG path data on a 24x24 viewport, so the settings screen needs no
 * image resources. Path data is parsed by {@link PathData}.
 */
final class IconDrawable extends Drawable {
    static final String ARROW_BACK = "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z";
    static final String SEARCH = "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 "
            + "5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 "
            + "5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z";
    static final String CLOSE = "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z";
    static final String RESTART = "M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8"
            + "c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6"
            + "c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z";
    static final String CHEVRON_RIGHT = "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z";

    private final Path source;
    private final Path scaled = new Path();
    private final Matrix matrix = new Matrix();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    IconDrawable(String pathData, int color) {
        source = new Path();
        PathData.parse(pathData, new PathData.Sink() {
            @Override
            public void moveTo(float x, float y) {
                source.moveTo(x, y);
            }

            @Override
            public void lineTo(float x, float y) {
                source.lineTo(x, y);
            }

            @Override
            public void cubicTo(float x1, float y1, float x2, float y2, float x, float y) {
                source.cubicTo(x1, y1, x2, y2, x, y);
            }

            @Override
            public void close() {
                source.close();
            }
        });
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        matrix.setScale(bounds.width() / 24f, bounds.height() / 24f);
        matrix.postTranslate(bounds.left, bounds.top);
        source.transform(matrix, scaled);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(scaled, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

}
