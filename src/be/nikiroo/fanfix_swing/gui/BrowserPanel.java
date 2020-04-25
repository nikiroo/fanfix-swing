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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.browser.BasicTab;
import be.nikiroo.fanfix_swing.gui.utils.DataTreeAuthors;
import be.nikiroo.fanfix_swing.gui.utils.DataTreeSources;
import be.nikiroo.fanfix_swing.gui.utils.DataTreeTag;
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

	private JTabbedPane tabs;
	private BasicTab sourceTab;
	private BasicTab authorTab;
	private BasicTab tagsTab;

	private boolean keepSelection;

	/**
	 * Create a nesw {@link BrowserPanel}.
	 */
	public BrowserPanel() {
		this.setPreferredSize(new Dimension(200, 800));

		this.setLayout(new BorderLayout());
		tabs = new JTabbedPane();

		int index = 0;
		tabs.add(sourceTab = new BasicTab(new DataTreeSources(false), index++,
				SOURCE_SELECTION));
		tabs.add(authorTab = new BasicTab(new DataTreeAuthors(true), index++,
				AUTHOR_SELECTION));
		tabs.add(tagsTab = new BasicTab(new DataTreeTag(true), index++,
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
	}

	private void unselect() {
		for (int i = 0; i < tabs.getTabCount(); i++) {
			if (i == tabs.getSelectedIndex())
				continue;

			BasicTab tab = (BasicTab) tabs.getComponent(i);
			tab.unselect();
		}
	}

	private void configureTab(JTabbedPane tabs, BasicTab tab, String name,
			String tooltip) {
		tab.setBaseTitle(name);
		tabs.setTitleAt(tab.getIndex(), tab.getTitle());
		tabs.setToolTipTextAt(tab.getIndex(), tooltip);
		listenTabs(tabs, tab);
	}

	private void listenTabs(final JTabbedPane tabs, final BasicTab tab) {
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
		if (selectedTab instanceof BasicTab) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<String> selectedAll = ((BasicTab) selectedTab)
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
		sourceTab.reloadData();
		authorTab.reloadData();
		tagsTab.reloadData();
	}
}
