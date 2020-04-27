package be.nikiroo.fanfix_swing.gui.book;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.BasicLibrary.Status;
import be.nikiroo.fanfix.library.MetaResultList;
import be.nikiroo.fanfix_swing.gui.BooksPanelActions;
import be.nikiroo.fanfix_swing.gui.BooksPanelActions.ChangeAction;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;

public class BookPopup extends JPopupMenu {
	public abstract interface Informer {

		public BooksPanelActions getActions();

		// not null
		public List<BookInfo> getSelected();

		// visual only!
		public void setCached(BookInfo book, boolean cached);

		public BookInfo getUniqueSelected();

		public void fireElementChanged(BookInfo book);

		public void invalidateCache();

		public void removeElement(BookInfo book);
	}

	private BasicLibrary lib;
	private Informer informer;
	private Map<String, List<String>> groupedSources;
	private Map<String, List<String>> groupedAuthors;

	public BookPopup(BasicLibrary lib, Informer informer) {
		this.lib = lib;
		this.informer = informer;

		initMenus();
		reloadData();
	}

	private void initMenus() {
		removeAll();

		Status status = lib.getStatus();
		add(createMenuItemOpenBook());
		addSeparator();
		add(createMenuItemExport());
		if (status.isWritable()) {
			add(createMenuItemMoveTo(groupedSources));
			add(createMenuItemSetCoverForSource());
			add(createMenuItemSetCoverForAuthor());
		}
		add(createMenuItemDownloadToCache());
		add(createMenuItemClearCache());
		if (status.isWritable()) {
			add(createMenuItemRedownload());
			addSeparator();
			add(createMenuItemRename());
			add(createMenuItemSetAuthor(groupedAuthors));
			addSeparator();
			add(createMenuItemDelete());
		}
		addSeparator();
		add(createMenuItemProperties());

		revalidate();
	}

	public void reloadData() {
		new SwingWorker<MetaResultList, Void>() {
			@Override
			protected MetaResultList doInBackground() throws Exception {
				return lib.getList();
			}

			@Override
			protected void done() {
				try {
					MetaResultList list = get();
					groupedSources = list.getSourcesGrouped();
					groupedAuthors = list.getAuthorsGrouped();
					initMenus();
				} catch (Exception e) {
					UiHelper.error(BookPopup.this.getParent(),
							e.getLocalizedMessage(), "IOException", e);
				}
			}
		}.execute();
	}

	/**
	 * Create the export menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemExport() {
		JMenuItem export = new JMenuItem(trans(StringIdGui.MENU_FILE_EXPORT),
				KeyEvent.VK_S);
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				informer.getActions().export();
			}
		});

		return export;
	}

	/**
	 * Create the refresh (delete cache) menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemClearCache() {
		JMenuItem refresh = new JMenuItem(
				trans(StringIdGui.MENU_EDIT_CLEAR_CACHE), KeyEvent.VK_C);
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				informer.getActions().clearCache();
			}
		});

		return refresh;
	}

	/**
	 * Create the "move to" menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemMoveTo(
			Map<String, List<String>> groupedSources) {
		if (groupedSources == null) {
			groupedSources = new HashMap<String, List<String>>();
		}

		JMenu changeTo = new JMenu(trans(StringIdGui.MENU_FILE_MOVE_TO));
		changeTo.setMnemonic(KeyEvent.VK_M);

		JMenuItem item = new JMenuItem(
				trans(StringIdGui.MENU_FILE_MOVE_TO_NEW_TYPE));
		item.addActionListener(createMoveAction(ChangeAction.SOURCE, null));
		changeTo.add(item);
		changeTo.addSeparator();

		for (final String type : groupedSources.keySet()) {
			List<String> list = groupedSources.get(type);
			if (list.size() == 1 && list.get(0).isEmpty()) {
				item = new JMenuItem(type);
				item.addActionListener(
						createMoveAction(ChangeAction.SOURCE, type));
				changeTo.add(item);
			} else {
				JMenu dir = new JMenu(type);
				for (String sub : list) {
					// " " instead of "" for the visual height
					String itemName = sub.isEmpty() ? " " : sub;
					String actualType = type;
					if (!sub.isEmpty()) {
						actualType += "/" + sub;
					}

					item = new JMenuItem(itemName);
					item.addActionListener(
							createMoveAction(ChangeAction.SOURCE, actualType));
					dir.add(item);
				}
				changeTo.add(dir);
			}
		}

		return changeTo;
	}

	/**
	 * Create the "set author" menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemSetAuthor(
			Map<String, List<String>> groupedAuthors) {
		if (groupedAuthors == null) {
			groupedAuthors = new HashMap<String, List<String>>();
		}

		JMenu changeTo = new JMenu(trans(StringIdGui.MENU_FILE_SET_AUTHOR));
		changeTo.setMnemonic(KeyEvent.VK_A);

		// New author
		JMenuItem newItem = new JMenuItem(
				trans(StringIdGui.MENU_FILE_MOVE_TO_NEW_AUTHOR));
		changeTo.add(newItem);
		changeTo.addSeparator();
		newItem.addActionListener(createMoveAction(ChangeAction.AUTHOR, null));

		// Existing authors
		if (groupedAuthors.size() > 1) {
			for (String key : groupedAuthors.keySet()) {
				JMenu group = new JMenu(key);
				for (String value : groupedAuthors.get(key)) {
					JMenuItem item = new JMenuItem(value.isEmpty()
							? trans(StringIdGui.MENU_AUTHORS_UNKNOWN)
							: value);
					item.addActionListener(
							createMoveAction(ChangeAction.AUTHOR, value));
					group.add(item);
				}
				changeTo.add(group);
			}
		} else if (groupedAuthors.size() == 1) {
			for (String value : groupedAuthors.values().iterator().next()) {
				JMenuItem item = new JMenuItem(value.isEmpty()
						? trans(StringIdGui.MENU_AUTHORS_UNKNOWN)
						: value);
				item.addActionListener(
						createMoveAction(ChangeAction.AUTHOR, value));
				changeTo.add(item);
			}
		}

		return changeTo;
	}

	/**
	 * Create the "rename" menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemRename() {
		JMenuItem changeTo = new JMenuItem(trans(StringIdGui.MENU_FILE_RENAME));
		changeTo.setMnemonic(KeyEvent.VK_R);
		changeTo.addActionListener(createMoveAction(ChangeAction.TITLE, null));
		return changeTo;
	}

	private ActionListener createMoveAction(final ChangeAction what,
			final String type) {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				informer.getActions().moveAction(what, type);
			}
		};
	}

	/**
	 * Create the re-download (then delete original) menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemRedownload() {
		JMenuItem refresh = new JMenuItem(
				trans(StringIdGui.MENU_EDIT_REDOWNLOAD), KeyEvent.VK_R);
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				informer.getActions().redownload();
			}
		});

		return refresh;
	}

	/**
	 * Create the download to cache menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemDownloadToCache() {
		JMenuItem refresh = new JMenuItem(
				trans(StringIdGui.MENU_EDIT_DOWNLOAD_TO_CACHE), KeyEvent.VK_T);
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				informer.getActions().prefetch();
			}
		});

		return refresh;
	}

	/**
	 * Create the delete menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemDelete() {
		JMenuItem delete = new JMenuItem(trans(StringIdGui.MENU_EDIT_DELETE),
				KeyEvent.VK_D);
		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				informer.getActions().deleteBooks();
			}
		});

		return delete;
	}

	/**
	 * Create the properties menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemProperties() {
		JMenuItem delete = new JMenuItem(
				trans(StringIdGui.MENU_FILE_PROPERTIES), KeyEvent.VK_P);
		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				informer.getActions().properties();
			}
		});

		return delete;
	}

	/**
	 * Create the open menu item for a book, a source/type or an author.
	 * 
	 * @return the item
	 */
	public JMenuItem createMenuItemOpenBook() {
		JMenuItem open = new JMenuItem(trans(StringIdGui.MENU_FILE_OPEN),
				KeyEvent.VK_O);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				informer.getActions().openBook();
			}
		});

		return open;
	}

	/**
	 * Create the SetCover menu item for a book to change the linked source
	 * cover.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemSetCoverForSource() {
		JMenuItem open = new JMenuItem(
				trans(StringIdGui.MENU_EDIT_SET_COVER_FOR_SOURCE),
				KeyEvent.VK_C);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				informer.getActions().setCoverFor(ChangeAction.SOURCE);
			}
		});

		return open;
	}

	/**
	 * Create the SetCover menu item for a book to change the linked source
	 * cover.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemSetCoverForAuthor() {
		JMenuItem open = new JMenuItem(
				trans(StringIdGui.MENU_EDIT_SET_COVER_FOR_AUTHOR),
				KeyEvent.VK_A);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				informer.getActions().setCoverFor(ChangeAction.AUTHOR);
			}
		});

		return open;
	}

	static private String trans(StringIdGui id, Object... values) {
		return Instance.getInstance().getTransGui().getString(id, values);
	}
}
