package be.nikiroo.fanfix_swing.gui.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix_swing.gui.book.BookInfo.Type;
import be.nikiroo.utils.ui.DataNode;
import be.nikiroo.utils.ui.DataTree;

public class DataTreeAuthors extends DataTree<DataNodeBook> {
	@Override
	protected boolean checkFilter(String filter, DataNodeBook userData) {
		return userData.toString().toLowerCase().contains(filter.toLowerCase());
	}

	@Override
	protected DataNode<DataNodeBook> extractData() throws IOException {
		List<DataNode<DataNodeBook>> nodes = new ArrayList<DataNode<DataNodeBook>>();

		// TODO: getResult() -> getTagList, getAuthorList... ?
		List<String> authors = Instance.getInstance().getLibrary().getAuthors();
		for (String author : authors) {
			nodes.add(new DataNode<DataNodeBook>(null,
					new DataNodeBook(Type.AUTHOR, author)));
		}

		return new DataNode<DataNodeBook>(nodes,
				new DataNodeBook(Type.AUTHOR, !nodes.isEmpty()));
	}
}
