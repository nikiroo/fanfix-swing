package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.book.BookBlock;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.book.BookLine;
import be.nikiroo.fanfix_swing.gui.book.BookPopup;
import be.nikiroo.fanfix_swing.gui.book.BookPopup.Informer;
import be.nikiroo.utils.compat.JList6;
import be.nikiroo.utils.compat.ListCellRenderer6;
import be.nikiroo.utils.ui.DelayWorker;
import be.nikiroo.utils.ui.ListModel;
import be.nikiroo.utils.ui.ListSnapshot;
import be.nikiroo.utils.ui.ListModel.Predicate;
import be.nikiroo.utils.ui.ListenerPanel;
import be.nikiroo.utils.ui.TreeSnapshot;
import be.nikiroo.utils.ui.UIUtils;

public class BooksPanel extends ListenerPanel {
	static public final String INVALIDATE_CACHE = "invalidate_cache";

	private Map<BookInfo, BookLine> books = new HashMap<BookInfo, BookLine>();
	private boolean seeWordCount;
	private boolean listMode;

	private JList6<BookInfo> list;
	private ListModel<BookInfo> data;
	private DelayWorker bookCoverUpdater;
	private String filter = "";

	private Informer informer;
	private BooksPanelActions actions;

	private Object[] lastLoad = new Object[4];

	public BooksPanel(boolean listMode) {
		setLayout(new BorderLayout());

		final SearchBar search = new SearchBar();
		add(search, BorderLayout.NORTH);

		search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				filter = search.getText();
				filter();
			}
		});

		bookCoverUpdater = new DelayWorker(20);
		bookCoverUpdater.start();

		list = initList();
		setListMode(listMode);
		add(UIUtils.scroll(list, false), BorderLayout.CENTER);
	}

	// null or empty -> all sources
	// sources hierarchy supported ("source/" will includes all "source" and
	// "source/*")
	public void loadData(final List<String> sources, final List<String> authors,
			final List<String> tags) {
		synchronized (lastLoad) {
			lastLoad[0] = "sources, authors, tags";
			lastLoad[1] = sources;
			lastLoad[2] = authors;
			lastLoad[3] = tags;
		}

		new SwingWorker<List<BookInfo>, Void>() {
			@Override
			protected List<BookInfo> doInBackground() throws Exception {
				List<BookInfo> bookInfos = new ArrayList<BookInfo>();
				BasicLibrary lib = Instance.getInstance().getLibrary();
				for (MetaData meta : lib.getList().filter(sources, authors,
						tags)) {
					bookInfos.add(BookInfo.fromMeta(lib, meta));
				}

				return bookInfos;
			}

			@Override
			protected void done() {
				try {
					loadData(get());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				// TODO: error
			}
		}.execute();
	}

	// TODO
	private void loadData(final BookInfo.Type type, final String value) {
		synchronized (lastLoad) {
			lastLoad[0] = "type";
			lastLoad[1] = type;
			lastLoad[2] = value;
		}

		// TODO todo todo
	}

	// TODO
	private void loadData(List<BookInfo> bookInfos) {
		// synchronized (lastLoad) {
		// lastLoad[0] = "bookInfos";
		// lastLoad[1] = bookInfos;
		// }

		data.clearItems();
		data.addAllItems(bookInfos);
		bookCoverUpdater.clear();

		filter();
	}

	public void reloadData() {
		Object[] lastLoad;
		synchronized (this.lastLoad) {
			lastLoad = this.lastLoad.clone();
		}

		if (lastLoad[0] == null) {
			return; // nothing was loaded yet
		}

		ListSnapshot snapshot = new ListSnapshot(list);

		if (lastLoad[0].toString().equals("sources, authors, tags")) {
			loadData((List<String>) lastLoad[1], (List<String>) lastLoad[2],
					(List<String>) lastLoad[3]);
		} else if (lastLoad[0].toString().equals("type")) {
			loadData((BookInfo.Type) lastLoad[1], (String) lastLoad[2]);
		} else if (lastLoad[0].toString().equals("bookInfos")) {
			loadData((List<BookInfo>) lastLoad[1]);
		} else {
			Instance.getInstance().getTraceHandler()
					.error("Unknown last load type: " + lastLoad[0]);
		}

		snapshot.apply();
	}

	// is UI!
	private void filter() {
		data.filter(new Predicate<BookInfo>() {
			@Override
			public boolean test(BookInfo item) {
				return item.getMainInfo() == null || filter.isEmpty()
						|| item.getMainInfo().toLowerCase()
								.contains(filter.toLowerCase());
			}
		});
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
			if (books != null) {
				for (BookLine book : books.values()) {
					book.setSeeWordCount(seeWordCount);
				}

				list.repaint();
			}
		}
	}

	private JList6<BookInfo> initList() {
		informer = initInformer();
		actions = new BooksPanelActions(this, informer);
		final JList6<BookInfo> list = new JList6<BookInfo>();
		data = new ListModel<BookInfo>(list,
				new BookPopup(Instance.getInstance().getLibrary(), informer));

		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getClickCount() == 2) {
					int index = list.locationToIndex(e.getPoint());
					list.setSelectedIndex(index);
					actions.openBook();
				}
			}
		});

		list.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER
						|| e.getKeyCode() == KeyEvent.VK_ACCEPT) {
					actions.openBook();
					e.consume();
				} else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					actions.deleteBooks();
					e.consume();
				}
				super.keyTyped(e);
			}
		});

		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setSelectedIndex(0);
		list.setCellRenderer(generateRenderer());
		list.setVisibleRowCount(0);

		return list;
	}

	private Informer initInformer() {
		return new BookPopup.Informer() {
			@Override
			public BooksPanelActions getActions() {
				return actions;
			}

			@Override
			public void setCached(BookInfo book, boolean cached) {
				book.setCached(cached);
				fireElementChanged(book);
			}

			@Override
			public void fireElementChanged(BookInfo book) {
				data.fireElementChanged(book);
			}

			@Override
			public void removeElement(BookInfo book) {
				data.removeElement(book);
			}

			@Override
			public List<BookInfo> getSelected() {
				return data.getSelectedElements();
			}

			@Override
			public BookInfo getUniqueSelected() {
				return data.getUniqueSelectedElement();
			}

			@Override
			public void invalidateCache() {
				// TODO: also reset the popup menu for sources/author
				fireActionPerformed(INVALIDATE_CACHE);
			}
		};
	}

	private ListCellRenderer6<BookInfo> generateRenderer() {
		return new ListCellRenderer6<BookInfo>() {
			@Override
			public Component getListCellRendererComponent(JList6<BookInfo> list,
					BookInfo value, int index, boolean isSelected,
					boolean cellHasFocus) {
				BookLine book = books.get(value);
				if (book == null) {
					if (listMode) {
						book = new BookLine(value, seeWordCount);
					} else {
						book = new BookBlock(value, seeWordCount);
						startUpdateBookCover((BookBlock) book);
					}
					books.put(value, book);
				}

				book.setSelected(isSelected);
				book.setHovered(data.isHovered(index));
				return book;
			}
		};
	}

	private void startUpdateBookCover(final BookBlock book) {
		bookCoverUpdater.delay(book.getInfo().getId(),
				new SwingWorker<Image, Void>() {
					@Override
					protected Image doInBackground() throws Exception {
						BasicLibrary lib = Instance.getInstance().getLibrary();
						return BookBlock.generateCoverImage(lib,
								book.getInfo());
					}

					@Override
					protected void done() {
						try {
							book.setCoverImage(get());
							data.fireElementChanged(book.getInfo());
						} catch (Exception e) {
							// TODO ? probably just log
						}
					}
				});
	}

	public boolean isListMode() {
		return listMode;
	}

	public void setListMode(boolean listMode) {
		this.listMode = listMode;
		books.clear();
		list.setLayoutOrientation(
				listMode ? JList6.VERTICAL : JList6.HORIZONTAL_WRAP);

		StringBuilder longString = new StringBuilder();
		for (int i = 0; i < 20; i++) {
			longString.append(
					"Some long string, which is 50 chars long itself...");
		}
		if (listMode) {
			bookCoverUpdater.clear();
			Dimension sz = new BookLine(
					BookInfo.fromSource(null, longString.toString()), true)
							.getPreferredSize();
			list.setFixedCellHeight((int) sz.getHeight());
			list.setFixedCellWidth(list.getWidth());
		} else {
			Dimension sz = new BookBlock(
					BookInfo.fromSource(null, longString.toString()), true)
							.getPreferredSize();
			list.setFixedCellHeight((int) sz.getHeight());
			list.setFixedCellWidth((int) sz.getWidth());
		}
	}
}
