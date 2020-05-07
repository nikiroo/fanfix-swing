package be.nikiroo.fanfix_swing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix_swing.gui.MainFrame;
import be.nikiroo.fanfix_swing.gui.TouchFrame;
import be.nikiroo.utils.VersionCheck;
import be.nikiroo.utils.ui.UIUtils;

/**
 * The main class of the application, the launcher.
 * 
 * @author niki
 */
public class Main extends be.nikiroo.fanfix.Main {
	private boolean busy;
	private boolean kiosk;
	private boolean touch;

	/**
	 * The main entry point of the application.
	 * <p>
	 * It overrides some function of Fanfix's Main.
	 * 
	 * @param args
	 *            in addition to the supported Fanfix arguments, we also
	 *            support:
	 *            <ul>
	 *            <li>(no arguments): we start normally</li>
	 *            <li><tt>--kisok</tt>: we start fullceen, without window
	 *            decorations on the main frame and with the Nimbus Look &amp;
	 *            Feel</li>
	 *            <li><tt>--touch</tt>: a special mode dedicated to small touch
	 *            devices</li>
	 *            </ul>
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
			} else if ("--touch".equals(arg)) {
				touch = true;
			} else {
				argsList.add(arg);
			}
		}

		super.start(argsList.toArray(new String[0]));
	}

	@Override
	protected VersionCheck checkUpdates() {
		// Kiosk mode should not want to know that
		// Touch mode: TODO get an adapted update message (setInfo, setError?)
		if (kiosk || touch) {
			return null;
		}

		// We use a SwingWorker instead of deferring to checkUpdates("...")
		// So we can check in BG
		new SwingWorker<VersionCheck, Void>() {
			@Override
			protected VersionCheck doInBackground() throws Exception {
				return VersionCheck.check("nikiroo/fanfix-swing",
						Instance.getInstance().getTrans().getLocale());
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
					e.printStackTrace();
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

		JFrame main = touch ? new TouchFrame() : new MainFrame();
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
		String title = trans(StringIdGui.NEW_VERSION_TITLE);
		String introText = trans(StringIdGui.NEW_VERSION_AVAILABLE,
				"<a href='https://github.com/nikiroo/fanfix-swing/releases'>"
						+ "https://github.com/nikiroo/fanfix-swing/releases"
						+ "</a>");
		if (UIUtils.showUpdatedDialog(null, updates, introText, title)) {
			Instance.getInstance().setVersionChecked();
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
