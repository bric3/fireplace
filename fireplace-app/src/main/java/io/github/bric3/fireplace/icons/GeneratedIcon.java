package io.github.bric3.fireplace.icons;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public interface GeneratedIcon extends Icon {
	/**
	 * Changes the dimension of <code>this</code> icon.
	 *
	 * @param newDimension New dimension for <code>this</code> icon.
	 */
	void setDimension(Dimension newDimension);

	// TODO work in progress, produced hidpi image

	// default BufferedImage toImage(double scale) {
	// 	GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
	// 	GraphicsConfiguration c = e.getDefaultScreenDevice().getDefaultConfiguration();
	// 	BufferedImage iconImage = c.createCompatibleImage(
	// 			(int) (getIconWidth()),
	// 			(int) (getIconHeight()),
	// 			Transparency.TRANSLUCENT
	// 	);
	//
	// 	var graphics = iconImage.createGraphics();
	// 	graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	// 	graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	// 	this.paintIcon(null, graphics, 0, 0);
	// 	return iconImage;
	// }
	//
	// default Icon toImageIcon(double scale) {
	// 	return new ImageIcon(toImage(scale)) {
	// 		@Override
	// 		public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
	// 			AffineTransform xform = AffineTransform.getScaleInstance(.5, .5);
	// 			var g2 = ((Graphics2D) g);
	// 			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	// 			g2.drawImage(this.getImage(), xform, getImageObserver());
	// 		}
	// 	};
	// }

	void setColorFilter(Function<Color, Color> colorFilter);
}
