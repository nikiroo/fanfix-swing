package be.nikiroo.fanfix_swing.gui.search;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.PropertiesPanel;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.viewer.ViewerPanel;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.ui.ProgressBar;

public class SearchAction extends JFrame {
	private static final long serialVersionUID = 1L;

	private BookInfo info;
	private ProgressBar pgBar;

	public SearchAction(BasicLibrary lib, BookInfo info) {
		super(info.getMainInfo());
		this.setSize(800, 600);
		this.info = info;

		setLayout(new BorderLayout());

		JPanel main = new JPanel(new BorderLayout());
		JPanel props = new PropertiesPanel(lib, info.getMeta());

		main.add(props, BorderLayout.NORTH);
		main.add(new ViewerPanel(info.getMeta(), info.getMeta()
				.isImageDocument()), BorderLayout.CENTER);
		main.add(createImportButton(lib), BorderLayout.SOUTH);

		add(main, BorderLayout.CENTER);

		pgBar = new ProgressBar();
		pgBar.setVisible(false);
		add(pgBar, BorderLayout.SOUTH);

		pgBar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pgBar.invalidate();
				pgBar.setProgress(null);
				setEnabled(true);
				validate();
			}
		});

		pgBar.addUpdateListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pgBar.invalidate();
				validate();
				repaint();
			}
		});
	}

	private Component createImportButton(final BasicLibrary lib) {
		JButton imprt = new JButton("Import into library");
		imprt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				final Progress pg = new Progress();
				pgBar.setProgress(pg);

				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							lib.imprt(new URL(info.getMeta().getUrl()), null);
						} catch (IOException e) {
							Instance.getInstance().getTraceHandler().error(e);
						}

						pg.done();
					}
				}).start();
			}
		});

		return imprt;
	}
}
