package be.nikiroo.fanfix_swing.gui.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.ui.ImageUtilsAwt;
import be.nikiroo.utils.ui.ImageUtilsAwt.Rotation;

public class UiHelper {
	static private Color buttonNormal;
	static private Color buttonPressed;

	static public void setButtonPressed(JButton button, boolean pressed) {
		if (buttonNormal == null) {
			JButton defButton = new JButton(" ");
			buttonNormal = defButton.getBackground();
			if (buttonNormal.getBlue() >= 128) {
				buttonPressed = new Color( //
						Math.max(buttonNormal.getRed() - 100, 0), //
						Math.max(buttonNormal.getGreen() - 100, 0), //
						Math.max(buttonNormal.getBlue() - 100, 0));
			} else {
				buttonPressed = new Color( //
						Math.min(buttonNormal.getRed() + 100, 255), //
						Math.min(buttonNormal.getGreen() + 100, 255), //
						Math.min(buttonNormal.getBlue() + 100, 255));
			}
		}

		button.setSelected(pressed);
		button.setBackground(pressed ? buttonPressed : buttonNormal);
	}

	/**
	 * Display an error message and log the linked {@link Exception}.
	 * 
	 * @param owner
	 *            the owner of the error (to link the messagebox to it)
	 * @param message
	 *            the message
	 * @param title
	 *            the title of the error message
	 * @param e
	 *            the exception to log if any
	 */
	static public void error(final Component owner, final String message,
			final String title, Exception e) {
		Instance.getInstance().getTraceHandler().error(title + ": " + message);
		if (e != null) {
			Instance.getInstance().getTraceHandler().error(e);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(owner, message, title,
						JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	static public void setFrameIcon(final Window win, final Image img) {
		setFrameIcon(win, null, new MetaData() {
			@Override
			public Image getCover() {
				return img;
			}
		});
	}

	static public void setFrameIcon(final Window win, final BasicLibrary lib,
			final MetaData meta) {
		new SwingWorker<List<BufferedImage>, Void>() {
			@Override
			protected List<BufferedImage> doInBackground() throws Exception {
				Image img = meta == null ? null : meta.getCover();
				if (img == null && meta != null && lib != null) {
					img = lib.getCover(meta.getLuid());
				}

				if (img == null) {
					return null;
				}

				BufferedImage image = ImageUtilsAwt.fromImage(img,
						Rotation.NONE);
				boolean zoomSnapWidth = image.getWidth() >= image.getHeight();

				List<BufferedImage> resizedImages = new ArrayList<BufferedImage>();
				for (int size : new Integer[] { 16, 20, 64, 400 }) {
					resizedImages.add(
							ImageUtilsAwt.scaleImage(new Dimension(size, size),
									image, -1, zoomSnapWidth));
				}

				return resizedImages;
			}

			@Override
			protected void done() {
				try {
					List<BufferedImage> imgs = get();
					if (imgs != null)
						win.setIconImages(imgs);
				} catch (InterruptedException e) {
				} catch (ExecutionException e) {
				}
			}
		}.execute();
	}

	static public void setFrameIcon(final Window win) {
		new SwingWorker<java.awt.Image, Void>() {
			@Override
			protected java.awt.Image doInBackground() throws Exception {
				String iconName = Instance.getInstance().getUiConfig()
						.getString(UiConfig.PROGRAM_ICON);
				Icon icon = Icon.valueOf("icon_" + iconName.replace("-", "_"));
				return IconGenerator.get(icon, Size.original).getImage();
			}

			@Override
			protected void done() {
				try {
					win.setIconImage(get());
				} catch (Exception e) {
				}
			}
		}.execute();
	}
}
