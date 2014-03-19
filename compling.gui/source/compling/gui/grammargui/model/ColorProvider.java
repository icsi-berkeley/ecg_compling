package compling.gui.grammargui.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorProvider {

	private static ColorProvider instance;

	protected Map<RGB, Color> colorTable = new HashMap<RGB, Color>(5);

	public static final RGB COMMENT = new RGB(128, 128, 0);
	public static final RGB KEYWORD = new RGB(0, 0, 128);
	public static final RGB OPERATOR = new RGB(128, 0, 128);
	public static final RGB STRING = new RGB(0, 128, 0);
	public static final RGB DEFAULT = new RGB(0, 0, 0);

	private ColorProvider() {
		// Nothing to do
	}

	/**
	 * Returns the singleton Java color provider.
	 * 
	 * @return the singleton Java color provider
	 */
	public static ColorProvider instance() {
		if (instance == null)
			instance = new ColorProvider();
		return instance;
	}

	/**
	 * Release all of the color resources held onto by the receiver.
	 */
	public void dispose() {
		for (Color c : colorTable.values())
			c.dispose();
	}

	/**
	 * Return the color that is stored in the color table under the given RGB value.
	 * 
	 * @param rgb
	 *           the RGB value
	 * @return the color stored in the color table for the given RGB value
	 */
	public Color getColor(RGB rgb) {
		Color color = colorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			colorTable.put(rgb, color);
		}
		return color;
	}

}
