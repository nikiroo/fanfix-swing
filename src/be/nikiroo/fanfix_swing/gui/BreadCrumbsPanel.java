package be.nikiroo.fanfix_swing.gui;

import java.io.IOException;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.utils.DataNodeBook;
import be.nikiroo.fanfix_swing.gui.utils.DataTreeBooks;
import be.nikiroo.utils.ui.BreadCrumbsBar;
import be.nikiroo.utils.ui.DataNode;
import be.nikiroo.utils.ui.DataTree;

public class BreadCrumbsPanel extends BreadCrumbsBar<DataNodeBook> {
	public BreadCrumbsPanel() {
		super(new DataTree<DataNodeBook>() {
			private DataTreeBooks dataTreeBooks = new DataTreeBooks(false,
					false, false);

			@Override
			protected DataNode<DataNodeBook> extractData() throws IOException {
				return dataTreeBooks.loadData();
			}

			@Override
			protected boolean checkFilter(String filter,
					DataNodeBook userData) {
				return dataTreeBooks.checkFilter(filter, userData);
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
