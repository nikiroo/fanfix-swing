package be.nikiroo.fanfix_swing.gui;

import java.awt.Container;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix_swing.Actions;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.book.BookPopup.Informer;
import be.nikiroo.fanfix_swing.gui.utils.CoverImager;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.utils.Progress;

public class BooksPanelActions {
	/**
	 * The different modification actions you can use on {@link Story} items.
	 * 
	 * @author niki
	 */
	public enum ChangeAction {
		/** Change the source/type, that is, move it to another source. */
		SOURCE,
		/** Change its name. */
		TITLE,
		/** Change its author. */
		AUTHOR
	}

	private Container owner;
	private Informer informer;

	public BooksPanelActions(Container owner, Informer informer) {
		this.owner = owner;
		this.informer = informer;
	}

	/**
	 * Open the currently selected book if it is the only one selected.
	 * 
	 * @return TRUE if a book was opened, FALSE if not (no book selected, or
	 *         more than one book selected)
	 */
	public boolean openBook() {
		BasicLibrary lib = Instance.getInstance().getLibrary();
		final BookInfo book = informer.getUniqueSelected();

		if (book != null) {
			Actions.openBook(lib, book.getMeta(), owner, new Runnable() {
				@Override
				public void run() {
					book.setCached(true);
					informer.fireElementChanged(book);
				}
			});

			return true;
		}

		return false;
	}

	public void deleteBooks() {
		final List<BookInfo> selected = informer.getSelected();

		// TODO: i18n is geared towards ONE item
		if (selected.size() > 0) {
			String one;
			String two;
			if (selected.size() == 1) {
				MetaData meta = selected.get(0).getMeta();
				one = meta.getLuid();
				two = meta.getTitle();
			} else {
				one = "";
				two = selected.size() + " stories";
			}

			int rep = JOptionPane.showConfirmDialog(owner,
					trans(StringIdGui.SUBTITLE_DELETE, one, two),
					trans(StringIdGui.TITLE_DELETE),
					JOptionPane.OK_CANCEL_OPTION);

			if (rep == JOptionPane.OK_OPTION) {
				new SwingWorker<Void, BookInfo>() {

					@Override
					public Void doInBackground() throws Exception {
						BasicLibrary lib = Instance.getInstance().getLibrary();

						for (BookInfo info : selected) {
							lib.delete(info.getMeta().getLuid());
							publish(info);
						}

						return null;
					}

					@Override
					protected void process(List<BookInfo> chunks) {
						for (BookInfo info : chunks) {
							informer.removeElement(info);
						}
					}

					@Override
					protected void done() {
						try {
							get();
						} catch (Exception e) {
							UiHelper.error(owner, e.getLocalizedMessage(),
									"IOException", e);
						}
					}
				}.execute();
			}
		}
	}

	public void redownload() {
		MainFrame.getImporter().setVisible(true);
		final List<BookInfo> selected = informer.getSelected();
		for (BookInfo book : selected) {
			MainFrame.getImporter().imprt(owner, book.getMeta().getUrl());
		}
	}

	public void export() {
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

		final BookInfo book = informer.getUniqueSelected();
		if (book != null) {
			fc.showDialog(owner, trans(StringIdGui.TITLE_SAVE));
			if (fc.getSelectedFile() != null) {
				final OutputType type = otherFilters.get(fc.getFileFilter());
				final String path = fc.getSelectedFile().getAbsolutePath()
						+ type.getDefaultExtension(false);
				final Progress pg = new Progress();

				new SwingWorker<Void, Void>() {
					@Override
					protected Void doInBackground() throws Exception {
						BasicLibrary lib = Instance.getInstance().getLibrary();
						lib.export(book.getMeta().getLuid(), type, path, pg);
						return null;
					}

					@Override
					protected void done() {
						try {
							get();
						} catch (Exception e) {
							UiHelper.error(owner, e.getLocalizedMessage(),
									"IOException", e);
						}
					}
				}.execute();
			}
		}
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

	public void clearCache() {
		final List<BookInfo> selected = informer.getSelected();
		if (!selected.isEmpty()) {
			new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					BasicLibrary lib = Instance.getInstance().getLibrary();
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
						UiHelper.error(owner, e.getLocalizedMessage(),
								"IOException", e);
					}
				}
			}.execute();
		}
	}

	public void moveAction(final ChangeAction what, final String type) {
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

				Object rep = JOptionPane.showInputDialog(owner,
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
					BasicLibrary lib = Instance.getInstance().getLibrary();
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
						UiHelper.error(owner, e.getLocalizedMessage(),
								"IOException", e);
					}
				}
			}.execute();
		}
	}

	public void prefetch() {
		final List<BookInfo> selected = informer.getSelected();

		new SwingWorker<Void, BookInfo>() {
			@Override
			protected Void doInBackground() throws Exception {
				BasicLibrary lib = Instance.getInstance().getLibrary();
				String luid = null;
				for (BookInfo book : selected) {
					switch (book.getType()) {
					case STORY:
						luid = book.getMeta().getLuid();
						break;
					case SOURCE:
						for (MetaData meta : lib.getList()
								.filter(book.getMainInfo(), null, null)) {
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
						for (MetaData meta : lib.getList().filter(null, null,
								book.getMainInfo())) {
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
					UiHelper.error(owner, e.getLocalizedMessage(),
							"IOException", e);
				}
			}
		}.execute();
	}

	public void properties() {
		BasicLibrary lib = Instance.getInstance().getLibrary();
		BookInfo selected = informer.getUniqueSelected();
		if (selected != null) {
			new PropertiesDialog(lib, selected.getMeta(), false)
					.setVisible(true);
		}
	}

	public void setCoverFor(final ChangeAction what) {
		final BookInfo book = informer.getUniqueSelected();
		if (book != null) {
			final BasicLibrary lib = Instance.getInstance().getLibrary();
			final String luid = book.getMeta().getLuid();

			new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					switch (what) {
					case SOURCE:
						lib.setSourceCover(book.getMeta().getSource(), luid);
						break;
					case AUTHOR:
						lib.setAuthorCover(book.getMeta().getAuthor(), luid);
						break;
					case TITLE:
						throw new IllegalArgumentException(
								"Cannot change TITLE cover");
					}

					return null;
				}

				@Override
				protected void done() {
					try {
						get();
						// TODO: informer.what? (notify cover for source/author
						// changed -> browser)
						informer.invalidateCache();
					} catch (Exception e) {
						UiHelper.error(owner, e.getLocalizedMessage(),
								"IOException", e);
					}
				}
			}.execute();
		}
	}

	static private String trans(StringIdGui id, Object... values) {
		return Instance.getInstance().getTransGui().getString(id, values);
	}
}
