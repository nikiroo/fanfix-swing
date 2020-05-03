package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.Actions;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.utils.CoverImager;
import be.nikiroo.utils.ui.UIUtils;

/**
 * A panel displaying properties and other information of a {@link Story}.
 * 
 * @author niki
 */
public class PropertiesPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final int space = 10; // empty space for visual correctness
	private final int hscroll = 10; // we reserve space at the bottom for a
									// potential HScroll
	private List<Component> listenables;

	/**
	 * Create a new {@link PropertiesPanel}.
	 * 
	 * @param lib
	 *            the library to use for the cover image
	 * @param meta
	 *            the meta to describe
	 * @param includeTitle
	 *            TRUE to include the title on top
	 */
	public PropertiesPanel(BasicLibrary lib, MetaData meta,
			boolean includeTitle) {
		listenables = new ArrayList<Component>();

		Color trans = new Color(0, 0, 0, 1);

		// Image
		ImageIcon img = new ImageIcon(CoverImager.generateCoverImage(lib,
				BookInfo.fromMeta(lib, meta)));

		setLayout(new BorderLayout());

		// Title
		JPanel title = null;
		if (includeTitle) {
			title = new JPanel(new BorderLayout());
			JTextArea titleLabel = new JTextArea(
					meta.getLuid() + ": " + meta.getTitle());
			titleLabel.setEditable(false);
			titleLabel.setLineWrap(true);
			titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
			titleLabel.setOpaque(false);
			titleLabel.setFocusable(false);
			titleLabel.setBorder(new EmptyBorder(3, 3, 3, 3));
			titleLabel.setAlignmentY(JLabel.CENTER_ALIGNMENT);
			title.add(titleLabel);
			Color fg = new JLabel("dummy").getForeground();
			Color bg = title.getBackground();
			title.setForeground(bg);
			title.setBackground(fg);
			titleLabel.setForeground(bg);
			titleLabel.setBackground(trans);

			listenables.add(title);
			listenables.add(titleLabel);
		}

		// Main panel
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel mainPanelKeys = new JPanel();
		mainPanelKeys.setLayout(new BoxLayout(mainPanelKeys, BoxLayout.Y_AXIS));
		JPanel mainPanelValues = new JPanel();
		mainPanelValues
				.setLayout(new BoxLayout(mainPanelValues, BoxLayout.Y_AXIS));

		mainPanel.add(mainPanelKeys, BorderLayout.WEST);
		mainPanel.add(UIUtils.scroll(mainPanelValues, true, false),
				BorderLayout.CENTER);

		Map<String, String> desc = Actions.getMetaDesc(meta);
		for (String key : desc.keySet()) {
			JTextArea jKey = new JTextArea(key);
			jKey.setFont(new Font(jKey.getFont().getFontName(), Font.BOLD,
					jKey.getFont().getSize()));
			jKey.setEditable(false);
			jKey.setLineWrap(false);
			jKey.setBackground(trans);
			listenables.add(jKey);
			mainPanelKeys.add(jKey);

			final JTextArea jValue = new JTextArea(desc.get(key));
			jValue.setEditable(false);
			jValue.setLineWrap(false);
			listenables.add(jValue);
			mainPanelValues.add(jValue);

			mainPanelValues.setBackground(jValue.getBackground());
		}

		// Image
		JLabel imgLabel = new JLabel(img);
		imgLabel.setVerticalAlignment(JLabel.TOP);

		// Borders
		mainPanelKeys.setBorder(BorderFactory.createEmptyBorder(space, space,
				space + hscroll, space));
		mainPanelValues.setBorder(BorderFactory.createEmptyBorder(space, space,
				space + hscroll, space));
		imgLabel.setBorder(
				BorderFactory.createEmptyBorder(0, space, space + hscroll, 0));

		// Add all
		if (includeTitle)
			add(title, BorderLayout.NORTH);
		add(imgLabel, BorderLayout.WEST);
		add(mainPanel, BorderLayout.CENTER);

		listenables.add(imgLabel);
		listenables.add(mainPanel);
		listenables.add(mainPanelKeys);
		listenables.add(mainPanelValues);
	}

	@Override
	public synchronized void addMouseListener(MouseListener l) {
		super.addMouseListener(l);

		for (Component comp : listenables) {
			comp.addMouseListener(l);
		}
	}
}
