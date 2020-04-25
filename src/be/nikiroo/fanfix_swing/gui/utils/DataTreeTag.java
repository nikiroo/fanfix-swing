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
	@Override
	protected boolean checkFilter(String filter, DataNodeBook userData) {
		return userData.toString().toLowerCase().contains(filter.toLowerCase());
	}

	@Override
	protected DataNode<DataNodeBook> extractData() throws IOException {
		List<String> tagList = new ArrayList<String>();
		MetaResultList metas = Instance.getInstance().getLibrary().getList();
		// TODO: getTagList, getAuthorList... ? including grouped?
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

		return getNodeFlat(tagList, Type.TAG);
	}
}
