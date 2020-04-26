package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import be.nikiroo.fanfix_swing.gui.utils.DataNodeBook;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;
import be.nikiroo.utils.ui.DataNode;
import be.nikiroo.utils.ui.DataTree;
import be.nikiroo.utils.ui.ListenerPanel;
import be.nikiroo.utils.ui.TreeCellSpanner;
import be.nikiroo.utils.ui.TreeSnapshot;
import be.nikiroo.utils.ui.UIUtils;

public class BrowserTab extends ListenerPanel {
	private int totalCount = 0;
	private List<String> selectedElements = new ArrayList<String>();
	private String baseTitle;
	private String listenerCommand;
	private int index;

	private JTree tree;
	private DefaultMutableTreeNode root;
	private DataTree<DataNodeBook> data;
	private SearchBar searchBar;

	public BrowserTab(DataTree<DataNodeBook> data, int index,
			String listenerCommand) {
		setLayout(new BorderLayout());

		this.data = data;

		this.index = index;
		this.listenerCommand = listenerCommand;

		totalCount = 0;

		root = new DefaultMutableTreeNode();
		tree = new JTree(root);

		tree.setUI(new BasicTreeUI());
		TreeCellSpanner spanner = new TreeCellSpanner(tree,
				generateCellRenderer());
		tree.setCellRenderer(spanner);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(false);

		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				List<String> elements = new ArrayList<String>();
				TreePath[] paths = tree.getSelectionPaths();
				if (paths != null) {
					for (TreePath path : paths) {
						if (path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
							DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
									.getLastPathComponent();
							if (node.getUserObject() instanceof DataNodeBook) {
								DataNodeBook book = (DataNodeBook) node
										.getUserObject();
								elements.add(book.getPath());
							}
						}
					}
				}

				BrowserTab.this.selectedElements = elements;

				fireActionPerformed(BrowserTab.this.listenerCommand);
			}
		});

		add(UIUtils.scroll(tree, false), BorderLayout.CENTER);

		searchBar = new SearchBar();
		add(searchBar, BorderLayout.NORTH);
		searchBar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reloadData(true);
			}
		});

		reloadData(true);
	}

	// does NOT send a change event
	public void reloadData() {
		reloadData(false);
	}

	private void reloadData(final boolean fireActionPerformed) {
		final TreeSnapshot snapshot = new TreeSnapshot(tree) {
			@Override
			protected boolean isSamePath(TreePath oldPath, TreePath newPath) {
				String oldString = oldPath.toString();
				if (oldString.endsWith("/]"))
					oldString = oldString.substring(0, oldString.length() - 2)
							+ "]";

				String newString = newPath.toString();
				if (newString.endsWith("/]"))
					newString = newString.substring(0, newString.length() - 2)
							+ "]";

				return oldString.equals(newString);
			}
		};
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				data.loadData();
				return null;
			}

			@Override
			protected void done() {
				try {
					get();

					DataNode<DataNodeBook> filtered = data
							.getRoot(searchBar.getText());

					node2node(root, filtered);
					totalCount = filtered.count() - 1; // root is counted

					((DefaultTreeModel) tree.getModel()).reload();

					snapshot.apply();

					if (fireActionPerformed) {
						fireActionPerformed(listenerCommand);
					}
				} catch (Exception e) {
					// TODO: error
				}
			}
		};
		worker.execute();
	}

	/**
	 * The currently selected elements, or an empty list.
	 * 
	 * @return the sources (cannot be NULL)
	 */
	public List<String> getSelectedElements() {
		return selectedElements;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public String getBaseTitle() {
		return baseTitle;
	}

	public void setBaseTitle(String baseTitle) {
		this.baseTitle = baseTitle;
	}

	public String getTitle() {
		String title = getBaseTitle();
		String count = "";
		if (totalCount > 0) {
			int selected = selectedElements.size();
			count = " (" + (selected > 0 ? selected + "/" : "") + totalCount
					+ ")";
		}

		return title + count;
	}

	public int getIndex() {
		return index;
	}

	public void unselect() {
		tree.clearSelection();
	}

	private TreeCellRenderer generateCellRenderer() {
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean selected, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {

				String display = value == null ? "" : value.toString();
				if (value instanceof DefaultMutableTreeNode) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
					if (node.getLevel() > 1) {
						setLeafIcon(null);
						setLeafIcon(IconGenerator.get(Icon.empty, Size.x4));
					} else {
						setLeafIcon(IconGenerator.get(Icon.empty, Size.x16));
					}

					if (node.getUserObject() instanceof DataNodeBook) {
						DataNodeBook book = (DataNodeBook) node.getUserObject();
						display = book.getDisplay();
					}
				}

				return super.getTreeCellRendererComponent(tree, display,
						selected, expanded, leaf, row, hasFocus);
			}
		};

		renderer.setClosedIcon(IconGenerator.get(Icon.arrow_right, Size.x16));
		renderer.setOpenIcon(IconGenerator.get(Icon.arrow_down, Size.x16));
		renderer.setLeafIcon(IconGenerator.get(Icon.empty, Size.x16));

		return renderer;
	}

	private MutableTreeNode node2node(DefaultMutableTreeNode root,
			DataNode<DataNodeBook> node) {
		if (root == null) {
			root = new DefaultMutableTreeNode(node.getUserData());
		}

		root.removeAllChildren();

		for (DataNode<DataNodeBook> child : node.getChildren()) {
			root.add(node2node(null, child));
		}

		return root;
	}
}
