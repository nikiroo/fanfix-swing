package be.nikiroo.fanfix_swing.gui.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.PropertiesPanel;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;
import be.nikiroo.utils.ui.DelayWorker;
import be.nikiroo.utils.ui.NavBar;
import be.nikiroo.utils.ui.UIUtils;

/**
 * An internal, Swing-based {@link Story} viewer.
 * <p>
 * Works on both text and image document (see {@link MetaData#isImageDocument()}
 * ).
 * 
 * @author niki
 */
public class ViewerNonImages extends JFrame {
	private static final long serialVersionUID = 1L;

	private Story story;
	private ViewerTextOutput html;

	private NavBar navbar;
	private JLabel title;
	private JScrollPane scroll;
	private JEditorPane area;
	private JPanel descPane;

	private DelayWorker worker;

	/**
	 * Create a new {@link Story} viewer.
	 * 
	 * @param lib
	 *            the {@link BasicLibrary} to use to retrieve the cover image in
	 *            the description panel
	 * @param story
	 *            the {@link Story} to display
	 * 
	 */
	public ViewerNonImages(BasicLibrary lib, Story story) {
		this.story = story;
		this.setTitle(Instance.getInstance().getTransGui().getString(
				StringIdGui.TITLE_STORY, story.getMeta().getLuid(),
				story.getMeta().getTitle()));

		this.setSize(800, 600);

		html = new ViewerTextOutput();
		worker = new DelayWorker(100);
		worker.start();

		initGui(lib);
		setChapter(0);

		UiHelper.setFrameIcon(this, lib, story.getMeta());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	/**
	 * Initialise the base panel.
	 * 
	 * @param lib
	 *            the {@link BasicLibrary} to use to retrieve the cover image in
	 *            the description panel
	 */
	private void initGui(BasicLibrary lib) {
		this.setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

		title = new JLabel();
		title.setFont(
				new Font(Font.SERIF, Font.BOLD, title.getFont().getSize() * 2));
		title.setText(story.getMeta().getTitle());
		title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		title.setToolTipText(story.getMeta().getTitle());

		JToolBar toolbarTitle = createToolBar();
		toolbarTitle.add(title);

		area = new JEditorPane("text/html", "");
		area.setEditable(false);
		area.setAlignmentY(TOP_ALIGNMENT);
		area.setOpaque(true);
		area.setFocusable(true);
		area.setBackground(new JTextField().getBackground());
		area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		scroll = UIUtils.scroll(area, false);

		JLabel descLabel = new JLabel("Description");
		descLabel.setFont(new Font(Font.SERIF, Font.BOLD,
				(int) Math.round(descLabel.getFont().getSize() * 1.5)));
		descLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		descLabel.setHorizontalAlignment(JLabel.CENTER);
		descLabel.setOpaque(true);
		Color bg = descLabel.getBackground();
		descLabel.setBackground(descLabel.getForeground());
		descLabel.setForeground(bg);

		descPane = new JPanel(new BorderLayout());
		PropertiesPanel desc = new PropertiesPanel(lib, story.getMeta(), false);
		desc.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		descPane.add(desc, BorderLayout.CENTER);
		descPane.add(descLabel, BorderLayout.SOUTH);

		area.setSize(scroll.getViewport().getSize());
		area.setPreferredSize(this.getSize()); // make it as big as possible
		area.requestFocus();

		this.add(toolbarTitle);
		this.add(descPane);
		this.add(scroll);

		listen();
	}

	private void listen() {
		navbar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setChapter(navbar.getIndex());
			}
		});
	}

	private JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar();
		navbar = new NavBar(0, story.getChapters().size());
		navbar.setIcons( //
				IconGenerator.get(Icon.arrow_double_left, Size.x32), //
				IconGenerator.get(Icon.arrow_left, Size.x32), //
				IconGenerator.get(Icon.arrow_right, Size.x32), //
				IconGenerator.get(Icon.arrow_double_right, Size.x32) //
		);
		toolbar.add(navbar);
		return toolbar;
	}

	/**
	 * Set the current chapter, 0-based.
	 * <p>
	 * Chapter 0 will also toggle the description page on top.
	 * 
	 * @param chapter
	 *            the chapter number to set
	 */
	private void setChapter(final int chapter) {
		worker.delay("update chapter", new SwingWorker<String, Void>() {
			@Override
			protected String doInBackground() throws Exception {
				if (chapter <= 0) {
					return html.convert(story.getMeta().getResume(), false);
				}

				return html.convert(story.getChapters().get(chapter - 1), true);
			}

			@Override
			protected void done() {
				try {
					String text = get();
					if (chapter <= 0) {
						descPane.setVisible(true);
					} else {
						descPane.setVisible(false);
					}

					area.setText(text);
					area.setSize(scroll.getViewport().getSize());
					area.setCaretPosition(0);
					area.scrollRectToVisible(new Rectangle());

					// To work around the fact that sometimes the space of the
					// descpane is kept and the title bar has to take it
					Rectangle pos = getBounds();
					pack();
					setBounds(pos);

					// So we can use the keyboard navigation even after a
					// toolbar click
					area.requestFocus();
				} catch (InterruptedException e) {
				} catch (ExecutionException e) {
				}
			}
		});
	}
}
