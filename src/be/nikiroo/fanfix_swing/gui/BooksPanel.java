package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.Actions;
import be.nikiroo.fanfix_swing.gui.book.BookBlock;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.book.BookLine;
import be.nikiroo.fanfix_swing.gui.book.BookPopup;
import be.nikiroo.fanfix_swing.gui.book.BookPopup.Informer;
import be.nikiroo.fanfix_swing.gui.utils.DelayWorker;
import be.nikiroo.fanfix_swing.gui.utils.ListModel;
import be.nikiroo.fanfix_swing.gui.utils.ListModel.Predicate;
import be.nikiroo.fanfix_swing.gui.utils.ListenerPanel;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;

public class BooksPanel extends ListenerPanel {
	static public final String INVALIDATE_CACHE = "invalidate_cache";

	private Map<BookInfo, BookLine> books = new HashMap<BookInfo, BookLine>();
	private boolean seeWordCount;
	private boolean listMode;

	@SuppressWarnings("rawtypes") // JList<BookInfo> is not java 1.6
	private JList list;
	private ListModel<BookInfo> data;
	private DelayWorker bookCoverUpdater;
	private String filter = "";

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
		add(UiHelper.scroll(list), BorderLayout.CENTER);
	}

	// null or empty -> all sources
	// sources hierarchy supported ("source/" will includes all "source" and
	// "source/*")
	public void load(final List<String> sources, final List<String> authors,
			final List<String> tags) {
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
					load(get());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				// TODO: error
			}
		}.execute();
	}

	public void load(List<BookInfo> bookInfos) {
		data.clearItems();
		data.addAllItems(bookInfos);
		bookCoverUpdater.clear();

		filter();
	}

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

	@SuppressWarnings("rawtypes") // JList<BookInfo> is not java 1.6
	private JList initList() {
		final JList<BookInfo> list = new JList<BookInfo>();
		data = new ListModel<BookInfo>(list, new BookPopup(
				Instance.getInstance().getLibrary(), initInformer()));

		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getClickCount() == 2) {
					int index = list.locationToIndex(e.getPoint());
					list.setSelectedIndex(index);

					final BookInfo book = data.get(index);
					BasicLibrary lib = Instance.getInstance().getLibrary();

					Actions.openExternal(lib, book.getMeta(), BooksPanel.this,
							new Runnable() {
								@Override
								public void run() {
									book.setCached(true);
									data.fireElementChanged(book);
								}
							});
				}
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

	@SuppressWarnings("rawtypes") // ListCellRenderer<BookInfo> is not java 1.6
	private ListCellRenderer generateRenderer() {
		return new ListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				BookLine book = books.get(value);
				if (book == null) {
					if (listMode) {
						book = new BookLine((BookInfo) value, seeWordCount);
					} else {
						book = new BookBlock((BookInfo) value, seeWordCount);
						startUpdateBookCover((BookBlock) book);
					}
					books.put((BookInfo) value, book);
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
				listMode ? JList.VERTICAL : JList.HORIZONTAL_WRAP);

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
