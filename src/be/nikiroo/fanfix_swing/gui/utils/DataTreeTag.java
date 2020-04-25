package be.nikiroo.fanfix_swing.gui.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.MetaResultList;
import be.nikiroo.fanfix_swing.gui.book.BookInfo.Type;
import be.nikiroo.utils.ui.DataNode;

public class DataTreeTag extends DataTreeSources {
	public DataTreeTag(boolean flat) {
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
					Instance.getInstance().getLibrary().getList().getTags(),
					Type.TAG);
		}

		return getNodeGrouped(
				Instance.getInstance().getLibrary().getList().getTagsGrouped(),
				Type.TAG);
	}
}
