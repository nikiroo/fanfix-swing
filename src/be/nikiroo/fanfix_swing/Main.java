package be.nikiroo.fanfix_swing;

import javax.swing.JFrame;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix_swing.gui.MainFrame;
import be.nikiroo.utils.ui.UIUtils;

/**
 * The main class of the application, the launcher.
 * 
 * @author niki
 */
public class Main {
	/**
	 * The main entry point of the application.
	 * <p>
	 * If arguments are passed, everything will be passed to Fanfix CLI; if no
	 * argument are present, Fanfix-Swing proper will be launched.
	 * 
	 * @param args
	 *            the arguments (none, or will be passed to Fanfix)
	 */
	public static void main(String[] args) {
		// Defer to main application if parameters (we are only a UI)
		// (though we could handle some of the parameters in the future,
		// maybe importing via ImporterFrame? but that would require a
		// unique instance of the UI to be usable...)
		if (args != null && args.length > 0) {
			be.nikiroo.fanfix.Main.main(args);
			return;
		}

		UIUtils.setLookAndFeel();
		Instance.init();

		JFrame main = new MainFrame();
		main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		main.setVisible(true);
	}
}
