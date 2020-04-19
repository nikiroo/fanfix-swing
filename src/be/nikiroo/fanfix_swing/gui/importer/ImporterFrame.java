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
import javax.swing.JList;
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
import be.nikiroo.fanfix_swing.gui.utils.ListModel;
import be.nikiroo.fanfix_swing.gui.utils.ListModel.Predicate;
import be.nikiroo.utils.Progress;

public class ImporterFrame extends JFrame {
	private ListModel<ImporterItem> data;
	private String filter = "";

	@SuppressWarnings("unchecked") // JList<ImporterItem> is not java 1.6
	public ImporterFrame() {
		setLayout(new BorderLayout());

		@SuppressWarnings("rawtypes") // JList<ImporterItem> is not java 1.6
		JList list = new JList();
		data = new ListModel<ImporterItem>(list);

		list.setCellRenderer(ListModel.generateRenderer(data));
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.setVisibleRowCount(5);

		this.add(list, BorderLayout.CENTER);

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

		this.add(top, BorderLayout.NORTH);

		setSize(800, 600);
	}

	/**
	 * Ask for and import an {@link URL} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * 
	 * @param parent
	 *            a container we can use to display the {@link URL} chooser and
	 *            to show error messages if any
	 * @param onSuccess
	 *            Action to execute on success
	 */
	public void imprtUrl(final Container parent, final Runnable onSuccess) {
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

		Progress pg = new Progress();
		String basename = null;
		try {
			BasicSupport support = BasicSupport
					.getSupport(BasicReader.getUrl(url.toString()));
			basename = support.getType().getSourceName();
		} catch (Exception e) {
		}

		add(pg, basename); // TODO: what when null?

		if (url != null && !url.toString().isEmpty()) {
			Actions.imprt(parent, url.toString(), pg, onSuccess);
		}
		// TODO what when not ok?

		setVisible(true);
	}

	/**
	 * Ask for and import a {@link File} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * 
	 * @param parent
	 *            a container we can use to display the {@link File} chooser and
	 *            to show error messages if any
	 * @param onSuccess
	 *            Action to execute on success
	 */

	public void imprtFile(final Container parent, final Runnable onSuccess) {
		JFileChooser fc = new JFileChooser();

		Progress pg = new Progress();
		add(pg, "File");

		if (fc.showOpenDialog(parent) != JFileChooser.CANCEL_OPTION) {
			Object url = fc.getSelectedFile().getAbsolutePath();
			if (url != null && !url.toString().isEmpty()) {
				Actions.imprt(parent, url.toString(), pg, onSuccess);
			}
		}

		setVisible(true);
	}

	private void add(Progress pg, final String basename) {
		final ImporterItem item = new ImporterItem(pg, basename);
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
				String text = item.getStoryName() + " " + item.getAction();
				return filter.isEmpty() || text.isEmpty()
						|| text.toLowerCase().contains(filter.toLowerCase());
			}
		});
	}
}
