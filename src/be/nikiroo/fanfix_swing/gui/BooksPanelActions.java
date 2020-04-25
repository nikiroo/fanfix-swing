package be.nikiroo.fanfix_swing.gui;

import java.awt.Container;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.Actions;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.book.BookPopup.Informer;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;

public class BooksPanelActions {
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

	static private String trans(StringIdGui id, Object... values) {
		return Instance.getInstance().getTransGui().getString(id, values);
	}
}
