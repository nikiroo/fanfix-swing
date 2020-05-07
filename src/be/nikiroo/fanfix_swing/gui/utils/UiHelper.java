package be.nikiroo.fanfix_swing.gui.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
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

/**
 * Some UI helper functions dedicated to Fanfix-Swing.
 * 
 * @author niki
 */
public class UiHelper {
	static private Color buttonNormal;
	static private Color buttonPressed;

	/**
	 * Set the given {@link JButton} as "pressed" (selected, but with more UI
	 * visibility).
	 * <p>
	 * The {@link JButton} will answer {@link JButton#isSelected()} if it is
	 * pressed.
	 * 
	 * @param button
	 *            the button to select/press
	 * @param pressed
	 *            the new "pressed" state
	 */
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

	/**
	 * Set the default icon for Fanfix on the provided {@link Window}.
	 * 
	 * @param win
	 *            the window to use
	 */
	static public void setFrameIcon(final Window win) {
		setFrameIcon(win, null, null);
	}

	/**
	 * Set the given icon on the provided {@link Window}.
	 * <p>
	 * If no icon found, the default one for Fanfix will be used.
	 * 
	 * @param win
	 *            the window to use
	 * @param img
	 *            the icon (can be NULL)
	 */
	static public void setFrameIcon(final Window win, final Image img) {
		setFrameIcon(win, null, new MetaData() {
			private static final long serialVersionUID = 1L;

			@Override
			public Image getCover() {
				return img;
			}
		});
	}

	/**
	 * Set the given icon on the provided {@link Window}.
	 * <p>
	 * If no icon found, the default one for Fanfix will be used.
	 * 
	 * @param win
	 *            the window to use
	 * @param lib
	 *            the {@link BasicLibrary} used to retrieve the image if the
	 *            meta doesn't already have it cached (can be null, requires a
	 *            meta with a valid LUID to find something)
	 * @param meta
	 *            the meta in which to look for the image first (can be null)
	 */
	static public void setFrameIcon(final Window win, final BasicLibrary lib,
			final MetaData meta) {
		new SwingWorker<List<java.awt.Image>, Void>() {
			@Override
			protected List<java.awt.Image> doInBackground() throws Exception {
				Image img = meta == null ? null : meta.getCover();
				if (img == null && meta != null && lib != null) {
					img = lib.getCover(meta.getLuid());
				}

				// If no image given, use the default one for Fanfix
				if (img == null) {
					String iconName = Instance.getInstance().getUiConfig()
							.getString(UiConfig.PROGRAM_ICON);
					Icon icon = Icon
							.valueOf("icon_" + iconName.replace("-", "_"));

					return Arrays.asList(
							IconGenerator.get(icon, Size.x16).getImage(),
							IconGenerator.get(icon, Size.x24).getImage(),
							IconGenerator.get(icon, Size.x32).getImage(),
							IconGenerator.get(icon, Size.x64).getImage(),
							IconGenerator.get(icon, Size.original).getImage());
				}

				// Resize the provided image
				BufferedImage image = ImageUtilsAwt.fromImage(img,
						Rotation.NONE);
				boolean zoomSnapWidth = image.getWidth() >= image.getHeight();

				List<java.awt.Image> resizedImages = new ArrayList<java.awt.Image>();
				for (int size : new Integer[] { 16, 20, 64, 400 }) {
					resizedImages.add(ImageUtilsAwt.scaleImage(image,
							new Dimension(size, size), -1, zoomSnapWidth));
				}

				return resizedImages;
			}

			@Override
			protected void done() {
				try {
					List<java.awt.Image> imgs = get();
					if (imgs != null)
						win.setIconImages(imgs);
				} catch (InterruptedException e) {
				} catch (ExecutionException e) {
				}
			}
		}.execute();
	}
}
