package be.nikiroo.fanfix_swing.gui.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.MetaResultList;
import be.nikiroo.fanfix_swing.gui.book.BookInfo.Type;
import be.nikiroo.utils.ui.DataNode;
import be.nikiroo.utils.ui.DataTree;

public class DataTreeTag extends DataTree<DataNodeBook> {
	@Override
	protected boolean checkFilter(String filter, DataNodeBook userData) {
		return userData.toString().toLowerCase().contains(filter.toLowerCase());
	}

	@Override
	protected DataNode<DataNodeBook> extractData() throws IOException {
		List<DataNode<DataNodeBook>> nodes = new ArrayList<DataNode<DataNodeBook>>();

		List<String> tagList = new ArrayList<String>();
		MetaResultList metas = Instance.getInstance().getLibrary().getList();
		// TODO: getTagList, getAuthorList... ?
		for (MetaData meta : metas.getMetas()) {
			List<String> tags = meta.getTags();
			if (tags != null) {
				for (String tag : tags) {
					if (!tagList.contains(tag)) {
						tagList.add(tag);
					}
				}
			}
		}
		sort(tagList);

		for (String tag : tagList) {
			nodes.add(new DataNode<DataNodeBook>(null,
					new DataNodeBook(Type.TAG, tag)));
		}

		return new DataNode<DataNodeBook>(nodes,
				new DataNodeBook(Type.TAG, !nodes.isEmpty()));
	}
}
