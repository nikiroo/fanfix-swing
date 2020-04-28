package be.nikiroo.fanfix_swing.gui.search;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;

import be.nikiroo.fanfix_swing.gui.utils.CoverImager;
import be.nikiroo.utils.ui.UIUtils;

/**
 * This class can create a cover icon ready to use for the graphical
 * application.
 * 
 * @author niki
 */
class COPY_OF_BookCoverImager {
	// TODO: export some of the configuration options?
	static final int COVER_WIDTH = 100;
	static final int COVER_HEIGHT = 150;
	static final int SPINE_WIDTH = 5;
	static final int SPINE_HEIGHT = 5;
	static final int HOFFSET = 20;
	static final Color SPINE_COLOR_BOTTOM = new Color(180, 180, 180);
	static final Color SPINE_COLOR_RIGHT = new Color(100, 100, 100);
	static final Color BORDER = Color.black;

	public static final Color UNCACHED_ICON_COLOR = Color.green.darker();
	// new Color(0, 80, 220);

	public static final int TEXT_HEIGHT = 50;
	public static final int TEXT_WIDTH = COVER_WIDTH + 40;

	/**
	 * Draw a partially transparent overlay if needed depending upon the
	 * selection and mouse-hover states on top of the normal component, as well
	 * as a possible "cached" icon if the item is cached.
	 * 
	 * @param g
	 *            the {@link Graphics} to paint onto
	 * @param enabled
	 *            draw an enabled overlay
	 * @param selected
	 *            draw a selected overlay
	 * @param hovered
	 *            draw a hovered overlay
	 * @param cached
	 *            draw a non-cached overlay if needed
	 */
	static public void paintOverlay(Graphics g, boolean enabled,
			boolean selected, boolean hovered, boolean cached) {
		Rectangle clip = g.getClipBounds();
		if (clip == null || clip.getWidth() <= 0 || clip.getHeight() <= 0) {
			return;
		}

		int h = COVER_HEIGHT;
		int w = COVER_WIDTH;
		int xOffset = (TEXT_WIDTH - COVER_WIDTH) - 1;
		int yOffset = HOFFSET;

		if (BORDER != null) {
			if (BORDER != null) {
				g.setColor(BORDER);
				g.drawRect(xOffset, yOffset, COVER_WIDTH, COVER_HEIGHT);
			}

			xOffset++;
			yOffset++;
		}

		int[] xs = new int[] { xOffset, xOffset + SPINE_WIDTH,
				xOffset + w + SPINE_WIDTH, xOffset + w };
		int[] ys = new int[] { yOffset + h, yOffset + h + SPINE_HEIGHT,
				yOffset + h + SPINE_HEIGHT, yOffset + h };
		g.setColor(SPINE_COLOR_BOTTOM);
		g.fillPolygon(new Polygon(xs, ys, xs.length));
		xs = new int[] { xOffset + w, xOffset + w + SPINE_WIDTH,
				xOffset + w + SPINE_WIDTH, xOffset + w };
		ys = new int[] { yOffset, yOffset + SPINE_HEIGHT,
				yOffset + h + SPINE_HEIGHT, yOffset + h };
		g.setColor(SPINE_COLOR_RIGHT);
		g.fillPolygon(new Polygon(xs, ys, xs.length));

		Color color = CoverImager.getBackground(enabled, selected, hovered);

		g.setColor(color);
		g.fillRect(clip.x, clip.y, clip.width, clip.height);

		UIUtils.drawEllipse3D(g, UNCACHED_ICON_COLOR,
				COVER_WIDTH + HOFFSET + 30, 10, 20, 20, cached);
	}
}
