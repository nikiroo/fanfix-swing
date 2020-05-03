package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseListener;

import javax.swing.JDialog;
import javax.swing.JPanel;

import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;

/**
 * A frame displaying properties and other information of a {@link Story}.
 * 
 * @author niki
 */
public class PropertiesFrame extends JDialog {
	private static final long serialVersionUID = 1L;
	private JPanel desc;

	/**
	 * Create a new {@link PropertiesFrame}.
	 * 
	 * @param lib
	 *            the library to use for the cover image
	 * @param meta
	 *            the meta to describe
	 * @param undecorated
	 *            do not draw the usual window decorations
	 *            (close/minimize/maximize)
	 */
	public PropertiesFrame(BasicLibrary lib, MetaData meta,
			boolean undecorated) {
		setTitle(MainFrame.trans(StringIdGui.TITLE_STORY, meta.getLuid(),
				meta.getTitle()));

		desc = new PropertiesPanel(lib, meta, undecorated);
		setLayout(new BorderLayout());
		add(desc, BorderLayout.NORTH);

		this.setSize(600, desc.getHeight() + 0);
		this.setUndecorated(undecorated);

		if (!undecorated) {
			UiHelper.setFrameIcon(this, lib, meta);
		}
	}

	@Override
	public void setVisible(final boolean b) {
		super.setVisible(b);

		if (b) {
			int titleBarHeight = Math
					.abs(getContentPane().getHeight() - getHeight());
			this.setSize(600, desc.getHeight() + titleBarHeight);
		}
	}

	@Override
	public synchronized void addMouseListener(MouseListener l) {
		super.addMouseListener(l);
		desc.addMouseListener(l);
	}
}
