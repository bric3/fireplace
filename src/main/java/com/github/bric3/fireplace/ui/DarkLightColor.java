/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.fireplace.ui;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

public class DarkLightColor extends Color {
    private final Color dark;

    public DarkLightColor(Color light, Color dark) {
        super(light.getRGB(), light.getAlpha() != 255);
        this.dark = dark;
    }

    public DarkLightColor(int light_rgba, int dark_rgba) {
        super(light_rgba, true);
        this.dark = new Color(dark_rgba, true);
    }

    private Color currentColor() {
        return Colors.darkMode ? dark : this;
    }

    @Override
    public int getRed() {
        var current = currentColor();
        return current == this ? super.getRed() : current.getRed();
    }

    @Override
    public int getGreen() {
        var current = currentColor();
        return current == this ? super.getGreen() : current.getGreen();
    }

    @Override
    public int getBlue() {
        var current = currentColor();
        return current == this ? super.getBlue() : current.getBlue();
    }

    @Override
    public int getAlpha() {
        var current = currentColor();
        return current == this ? super.getAlpha() : current.getAlpha();
    }

    @Override
    public int getRGB() {
        var current = currentColor();
        return current == this ? super.getRGB() : current.getRGB();
    }

    @Override
    public Color brighter() {
        var current = currentColor();
        return current == this ? super.brighter() : current.brighter();
    }

    @Override
    public Color darker() {
        var current = currentColor();
        return current == this ? super.darker() : current.darker();
    }

    @Override
    public float[] getRGBComponents(float[] compArray) {
        var current = currentColor();
        return current == this ? super.getRGBComponents(compArray) : current.getRGBComponents(compArray);
    }

    @Override
    public float[] getRGBColorComponents(float[] compArray) {
        var current = currentColor();
        return current == this ? super.getRGBColorComponents(compArray) : current.getRGBColorComponents(compArray);
    }

    @Override
    public float[] getComponents(float[] compArray) {
        var current = currentColor();
        return current == this ? super.getComponents(compArray) : current.getComponents(compArray);
    }

    @Override
    public float[] getColorComponents(float[] compArray) {
        var current = currentColor();
        return current == this ? super.getColorComponents(compArray) : current.getColorComponents(compArray);
    }

    @Override
    public float[] getComponents(ColorSpace cspace, float[] compArray) {
        var current = currentColor();
        return current == this ? super.getComponents(cspace, compArray) : current.getComponents(cspace, compArray);
    }

    @Override
    public float[] getColorComponents(ColorSpace cspace, float[] compArray) {
        var current = currentColor();
        return current == this ? super.getColorComponents(cspace, compArray) : current.getColorComponents(cspace, compArray);
    }

    @Override
    public ColorSpace getColorSpace() {
        var current = currentColor();
        return current == this ? super.getColorSpace() : current.getColorSpace();
    }

    @Override
    public synchronized PaintContext createContext(ColorModel cm, Rectangle r, Rectangle2D r2d, AffineTransform xform, RenderingHints hints) {
        var current = currentColor();
        return current == this ? super.createContext(cm, r, r2d, xform, hints) : current.createContext(cm, r, r2d, xform, hints);
    }

    @Override
    public int getTransparency() {
        var current = currentColor();
        return current == this ? super.getTransparency() : current.getTransparency();
    }

    @Override
    public int hashCode() {
        var current = currentColor();
        return current == this ? super.hashCode() : current.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        var current = currentColor();
        return current == this ? super.equals(obj) : current.equals(obj);
    }

    @Override
    public String toString() {
        var current = currentColor();
        return current == this ? super.toString() : current.toString();
    }
}
