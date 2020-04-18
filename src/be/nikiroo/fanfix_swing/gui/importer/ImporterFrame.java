package be.nikiroo.fanfix_swing.gui.importer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix_swing.Actions;
import be.nikiroo.fanfix_swing.gui.SearchBar;
import be.nikiroo.fanfix_swing.gui.book.BookBlock;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.book.BookLine;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Progress.ProgressListener;

public class ImporterFrame extends JFrame {
	private class ListModel extends DefaultListModel<ImporterItem> {
		public void fireElementChanged(int index) {
			if (index >= 0) {
				fireContentsChanged(this, index, index);
			}
		}

		public void fireElementChanged(ImporterItem element) {
			int index = indexOf(element);
			if (index >= 0) {
				fireContentsChanged(this, index, index);
			}
		}
	}

	private JList<ImporterItem> list;
	private ListModel data = new ListModel();
	private List<ImporterItem> items = new ArrayList<ImporterItem>();
	private String filter = "";
	private int hoveredIndex = -1;

	public ImporterFrame() {
		setLayout(new BorderLayout());

		list = new JList<ImporterItem>(data);
		this.add(list, BorderLayout.CENTER);

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.setCellRenderer(generateRenderer());
		list.setVisibleRowCount(5);

		list.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent me) {
				Point p = new Point(me.getX(), me.getY());
				int index = list.locationToIndex(p);
				if (index != hoveredIndex) {
					int oldIndex = hoveredIndex;
					hoveredIndex = index;
					data.fireElementChanged(oldIndex);
					data.fireElementChanged(index);
				}
			}
		});
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseExited(MouseEvent e) {
				if (hoveredIndex > -1) {
					int oldIndex = hoveredIndex;
					hoveredIndex = -1;
					data.fireElementChanged(oldIndex);
				}
			}
		});

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
				boolean changed = false;
				for (int i = 0; i < items.size(); i++) {
					if (items.get(i).isDone()) {
						items.remove(i--);
						changed = true;
					}
				}

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

		items.add(item);
		filter();
	}

	private void filter() {
		data.clear();
		for (ImporterItem item : items) {
			String text = item.getStoryName() + " " + item.getAction();
			if (filter.isEmpty() || text.isEmpty()
					|| text.toLowerCase().contains(filter.toLowerCase())) {
				data.addElement(item);
			}
		}
		list.repaint();
	}

	private ListCellRenderer<ImporterItem> generateRenderer() {
		return new ListCellRenderer<ImporterItem>() {
			@Override
			public Component getListCellRendererComponent(
					JList<? extends ImporterItem> list, ImporterItem item,
					int index, boolean isSelected, boolean cellHasFocus) {
				item.setSelected(isSelected);
				item.setHovered(index == hoveredIndex);
				return item;
			}
		};
	}
}
