
package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
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
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.importer.ImporterFrame;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.ui.ConfigEditor;

public class MainFrame extends JFrame {
	static private ImporterFrame importer;

	private BooksPanel books;
	private DetailsPanel details;
	private BrowserPanel browser;
	private BreadCrumbsPanel goBack;

	private List<JComponent> modeItems = new ArrayList<JComponent>();
	private boolean sidePanel;
	private boolean detailsPanel;
	private Runnable onCrumbsbreadChange;

	public MainFrame(boolean sidePanel, boolean detailsPanel) {
		super("Fanfix " + Version.getCurrentVersion());

		if (importer == null) {
			importer = new ImporterFrame();
		}

		importer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e != null && ImporterFrame.IMPORTED
						.equals(e.getActionCommand())) {
					browser.reloadData();
					books.reloadData();
					details.setBook(browser.getHighlight());
				}
			}
		});

		browser = new BrowserPanel();
		books = new BooksPanel(true); // TODO: very slow here!!
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

		// To force an update
		this.sidePanel = !sidePanel;
		this.detailsPanel = !detailsPanel;
		setMode(sidePanel, detailsPanel);

		setJMenuBar(createMenuBar());
		setSize(800, 600);
	}

	static public ImporterFrame getImporter() {
		return importer;
	}

	public void setSidePanel(boolean sidePanel) {
		setMode(sidePanel, detailsPanel);
	}

	public void setDetailsPanel(boolean detailsPanel) {
		setMode(sidePanel, detailsPanel);
	}

	private JSplitPane split(JComponent leftTop, JComponent rightBottom,
			boolean horizontal, double ratio, double weight) {
		JSplitPane split = new JSplitPane(
				horizontal ? JSplitPane.HORIZONTAL_SPLIT
						: JSplitPane.VERTICAL_SPLIT,
				leftTop, rightBottom);
		split.setOneTouchExpandable(true);
		split.setResizeWeight(weight);
		split.setContinuousLayout(true);
		split.setDividerLocation(ratio);

		return split;
	}

	private JMenuBar createMenuBar() {
		// TODO: use a correct checkmark image
		final ImageIcon yesIcon = IconGenerator.get(Icon.clear, Size.x16);
		final ImageIcon noIcon = IconGenerator.get(Icon.empty, Size.x16);

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

		file.add(mnuDownload);
		file.add(mnuImprtFile);

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
				tabs.add(ed, "Core preferences");
				tabs.add(edUi, "Graphical preferences");
				frame.add(tabs);
				frame.setSize(900, 600);
				frame.setVisible(true);
			}
		});

		edit.add(mnuPrefs);

		// VIEW

		JMenu view = new JMenu("View");
		view.setMnemonic(KeyEvent.VK_V);

		final JMenuItem mnuListMode = new JMenuItem("Show thumbnails",
				KeyEvent.VK_T);
		mnuListMode.setIcon(books.isListMode() ? noIcon : yesIcon);
		mnuListMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				books.setListMode(!books.isListMode());
				mnuListMode.setIcon(books.isListMode() ? noIcon : yesIcon);
			}
		});

		final JMenuItem mnuSidePane = new JMenuItem("Show story browser",
				KeyEvent.VK_B);
		mnuSidePane.setIcon(sidePanel ? yesIcon : noIcon);
		mnuSidePane.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setSidePanel(!sidePanel);
				mnuSidePane.setIcon(sidePanel ? yesIcon : noIcon);
			}
		});

		final JMenuItem mnuDetailsPane = new JMenuItem("Show details panel",
				KeyEvent.VK_D);
		mnuDetailsPane.setIcon(detailsPanel ? yesIcon : noIcon);
		mnuDetailsPane.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setDetailsPanel(!detailsPanel);
				mnuDetailsPane.setIcon(detailsPanel ? yesIcon : noIcon);
			}
		});

		view.add(mnuSidePane);
		view.add(mnuDetailsPane);
		view.add(mnuListMode);

		//

		bar.add(file);
		bar.add(edit);
		bar.add(view);

		return bar;
	}

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

		if (sidePanel && !detailsPanel) {
			JSplitPane split = split(browser, books, true, 0.5, 0);
			modeItems.add(split);
			this.add(split);
		} else if (sidePanel && detailsPanel) {
			JSplitPane other = split(browser, details, false, 0.5, 1);
			JSplitPane split = split(other, books, true, 0.5, 0);
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
			final JSplitPane other = split(goBack, details, false, 0.5, 0);
			JSplitPane split = split(other, books, true, 0.5, 0);
			modeItems.add(split);
			this.add(split);

			onCrumbsbreadChange = new Runnable() {
				@Override
				public void run() {
					other.setDividerLocation(
							other.getTopComponent().getMinimumSize().height);
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
}
