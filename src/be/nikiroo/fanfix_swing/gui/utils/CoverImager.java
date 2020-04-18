package be.nikiroo.fanfix_swing.gui.utils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.ui.ImageUtilsAwt;
import be.nikiroo.utils.ui.UIUtils;

/**
 * This class can create a cover icon ready to use for the graphical
 * application.
 * 
 * @author niki
 */
public class CoverImager {
	// TODO: export some of the configuration options?
	static final int COVER_WIDTH = 100;
	static final int COVER_HEIGHT = 150;
	static final int SPINE_WIDTH = 5;
	static final int SPINE_HEIGHT = 5;
	static final int HOFFSET = 20;
	static final Color SPINE_COLOR_BOTTOM = new Color(180, 180, 180);
	static final Color SPINE_COLOR_RIGHT = new Color(100, 100, 100);
	static final Color BORDER = Color.black;

	public static final int TEXT_HEIGHT = 50;
	public static final int TEXT_WIDTH = COVER_WIDTH + 40;

	//

	static public Color getBackground(boolean enabled, boolean selected,
			boolean hovered) {
		Color color = new Color(255, 255, 255, 0);
		if (!enabled) {
		} else if (selected && !hovered) {
			color = new Color(80, 80, 100, 40);
		} else if (!selected && hovered) {
			color = new Color(230, 230, 255, 100);
		} else if (selected && hovered) {
			color = new Color(200, 200, 255, 100);
		}

		return color;
	}

	/**
	 * The width of a cover image.
	 * 
	 * @return the width
	 */
	static public int getCoverWidth() {
		return SPINE_WIDTH + COVER_WIDTH;
	}

	/**
	 * The height of a cover image.
	 * 
	 * @return the height
	 */
	static public int getCoverHeight() {
		return COVER_HEIGHT + HOFFSET;
	}

	/**
	 * Generate a cover icon based upon the given {@link GuiReaderBookInfo}.
	 * 
	 * @param lib
	 *            the library the meta comes from (can be NULL)
	 * @param info
	 *            the {@link GuiReaderBookInfo}
	 * 
	 * @return the image
	 */
	static public java.awt.Image generateCoverImage(BasicLibrary lib,
			BookInfo info) {
		BufferedImage resizedImage = null;
		String id = getIconId(info);

		InputStream in = Instance.getInstance().getCache().getFromCache(id);
		if (in != null) {
			try {
				resizedImage = ImageUtilsAwt.fromImage(new Image(in));
				in.close();
				in = null;
			} catch (IOException e) {
				Instance.getInstance().getTraceHandler().error(e);
			}
		}

		if (resizedImage == null) {
			try {
				Image cover = null;
				if (info != null) {
					cover = info.getBaseImage(lib);
				}

				resizedImage = new BufferedImage(getCoverWidth(),
						getCoverHeight(), BufferedImage.TYPE_4BYTE_ABGR);

				Graphics2D g = resizedImage.createGraphics();
				try {
					if (info != null && info.supportsCover()) {
						g.setColor(Color.white);
						g.fillRect(0, HOFFSET, COVER_WIDTH, COVER_HEIGHT);

						if (cover != null) {
							BufferedImage coverb = ImageUtilsAwt
									.fromImage(cover);
							g.drawImage(coverb, 0, HOFFSET, COVER_WIDTH,
									COVER_HEIGHT, null);
						} else {
							g.setColor(Color.black);
							g.drawLine(0, HOFFSET, COVER_WIDTH,
									HOFFSET + COVER_HEIGHT);
							g.drawLine(COVER_WIDTH, HOFFSET, 0,
									HOFFSET + COVER_HEIGHT);
						}
					}
				} finally {
					g.dispose();
				}

				// Only save image with a cover, not the X thing
				if (id != null && cover != null) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					ImageIO.write(resizedImage, "png", out);
					byte[] imageBytes = out.toByteArray();
					in = new ByteArrayInputStream(imageBytes);
					Instance.getInstance().getCache().addToCache(in, id);
					in.close();
					in = null;
				}
			} catch (MalformedURLException e) {
				Instance.getInstance().getTraceHandler().error(e);
			} catch (IOException e) {
				Instance.getInstance().getTraceHandler().error(e);
			}
		}

		return resizedImage;
	}

	/**
	 * Manually clear the icon set for this item.
	 * 
	 * @param info
	 *            the info about the story or source/type or author
	 */
	static public void clearIcon(BookInfo info) {
		String id = getIconId(info);
		Instance.getInstance().getCache().removeFromCache(id);
	}

	/**
	 * Get a unique ID from this {@link GuiReaderBookInfo} (note that it can be
	 * a story, a fake item for a source/type or a fake item for an author).
	 * 
	 * @param info
	 *            the info or NULL for a generic (non unique!) ID
	 * @return the unique ID
	 */
	static private String getIconId(BookInfo info) {
		return (info == null ? "" : info.getId() + ".") + "book-thumb_"
				+ SPINE_WIDTH + "x" + COVER_WIDTH + "+" + SPINE_HEIGHT + "+"
				+ COVER_HEIGHT + "@" + HOFFSET;
	}
}
