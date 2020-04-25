package be.nikiroo.fanfix_swing.gui.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix_swing.gui.book.BookInfo.Type;
import be.nikiroo.utils.ui.DataNode;
import be.nikiroo.utils.ui.DataTree;

public class DataTreeAuthors extends DataTreeSources {
	public DataTreeAuthors(boolean flat) {
		super(flat);
	}

	@Override
	protected boolean checkFilter(String filter, DataNodeBook userData) {
		return userData.toString().toLowerCase().contains(filter.toLowerCase());
	}

	@Override
	protected DataNode<DataNodeBook> extractData() throws IOException {
		if (isFlat()) {
			return getNodeFlat(
					Instance.getInstance().getLibrary().getList().getAuthors(),
					Type.AUTHOR);
		}

		Map<String, List<String>> authorsGrouped = Instance.getInstance()
				.getLibrary().getList().getAuthorsGrouped();

		if (authorsGrouped.size() == 1) {
			List<String> authors = authorsGrouped.values().iterator().next();
			return getNodeFlat(authors, Type.AUTHOR);
		}

		return getNodeGrouped(authorsGrouped, Type.AUTHOR);
	}
}
