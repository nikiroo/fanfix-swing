package be.nikiroo.fanfix_swing.gui.book;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.MetaResultList;
import be.nikiroo.fanfix.library.BasicLibrary.Status;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix_swing.gui.BooksPanelActions;
import be.nikiroo.fanfix_swing.gui.PropertiesFrame;
import be.nikiroo.fanfix_swing.gui.utils.CoverImager;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.ui.ConfigEditor;

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

		void removeElement(BookInfo book);
	}

	/**
	 * The different modification actions you can use on {@link Story} items.
	 * 
	 * @author niki
	 */
	private enum ChangeAction {
		/** Change the source/type, that is, move it to another source. */
		SOURCE,
		/** Change its name. */
		TITLE,
		/** Change its author. */
		AUTHOR
	}

	private BasicLibrary lib;
	private Informer informer;
	private JMenuItem moveTo; // to update later
	private JMenuItem setAuthor; // to update later

	public BookPopup(BasicLibrary lib, Informer informer) {
		this.lib = lib;
		this.informer = informer;

		Status status = lib.getStatus();
		add(createMenuItemOpenBook());
		addSeparator();
		add(createMenuItemExport());
		if (status.isWritable()) {
			moveTo = add(createMenuItemMoveTo(null));
			add(createMenuItemSetCoverForSource());
			add(createMenuItemSetCoverForAuthor());
		}
		add(createMenuItemDownloadToCache());
		add(createMenuItemClearCache());
		if (status.isWritable()) {
			add(createMenuItemRedownload());
			addSeparator();
			add(createMenuItemRename());
			setAuthor = add(createMenuItemSetAuthor(null));
			addSeparator();
			add(createMenuItemDelete());
		}
		addSeparator();
		add(createMenuItemProperties());

		reloadMoveToSetAuthor();
	}

	public void reloadData() {
		reloadMoveToSetAuthor();
	}

	private void reloadMoveToSetAuthor() {
		new SwingWorker<MetaResultList, Void>() {
			@Override
			protected MetaResultList doInBackground() throws Exception {
				return lib.getList();
			}

			@Override
			protected void done() {
				try {
					MetaResultList list = get();

					if (moveTo != null) {
						remove(moveTo);
					}
					moveTo = add(
							createMenuItemMoveTo(list.getSourcesGrouped()));

					if (setAuthor != null) {
						remove(setAuthor);
					}
					setAuthor = add(
							createMenuItemSetAuthor(list.getAuthorsGrouped()));

				} catch (Exception e) {
					UiHelper.error(BookPopup.this.getParent(),
							e.getLocalizedMessage(), "IOException", e);
				}
			}
		}.execute();
	}

	/**
	 * Create the Fanfix Configuration menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemConfig() {
		final String title = trans(StringIdGui.TITLE_CONFIG);
		JMenuItem item = new JMenuItem(title);
		item.setMnemonic(KeyEvent.VK_F);

		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConfigEditor<Config> ed = new ConfigEditor<Config>(Config.class,
						Instance.getInstance().getConfig(),
						trans(StringIdGui.SUBTITLE_CONFIG));
				JFrame frame = new JFrame(title);
				frame.add(ed);
				frame.setSize(850, 600);
				frame.setVisible(true);
			}
		});

		return item;
	}

	/**
	 * Create the UI Configuration menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemUiConfig() {
		final String title = trans(StringIdGui.TITLE_CONFIG_UI);
		JMenuItem item = new JMenuItem(title);
		item.setMnemonic(KeyEvent.VK_U);

		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConfigEditor<UiConfig> ed = new ConfigEditor<UiConfig>(
						UiConfig.class, Instance.getInstance().getUiConfig(),
						trans(StringIdGui.SUBTITLE_CONFIG_UI));
				JFrame frame = new JFrame(title);
				frame.add(ed);
				frame.setSize(800, 600);
				frame.setVisible(true);
			}
		});

		return item;
	}

	/**
	 * Create the export menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemExport() {

		// TODO: allow dir for multiple selection?

		final JFileChooser fc = new JFileChooser();
		fc.setAcceptAllFileFilterUsed(false);

		// Add the "ALL" filters first, then the others
		final Map<FileFilter, OutputType> otherFilters = new HashMap<FileFilter, OutputType>();
		for (OutputType type : OutputType.values()) {
			String ext = type.getDefaultExtension(false);
			String desc = type.getDesc(false);

			if (ext == null || ext.isEmpty()) {
				fc.addChoosableFileFilter(createAllFilter(desc));
			} else {
				otherFilters.put(new FileNameExtensionFilter(desc, ext), type);
			}
		}

		for (Entry<FileFilter, OutputType> entry : otherFilters.entrySet()) {
			fc.addChoosableFileFilter(entry.getKey());
		}
		//

		JMenuItem export = new JMenuItem(trans(StringIdGui.MENU_FILE_EXPORT),
				KeyEvent.VK_S);
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final BookInfo book = informer.getUniqueSelected();
				if (book != null) {
					fc.showDialog(BookPopup.this.getParent(),
							trans(StringIdGui.TITLE_SAVE));
					if (fc.getSelectedFile() != null) {
						final OutputType type = otherFilters
								.get(fc.getFileFilter());
						final String path = fc.getSelectedFile()
								.getAbsolutePath()
								+ type.getDefaultExtension(false);
						final Progress pg = new Progress();

						new SwingWorker<Void, Void>() {
							@Override
							protected Void doInBackground() throws Exception {
								lib.export(book.getMeta().getLuid(), type, path,
										pg);
								return null;
							}

							@Override
							protected void done() {
								try {
									get();
								} catch (Exception e) {
									UiHelper.error(BookPopup.this.getParent(),
											e.getLocalizedMessage(),
											"IOException", e);
								}
							}
						}.execute();
					}
				}
			}
		});

		return export;
	}

	/**
	 * Create a {@link FileFilter} that accepts all files and return the given
	 * description.
	 * 
	 * @param desc
	 *            the description
	 * 
	 * @return the filter
	 */
	private FileFilter createAllFilter(final String desc) {
		return new FileFilter() {
			@Override
			public String getDescription() {
				return desc;
			}

			@Override
			public boolean accept(File f) {
				return true;
			}
		};
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
				final List<BookInfo> selected = informer.getSelected();
				if (!selected.isEmpty()) {
					new SwingWorker<Void, Void>() {
						@Override
						protected Void doInBackground() throws Exception {
							for (BookInfo book : selected) {
								lib.clearFromCache(book.getMeta().getLuid());
								CoverImager.clearIcon(book);
							}
							return null;
						}

						@Override
						protected void done() {
							try {
								get();
								for (BookInfo book : selected) {
									informer.setCached(book, false);
								}
							} catch (Exception e) {
								UiHelper.error(BookPopup.this.getParent(),
										e.getLocalizedMessage(), "IOException",
										e);
							}
						}
					}.execute();
				}
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
				final List<BookInfo> selected = informer.getSelected();
				if (!selected.isEmpty()) {
					String changeTo = type;
					if (type == null) {
						String init = "";

						if (selected.size() == 1) {
							MetaData meta = selected.get(0).getMeta();
							if (what == ChangeAction.SOURCE) {
								init = meta.getSource();
							} else if (what == ChangeAction.TITLE) {
								init = meta.getTitle();
							} else if (what == ChangeAction.AUTHOR) {
								init = meta.getAuthor();
							}
						}

						Object rep = JOptionPane.showInputDialog(
								BookPopup.this.getParent(),
								trans(StringIdGui.SUBTITLE_MOVE_TO),
								trans(StringIdGui.TITLE_MOVE_TO),
								JOptionPane.QUESTION_MESSAGE, null, null, init);

						if (rep == null) {
							return;
						}

						changeTo = rep.toString();
					}

					final String fChangeTo = changeTo;
					new SwingWorker<Void, Void>() {
						@Override
						protected Void doInBackground() throws Exception {
							for (BookInfo book : selected) {
								String luid = book.getMeta().getLuid();
								if (what == ChangeAction.SOURCE) {
									lib.changeSource(luid, fChangeTo, null);
								} else if (what == ChangeAction.TITLE) {
									lib.changeTitle(luid, fChangeTo, null);
								} else if (what == ChangeAction.AUTHOR) {
									lib.changeAuthor(luid, fChangeTo, null);
								}
							}

							return null;
						}

						@Override
						protected void done() {
							try {
								// this can create new sources/authors, so a
								// simple fireElementChanged is not
								// enough, we need to clear the whole cache (for
								// BrowserPanel for instance)
								//
								// Note:
								// This will also reresh the authors/sources
								// lists here
								if (what != ChangeAction.TITLE) {
									informer.invalidateCache();
								}

								// But we ALSO fire those, because they appear
								// before the whole refresh...
								for (BookInfo book : selected) {
									informer.fireElementChanged(book);
								}

								// Even if problems occurred, still invalidate
								// the cache above
								get();
							} catch (Exception e) {
								UiHelper.error(BookPopup.this.getParent(),
										e.getLocalizedMessage(), "IOException",
										e);
							}
						}
					}.execute();
				}
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
				final List<BookInfo> selected = informer.getSelected();

				new SwingWorker<Void, BookInfo>() {
					@Override
					protected Void doInBackground() throws Exception {
						String luid = null;
						for (BookInfo book : selected) {
							switch (book.getType()) {
							case STORY:
								luid = book.getMeta().getLuid();
								break;
							case SOURCE:
								for (MetaData meta : lib.getList().filter(
										book.getMainInfo(), null, null)) {
									luid = meta.getLuid();
								}
								break;
							case AUTHOR:
								for (MetaData meta : lib.getList().filter(null,
										book.getMainInfo(), null)) {
									luid = meta.getLuid();
								}
								break;
							case TAG:
								for (MetaData meta : lib.getList().filter(null,
										null, book.getMainInfo())) {
									luid = meta.getLuid();
								}
								break;
							}

							if (luid != null) {
								lib.getFile(luid, null);
								publish(book);
							}
						}

						return null;
					}

					@Override
					protected void process(java.util.List<BookInfo> chunks) {
						for (BookInfo book : chunks) {
							informer.setCached(book, true);
						}
					}

					@Override
					protected void done() {
						try {
							get();
						} catch (Exception e) {
							UiHelper.error(BookPopup.this.getParent(),
									e.getLocalizedMessage(), "IOException", e);
						}
					}
				}.execute();
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
				BookInfo selected = informer.getUniqueSelected();
				if (selected != null) {
					new PropertiesFrame(lib, selected.getMeta())
							.setVisible(true);
				}
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
				// final GuiReaderBook selectedBook =
				// mainPanel.getSelectedBook();
				// if (selectedBook != null) {
				// BasicLibrary lib = lib;
				// String luid = selectedBook.getInfo().getMeta().getLuid();
				// String source = selectedBook.getInfo().getMeta().getSource();
				//
				// try {
				// lib.setSourceCover(source, luid);
				// } catch (IOException e) {
				// error(e.getLocalizedMessage(), "IOException", e);
				// }
				//
				// GuiReaderBookInfo sourceInfo =
				// GuiReaderBookInfo.fromSource(lib, source);
				// GuiReaderCoverImager.clearIcon(sourceInfo);
				// }
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
				// final GuiReaderBook selectedBook =
				// mainPanel.getSelectedBook();
				// if (selectedBook != null) {
				// String luid = selectedBook.getInfo().getMeta().getLuid();
				// String author = selectedBook.getInfo().getMeta().getAuthor();
				//
				// try {
				// lib.setAuthorCover(author, luid);
				// } catch (IOException e) {
				// error(e.getLocalizedMessage(), "IOException", e);
				// }
				//
				// GuiReaderBookInfo authorInfo =
				// GuiReaderBookInfo.fromAuthor(lib, author);
				// GuiReaderCoverImager.clearIcon(authorInfo);
				// }
			}
		});

		return open;
	}

	static private String trans(StringIdGui id, Object... values) {
		return Instance.getInstance().getTransGui().getString(id, values);
	}
}
