package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
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
import be.nikiroo.fanfix_swing.gui.book.BookPopup.Informer;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.fanfix_swing.gui.viewer.ViewerImages;
import be.nikiroo.fanfix_swing.gui.viewer.ViewerNonImages;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;

public class TouchFrame extends JFrame {
	private JPanel root;

	private List<JComponent> active;
	private JPanel wait;
	private JPanel booksPane;

	public TouchFrame() {
		setLayout(new BorderLayout());

		active = new ArrayList<JComponent>();

		root = new JPanel(new BorderLayout());

		wait = new JPanel();
		wait.add(new JLabel("Waiting..."));

		booksPane = createBooksPane();

		this.add(root, BorderLayout.CENTER);
		showBooks();

		UiHelper.setFrameIcon(this);
		setSize(355, 465);
	}

	private JPanel createBooksPane() {
		final BooksPanel books = new BooksPanel(false, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected BooksPanelActions initActions() {
				return new BooksPanelActions(null, getInformer()) {
					@Override
					public boolean openBook() {
						BookInfo book = getInformer().getUniqueSelected();
						if (book != null) {
							showWait(); // TODO: some details
							open(book, getInformer());
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
					open(book, books.getInformer());
				}
			}
		});

		final BreadCrumbsPanel breadcrumbs = new BreadCrumbsPanel();
		breadcrumbs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				BookInfo book = breadcrumbs.getHighlight();
				books.loadData(book == null ? null : book.getType(),
						book == null ? null : book.getMainInfo());
			}
		});

		JPanel booksPane = new JPanel(new BorderLayout());
		booksPane.add(breadcrumbs, BorderLayout.NORTH);
		booksPane.add(books, BorderLayout.CENTER);

		return booksPane;
	}

	private void open(final BookInfo book, final Informer informer) {
		new SwingWorker<Story, Void>() {
			@Override
			protected Story doInBackground() throws Exception {
				BasicLibrary lib = Instance.getInstance().getLibrary();
				return lib.getStory(book.getMeta().getLuid(), null);
			}

			@Override
			protected void done() {
				try {
					showStory(get());
					informer.setCached(book, true);
					informer.fireElementChanged(book);
				} catch (Exception e) {
					// TODO: i18n
					UiHelper.error(TouchFrame.this, e.getLocalizedMessage(),
							"Cannot open the story", e);
				}
			}
		}.execute();
	}

	private void showStory(Story story) {
		final JComponent[] comps = new JComponent[3];

		final JFrame viewer;
		if (story.getMeta().isImageDocument()) {
			viewer = new ViewerImages(story) {
				private static final long serialVersionUID = 1L;

				@Override
				protected JToolBar createToolbar() {
					// we need it to be created to steal its content
					super.createToolbar();
					return null;
				}

				@Override
				protected void initGui() {
					super.initGui();
					zoombox.setSmall(true);
					comps[0] = scroll;
					comps[1] = navbar;
					comps[2] = zoombox;
				}
			};
		} else {
			viewer = new ViewerNonImages(Instance.getInstance().getLibrary(),
					story) {
				private static final long serialVersionUID = 1L;

				@Override
				protected JToolBar createToolbar() {
					// we need it to be created to steal its content
					super.createToolbar();
					return null;
				}

				@Override
				protected JPanel createDescPane() {
					return null;
				}

				@Override
				protected void initGui() {
					super.initGui();
					title.setFont(title.getFont()
							.deriveFont(title.getFont().getSize() * 0.75f));
					comps[0] = scroll;
					comps[1] = navbar;
					comps[2] = title;
				}
			};
		}

		removeShows();

		JComponent scroll = comps[0];

		JPanel navbar = new JPanel();
		navbar.add(comps[1]);

		JButton exit = new JButton(IconGenerator.get(Icon.back, Size.x32));
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeShows();
				viewer.dispose();
				showBooks();
			}
		});

		JPanel zoombox = new JPanel();
		zoombox.add(exit);
		zoombox.add(comps[2]);

		active.add(scroll);
		active.add(navbar);
		active.add(zoombox);

		TouchFrame.this.add(navbar, BorderLayout.NORTH);
		TouchFrame.this.add(zoombox, BorderLayout.SOUTH);
		root.add(scroll);

		revalidate();
		repaint();
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
		active.add(booksPane);
		root.add(booksPane);
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
