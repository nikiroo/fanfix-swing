package be.nikiroo.fanfix_swing;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.VersionCheck;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix_swing.gui.MainFrame;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.ui.UIUtils;

/**
 * The main class of the application, the launcher.
 * 
 * @author niki
 */
public class Main extends be.nikiroo.fanfix.Main {
	private boolean busy;
	private boolean kiosk;

	/**
	 * The main entry point of the application.
	 * <p>
	 * It overrides some function of Fanfix's Main.
	 * 
	 * @param args
	 *            the arguments (none, "--kiosk" (fullceen, no decorations,
	 *            Nimbus Look &amp; Feel) or will be passed to Fanfix)
	 */
	public static void main(String[] args) {
		new Main().start(args);
	}

	@Override
	public void start(String[] args) {
		List<String> argsList = new ArrayList<String>();
		for (String arg : args) {
			if ("--kiosk".equals(arg)) {
				kiosk = true;
			} else {
				argsList.add(arg);
			}
		}

		super.start(argsList.toArray(new String[0]));
	}

	@Override
	protected VersionCheck checkUpdates() {
		new SwingWorker<VersionCheck, Void>() {
			@Override
			protected VersionCheck doInBackground() throws Exception {
				return VersionCheck.check("nikiroo/fanfix-swing");
			}

			@Override
			protected void done() {
				try {
					VersionCheck v = get();
					if (v != null && v.isNewVersionAvailable()) {
						notifyUpdates(v);
					}
				} catch (InterruptedException e) {
				} catch (ExecutionException e) {
				}
			}
		}.execute();

		return null;
	}

	@Override
	protected void exit(final int status) {
		if (busy) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (busy) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}

						Main.super.exit(status);
					}
				}
			}).start();
		} else {
			super.exit(status);
		}
	}

	@Override
	protected void start() throws IOException {
		if (kiosk) {
			UIUtils.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} else {
			UIUtils.setLookAndFeel();
		}

		Instance.init();

		JFrame main = new MainFrame();

		if (kiosk) {
			main.setUndecorated(kiosk);
			main.setExtendedState(JFrame.MAXIMIZED_BOTH);
		}

		main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		main.setVisible(true);
	}

	@Override
	protected int read(Story story, Integer chap) {
		if (chap == null) {
			Actions.openBook(Instance.getInstance().getLibrary(),
					story.getMeta(), null, null);
			return 0;
		}

		return super.read(story, chap);
	}

	@Override
	protected void notifyUpdates(VersionCheck updates) {
		StringBuilder builder = new StringBuilder();
		final JEditorPane updateMessage = new JEditorPane("text/html", "");
		builder.append(trans(StringIdGui.NEW_VERSION_AVAILABLE,
				"<a href='https://github.com/nikiroo/fanfix-swing/releases'>"
						+ "https://github.com/nikiroo/fanfix-swing/releases"
						+ "</a>"));
		builder.append("<br>");
		builder.append("<br>");
		for (Version v : updates.getNewer()) {
			builder.append("\t<b>"
					+ trans(StringIdGui.NEW_VERSION_VERSION, v.toString())
					+ "</b>");
			builder.append("<br>");
			builder.append("<ul>");
			for (String item : updates.getChanges().get(v)) {
				builder.append("<li>" + item + "</li>");
			}
			builder.append("</ul>");
		}

		// html content
		updateMessage.setText("<html><body>" //
				+ builder//
				+ "</body></html>");

		// handle link events
		updateMessage.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
					try {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} catch (IOException ee) {
						Instance.getInstance().getTraceHandler().error(ee);
					} catch (URISyntaxException ee) {
						Instance.getInstance().getTraceHandler().error(ee);
					}
			}
		});
		updateMessage.setEditable(false);
		updateMessage.setBackground(new JLabel().getBackground());
		updateMessage.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent evn) {
				if (evn.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(evn.getURL().toURI());
						} catch (IOException e) {
						} catch (URISyntaxException e) {
						}
					}
				}
			}
		});

		int rep = JOptionPane.showConfirmDialog(null, updateMessage,
				trans(StringIdGui.NEW_VERSION_TITLE),
				JOptionPane.OK_CANCEL_OPTION);
		if (rep == JOptionPane.OK_OPTION) {
			updates.ok();
		} else {
			updates.ignore();
		}
	}

	/**
	 * Translate the given id into user text.
	 * 
	 * @param id
	 *            the ID to translate
	 * @param values
	 *            the values to insert instead of the place holders in the
	 *            translation
	 * 
	 * @return the translated text with the given value where required or NULL
	 *         if not found (not present in the resource file)
	 */
	static public String trans(StringIdGui id, Object... values) {
		return Instance.getInstance().getTransGui().getString(id, values);
	}
}
