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

	/**
	 * Sets a color filter
	 *
	 * @param colorFilter
	 */
	void setColorFilter(Function<Color, Color> colorFilter);
}
