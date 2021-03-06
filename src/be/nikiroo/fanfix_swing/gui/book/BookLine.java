package be.nikiroo.fanfix_swing.gui.book;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix_swing.gui.BooksPanel;
import be.nikiroo.fanfix_swing.gui.utils.CoverImager;
import be.nikiroo.utils.ui.UIUtils;

/**
 * A book item presented in a {@link BooksPanel}.
 * <p>
 * Can be a story, or a comic or... a group.
 * 
 * @author niki
 */
public class BookLine extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final int MAX_DISPLAY_SIZE = 40;

	/** Colour used for the secondary item (author/word count). */
	protected static final Color AUTHOR_COLOR = new Color(128, 128, 128);

	private boolean selected;
	private boolean hovered;

	private BookInfo info;
	private boolean seeWordCount;

	private JLabel title;
	private JLabel secondary;
	private JLabel iconCached;
	private JLabel iconNotCached;

	/**
	 * Create a new {@link BookLine} item for the given {@link Story}.
	 * 
	 * @param info
	 *            the information about the story to represent
	 * @param seeWordCount
	 *            TRUE to see word counts, FALSE to see authors
	 */
	public BookLine(BookInfo info, boolean seeWordCount) {
		this.info = info;
		this.seeWordCount = seeWordCount;

		init();
	}

	/**
	 * Initialise this {@link BookLine}.
	 */
	protected void init() {
		iconCached = new JLabel("   ") {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {
				super.paint(g);

				Dimension sz = iconCached.getSize();
				int s = Math.min(sz.width, sz.height);
				int x = Math.max(0, (sz.width - sz.height) / 2);
				int y = Math.max(0, (sz.height - sz.width) / 2);

				UIUtils.drawEllipse3D(g, BookCoverImager.CACHED_ICON_COLOR, x,
						y, s, s, true);
			}
		};
		iconNotCached = new JLabel("   ") {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {
				super.paint(g);

				Dimension sz = iconNotCached.getSize();
				int s = Math.min(sz.width, sz.height);
				int x = Math.max(0, (sz.width - sz.height) / 2);
				int y = Math.max(0, (sz.height - sz.width) / 2);

				UIUtils.drawEllipse3D(g, BookCoverImager.UNCACHED_ICON_COLOR, x,
						y, s, s, false);
			}
		};

		iconNotCached.setForeground(BookCoverImager.UNCACHED_ICON_COLOR);
		iconCached.setForeground(BookCoverImager.CACHED_ICON_COLOR);
		iconCached.setPreferredSize(iconNotCached.getPreferredSize());

		title = new JLabel();
		secondary = new JLabel();
		secondary.setForeground(AUTHOR_COLOR);

		String luid = null;
		if (info.getMeta() != null) {
			luid = info.getMeta().getLuid();
		}
		JLabel id = new JLabel(luid);
		id.setPreferredSize(new JLabel(" 999 ").getPreferredSize());
		id.setForeground(Color.gray);
		id.setHorizontalAlignment(SwingConstants.CENTER);

		JPanel idTitle = new JPanel(new BorderLayout());
		idTitle.setOpaque(false);
		idTitle.add(id, BorderLayout.WEST);
		idTitle.add(title, BorderLayout.CENTER);

		setLayout(new BorderLayout());
		add(idTitle, BorderLayout.CENTER);
		add(secondary, BorderLayout.EAST);

		updateMeta();
	}

	/**
	 * The book current selection state.
	 * 
	 * @return the selection state
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * The book current selection state,
	 * 
	 * @param selected
	 *            TRUE if it is selected
	 */
	public void setSelected(boolean selected) {
		if (this.selected != selected) {
			this.selected = selected;
			repaint();
		}
	}

	/**
	 * The item mouse-hover state.
	 * 
	 * @return TRUE if it is mouse-hovered
	 */
	public boolean isHovered() {
		return this.hovered;
	}

	/**
	 * The item mouse-hover state.
	 * 
	 * @param hovered
	 *            TRUE if it is mouse-hovered
	 */
	public void setHovered(boolean hovered) {
		if (this.hovered != hovered) {
			this.hovered = hovered;
			repaint();
		}
	}

	/**
	 * The secondary value content: word count or author.
	 * 
	 * @return TRUE to see word counts, FALSE to see authors
	 */
	public boolean isSeeWordCount() {
		return seeWordCount;
	}

	/**
	 * The secondary value content: word count or author.
	 * 
	 * @param seeWordCount
	 *            TRUE to see word counts, FALSE to see authors
	 */
	public void setSeeWordCount(boolean seeWordCount) {
		if (this.seeWordCount != seeWordCount) {
			this.seeWordCount = seeWordCount;
			repaint();
		}
	}

	/**
	 * The information about the book represented by this item.
	 * 
	 * @return the meta
	 */
	public BookInfo getInfo() {
		return info;
	}

	/**
	 * Update the title, paint the item.
	 */
	@Override
	public void paint(Graphics g) {
		updateMeta();
		super.paint(g);
	}

	/**
	 * Return a display-ready version of {@link BookInfo#getMainInfo()}.
	 * 
	 * @param limit
	 *            TRUE to allow shortening
	 * 
	 * @return the main info in a ready-to-display version
	 */
	protected String getMainInfoDisplay(boolean limit) {
		return toDisplay(getInfo().getMainInfo(), limit);
	}

	/**
	 * Return a display-ready version of
	 * {@link BookInfo#getSecondaryInfo(boolean)}.
	 * 
	 * @param seeCount
	 *            TRUE for word/image/story count, FALSE for author name
	 * @param limit
	 *            TRUE to allow shortening
	 * 
	 * @return the main info in a ready-to-display version
	 */
	protected String getSecondaryInfoDisplay(boolean seeCount, boolean limit) {
		return toDisplay(getInfo().getSecondaryInfo(seeCount), limit);
	}

	/**
	 * Update the title with the currently registered information.
	 */
	protected void updateMeta() {
		String main = getMainInfoDisplay(false);
		String optSecondary = getSecondaryInfoDisplay(isSeeWordCount(), false);

		title.setText(main);
		secondary.setText(optSecondary + " ");

		setBackground(CoverImager.getBackground(isEnabled(), isSelected(),
				isHovered()));

		remove(iconCached);
		remove(iconNotCached);
		add(getInfo().isCached() ? iconCached : iconNotCached,
				BorderLayout.WEST);
		validate();
	}

	/**
	 * Make the given {@link String} display-ready (i.e., shorten it if it is
	 * too long).
	 * 
	 * @param value
	 *            the full value
	 * @param limit
	 *            TRUE to allow shortening
	 * 
	 * @return the display-ready value
	 */
	private String toDisplay(String value, boolean limit) {
		if (value == null)
			value = "";

		if (limit && value.length() > MAX_DISPLAY_SIZE) {
			value = value.substring(0, MAX_DISPLAY_SIZE - 3) + "...";
		}

		return value;
	}
}
