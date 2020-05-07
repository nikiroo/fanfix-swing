package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.fanfix_swing.gui.viewer.ViewerImages;
import be.nikiroo.fanfix_swing.gui.viewer.ViewerNonImages;

public class TouchFrame extends JFrame {
	private JPanel root;

	private List<JComponent> active;
	private JPanel wait;
	private BooksPanel books;

	public TouchFrame() {
		setLayout(new BorderLayout());

		active = new ArrayList<JComponent>();

		root = new JPanel(new BorderLayout());

		wait = new JPanel();
		wait.add(new JLabel("Waiting..."));

		books = new BooksPanel(false, true) {
			@Override
			protected BooksPanelActions initActions() {
				return new BooksPanelActions(books, getInformer()) {
					@Override
					public boolean openBook() {
						BookInfo book = getInformer().getUniqueSelected();
						if (book != null) {
							showWait(); // TODO: some details
							open(book);
							return true;
						}

						return false;
					}
				};
			}
		};

		books.setTooltip(false);
		books.loadData(null, null, null);

		// We hijack the popup to generate an action on right click/long-press.
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
					showWait(); // TODO: some details
					open(book);
				}
			}
		});

		this.add(root, BorderLayout.CENTER);
		showBooks();

		UiHelper.setFrameIcon(this);
		setSize(355, 465);
	}

	private void open(final BookInfo book) {
		new SwingWorker<Story, Void>() {
			@Override
			protected Story doInBackground() throws Exception {
				BasicLibrary lib = Instance.getInstance().getLibrary();
				return lib.getStory(book.getMeta().getLuid(), null);
			}

			@Override
			protected void done() {
				try {
					open(get());
				} catch (Exception e) {
					// TODO: i18n
					UiHelper.error(TouchFrame.this, e.getLocalizedMessage(),
							"Cannot open the story", e);
				}
			}
		}.execute();
	}

	private void open(Story story) {
		final JComponent[] comps = new JComponent[2];

		// Integrate it with showViewer or something
		if (story.getMeta().isImageDocument()) {
			ViewerImages viewer = new ViewerImages(story) {
				@Override
				protected JToolBar createToolbar() {
					comps[0] = super.createToolbar();
					return null;
				}

				@Override
				protected void initGui() {
					super.initGui();
					comps[1] = scroll;
				}
			};

			removeShows();

			// TODO: toolbar not so nice + add EXIT button
			active.add(comps[0]);
			active.add(comps[1]);
			TouchFrame.this.add(comps[0], BorderLayout.NORTH);
			root.add(comps[1]);

			revalidate();
			repaint();

			// TODO: dispose viewer when changed
		} else {
			ViewerNonImages viewer = new ViewerNonImages(
					Instance.getInstance().getLibrary(), story);
			viewer.setVisible(true);
		}
	}

	private void removeShows() {
		for (JComponent comp : active) {
			this.remove(comp);
			root.remove(comp);
		}
		active.clear();
	}

	private void showBooks() {
		removeShows();
		active.add(books);
		root.add(books);
		revalidate();
		repaint();
	}

	private void showWait() {
		removeShows();
		active.add(wait);
		root.add(wait);
		revalidate();
		repaint();
	}
}
