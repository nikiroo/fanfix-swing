package be.nikiroo.fanfix_swing.gui;

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
import javax.swing.JSplitPane;

import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.importer.ImporterFrame;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;
import be.nikiroo.utils.Version;

public class MainFrame extends JFrame {
	private BooksPanel books;
	private DetailsPanel details;
	private BrowserPanel browser;
	private BreadCrumbsPanel goBack;
	private ImporterFrame importer = new ImporterFrame();
	
	private List<JComponent> modeItems = new ArrayList<JComponent>();
	private boolean sidePanel;
	private boolean detailsPanel;

	public MainFrame(boolean sidePanel, boolean detailsPanel) {
		super("Fanfix " + Version.getCurrentVersion());

		browser = new BrowserPanel();
		books = new BooksPanel(true);
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

		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);

		JMenuItem item1 = new JMenuItem("Download", KeyEvent.VK_D);
		item1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				importer.imprtUrl(MainFrame.this, new Runnable() {
					@Override
					public void run() {
						browser.reloadData();
						books.reloadData();
						details.setBook(browser.getHighlight());
					}
				});
			}
		});

		JMenuItem item2 = new JMenuItem("Import file", KeyEvent.VK_I);
		item2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				importer.imprtFile(MainFrame.this, new Runnable() {
					@Override
					public void run() {
						browser.reloadData();
						books.reloadData();
						details.setBook(browser.getHighlight());
					}
				});
			}
		});

		file.add(item1);
		file.add(item2);

		JMenu edit = new JMenu("Edit");
		edit.setMnemonic(KeyEvent.VK_E);

		JMenu view = new JMenu("View");
		view.setMnemonic(KeyEvent.VK_V);
		
		final JMenuItem listMode = new JMenuItem("Show thumbnails", KeyEvent.VK_T);
		listMode.setIcon(books.isListMode() ? noIcon : yesIcon);
		listMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				books.setListMode(!books.isListMode());
				listMode.setIcon(books.isListMode() ? noIcon : yesIcon);
			}
		});

		final JMenuItem sidePane = new JMenuItem("Show story browser", KeyEvent.VK_B);
		sidePane.setIcon(sidePanel ? yesIcon : noIcon);
		sidePane.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setSidePanel(!sidePanel);
				sidePane.setIcon(sidePanel ? yesIcon : noIcon);
			}
		});

		final JMenuItem detailsPane = new JMenuItem("Show details panel", KeyEvent.VK_D);
		detailsPane.setIcon(detailsPanel ? yesIcon : noIcon);
		detailsPane.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setDetailsPanel(!detailsPanel);
				detailsPane.setIcon(detailsPanel ? yesIcon : noIcon);
			}
		});

		view.add(sidePane);
		view.add(detailsPane);
		view.add(listMode);
		
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
			JSplitPane split = split(goBack, books, false, 0.5, 0);
			modeItems.add(split);
			this.add(split);
		} else if (!sidePanel && detailsPanel) {
			goBack.setVertical(true);
			JSplitPane other = split(goBack, details, false, 0.5, 1);
			JSplitPane split = split(other, books, true, 0.5, 0);
			modeItems.add(split);
			this.add(split);
		}
		
		this.revalidate();
		this.repaint();
	}
}
