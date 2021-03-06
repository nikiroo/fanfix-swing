package be.nikiroo.fanfix_swing.gui.utils;

import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.book.BookInfo.Type;

public class DataNodeBook {
	private Type type;
	private String subname;
	private String name;
	private boolean children;
	private boolean root;

	private String display;

	// root node
	public DataNodeBook(Type type, boolean children) {
		this(type, null, null, children);
		this.root = true;
	}

	public DataNodeBook(Type type, String name, boolean root,
			boolean children) {
		this(type, name, null, children);
		this.root = root;
	}

	// no root, no children, empty path
	public DataNodeBook(Type type, String name) {
		this(type, name, null, false);
		this.root = false;
	}

	// name empty = main value for the group
	// not root
	public DataNodeBook(Type type, String name, String subname,
			boolean children) {
		this.type = type;
		this.name = name == null ? "" : name;
		this.subname = subname == null ? "" : subname;
		this.children = children;
		this.root = false;
	}

	public BookInfo.Type getType() {
		return type;
	}

	public String getPath() {
		if (type == Type.SOURCE) {
			// Sources are more complex
			String slash = "";
			if (!name.isEmpty()) {
				if (children || !subname.isEmpty()) {
					slash = "/";
				}
			}

			return name + slash + subname;
		}

		return getSubnameOrName();
	}

	public String getName() {
		return name;
	}

	public String getSubname() {
		return subname;
	}

	public String getSubnameOrName() {
		if (!subname.isEmpty()) {
			return subname;
		}

		return name;
	}

	public String getDisplay() {
		if (display != null)
			return display;

		return getSubnameOrName();
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	public boolean isRoot() {
		return root;
	}

	@Override
	public String toString() {
		return getDisplay();
	}
}
