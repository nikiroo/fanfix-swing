package be.nikiroo.fanfix_swing.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.book.BookInfo.Type;
import be.nikiroo.fanfix_swing.gui.utils.DataNodeBook;
import be.nikiroo.fanfix_swing.gui.utils.DataTreeAuthors;
import be.nikiroo.fanfix_swing.gui.utils.DataTreeSources;
import be.nikiroo.fanfix_swing.gui.utils.DataTreeTag;
import be.nikiroo.utils.ui.BreadCrumbsBar;
import be.nikiroo.utils.ui.DataNode;
import be.nikiroo.utils.ui.DataTree;

public class BreadCrumbsPanel extends BreadCrumbsBar<DataNodeBook> {
	public BreadCrumbsPanel() {
		super(new DataTree<DataNodeBook>() {
			@Override
			protected DataNode<DataNodeBook> extractData() throws IOException {
				List<? extends DataNode<DataNodeBook>> children = null;

				children = new DataTreeSources(false).loadData().getChildren();
				DataNode<DataNodeBook> sources = new DataNode<DataNodeBook>(
						children, new DataNodeBook(Type.SOURCE, "Sources", true,
								!children.isEmpty()));
				children = new DataTreeAuthors(false).loadData().getChildren();
				DataNode<DataNodeBook> authors = new DataNode<DataNodeBook>(
						children, new DataNodeBook(Type.AUTHOR, "Authors", true,
								!children.isEmpty()));
				children = new DataTreeTag(false).loadData().getChildren();
				DataNode<DataNodeBook> tags = new DataNode<DataNodeBook>(
						children, new DataNodeBook(Type.TAG, "Tags", true,
								!children.isEmpty()));

				return new DataNode<DataNodeBook>(
						Arrays.asList(sources, authors, tags),
						new DataNodeBook(null, false));
			}

			@Override
			protected boolean checkFilter(String filter,
					DataNodeBook userData) {
				return userData.toString().contains(filter.toLowerCase());
			}
		});
	}

	public BookInfo getHighlight() {
		DataNode<DataNodeBook> node = getSelectedNode();

		if (node != null && node.getUserData() != null) {
			BasicLibrary lib = Instance.getInstance().getLibrary();

			DataNodeBook book = node.getUserData();
			if (book.getType() != null) {
				switch (book.getType()) {
				case SOURCE:
					return BookInfo.fromSource(lib,
							book.isRoot() ? null : book.getPath());
				case AUTHOR:
					return BookInfo.fromAuthor(lib,
							book.isRoot() ? null : book.getPath());
				case TAG:
					return BookInfo.fromTag(lib,
							book.isRoot() ? null : book.getPath());

				default:
					break;
				}
			}
		}

		return null;
	}
}
