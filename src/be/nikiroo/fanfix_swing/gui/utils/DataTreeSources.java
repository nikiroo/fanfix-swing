package be.nikiroo.fanfix_swing.gui.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix_swing.gui.book.BookInfo.Type;
import be.nikiroo.utils.ui.DataNode;
import be.nikiroo.utils.ui.DataTree;

public class DataTreeSources extends DataTree<DataNodeBook> {
	@Override
	protected boolean checkFilter(String filter, DataNodeBook userData) {
		// TODO
		return userData.toString().toLowerCase().contains(filter.toLowerCase());
	}

	@Override
	protected DataNode<DataNodeBook> extractData() throws IOException {
		Map<String, List<String>> sourcesGrouped = Instance.getInstance()
				.getLibrary().getSourcesGrouped();

		return getNodeGrouped(sourcesGrouped, Type.SOURCE);
	}

	protected DataNode<DataNodeBook> getNodeFlat(List<String> flatData,
			Type type) throws IOException {
		List<DataNode<DataNodeBook>> nodes = new ArrayList<DataNode<DataNodeBook>>();

		// TODO: getResult() -> getTagList, getAuthorList... ?
		List<String> authors = Instance.getInstance().getLibrary().getAuthors();
		for (String author : authors) {
			nodes.add(new DataNode<DataNodeBook>(null,
					new DataNodeBook(type, author)));
		}

		return new DataNode<DataNodeBook>(nodes,
				new DataNodeBook(type, !nodes.isEmpty()));
	}

	protected DataNode<DataNodeBook> getNodeGrouped(
			Map<String, List<String>> sourcesGrouped, Type type)
			throws IOException {
		List<DataNode<DataNodeBook>> nodes = new ArrayList<DataNode<DataNodeBook>>();

		List<String> sources = new ArrayList<String>(sourcesGrouped.keySet());
		sort(sources);
		for (String source : sources) {
			List<String> children = sourcesGrouped.get(source);
			boolean hasChildren = (children.size() > 1) || (children.size() == 1
					&& !children.get(0).trim().isEmpty());

			List<DataNode<DataNodeBook>> subnodes = new ArrayList<DataNode<DataNodeBook>>();
			if (hasChildren) {
				sort(children);
				for (String subSource : children) {
					boolean baseSubSource = subSource.isEmpty()
							&& children.size() > 1;
					DataNodeBook book = new DataNodeBook(type, source,
							subSource, false);
					if (baseSubSource)
						book.setDisplay("*");
					subnodes.add(new DataNode<DataNodeBook>(null, book));
				}
			}

			nodes.add(new DataNode<DataNodeBook>(subnodes,
					new DataNodeBook(type, source, "", hasChildren)));
		}

		return new DataNode<DataNodeBook>(nodes,
				new DataNodeBook(type, !nodes.isEmpty()));
	}
}
