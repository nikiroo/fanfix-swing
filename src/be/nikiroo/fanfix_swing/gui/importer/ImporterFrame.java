package be.nikiroo.fanfix_swing.gui.importer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix_swing.Actions;
import be.nikiroo.fanfix_swing.gui.SearchBar;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.ui.ListModel;
import be.nikiroo.utils.ui.ListModel.Predicate;
import be.nikiroo.utils.ui.compat.JList6;
import be.nikiroo.utils.ui.ListenerItem;
import be.nikiroo.utils.ui.ListenerPanel;
import be.nikiroo.utils.ui.UIUtils;

/**
 * A window showing the items currently being processed (downloaded,
 * converted...).
 * <p>
 * You can keep it in memory and let the user close it, it will unhide itself on
 * import.
 * 
 * @author niki
 */
public class ImporterFrame extends JFrame implements ListenerItem {
	private static final long serialVersionUID = 1L;

	/**
	 * The {@link ActionEvent} you receive from
	 * {@link ImporterFrame#addActionListener(ActionListener)} (see
	 * {@link ActionEvent#getActionCommand()}) when an item is imported
	 * successfully.
	 */
	static public final String IMPORTED_SUCCESS = "imported_success";

	/**
	 * The {@link ActionEvent} you receive from
	 * {@link ImporterFrame#addActionListener(ActionListener)} (see
	 * {@link ActionEvent#getActionCommand()}) when an item failed to be
	 * imported.
	 */
	static public final String IMPORTED_FAIL = "imported_fail";

	private ListenerPanel root;
	private ListModel<ImporterItem> data;
	private String filter = "";

	/**
	 * Create a new {@link ImporterFrame}.
	 * <p>
	 * You can keep it in memory and let the user close it, it will unhode
	 * itself on import.
	 */
	public ImporterFrame() {
		root = new ListenerPanel();
		setLayout(new BorderLayout());
		root.setLayout(new BorderLayout());
		this.add(UIUtils.scroll(root, false));

		JList6<ImporterItem> list = new JList6<ImporterItem>();
		data = new ListModel<ImporterItem>(list);

		list.setCellRenderer(ListModel.generateRenderer(data));
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.setVisibleRowCount(5);

		root.add(list, BorderLayout.CENTER);

		JPanel top = new JPanel();
		top.setLayout(new BorderLayout());

		final SearchBar search = new SearchBar();
		top.add(search, BorderLayout.CENTER);

		search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				filter = search.getText();
				filter();
			}
		});

		JButton clear = new JButton("Clear");
		top.add(clear, BorderLayout.EAST);
		clear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean changed = data
						.removeItemIf(new Predicate<ImporterItem>() {
							@Override
							public boolean test(ImporterItem item) {
								return item.isDone(false);
							}
						});

				if (changed) {
					filter();
				}
			}
		});

		root.add(top, BorderLayout.NORTH);

		setSize(800, 600);
		UiHelper.setFrameIcon(this);
	}

	/**
	 * Ask for and import an {@link URL} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * <p>
	 * Will fire {@link ImporterFrame#IMPORTED_SUCCESS} or
	 * {@link ImporterFrame#IMPORTED_SUCCESS} when done.
	 * 
	 * @param parent
	 *            a container we can use to display the {@link URL} chooser and
	 *            to show error messages if any
	 */
	public void imprtUrl(final Container parent) {
		String clipboard = "";
		try {
			clipboard = ("" + Toolkit.getDefaultToolkit().getSystemClipboard()
					.getData(DataFlavor.stringFlavor)).trim();
		} catch (Exception e) {
			// No data will be handled
		}

		if (clipboard == null || !(clipboard.startsWith("http://") || //
				clipboard.startsWith("https://"))) {
			clipboard = "";
		}

		Object url = JOptionPane.showInputDialog(parent,
				Instance.getInstance().getTransGui()
						.getString(StringIdGui.SUBTITLE_IMPORT_URL),
				Instance.getInstance().getTransGui()
						.getString(StringIdGui.TITLE_IMPORT_URL),
				JOptionPane.QUESTION_MESSAGE, null, null, clipboard);

		if (url != null && !url.toString().isEmpty()) {
			imprt(parent, url.toString());
		}
	}

	/**
	 * Ask for and import a {@link File} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * <p>
	 * Will fire {@link ImporterFrame#IMPORTED_SUCCESS} or
	 * {@link ImporterFrame#IMPORTED_SUCCESS} when done.
	 * 
	 * @param parent
	 *            a container we can use to display the {@link File} chooser and
	 *            to show error messages if any
	 */

	public void imprtFile(final Container parent) {
		Progress pg = new Progress();
		JFileChooser fc = new JFileChooser();
		if (fc.showOpenDialog(parent) != JFileChooser.CANCEL_OPTION) {
			Object url = fc.getSelectedFile().getAbsolutePath();
			if (url != null && !url.toString().isEmpty()) {
				final ImporterItem item = add(pg, "File",
						fc.getSelectedFile().getName());
				Actions.imprt(parent, url.toString(), pg, new Runnable() {
					@Override
					public void run() {
						item.setDone(true);
						fireActionPerformed(IMPORTED_SUCCESS);
					}
				}, new Runnable() {
					@Override
					public void run() {
						item.setFailed(true);
						item.setDone(true);
						fireActionPerformed(IMPORTED_FAIL);
					}
				});

				setVisible(true);
			}
		}
	}

	/**
	 * Import an {@link URL} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * <p>
	 * Will fire {@link ImporterFrame#IMPORTED_SUCCESS} or
	 * {@link ImporterFrame#IMPORTED_SUCCESS} when done.
	 * 
	 * @param parent
	 *            a container we can use to display the {@link URL} chooser and
	 *            to show error messages if any
	 * @param url
	 *            the URL to import
	 */
	public void imprt(final Container parent, String url) {
		Progress pg = new Progress();
		String basename = null;
		try {
			BasicSupport support = BasicSupport.getSupport(Actions.getUrl(url));
			basename = support.getType().getSourceName();
		} catch (Exception e) {
			basename = "unknown website";
		}

		if (url != null && !url.isEmpty()) {
			final ImporterItem item = add(pg, basename, null);
			Actions.imprt(parent, url, pg, new Runnable() {
				@Override
				public void run() {
					item.setDone(true);
					fireActionPerformed(IMPORTED_SUCCESS);
				}
			}, new Runnable() {
				@Override
				public void run() {
					item.setFailed(true);
					item.setDone(true);
					fireActionPerformed(IMPORTED_FAIL);
				}
			});

			setVisible(true);
		}
	}

	/**
	 * Add a new {@link ImporterItem} linked to the given {@link Progress}.
	 * 
	 * @param pg
	 *            the {@link Progress} to link, must not be NULL
	 * @param basename
	 *            the base name given to the {@link ImporterItem} (should be the
	 *            name of the web site you download/convert from)
	 * @param storyName
	 *            the name of the story, if already known
	 * 
	 * @return the new item, already linked (you still need to flag it Done or
	 *         Failed as needed)
	 */
	private ImporterItem add(Progress pg, final String basename,
			String storyName) {
		final ImporterItem item = new ImporterItem(pg, basename, storyName);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				data.fireElementChanged(item);
			}
		});

		data.addItem(item);
		filter();

		return item;
	}

	/**
	 * Filter the {@link ImporterItem} and keep only those that conform to
	 * {@link ImporterFrame#filter}.
	 */
	private void filter() {
		data.filter(new Predicate<ImporterItem>() {
			@Override
			public boolean test(ImporterItem item) {
				if (filter.isEmpty())
					return true;

				if (item.getStoryName().isEmpty() && item.getAction().isEmpty())
					return true;

				if (item.getStoryName().toLowerCase()
						.contains(filter.toLowerCase()))
					return true;

				if (item.getAction().toLowerCase()
						.contains(filter.toLowerCase()))
					return true;

				return false;
			}
		});
	}

	@Override
	public boolean hasListeners() {
		return root.hasListeners();
	}

	@Override
	public int getWaitingEventCount() {
		return root.getWaitingEventCount();
	}

	@Override
	public void addActionListener(ActionListener listener) {
		root.addActionListener(listener);
	}

	@Override
	public void removeActionListener(ActionListener listener) {
		root.removeActionListener(listener);
	}

	@Override
	public void fireActionPerformed(String listenerCommand) {
		root.fireActionPerformed(listenerCommand);
	}
}
