package be.nikiroo.fanfix_swing.gui.utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.ui.WaitingDialog;

/**
 * A {@link WaitingDialog} tailored for opening {@link MetaData}.
 * 
 * @author niki
 */
public class WaitingDialogMeta extends WaitingDialog {
	private static final long serialVersionUID = 1L;

	private JLabel imgLabel;

	/**
	 * Create a new {@link WaitingDialogMeta}.
	 * 
	 * @param parent
	 *            the parent/owner
	 * @param lib
	 *            the {@link BasicLibrary} with which to retrieve the cover
	 *            image from the {@link MetaData} if needed
	 * @param meta
	 *            the {@link MetaData} for which we wait
	 */
	public WaitingDialogMeta(Window parent, final BasicLibrary lib,
			final MetaData meta) {
		this(parent, lib, meta, null);
	}

	/**
	 * Create a new {@link WaitingDialogMeta}.
	 * 
	 * @param parent
	 *            the parent/owner
	 * @param lib
	 *            the {@link BasicLibrary} with which to retrieve the cover
	 *            image from the {@link MetaData} if needed
	 * @param meta
	 *            the {@link MetaData} for which we wait
	 * @param pg
	 *            the progress to follow (can be NULL)
	 */
	public WaitingDialogMeta(Window parent, final BasicLibrary lib,
			final MetaData meta, Progress pg) {
		super(parent, 400, pg);

		// TODO fix ugly UI

		this.setLayout(new BorderLayout());
		this.setTitle(meta.getTitle());
		this.add(new JLabel("Waiting for " + meta.getTitle() + "..."),
				BorderLayout.NORTH);
		imgLabel = new JLabel();
		imgLabel.setPreferredSize(new Dimension(CoverImager.getCoverWidth(),
				CoverImager.getCoverHeight()));
		this.add(imgLabel, BorderLayout.CENTER);

		// Image
		new SwingWorker<ImageIcon, Void>() {
			@Override
			protected ImageIcon doInBackground() throws Exception {
				return new ImageIcon(CoverImager.generateCoverImage(lib,
						BookInfo.fromMeta(lib, meta)));
			}

			@Override
			public void done() {
				try {
					imgLabel.setIcon(get());
				} catch (Exception e) {
				}
			}
		}.execute();

		this.setSize(400, 300);
	}
}
