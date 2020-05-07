package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Window;
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

import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.book.BookBlock;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.book.BookInfo.Type;
import be.nikiroo.fanfix_swing.gui.book.BookLine;
import be.nikiroo.fanfix_swing.gui.book.BookPopup;
import be.nikiroo.fanfix_swing.gui.book.BookPopup.Informer;
import be.nikiroo.utils.ui.DelayWorker;
import be.nikiroo.utils.ui.ListModel;
import be.nikiroo.utils.ui.ListModel.Predicate;
import be.nikiroo.utils.ui.ListSnapshot;
import be.nikiroo.utils.ui.ListenerPanel;
import be.nikiroo.utils.ui.UIUtils;
import be.nikiroo.utils.ui.compat.JList6;
import be.nikiroo.utils.ui.compat.ListCellRenderer6;

public class BooksPanel extends ListenerPanel {
	/**
	 * The {@link ActionEvent} you receive from
	 * {@link BooksPanel#addActionListener(ActionListener)} (see
	 * {@link ActionEvent#getActionCommand()}) when the cache should be
	 * invalidated.
	 */
	static public final String INVALIDATE_CACHE = "invalidate_cache";

	private enum ReloadMode {
		NONE, STA, TYPE_VALUE
	}

	private class ReloadData {
		public ReloadMode mode;

		public List<String> sources;
		public List<String> authors;
		public List<String> tags;

		public Type type;
		public String value;

		public ReloadData() {
			this.mode = ReloadMode.NONE;
		}

		public ReloadData(List<String> sources, List<String> authors,
				List<String> tags) {
			this.mode = ReloadMode.STA;
			this.sources = sources;
			this.authors = authors;
			this.tags = tags;
		}

		public ReloadData(Type type, String value) {
			this.mode = ReloadMode.TYPE_VALUE;
			this.type = type;
			this.value = value;
		}
	}

	private Map<BookInfo, BookLine> books = new HashMap<BookInfo, BookLine>();
	private boolean seeWordCount;
	private boolean showThumbnails;

	private JList6<BookInfo> list;
	private ListModel<BookInfo> data;
	private DelayWorker bookCoverUpdater;
	private String filter = "";

	private Informer informer;
	private BooksPanelActions actions;

	private ReloadData lastLoad = new ReloadData();

	/**
	 * Create a new {@link BooksPanel}.
	 * <p>
	 * It will come by default with a popup and a tooltip.
	 * 
	 * @param showThumbnails
	 *            show thumbnails instead of lsit items
	 * @param seeWordCount
	 *            show the number of words/images of the book instead of its
	 *            author
	 */
	public BooksPanel(boolean showThumbnails, boolean seeWordCount) {
		setLayout(new BorderLayout());
		this.seeWordCount = seeWordCount;

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
		setShowThumbnails(showThumbnails);
		add(UIUtils.scroll(list, false), BorderLayout.CENTER);
	}

	// null or empty -> all sources
	// sources hierarchy supported ("source/" will includes all "source" and
	// "source/*")
	public void loadData(final List<String> sources, final List<String> authors,
			final List<String> tags) {
		synchronized (lastLoad) {
			lastLoad = new ReloadData(sources, authors, tags);
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
					doLoadData(get());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				// TODO: error
			}
		}.execute();
	}

	// loadData by Book.Type + value
	public void loadData(final Type type, final String value) {
		synchronized (lastLoad) {
			lastLoad = new ReloadData(type, value);
		}

		final List<String> sources = new ArrayList<String>();
		final List<String> authors = new ArrayList<String>();
		final List<String> tags = new ArrayList<String>();

		if (type != null && value != null) {
			switch (type) {
			case SOURCE:
				sources.add(value);
				break;
			case AUTHOR:
				authors.add(value);
				break;
			case TAG:
				tags.add(value);
				break;

			default:
				break;
			}
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
					doLoadData(get());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				// TODO: error
			}
		}.execute();
	}

	private void doLoadData(List<BookInfo> bookInfos) {
		data.clearItems();
		data.addAllItems(bookInfos);
		bookCoverUpdater.clear();

		filter();
	}

	public void reloadData() {
		ReloadData lastLoad;
		synchronized (this.lastLoad) {
			lastLoad = this.lastLoad;
		}

		// Reset the popup menu items for for sources/author
		JPopupMenu popup = data.getPopup();
		if (popup instanceof BookPopup) {
			((BookPopup) popup).reloadData();
		}

		if (lastLoad.mode == ReloadMode.NONE) {
			return; // nothing was loaded yet
		}

		ListSnapshot snapshot = new ListSnapshot(list);

		switch (lastLoad.mode) {
		case STA:
			loadData(lastLoad.sources, lastLoad.authors, lastLoad.tags);
			break;
		case TYPE_VALUE:
			loadData(lastLoad.type, lastLoad.value);
			break;
		default:
			Instance.getInstance().getTraceHandler()
					.error("Unknown last load type: " + lastLoad.mode);
			break;
		}

		snapshot.apply();
	}

	/**
	 * The informer we use for the popup or other actions.
	 * 
	 * @return the informer
	 */
	public Informer getInformer() {
		return informer;
	}

	/**
	 * The popup that we use to generate a {@link BookPopup} on right click.
	 * <p>
	 * Note that a {@link BookPopup} is set by default, with the informer from
	 * this panel.
	 * 
	 * @return the current popup (can be NULL)
	 */
	public JPopupMenu hasPopup() {
		return data.getPopup();
	}

	/**
	 * The popup that we use to generate a {@link BookPopup} on right click.
	 * <p>
	 * Note that a {@link BookPopup} is set by default, with the informer from
	 * this panel.
	 * 
	 * @param popup
	 *            the new popup (can be NULL)
	 */
	public void setPopup(JPopupMenu popup) {
		data.setPopup(popup);
	}

	/**
	 * Generate a tooltip on mouse hover that shows the details of the book
	 * under the mouse.
	 * 
	 * @return TRUE if we use it, FALSE if not
	 */
	public boolean hasTooltip() {
		return data.getTooltipCreator() != null;
	}

	/**
	 * Generate a tooltip on mouse hover that shows the details of the book
	 * under the mouse.
	 * 
	 * @param tooltip
	 *            TRUE to use it, FALSE not to
	 */
	public void setTooltip(boolean tooltip) {
		if (tooltip) {
			data.setTooltipCreator(new ListModel.TooltipCreator<BookInfo>() {
				@Override
				public Window generateTooltip(BookInfo book,
						boolean undecorated) {
					MetaData meta = book == null ? null : book.getMeta();
					if (meta != null) {
						PropertiesDialog tooltip = new PropertiesDialog(
								Instance.getInstance().getLibrary(), meta,
								undecorated);
						return tooltip;
					}

					return null;
				}
			});
		} else {
			data.setTooltipCreator(null);
		}
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
			this.seeWordCount = seeWordCount;

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
		actions = initActions(); // needs informer
		final JList6<BookInfo> list = new JList6<BookInfo>();
		data = new ListModel<BookInfo>(list);
		setPopup(new BookPopup(Instance.getInstance().getLibrary(), informer));
		setTooltip(true);

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
				fireActionPerformed(INVALIDATE_CACHE);
				reloadData();
			}
		};
	}

	protected BooksPanelActions initActions() {
		return new BooksPanelActions(this, getInformer());
	}

	private ListCellRenderer6<BookInfo> generateRenderer() {
		return new ListCellRenderer6<BookInfo>() {
			@Override
			public Component getListCellRendererComponent(JList6<BookInfo> list,
					BookInfo value, int index, boolean isSelected,
					boolean cellHasFocus) {
				BookLine book = books.get(value);
				if (book == null) {
					if (!showThumbnails) {
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

	public boolean isShowThumbnails() {
		return showThumbnails;
	}

	public void setShowThumbnails(boolean showThumbnails) {
		this.showThumbnails = showThumbnails;
		books.clear();
		list.setLayoutOrientation(
				showThumbnails ? JList6.HORIZONTAL_WRAP : JList6.VERTICAL);

		StringBuilder longString = new StringBuilder();
		for (int i = 0; i < 20; i++) {
			longString.append(
					"Some long string, which is 50 chars long itself...");
		}
		if (!showThumbnails) {
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
