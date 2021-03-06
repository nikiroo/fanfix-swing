
package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;
import be.nikiroo.utils.ui.ListenerPanel;

/**
 * A generic search/filter bar.
 * 
 * @author niki
 */
public class SearchBar extends ListenerPanel {
	static private final long serialVersionUID = 1L;

	private JButton search;
	private JTextField text;
	private JButton clear;

	private boolean realTime;

	/**
	 * Create a new {@link SearchBar}.
	 */
	public SearchBar() {
		setLayout(new BorderLayout());

		// TODO: make an option to change the default setting here:
		// (can already be manually toggled by the user)
		realTime = true;

		search = new JButton(IconGenerator.get(Icon.search, Size.x16));
		UiHelper.setButtonPressed(search, realTime);
		search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				realTime = !realTime;
				UiHelper.setButtonPressed(search, realTime);
				text.requestFocus();

				if (realTime) {
					fireActionPerformed(getText());
				}
			}
		});

		text = new JTextField();
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(final KeyEvent e) {
				super.keyTyped(e);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						boolean empty = (text.getText().isEmpty());
						clear.setVisible(!empty);

						if (realTime) {
							fireActionPerformed(getText());
						}
					}
				});
			}
		});
		text.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!realTime) {
					fireActionPerformed(getText());
				}
			}
		});

		clear = new JButton(IconGenerator.get(Icon.clear, Size.x16));
		clear.setBackground(text.getBackground());
		clear.setVisible(false);
		clear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				text.setText("");
				clear.setVisible(false);
				text.requestFocus();

				fireActionPerformed(getText());
			}
		});

		add(search, BorderLayout.WEST);
		add(text, BorderLayout.CENTER);
		add(clear, BorderLayout.EAST);
	}

	/**
	 * Return the current text displayed by this {@link SearchBar}, or an empty
	 * {@link String} if none.
	 * 
	 * @return the text, cannot be NULL
	 */
	public String getText() {
		// Should usually not be NULL, but not impossible
		String text = this.text.getText();
		return text == null ? "" : text;
	}

	@Override
	public void setEnabled(boolean enabled) {
		search.setEnabled(enabled);
		clear.setEnabled(enabled);
		text.setEnabled(enabled);
		super.setEnabled(enabled);
	}
}
