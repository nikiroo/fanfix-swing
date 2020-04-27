package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.utils.DataTreeBooks;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.utils.ui.ListenerPanel;

/**
 * Panel dedicated to browse the stories through different means: by authors, by
 * tags or by sources.
 * 
 * @author niki
 */
public class BrowserPanel extends ListenerPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * The {@link ActionEvent} you receive from
	 * {@link BrowserPanel#addActionListener(ActionListener)} can return this as
	 * a command (see {@link ActionEvent#getActionCommand()}) if they were
	 * created in the scope of a source.
	 */
	static public final String SOURCE_SELECTION = "source_selection";
	/**
	 * The {@link ActionEvent} you receive from
	 * {@link BrowserPanel#addActionListener(ActionListener)} can return this as
	 * a command (see {@link ActionEvent#getActionCommand()}) if they were
	 * created in the scope of an author.
	 */
	static public final String AUTHOR_SELECTION = "author_selection";
	/**
	 * The {@link ActionEvent} you receive from
	 * {@link BrowserPanel#addActionListener(ActionListener)} can return this as
	 * a command (see {@link ActionEvent#getActionCommand()}) if they were
	 * created in the scope of a tag.
	 */
	static public final String TAGS_SELECTION = "tags_selection";
	/**
	 * The {@link ActionEvent} you receive from
	 * {@link BrowserPanel#addActionListener(ActionListener)} can return this as
	 * a command (see {@link ActionEvent#getActionCommand()}) if they were
	 * created in the scope of a tab change.
	 */
	static public final String TAB_CHANGE = "tab_change";

	private DataTreeBooks dataTreeBooks;

	private JTabbedPane tabs;
	private BrowserTab sourceTab;
	private BrowserTab authorTab;
	private BrowserTab tagsTab;

	private boolean keepSelection;

	/**
	 * Create a nesw {@link BrowserPanel}.
	 */
	public BrowserPanel() {
		this.setPreferredSize(new Dimension(200, 800));

		this.setLayout(new BorderLayout());
		tabs = new JTabbedPane();

		int index = 0;
		dataTreeBooks = new DataTreeBooks(false, true, true);
		tabs.add(sourceTab = new BrowserTab(dataTreeBooks.getSources(), index++,
				SOURCE_SELECTION));
		tabs.add(authorTab = new BrowserTab(dataTreeBooks.getAuthors(), index++,
				AUTHOR_SELECTION));
		tabs.add(tagsTab = new BrowserTab(dataTreeBooks.getTags(), index++,
				TAGS_SELECTION));

		configureTab(tabs, sourceTab, "Sources", "Tooltip for Sources");
		configureTab(tabs, authorTab, "Authors", "Tooltip for Authors");
		configureTab(tabs, tagsTab, "Tags", "Tooltip for Tags");

		JPanel options = new JPanel();
		options.setLayout(new BorderLayout());

		final JButton keep = new JButton("Keep selection");
		UiHelper.setButtonPressed(keep, keepSelection);
		keep.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				keepSelection = !keepSelection;
				UiHelper.setButtonPressed(keep, keepSelection);
				keep.setSelected(keepSelection);
				if (!keepSelection) {
					unselect();
				}
			}
		});

		options.add(keep, BorderLayout.CENTER);

		add(tabs, BorderLayout.CENTER);
		add(options, BorderLayout.SOUTH);

		tabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (!keepSelection) {
					unselect();
				}

				fireActionPerformed(TAB_CHANGE);
			}
		});

		reloadData(true);
	}

	private void unselect() {
		for (int i = 0; i < tabs.getTabCount(); i++) {
			if (i == tabs.getSelectedIndex())
				continue;

			BrowserTab tab = (BrowserTab) tabs.getComponent(i);
			tab.unselect();
		}
	}

	private void configureTab(JTabbedPane tabs, BrowserTab tab, String name,
			String tooltip) {
		tab.setBaseTitle(name);
		tabs.setTitleAt(tab.getIndex(), tab.getTitle());
		tabs.setToolTipTextAt(tab.getIndex(), tooltip);
		listenTabs(tabs, tab);
	}

	private void listenTabs(final JTabbedPane tabs, final BrowserTab tab) {
		tab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tabs.setTitleAt(tab.getIndex(), tab.getTitle());
				fireActionPerformed(e.getActionCommand());
			}
		});
	}

	/**
	 * Get the {@link BookInfo} to highlight, even if none or more than one are
	 * selected.
	 * <p>
	 * Return a special "all" {@link BookInfo} of the correct type when nothing
	 * is selected.
	 * 
	 * @return the {@link BookInfo} to highlight, cannot be NULL
	 */
	public BookInfo getHighlight() {
		String selected1 = null;
		Component selectedTab = tabs.getSelectedComponent();
		if (selectedTab instanceof BrowserTab) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<String> selectedAll = ((BrowserTab) selectedTab)
					.getSelectedElements();
			if (!selectedAll.isEmpty()) {
				selected1 = selectedAll.get(0);
			}
		}

		BasicLibrary lib = Instance.getInstance().getLibrary();
		if (tabs.getSelectedComponent() == sourceTab) {
			return BookInfo.fromSource(lib, selected1);
		} else if (tabs.getSelectedComponent() == authorTab) {
			return BookInfo.fromAuthor(lib, selected1);
		} else if (tabs.getSelectedComponent() == tagsTab) {
			return BookInfo.fromTag(lib, selected1);
		}

		// ...what?
		return BookInfo.fromSource(lib, selected1);
	}

	/**
	 * The currently selected sources, or an empty list.
	 * 
	 * @return the sources (cannot be NULL)
	 */
	public List<String> getSelectedSources() {
		return sourceTab.getSelectedElements();
	}

	/**
	 * The currently selected authors, or an empty list.
	 * 
	 * @return the sources (cannot be NULL)
	 */
	public List<String> getSelectedAuthors() {
		return authorTab.getSelectedElements();
	}

	/**
	 * The currently selected tags, or an empty list.
	 * 
	 * @return the sources (cannot be NULL)
	 */
	public List<String> getSelectedTags() {
		return tagsTab.getSelectedElements();
	}

	/**
	 * Reload all the data from the 3 tabs (without firing an action).
	 */
	public void reloadData() {
		reloadData(false);
	}

	public void reloadData(final boolean fireActionPerformed) {
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				dataTreeBooks.loadData();
				return null;
			}

			@Override
			protected void done() {
				sourceTab.filter(fireActionPerformed);
				authorTab.filter(fireActionPerformed);
				tagsTab.filter(fireActionPerformed);

			}
		}.execute();
	}
}
