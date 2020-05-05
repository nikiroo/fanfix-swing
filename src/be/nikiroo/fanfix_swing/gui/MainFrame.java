
package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.bundles.UiConfigBundle;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.importer.ImporterFrame;
import be.nikiroo.fanfix_swing.gui.search.SearchFrame;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.ui.BreadCrumbsBar;
import be.nikiroo.utils.ui.ConfigEditor;

/**
 * The main frame of this application, where everything should start.
 * 
 * @author niki
 */
public class MainFrame extends JFrame {
	static private final long serialVersionUID = 1L;

	static private ImporterFrame importer;

	private BooksPanel books;
	private DetailsPanel details;
	private BrowserPanel browser;
	private BreadCrumbsPanel goBack;

	private List<JComponent> modeItems = new ArrayList<JComponent>();
	private boolean sidePanel;
	private boolean detailsPanel;
	private Runnable onCrumbsbreadChange;

	/**
	 * Create a new main frame.
	 * <p>
	 * You should probably only use one for the life of the application.
	 */
	public MainFrame() {
		super("Fanfix " + Version.getCurrentVersion());

		if (importer == null) {
			importer = new ImporterFrame();
		}

		importer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e != null && ImporterFrame.IMPORTED_SUCCESS
						.equals(e.getActionCommand())) {
					browser.reloadData();
					books.reloadData();
					details.setBook(browser.getHighlight());
				}
			}
		});

		UiConfigBundle bundle = Instance.getInstance().getUiConfig();

		browser = new BrowserPanel();
		books = new BooksPanel(
				bundle.getBoolean(UiConfig.SHOW_THUMBNAILS, false),
				bundle.getBoolean(UiConfig.SHOW_WORDCOUNT, false));
		details = new DetailsPanel();
		goBack = new BreadCrumbsPanel();

		browser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				books.loadData(browser.getSelectedSources(),
						browser.getSelectedAuthors(),
						browser.getSelectedTags());
				details.setBook(browser.getHighlight());
			}
		});
		goBack.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				BookInfo book = goBack.getHighlight();
				List<String> sources = new ArrayList<String>();
				List<String> authors = new ArrayList<String>();
				List<String> tags = new ArrayList<String>();

				if (book != null && book.getMainInfo() != null) {
					switch (book.getType()) {
					case SOURCE:
						sources.add(book.getMainInfo());
						break;
					case AUTHOR:
						authors.add(book.getMainInfo());
						break;
					case TAG:
						tags.add(book.getMainInfo());
						break;

					default:
						break;
					}
				}

				books.loadData(sources, authors, tags);
				details.setBook(book);

				if (onCrumbsbreadChange != null) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							if (onCrumbsbreadChange != null)
								onCrumbsbreadChange.run();
						}
					});
				}
			}
		});
		books.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (BooksPanel.INVALIDATE_CACHE.equals(e.getActionCommand())) {
					browser.reloadData();
				}
			}
		});

		// Check config
		boolean sidePanel = Instance.getInstance().getUiConfig()
				.getBoolean(UiConfig.SHOW_SIDE_PANEL, true);
		boolean detailsPanel = Instance.getInstance().getUiConfig()
				.getBoolean(UiConfig.SHOW_DETAILS_PANEL, true);

		// To force an update
		this.sidePanel = !sidePanel;
		this.detailsPanel = !detailsPanel;
		setMode(sidePanel, detailsPanel);

		setJMenuBar(createMenuBar());
		setSize(800, 600);
		UiHelper.setFrameIcon(this);
	}

	/**
	 * Change the side panel mode.
	 * <p>
	 * The side panel contains an author/source/tag/... browser.
	 * <p>
	 * If we have no side panel, a {@link BreadCrumbsBar} will be used instead.
	 * 
	 * @param sidePanel
	 *            TRUE to have a side panel (if FALSE, only a
	 *            {@link BreadCrumbsBar} will be used)
	 */
	public void setSidePanel(boolean sidePanel) {
		setMode(sidePanel, detailsPanel);
	}

	/**
	 * Change the details panel mode.
	 * <p>
	 * The details panel is a side panel with some details about the current
	 * item selected by the browser side panel or the {@link BreadCrumbsBar}.
	 * 
	 * @param detailsPanel
	 *            TRUE for a details panel, FALSE for no details panel
	 * 
	 */
	public void setDetailsPanel(boolean detailsPanel) {
		setMode(sidePanel, detailsPanel);
	}

	/**
	 * Split the two component via a {@link JSplitPane}.
	 * 
	 * @param leftTop
	 *            the first component, that will go on top or on the left
	 * @param rightBottom
	 *            the second component, that will go on the bottom or on the
	 *            right
	 * @param horizontal
	 *            TRUE for horisontal layout (left/right), FALSE for vertical
	 *            (top/bottom)
	 * @param dividerLocation
	 *            the location o the divider, usually in pixels (0 means
	 *            "default to preferred sizes")
	 * @param weight
	 *            the way to divide newly created space on this panel between
	 *            the two component (1 = everything goes to the left/top
	 *            component, 0 means everything to the other component, 0.5
	 *            means equally shared)
	 * 
	 * @return the newly created {@link JSplitPane}
	 */
	private JSplitPane split(JComponent leftTop, JComponent rightBottom,
			boolean horizontal, int dividerLocation, double weight) {
		JSplitPane split = new JSplitPane(
				horizontal ? JSplitPane.HORIZONTAL_SPLIT
						: JSplitPane.VERTICAL_SPLIT,
				leftTop, rightBottom);
		split.setOneTouchExpandable(true);
		split.setResizeWeight(weight);
		split.setContinuousLayout(true);
		split.setDividerLocation(dividerLocation);

		return split;
	}

	/**
	 * Create the menu bar for the main frame.
	 * 
	 * @return the new menu bar
	 */
	private JMenuBar createMenuBar() {
		JMenuBar bar = new JMenuBar();

		// FILE

		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);

		JMenuItem mnuDownload = new JMenuItem("Download", KeyEvent.VK_D);
		mnuDownload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				importer.imprtUrl(MainFrame.this);
			}
		});

		JMenuItem mnuImprtFile = new JMenuItem("Import file", KeyEvent.VK_I);
		mnuImprtFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				importer.imprtFile(MainFrame.this);
			}
		});

		// TODO: un-beta it
		JMenuItem mnuSearch = new JMenuItem("Find a book (EARLY BETA)",
				KeyEvent.VK_F);
		mnuSearch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new SearchFrame(Instance.getInstance().getLibrary())
						.setVisible(true);
			}
		});

		file.add(mnuDownload);
		file.add(mnuImprtFile);
		file.add(mnuSearch);

		// EDIT

		JMenu edit = new JMenu("Edit");
		edit.setMnemonic(KeyEvent.VK_E);

		JMenuItem mnuPrefs = new JMenuItem("Preferences", KeyEvent.VK_P);
		mnuPrefs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConfigEditor<Config> ed = new ConfigEditor<Config>(Config.class,
						Instance.getInstance().getConfig(),
						Instance.getInstance().getTransGui()
								.getString(StringIdGui.SUBTITLE_CONFIG));
				ConfigEditor<UiConfig> edUi = new ConfigEditor<UiConfig>(
						UiConfig.class, Instance.getInstance().getUiConfig(),
						Instance.getInstance().getTransGui()
								.getString(StringIdGui.SUBTITLE_CONFIG_UI));

				// TODO: Very bad UI...
				JFrame frame = new JFrame("Preferences");
				JTabbedPane tabs = new JTabbedPane();
				tabs.add(edUi, "Graphical preferences");
				tabs.add(ed, "Core preferences");
				frame.add(tabs);
				frame.setSize(900, 600);
				frame.setVisible(true);
			}
		});

		edit.add(mnuPrefs);

		// VIEW

		JMenu view = new JMenu("View");
		view.setMnemonic(KeyEvent.VK_V);

		final JMenuItem mnuSidePane = new JCheckBoxMenuItem(
				"Show story browser");
		mnuSidePane.setMnemonic(KeyEvent.VK_B);
		mnuSidePane.setSelected(sidePanel);
		mnuSidePane.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean newValue = !sidePanel;
				setSidePanel(newValue);
				mnuSidePane.setSelected(newValue);

				saveConfig(UiConfig.SHOW_SIDE_PANEL, newValue);
			}
		});

		final JMenuItem mnuDetailsPane = new JCheckBoxMenuItem(
				"Show details panel");
		mnuDetailsPane.setMnemonic(KeyEvent.VK_D);
		mnuDetailsPane.setSelected(detailsPanel);
		mnuDetailsPane.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean newValue = !detailsPanel;
				setDetailsPanel(newValue);
				mnuDetailsPane.setSelected(newValue);

				saveConfig(UiConfig.SHOW_DETAILS_PANEL, newValue);
			}
		});

		final JMenuItem mnuThumbs = new JCheckBoxMenuItem("Show thumbnails");
		mnuThumbs.setMnemonic(KeyEvent.VK_T);
		mnuThumbs.setSelected(books.isShowThumbnails());
		mnuThumbs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean newValue = !books.isShowThumbnails();
				books.setShowThumbnails(newValue);
				mnuThumbs.setSelected(newValue);

				saveConfig(UiConfig.SHOW_THUMBNAILS, newValue);
			}
		});

		final JMenuItem mnuWord = new JMenuItem(
				books.isSeeWordCount() ? "Show author" : "Show word count");
		mnuWord.setMnemonic(
				books.isSeeWordCount() ? KeyEvent.VK_A : KeyEvent.VK_W);
		mnuWord.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean newValue = !books.isSeeWordCount();
				books.setSeeWordCount(newValue);
				mnuWord.setText(newValue ? "Show author" : "Show word count");
				mnuWord.setMnemonic(newValue ? KeyEvent.VK_A : KeyEvent.VK_W);

				saveConfig(UiConfig.SHOW_WORDCOUNT, newValue);
			}
		});

		view.add(mnuSidePane);
		view.add(mnuDetailsPane);
		view.add(mnuThumbs);
		view.add(mnuWord);

		//

		bar.add(file);
		bar.add(edit);
		bar.add(view);

		return bar;
	}

	/**
	 * Save the given option into the config file (as in, save back to disk,
	 * now).
	 * 
	 * @param option
	 *            the option to change
	 * @param value
	 *            the new value
	 */
	private void saveConfig(UiConfig option, boolean value) {
		Instance.getInstance().getUiConfig().setBoolean(option, value);
		try {
			Instance.getInstance().getUiConfig().updateFile();
		} catch (IOException ioe) {
			Instance.getInstance().getTraceHandler().error(
					new IOException("Cannot save configuration file", ioe));
		}
	}

	/**
	 * Change the side panels mode.
	 * 
	 * @param sidePanel
	 *            a side panel with an author/source/tag/... browser (if not,
	 *            only a {@link BreadCrumbsBar} will be used)
	 * @param detailsPanel
	 *            a side panel with some details about the current item selected
	 *            by the browser side panel or the {@link BreadCrumbsBar}
	 */
	private void setMode(boolean sidePanel, boolean detailsPanel) {
		if (this.sidePanel == sidePanel && this.detailsPanel == detailsPanel) {
			return;
		}

		this.sidePanel = sidePanel;
		this.detailsPanel = detailsPanel;

		onCrumbsbreadChange = null;
		for (JComponent comp : modeItems) {
			this.remove(comp);
		}
		modeItems.clear();

		int sidePanelWidth = 300;
		int detailsPanelHeight = 100;
		if (sidePanel && !detailsPanel) {
			JSplitPane split = split(browser, books, true, sidePanelWidth, 0);
			modeItems.add(split);
			this.add(split);
		} else if (sidePanel && detailsPanel) {
			JSplitPane other = split(browser, details, false, 0, 1);
			other.setDividerLocation(other.getHeight() - detailsPanelHeight);
			JSplitPane split = split(other, books, true, sidePanelWidth, 0);
			modeItems.add(split);
			this.add(split);
		} else if (!sidePanel && !detailsPanel) {
			goBack.setVertical(false);
			final JPanel pane = new JPanel(new BorderLayout());
			pane.add(books, BorderLayout.CENTER);
			pane.add(goBack, BorderLayout.NORTH);
			modeItems.add(pane);
			this.add(pane);
		} else if (!sidePanel && detailsPanel) {
			goBack.setVertical(true);
			final JSplitPane other = split(goBack, details, false,
					goBack.getMinimumSize().height, 0);
			JSplitPane split = split(other, books, true, sidePanelWidth, 0);
			modeItems.add(split);
			this.add(split);

			onCrumbsbreadChange = new Runnable() {
				@Override
				public void run() {
					other.setDividerLocation(goBack.getMinimumSize().height);
					other.revalidate();
					other.repaint();
				}
			};
		}

		this.validate();
		this.repaint();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				browser.revalidate();
				browser.repaint();
				books.revalidate();
				books.repaint();
				details.revalidate();
				details.repaint();
				goBack.revalidate();
				goBack.repaint();
			}
		});
	}

	/**
	 * The (unique) {@link ImporterFrame} used by Fanfix-Swing.
	 * 
	 * @return the importer
	 */
	static public ImporterFrame getImporter() {
		return importer;
	}
}
