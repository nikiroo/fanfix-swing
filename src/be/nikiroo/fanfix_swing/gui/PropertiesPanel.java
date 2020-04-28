package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.reader.BasicReader;
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

	/**
	 * Create a new {@link PropertiesPanel}.
	 * 
	 * @param lib
	 *            the library to use for the cover image
	 * @param meta
	 *            the meta to describe
	 */
	public PropertiesPanel(BasicLibrary lib, MetaData meta) {
		// Image
		ImageIcon img = new ImageIcon(CoverImager.generateCoverImage(lib,
				BookInfo.fromMeta(lib, meta)));

		setLayout(new BorderLayout());

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

		Map<String, String> desc = BasicReader.getMetaDesc(meta);

		Color trans = new Color(0, 0, 0, 1);
		Color base = mainPanelValues.getBackground();
		for (String key : desc.keySet()) {
			JTextArea jKey = new JTextArea(key);
			jKey.setFont(new Font(jKey.getFont().getFontName(), Font.BOLD,
					jKey.getFont().getSize()));
			jKey.setEditable(false);
			jKey.setLineWrap(false);
			jKey.setBackground(trans);
			mainPanelKeys.add(jKey);

			final JTextArea jValue = new JTextArea(desc.get(key));
			jValue.setEditable(false);
			jValue.setLineWrap(false);
			jValue.setBackground(base);
			mainPanelValues.add(jValue);
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
		add(imgLabel, BorderLayout.WEST);
		add(mainPanel, BorderLayout.CENTER);
	}

	/**
	 * The invisible border size (multiply by 2 if you need the total width or
	 * the total height).
	 * 
	 * @return the invisible border thickness
	 */
	public int getBorderThickness() {
		return space;
	}
}
