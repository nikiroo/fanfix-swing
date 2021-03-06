package be.nikiroo.fanfix_swing.gui.search;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.searchable.BasicSearchable;
import be.nikiroo.fanfix.searchable.SearchableTag;
import be.nikiroo.fanfix.supported.SupportType;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.search.GRBook.BookActionListener;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.utils.ui.NavBar;
import be.nikiroo.utils.ui.UIUtils;

/**
 * This frame will allow you to search through the supported websites for new
 * stories/comics.
 * 
 * @author niki
 */
// JCombobox<E> not 1.6 compatible
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SearchFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private List<SupportType> supportTypes;

	private JComboBox comboSupportTypes;
	private ActionListener comboSupportTypesListener;
	private SearchByPanel searchPanel;
	private NavBar navbar;

	private boolean seeWordcount;
	private GRGroup books;

	public SearchFrame(final BasicLibrary lib) {
		super("Browse stories");
		setLayout(new BorderLayout());
		setSize(800, 600);

		supportTypes = new ArrayList<SupportType>();
		supportTypes.add(null);
		for (SupportType type : SupportType.values()) {
			if (BasicSearchable.getSearchable(type) != null) {
				supportTypes.add(type);
			}
		}

		comboSupportTypes = new JComboBox(
				supportTypes.toArray(new SupportType[] {}));

		comboSupportTypesListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final SupportType support = (SupportType) comboSupportTypes
						.getSelectedItem();
				setWaiting(true);
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							updateSupportType(support);
						} finally {
							setWaiting(false);
						}
					}
				}).start();
			}
		};
		comboSupportTypes.addActionListener(comboSupportTypesListener);

		JPanel searchSites = new JPanel(new BorderLayout());
		searchSites.add(comboSupportTypes, BorderLayout.CENTER);
		searchSites.add(new JLabel(" " + "Website : "), BorderLayout.WEST);

		searchPanel = new SearchByPanel(new SearchByPanel.Waitable() {
			@Override
			public void setWaiting(boolean waiting) {
				SearchFrame.this.setWaiting(waiting);
			}

			@Override
			public void fireEvent() {
				updatePages(searchPanel.getPage(), searchPanel.getMaxPage());
				List<BookInfo> infos = new ArrayList<BookInfo>();
				for (MetaData meta : searchPanel.getStories()) {
					infos.add(BookInfo.fromMeta(lib, meta));
				}

				int page = searchPanel.getPage();
				if (page <= 0) {
					navbar.setMin(1);
					navbar.setMax(1);
				} else {
					int max = searchPanel.getMaxPage();
					navbar.setMin(1);
					navbar.setMax(max);
					navbar.setIndex(page);
				}
				updateBooks(infos);

				// ! 1-based index !
				int item = searchPanel.getStoryItem();
				if (item > 0 && item <= books.getBooksCount()) {
					books.setSelectedBook(item - 1, false);
				}
			}
		});

		JPanel top = new JPanel(new BorderLayout());
		top.add(searchSites, BorderLayout.NORTH);
		top.add(searchPanel, BorderLayout.CENTER);

		add(top, BorderLayout.NORTH);

		books = new GRGroup(lib, null, null);
		books.setActionListener(new BookActionListener() {
			@Override
			public void select(GRBook book) {
			}

			@Override
			public void popupRequested(GRBook book, Component target,
					int x, int y) {
			}

			@Override
			public void action(GRBook book) {
				new SearchAction(lib, book.getInfo()).setVisible(true);
			}
		});
		add(UIUtils.scroll(books, false), BorderLayout.CENTER);

		navbar = new NavBar(-1, -1) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getExtraLabel() {
				if (getIndex() <= 0) {
					return "";
				}
				
				return super.getExtraLabel();
			}
		};

		navbar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				searchPanel.setPage(navbar.getIndex());
			}
		});

		add(navbar, BorderLayout.SOUTH);
		
		UiHelper.setFrameIcon(this);
	}

	/**
	 * Update the {@link SupportType} currently displayed to the user.
	 * <p>
	 * Will also cause a search for the new base tags of the given support if
	 * not NULL.
	 * <p>
	 * This operation can be long and should be run outside the UI thread.
	 * 
	 * @param supportType
	 *            the new {@link SupportType}
	 */
	private void updateSupportType(final SupportType supportType) {
		inUi(new Runnable() {
			@Override
			public void run() {
				books.clear();

				comboSupportTypes
						.removeActionListener(comboSupportTypesListener);
				comboSupportTypes.setSelectedItem(supportType);
				comboSupportTypes.addActionListener(comboSupportTypesListener);
			}
		});

		searchPanel.setSupportType(supportType);
	}

	/**
	 * Update the pages and the lined buttons currently displayed on screen.
	 * <p>
	 * Those are the same pages and maximum pages used by
	 * {@link SearchByPanel#search(String, int, int)} and
	 * {@link SearchByPanel#searchTag(SearchableTag, int, int)}.
	 * 
	 * @param page
	 *            the current page of results
	 * @param maxPage
	 *            the maximum number of pages of results
	 */
	private void updatePages(final int page, final int maxPage) {
		inUi(new Runnable() {
			@Override
			public void run() {
				if (maxPage >= 1) {
					navbar.setMin(1);
					navbar.setMax(maxPage);
					navbar.setIndex(page);
				} else {
					navbar.setMin(-1);
					navbar.setMax(-1);
				}
			}
		});
	}

	/**
	 * Update the currently displayed books.
	 * 
	 * @param infos
	 *            the new books
	 */
	private void updateBooks(final List<BookInfo> infos) {
		inUi(new Runnable() {
			@Override
			public void run() {
				books.refreshBooks(infos, seeWordcount);
			}
		});
	}

	/**
	 * Search for the given terms on the currently selected searchable. This
	 * will update the displayed books if needed.
	 * <p>
	 * This operation is asynchronous.
	 * 
	 * @param keywords
	 *            the keywords to search for
	 * @param page
	 *            the page of results to load
	 * @param item
	 *            the item to select (or 0 for none by default)
	 */
	public void search(final SupportType searchOn, final String keywords,
			final int page, final int item) {
		setWaiting(true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					updateSupportType(searchOn);
					searchPanel.search(keywords, page, item);
				} finally {
					setWaiting(false);
				}
			}
		}).start();
	}

	/**
	 * Search for the given tag on the currently selected searchable. This will
	 * update the displayed books if needed.
	 * <p>
	 * If the tag contains children tags, those will be displayed so you can
	 * select them; if the tag is a leaf tag, the linked stories will be
	 * displayed.
	 * <p>
	 * This operation is asynchronous.
	 * 
	 * @param tag
	 *            the tag to search for, or NULL for base tags
	 * @param page
	 *            the page of results to load
	 * @param item
	 *            the item to select (or 0 for none by default)
	 */
	public void searchTag(final SupportType searchOn, final int page,
			final int item, final SearchableTag tag) {
		setWaiting(true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					updateSupportType(searchOn);
					searchPanel.searchTag(tag, page, item);
				} finally {
					setWaiting(false);
				}
			}
		}).start();
	}

	/**
	 * Process the given action in the main Swing UI thread.
	 * <p>
	 * The code will make sure the current thread is the main UI thread and, if
	 * not, will switch to it before executing the runnable.
	 * <p>
	 * Synchronous operation.
	 * 
	 * @param run
	 *            the action to run
	 */
	static void inUi(final Runnable run) {
		if (EventQueue.isDispatchThread()) {
			run.run();
		} else {
			try {
				EventQueue.invokeAndWait(run);
			} catch (InterruptedException e) {
				error(e);
			} catch (InvocationTargetException e) {
				error(e);
			}
		}
	}

	/**
	 * An error occurred, inform the user and/or log the error.
	 * 
	 * @param e
	 *            the error
	 */
	static void error(Exception e) {
		Instance.getInstance().getTraceHandler().error(e);
	}

	/**
	 * An error occurred, inform the user and/or log the error.
	 * 
	 * @param e
	 *            the error message
	 */
	static void error(String e) {
		Instance.getInstance().getTraceHandler().error(e);
	}

	/**
	 * Enables or disables this component, depending on the value of the
	 * parameter <code>b</code>. An enabled component can respond to user input
	 * and generate events. Components are enabled initially by default.
	 * <p>
	 * Disabling this component will also affect its children.
	 * 
	 * @param b
	 *            If <code>true</code>, this component is enabled; otherwise
	 *            this component is disabled
	 */
	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		books.setEnabled(b);
		searchPanel.setEnabled(b);
	}

	/**
	 * Set the item in wait mode, blocking it from accepting UI input.
	 * 
	 * @param waiting
	 *            TRUE for wait more, FALSE to restore normal mode
	 */
	private void setWaiting(final boolean waiting) {
		inUi(new Runnable() {
			@Override
			public void run() {
				SearchFrame.this.setEnabled(!waiting);
			}
		});
	}
}
