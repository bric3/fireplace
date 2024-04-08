package io.github.bric3.fireplace.icons;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Stack;
import java.util.function.Function;

/**
 * This class has been automatically generated using
 * <a href="https://github.com/kirill-grouchnikov/radiance">Radiance SVG transcoder</a>.
 */
public class darkMode_sun implements GeneratedIcon {
    private Shape shape = null;
    private Paint paint = null;
    private Stroke stroke = null;
    private Function<Color, Color> colorFilter = null;
    private final Stack<AffineTransform> transformsStack = new Stack<>();


    private void _paint0(Graphics2D g, float origAlpha) {
        transformsStack.push(g.getTransform());
// 
        g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
        transformsStack.push(g.getTransform());
        g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, -0.0f, -0.0f));
// _0
        g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
        transformsStack.push(g.getTransform());
        g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_0
        shape = new Ellipse2D.Double(7.0, 7.0, 10.0, 10.0);
        paint = (colorFilter != null) ? colorFilter.apply(new Color(0, 0, 0, 255)) : new Color(0, 0, 0, 255);
        g.setPaint(paint);
        g.fill(shape);
        paint = (colorFilter != null) ? colorFilter.apply(new Color(0, 0, 0, 255)) : new Color(0, 0, 0, 255);
        stroke = new BasicStroke(2.0f, 1, 1, 4.0f, null, 0.0f);
        shape = new Ellipse2D.Double(7.0, 7.0, 10.0, 10.0);
        g.setPaint(paint);
        g.setStroke(stroke);
        g.draw(shape);
        g.setTransform(transformsStack.pop());
        g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
        transformsStack.push(g.getTransform());
        g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1
        g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
        transformsStack.push(g.getTransform());
        g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_0
        paint = (colorFilter != null) ? colorFilter.apply(new Color(0, 0, 0, 255)) : new Color(0, 0, 0, 255);
        stroke = new BasicStroke(2.0f, 1, 1, 4.0f, null, 0.0f);
        shape = new Line2D.Float(12.000000f, 1.000000f, 12.000000f, 3.000000f);
        g.setPaint(paint);
        g.setStroke(stroke);
        g.draw(shape);
        g.setTransform(transformsStack.pop());
        g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
        transformsStack.push(g.getTransform());
        g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_1
        paint = (colorFilter != null) ? colorFilter.apply(new Color(0, 0, 0, 255)) : new Color(0, 0, 0, 255);
        stroke = new BasicStroke(2.0f, 1, 1, 4.0f, null, 0.0f);
        shape = new Line2D.Float(12.000000f, 21.000000f, 12.000000f, 23.000000f);
        g.setPaint(paint);
        g.setStroke(stroke);
        g.draw(shape);
        g.setTransform(transformsStack.pop());
        g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
        transformsStack.push(g.getTransform());
        g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_2
        paint = (colorFilter != null) ? colorFilter.apply(new Color(0, 0, 0, 255)) : new Color(0, 0, 0, 255);
        stroke = new BasicStroke(2.0f, 1, 1, 4.0f, null, 0.0f);
        shape = new Line2D.Float(4.220000f, 4.220000f, 5.640000f, 5.640000f);
        g.setPaint(paint);
        g.setStroke(stroke);
        g.draw(shape);
        g.setTransform(transformsStack.pop());
        g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
        transformsStack.push(g.getTransform());
        g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_3
        paint = (colorFilter != null) ? colorFilter.apply(new Color(0, 0, 0, 255)) : new Color(0, 0, 0, 255);
        stroke = new BasicStroke(2.0f, 1, 1, 4.0f, null, 0.0f);
        shape = new Line2D.Float(18.360001f, 18.360001f, 19.780001f, 19.780001f);
        g.setPaint(paint);
        g.setStroke(stroke);
        g.draw(shape);
        g.setTransform(transformsStack.pop());
        g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
        transformsStack.push(g.getTransform());
        g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_4
        paint = (colorFilter != null) ? colorFilter.apply(new Color(0, 0, 0, 255)) : new Color(0, 0, 0, 255);
        stroke = new BasicStroke(2.0f, 1, 1, 4.0f, null, 0.0f);
        shape = new Line2D.Float(1.000000f, 12.000000f, 3.000000f, 12.000000f);
        g.setPaint(paint);
        g.setStroke(stroke);
        g.draw(shape);
        g.setTransform(transformsStack.pop());
        g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
        transformsStack.push(g.getTransform());
        g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_5
        paint = (colorFilter != null) ? colorFilter.apply(new Color(0, 0, 0, 255)) : new Color(0, 0, 0, 255);
        stroke = new BasicStroke(2.0f, 1, 1, 4.0f, null, 0.0f);
        shape = new Line2D.Float(21.000000f, 12.000000f, 23.000000f, 12.000000f);
        g.setPaint(paint);
        g.setStroke(stroke);
        g.draw(shape);
        g.setTransform(transformsStack.pop());
        g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
        transformsStack.push(g.getTransform());
        g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_6
        paint = (colorFilter != null) ? colorFilter.apply(new Color(0, 0, 0, 255)) : new Color(0, 0, 0, 255);
        stroke = new BasicStroke(2.0f, 1, 1, 4.0f, null, 0.0f);
        shape = new Line2D.Float(4.220000f, 19.780001f, 5.640000f, 18.360001f);
        g.setPaint(paint);
        g.setStroke(stroke);
        g.draw(shape);
        g.setTransform(transformsStack.pop());
        g.setComposite(AlphaComposite.getInstance(3, 1.0f * origAlpha));
        transformsStack.push(g.getTransform());
        g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_7
        paint = (colorFilter != null) ? colorFilter.apply(new Color(0, 0, 0, 255)) : new Color(0, 0, 0, 255);
        stroke = new BasicStroke(2.0f, 1, 1, 4.0f, null, 0.0f);
        shape = new Line2D.Float(18.360001f, 5.640000f, 19.780001f, 4.220000f);
        g.setPaint(paint);
        g.setStroke(stroke);
        g.draw(shape);
        g.setTransform(transformsStack.pop());
        g.setTransform(transformsStack.pop());
        g.setTransform(transformsStack.pop());
        g.setTransform(transformsStack.pop());

    }


    @SuppressWarnings("unused")
    private void innerPaint(Graphics2D g) {
        float origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite =
                    (AlphaComposite) origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }

        _paint0(g, origAlpha);


        shape = null;
        paint = null;
        stroke = null;
        transformsStack.clear();
    }

    /**
     * Returns the X of the bounding box of the original SVG image.
     *
     * @return The X of the bounding box of the original SVG image.
     */
    public static double getOrigX() {
        return 0.0;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     *
     * @return The Y of the bounding box of the original SVG image.
     */
    public static double getOrigY() {
        return 0.0;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     *
     * @return The width of the bounding box of the original SVG image.
     */
    public static double getOrigWidth() {
        return 24.0;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     *
     * @return The height of the bounding box of the original SVG image.
     */
    public static double getOrigHeight() {
        return 24.0;
    }

    /**
     * The current width of this icon.
     */
    private int width;

    /**
     * The current height of this icon.
     */
    private int height;

    /**
     * Creates a new transcoded SVG image. This is marked as private to indicate that app
     * code should be using the {@link #of(int, int)} method to obtain a pre-configured instance.
     */
    private darkMode_sun() {
        this.width = (int) getOrigWidth();
        this.height = (int) getOrigHeight();
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public synchronized void setDimension(Dimension newDimension) {
        this.width = newDimension.width;
        this.height = newDimension.height;
    }

    @Override
    public void setColorFilter(Function<Color, Color> colorFilter) {
        this.colorFilter = colorFilter;
    }

    @Override
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.translate(x, y);

        double coef1 = (double) this.width / getOrigWidth();
        double coef2 = (double) this.height / getOrigHeight();
        double coef = Math.min(coef1, coef2);
        g2d.clipRect(0, 0, this.width, this.height);
        g2d.scale(coef, coef);
        g2d.translate(-getOrigX(), -getOrigY());
        if (coef1 != coef2) {
            if (coef1 < coef2) {
                int extraDy = (int) ((getOrigWidth() - getOrigHeight()) / 2.0);
                g2d.translate(0, extraDy);
            } else {
                int extraDx = (int) ((getOrigHeight() - getOrigWidth()) / 2.0);
                g2d.translate(extraDx, 0);
            }
        }
        Graphics2D g2ForInner = (Graphics2D) g2d.create();
        innerPaint(g2ForInner);
        g2ForInner.dispose();
        g2d.dispose();
    }

    /**
     * Returns a new instance of this icon with specified dimensions.
     *
     * @param width  Required width of the icon
     * @param height Required height of the icon
     * @return A new instance of this icon with specified dimensions.
     */
    public static GeneratedIcon of(int width, int height) {
        darkMode_sun base = new darkMode_sun();
        base.width = width;
        base.height = height;
        return base;
    }
}

