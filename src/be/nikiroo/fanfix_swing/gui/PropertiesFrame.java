package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;

/**
 * A frame displaying properties and other information of a {@link Story}.
 * 
 * @author niki
 */
public class PropertiesFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@link PropertiesFrame}.
	 * 
	 * @param lib
	 *            the library to use for the cover image
	 * @param meta
	 *            the meta to describe
	 */
	public PropertiesFrame(BasicLibrary lib, MetaData meta) {
		setTitle(Instance.getInstance().getTransGui().getString(
				StringIdGui.TITLE_STORY, meta.getLuid(), meta.getTitle()));

		PropertiesPane desc = new PropertiesPane(lib, meta);
		setSize(800, (int) desc.getPreferredSize().getHeight()
				+ 2 * desc.getBorderThickness());

		setLayout(new BorderLayout());
		add(desc, BorderLayout.NORTH);
	}
}
