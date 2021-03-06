package be.nikiroo.fanfix_swing;

import java.awt.Container;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.fanfix_swing.gui.utils.WaitingDialogMeta;
import be.nikiroo.fanfix_swing.gui.viewer.ViewerImages;
import be.nikiroo.fanfix_swing.gui.viewer.ViewerNonImages;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.ui.WaitingDialog;

public class Actions {
	static public void openBook(final BasicLibrary lib, MetaData meta,
			final Container parent, final Runnable onDone) {
		Container parentWindow = parent;
		while (!(parentWindow instanceof Window) && parentWindow != null) {
			parentWindow = parentWindow.getParent();
		}

		final WaitingDialog wait = new WaitingDialogMeta((Window) parentWindow,
				lib, meta);

		boolean internalImg = Instance.getInstance().getUiConfig()
				.getBoolean(UiConfig.IMAGES_DOCUMENT_USE_INTERNAL_READER, true);
		boolean internalNonImg = Instance.getInstance().getUiConfig()
				.getBoolean(UiConfig.NON_IMAGES_DOCUMENT_USE_INTERNAL_READER,
						true);

		final boolean isImageDocument = meta.isImageDocument();
		final boolean internalReader = (isImageDocument && internalImg
				|| !isImageDocument && internalNonImg);
		final String luid = meta.getLuid();

		new SwingWorker<File, Void>() {
			private File target;
			private Story story;

			@Override
			protected File doInBackground() throws Exception {
				target = lib.getFile(luid, null);
				story = lib.getStory(luid, null);
				return null;
			}

			@Override
			protected void done() {
				try {
					get();
					if (internalReader) {
						openInternal(story);
					} else {
						openExternal(target, isImageDocument);
					}
				} catch (Exception e) {
					// TODO: i18n
					UiHelper.error(parent, e.getLocalizedMessage(),
							"Cannot open the story", e);
				}

				// Dismiss the waiting screen if needed
				wait.dismiss();

				if (onDone != null) {
					onDone.run();
				}
			}
		}.execute();
	}

	/**
	 * Open the {@link Story} with an internal reader.
	 * <p>
	 * Asynchronous.
	 * 
	 * @param story
	 *            the story to open
	 */
	static public void openInternal(Story story) {
		if (story.getMeta().isImageDocument()) {
			ViewerImages viewer = new ViewerImages(story);
			viewer.setVisible(true);
		} else {
			ViewerNonImages viewer = new ViewerNonImages(
					Instance.getInstance().getLibrary(), story);
			viewer.setVisible(true);
		}

	}

	/**
	 * Open the {@link Story} with an external reader (the program will be
	 * passed the given target file).
	 * 
	 * @param target
	 *            the target {@link File}
	 * @param isImageDocument
	 *            TRUE for image documents, FALSE for not-images documents
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	static public void openExternal(File target, boolean isImageDocument)
			throws IOException {
		String program = null;
		if (isImageDocument) {
			program = Instance.getInstance().getUiConfig()
					.getString(UiConfig.IMAGES_DOCUMENT_READER);
		} else {
			program = Instance.getInstance().getUiConfig()
					.getString(UiConfig.NON_IMAGES_DOCUMENT_READER);
		}

		if (program != null && program.trim().isEmpty()) {
			program = null;
		}

		start(target, program, false);
	}

	/**
	 * Start a file and open it with the given program if given or the first
	 * default system starter we can find.
	 * 
	 * @param target
	 *            the target to open
	 * @param program
	 *            the program to use or NULL for the default system starter
	 * @param sync
	 *            execute the process synchronously (wait until it is terminated
	 *            before returning)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	static protected void start(File target, String program, boolean sync)
			throws IOException {
		Process proc = null;
		if (program == null) {
			boolean ok = false;
			for (String starter : new String[] { "xdg-open", "see", "start",
					"run", "open" }) {
				try {
					Instance.getInstance().getTraceHandler()
							.trace("starting external program: " + starter);
					proc = Runtime.getRuntime().exec(
							new String[] { starter, target.getAbsolutePath() });
					ok = true;
					break;
				} catch (IOException e) {
				}
			}
			if (!ok) {
				throw new IOException(
						"Cannot find a program to start the file");
			}
		} else {
			Instance.getInstance().getTraceHandler()
					.trace("starting external program: " + program);
			proc = Runtime.getRuntime()
					.exec(new String[] { program, target.getAbsolutePath() });
		}

		if (proc != null && sync) {
			try {
				proc.waitFor();
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Actually import the {@link Story} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread, will start a worker (i.e., this is
	 * asynchronous).
	 * 
	 * @param parent
	 *            a container we can use to show error messages if any
	 * @param url
	 *            the {@link Story} to import by {@link URL}
	 * @param pg
	 *            the optional progress reporter
	 * @param onSuccess
	 *            Action to execute on success
	 * @param onFailure
	 *            Action to execute on failure
	 */
	static public void imprt(final Container parent, final String url,
			Progress pg, final Runnable onSuccess, final Runnable onFailure) {
		final Progress fpg = pg;
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				Progress pg = fpg;
				if (pg == null)
					pg = new Progress();

				try {
					Instance.getInstance().getLibrary().imprt(getUrl(url), pg);
					pg.done();

					if (onSuccess != null) {
						onSuccess.run();
					}
				} catch (IOException e) {
					pg.done();

					if (e instanceof UnknownHostException) {
						UiHelper.error(parent,
								Instance.getInstance().getTransGui().getString(
										StringIdGui.ERROR_URL_NOT_SUPPORTED,
										url),
								Instance.getInstance().getTransGui().getString(
										StringIdGui.TITLE_ERROR),
								null);
					} else {
						UiHelper.error(parent,
								Instance.getInstance().getTransGui().getString(
										StringIdGui.ERROR_URL_IMPORT_FAILED,
										url, e.getMessage()),
								Instance.getInstance().getTransGui()
										.getString(StringIdGui.TITLE_ERROR),
								e);
					}

					if (onFailure != null) {
						onFailure.run();
					}
				}

				return null;
			}
		}.execute();
	}

	/**
	 * Return an {@link URL} from this {@link String}, be it a file path or an
	 * actual {@link URL}.
	 * 
	 * @param sourceString
	 *            the source
	 * 
	 * @return the corresponding {@link URL}
	 * 
	 * @throws MalformedURLException
	 *             if this is neither a file nor a conventional {@link URL}
	 */
	static public URL getUrl(String sourceString) throws MalformedURLException {
		if (sourceString == null || sourceString.isEmpty()) {
			throw new MalformedURLException("Empty url");
		}

		URL source = null;
		try {
			source = new URL(sourceString);
		} catch (MalformedURLException e) {
			File sourceFile = new File(sourceString);
			source = sourceFile.toURI().toURL();
		}

		return source;
	}
}
