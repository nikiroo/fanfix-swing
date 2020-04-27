package be.nikiroo.fanfix_swing.gui.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.MetaResultList;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.book.BookInfo.Type;
import be.nikiroo.utils.ui.DataNode;
import be.nikiroo.utils.ui.DataTree;

public class DataTreeBooks {
	abstract private class DataTreeSort extends DataTree<DataNodeBook> {
		// make it PUBLIC instead of PROTECTED
		@Override
		abstract public boolean checkFilter(String filter,
				DataNodeBook userData);

		@Override
		public void sort(List<String> values) {
			super.sort(values);
		}

		@Override
		public DataNode<DataNodeBook> loadData() throws IOException {
			return loadData(true);
		}

		public DataNode<DataNodeBook> loadData(boolean reload)
				throws IOException {
			if (reload) {
				reload();
			}

			return data = extractData();
		}

		abstract public void reload() throws IOException;
	}

	private DataTreeSort sources;
	private DataTreeSort authors;
	private DataTreeSort tags;

	private MetaResultList list;

	// flat = force flat mode (no [A-B] groups)
	public DataTreeBooks(final boolean flatSources, final boolean flatAuthors,
			final boolean flatTags) {
		list = new MetaResultList(null);

		sources = new DataTreeSort() {
			@Override
			public boolean checkFilter(String filter, DataNodeBook userData) {
				return userData.getName().toLowerCase()
						.contains(filter.toLowerCase())
						|| userData.getSubname().toLowerCase()
								.contains(filter.toLowerCase());
			}

			@Override
			protected DataNode<DataNodeBook> extractData() throws IOException {
				if (flatSources) {
					return getNodeFlat(list.getSources(), Type.SOURCE);
				}

				Map<String, List<String>> sourcesGrouped = list
						.getSourcesGrouped();
				return getNodeGrouped(this, sourcesGrouped, Type.SOURCE);
			}

			@Override
			public void reload() throws IOException {
				reloadList();
			}
		};

		authors = new DataTreeSort() {
			@Override
			public boolean checkFilter(String filter, DataNodeBook userData) {
				return userData.getSubnameOrName().toLowerCase()
						.contains(filter.toLowerCase());
			}

			@Override
			protected DataNode<DataNodeBook> extractData() throws IOException {
				if (flatAuthors) {
					return getNodeFlat(list.getAuthors(), Type.AUTHOR);
				}

				Map<String, List<String>> authorsGrouped = list
						.getAuthorsGrouped();

				if (authorsGrouped.size() == 1) {
					List<String> authors = authorsGrouped.values().iterator()
							.next();
					return getNodeFlat(authors, Type.AUTHOR);
				}

				return getNodeGrouped(this, authorsGrouped, Type.AUTHOR);
			}

			@Override
			public void reload() throws IOException {
				reloadList();
			}
		};

		tags = new DataTreeSort() {
			@Override
			public boolean checkFilter(String filter, DataNodeBook userData) {
				return userData.getSubnameOrName().toLowerCase()
						.contains(filter.toLowerCase());
			}

			@Override
			protected DataNode<DataNodeBook> extractData() throws IOException {
				if (flatTags) {
					return getNodeFlat(list.getTags(), Type.TAG);
				}

				return getNodeGrouped(this, list.getTagsGrouped(), Type.TAG);
			}

			@Override
			public void reload() throws IOException {
				reloadList();
			}
		};
	}

	public DataTree<DataNodeBook> getSources() {
		return sources;
	}

	public DataTree<DataNodeBook> getAuthors() {
		return authors;
	}

	public DataTree<DataNodeBook> getTags() {
		return tags;
	}

	public DataNode<DataNodeBook> loadData() throws IOException {
		reloadList();

		List<? extends DataNode<DataNodeBook>> children = null;

		children = sources.loadData(false).getChildren();
		DataNode<DataNodeBook> sources = new DataNode<DataNodeBook>(children,
				new DataNodeBook(Type.SOURCE, "Sources", true,
						!children.isEmpty()));

		children = authors.loadData(false).getChildren();
		DataNode<DataNodeBook> authors = new DataNode<DataNodeBook>(children,
				new DataNodeBook(Type.AUTHOR, "Authors", true,
						!children.isEmpty()));

		children = tags.loadData(false).getChildren();
		DataNode<DataNodeBook> tags = new DataNode<DataNodeBook>(children,
				new DataNodeBook(Type.TAG, "Tags", true, !children.isEmpty()));

		return new DataNode<DataNodeBook>(Arrays.asList(sources, authors, tags),
				new DataNodeBook(null, false));
	}

	public boolean checkFilter(String filter, DataNodeBook userData) {
		if (userData.getType() != null) {
			switch (userData.getType()) {
			case SOURCE:
				return sources.checkFilter(filter, userData);
			case AUTHOR:
				return authors.checkFilter(filter, userData);
			case TAG:
				return tags.checkFilter(filter, userData);
			default:
				break;
			}
		}

		return false;
	}

	private void reloadList() throws IOException {
		list = Instance.getInstance().getLibrary().getList();
	}

	private DataNode<DataNodeBook> getNodeFlat(List<String> flatData,
			Type type) {
		List<DataNode<DataNodeBook>> nodes = new ArrayList<DataNode<DataNodeBook>>();

		for (String data : flatData) {
			nodes.add(new DataNode<DataNodeBook>(null,
					new DataNodeBook(type, data)));
		}

		return new DataNode<DataNodeBook>(nodes,
				new DataNodeBook(type, !nodes.isEmpty()));
	}

	private DataNode<DataNodeBook> getNodeGrouped(DataTreeSort tree,
			Map<String, List<String>> sourcesGrouped, Type type) {
		List<DataNode<DataNodeBook>> nodes = new ArrayList<DataNode<DataNodeBook>>();

		List<String> sources = new ArrayList<String>(sourcesGrouped.keySet());
		tree.sort(sources);
		for (String source : sources) {
			List<String> children = sourcesGrouped.get(source);
			boolean hasChildren = (children.size() > 1) || (children.size() == 1
					&& !children.get(0).trim().isEmpty());

			List<DataNode<DataNodeBook>> subnodes = new ArrayList<DataNode<DataNodeBook>>();
			if (hasChildren) {
				tree.sort(children);
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
