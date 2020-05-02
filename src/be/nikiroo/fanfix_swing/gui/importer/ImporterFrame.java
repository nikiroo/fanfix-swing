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
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix_swing.Actions;
import be.nikiroo.fanfix_swing.gui.SearchBar;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.compat.JList6;
import be.nikiroo.utils.ui.ListModel;
import be.nikiroo.utils.ui.ListModel.Predicate;
import be.nikiroo.utils.ui.ListenerItem;
import be.nikiroo.utils.ui.ListenerPanel;

public class ImporterFrame extends JFrame implements ListenerItem {
	static public final String IMPORTED = "imported";

	private ListenerPanel root = new ListenerPanel();
	private ListModel<ImporterItem> data;
	private String filter = "";

	public ImporterFrame() {
		setLayout(new BorderLayout());
		root.setLayout(new BorderLayout());
		this.add(root);

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
								return item.isDone();
							}
						});

				if (changed) {
					filter();
				}
			}
		});

		root.add(top, BorderLayout.NORTH);

		setSize(800, 600);
	}

	/**
	 * Ask for and import an {@link URL} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * <p>
	 * Will fire {@link ImporterFrame#IMPORTED} if/when successful.
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
	 * Will fire {@link ImporterFrame#IMPORTED} if/when successful.
	 * 
	 * @param parent
	 *            a container we can use to display the {@link File} chooser and
	 *            to show error messages if any
	 */

	public void imprtFile(final Container parent) {
		JFileChooser fc = new JFileChooser();
		if (fc.showOpenDialog(parent) != JFileChooser.CANCEL_OPTION) {
			Object url = fc.getSelectedFile().getAbsolutePath();
			if (url != null && !url.toString().isEmpty()) {
				Progress pg = new Progress();
				add(pg, "File", fc.getSelectedFile().getName());

				Actions.imprt(parent, url.toString(), pg, new Runnable() {
					@Override
					public void run() {
						fireActionPerformed(IMPORTED);
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
	 * Will fire {@link ImporterFrame#IMPORTED} if/when successful.
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
			add(pg, basename, null);

			Actions.imprt(parent, url, pg, new Runnable() {
				@Override
				public void run() {
					fireActionPerformed(IMPORTED);
				}
			});

			setVisible(true);
		}
	}

	private void add(Progress pg, final String basename, String storyName) {
		final ImporterItem item = new ImporterItem(pg, basename, storyName);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				data.fireElementChanged(item);
			}
		});

		data.addItem(item);
		filter();
	}

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
