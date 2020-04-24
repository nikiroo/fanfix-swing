package be.nikiroo.fanfix_swing.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;

import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.importer.ImporterFrame;
import be.nikiroo.utils.Version;

public class MainFrame extends JFrame {
	private BooksPanel books;
	private DetailsPanel details;
	private BrowserPanel browser;
	private BreadCrumbsPanel goBack;
	private ImporterFrame importer = new ImporterFrame();

	public MainFrame(boolean sidePanel, boolean detailsPanel) {
		super("Fanfix " + Version.getCurrentVersion());
		setSize(800, 600);
		setJMenuBar(createMenuBar());

		// TODO: setSidePanel() and setDetailsPanel();

		browser = new BrowserPanel();
		books = new BooksPanel(true);
		details = new DetailsPanel();
		goBack = new BreadCrumbsPanel();

		JComponent other = null;
		boolean orientationH = true;
		if (sidePanel && !detailsPanel) {
			other = browser;
		} else if (sidePanel && detailsPanel) {
			JComponent side = browser;
			other = split(side, details, false, 0.5, 1);
		} else if (!sidePanel && !detailsPanel) {
			orientationH = false;
			other = goBack;
			goBack.setVertical(false);
		} else if (!sidePanel && detailsPanel) {
			other = split(goBack, details, false, 0.5, 1);
			goBack.setVertical(true);
		}

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

		JSplitPane split = split(other, books, orientationH, 0.5, 0);

		this.add(split);
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

		JMenuItem listMode = new JMenuItem("List mode", KeyEvent.VK_L);
		listMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				books.setListMode(!books.isListMode());
			}
		});

		view.add(listMode);

		bar.add(file);
		bar.add(edit);
		bar.add(view);

		return bar;
	}
}
