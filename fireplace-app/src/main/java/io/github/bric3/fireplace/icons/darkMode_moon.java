package io.github.bric3.fireplace.icons;


import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.Stack;
import java.util.function.Function;

/**
 * This class has been automatically generated using
 * <a href="https://github.com/kirill-grouchnikov/radiance">Radiance SVG transcoder</a>.
 */
public class darkMode_moon implements GeneratedIcon {
    private Shape shape = null;
    private Paint paint = null;
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
        shape = new Ellipse2D.Double(3.0, 3.0, 18.0, 18.0);
        var area = new Area(shape);
        area.subtract(new Area(new Ellipse2D.Double(3.0, -6.0, 18.0, 18.0)));

        paint = (colorFilter != null) ? colorFilter.apply(new Color(0, 0, 0, 255)) : new Color(0, 0, 0, 255);
        g.setPaint(paint);
        g.fill(AffineTransform.getRotateInstance(
                Math.toRadians(45),
                shape.getBounds().getCenterY(),
                shape.getBounds().getCenterX()
        ).createTransformedShape(area));

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
        Stroke stroke = null;
        transformsStack.clear();
    }

    /**
     * Returns the X of the bounding box of the original SVG image.
     *
     * @return The X of the bounding box of the original SVG image.
     */
    public static double getOrigX() {
        return 2.0;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     *
     * @return The Y of the bounding box of the original SVG image.
     */
    public static double getOrigY() {
        return 2.0;
    }

    /**
     * Returns the width of the bounding box of the original SVG image.
     *
     * @return The width of the bounding box of the original SVG image.
     */
    public static double getOrigWidth() {
        return 20.0;
    }

    /**
     * Returns the height of the bounding box of the original SVG image.
     *
     * @return The height of the bounding box of the original SVG image.
     */
    public static double getOrigHeight() {
        return 20.0;
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
    private darkMode_moon() {
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
        darkMode_moon base = new darkMode_moon();
        base.width = width;
        base.height = height;
        return base;
    }
}

