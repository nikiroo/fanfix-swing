package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.Actions;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.fanfix_swing.gui.utils.WaitingDialogMeta;
import be.nikiroo.fanfix_swing.gui.viewer.ViewerImages;
import be.nikiroo.fanfix_swing.gui.viewer.ViewerNonImages;
import be.nikiroo.utils.ui.WaitingDialog;

public class TouchFrame extends JFrame {
	private JPanel root;

	private JPanel wait;
	private BooksPanel books;

	public TouchFrame() {
		root = new JPanel(new BorderLayout());

		wait = new JPanel();
		wait.add(new JLabel("Waiting..."));

		books = new BooksPanel(false, true);
		books.setTooltip(false);
		books.loadData(null, null, null);
		// We hijack the popup to generate an action on long-press.
		books.setPopup(new JPopupMenu() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isShowing() {
				return false;
			}

			@Override
			public void show(Component invoker, int x, int y) {
				final BookInfo book = books.getInformer().getUniqueSelected();
				if (book != null && book.getMeta() != null) {
					final BasicLibrary lib = Instance.getInstance()
							.getLibrary();

					showWait(); // TODO: some details

					new SwingWorker<File, Void>() {
						private Story story;

						@Override
						protected File doInBackground() throws Exception {
							story = lib.getStory(book.getMeta().getLuid(),
									null);
							return null;
						}

						@Override
						protected void done() {
							try {
								get();
								Actions.openInternal(story);
							} catch (Exception e) {
								// TODO: i18n
								UiHelper.error(TouchFrame.this,
										e.getLocalizedMessage(),
										"Cannot open the story", e);
							}

							// Integrate it with showViewer or something
							if (story.getMeta().isImageDocument()) {
								ViewerImages viewer = new ViewerImages(story);
								viewer.setVisible(true);
							} else {
								ViewerNonImages viewer = new ViewerNonImages(
										Instance.getInstance().getLibrary(),
										story);
								viewer.setVisible(true);
							}

						}
					}.execute();
				}
			}
		});

		this.add(root);
		showBooks();
		setSize(355, 465);
	}

	private void removeShows() {
		root.remove(wait);
		root.remove(books);
	}

	private void showBooks() {
		removeShows();
		root.add(books);
		revalidate();
	}

	private void showWait() {
		removeShows();
		root.add(wait);
		revalidate();
	}
}
