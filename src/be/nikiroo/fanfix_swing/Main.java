package be.nikiroo.fanfix_swing;

import javax.swing.JFrame;

import be.nikiroo.fanfix.DataLoader;
import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix_swing.gui.MainFrame;
import be.nikiroo.utils.ui.UIUtils;

public class Main {
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

		JFrame main = new MainFrame(true, true);
		main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		main.setVisible(true);
	}
}
