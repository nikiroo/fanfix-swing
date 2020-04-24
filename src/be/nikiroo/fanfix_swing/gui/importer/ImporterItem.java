package be.nikiroo.fanfix_swing.gui.importer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import be.nikiroo.fanfix_swing.gui.utils.CoverImager;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Progress.ProgressListener;
import be.nikiroo.utils.ui.ListModel;
import be.nikiroo.utils.ui.ListenerPanel;
import be.nikiroo.utils.ui.ListModel.Hoverable;

public class ImporterItem extends ListenerPanel implements Hoverable {
	static public final String CHANGE = "change";

	private String basename = "";
	private String storyName = "";
	private String action = "";
	private double progress = -1;

	private boolean hovered;
	private boolean selected;
	private boolean done;

	private JLabel labelName;
	private JLabel labelAction;

	public ImporterItem(Progress pg, String basename) {
		this.basename = basename == null ? "" : basename;

		labelName = new JLabel(getStoryName());
		labelAction = new JLabel(getAction());

		setDone(true);
		setDone(false); // to trigger the colour change

		setLayout(new BorderLayout());
		add(labelName, BorderLayout.NORTH);
		add(labelAction, BorderLayout.SOUTH);

		init(pg);
	}

	public String getStoryName() {
		return basename + ": " + storyName;
	}

	public String getAction() {
		// space is for the default size
		if (done) {
			return "Done";
		}

		return action.isEmpty() ? " " : action;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		if (this.selected != selected) {
			this.selected = selected;
			setBackground(
					CoverImager.getBackground(isEnabled(), selected, hovered));
		}
	}

	public boolean isHovered() {
		return hovered;
	}

	public void setHovered(boolean hovered) {
		if (this.hovered != hovered) {
			this.hovered = hovered;
			setBackground(
					CoverImager.getBackground(isEnabled(), selected, hovered));
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (isEnabled() != enabled) {
			super.setEnabled(enabled);
			setBackground(
					CoverImager.getBackground(isEnabled(), selected, hovered));
		}
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		if (this.done != done) {
			this.done = done;
			if (done) {
				labelAction.setForeground(Color.green.darker());
				labelAction
						.setFont(labelAction.getFont().deriveFont(Font.BOLD));
			} else {
				labelAction.setForeground(Color.gray);
				labelAction
						.setFont(labelAction.getFont().deriveFont(Font.PLAIN));
			}
		}
	}

	private void init(final Progress pg) {
		pg.addProgressListener(new ProgressListener() {
			@Override
			public void progress(Progress notUsed, String currentAction) {
				// TODO: get/setSubject on Progress?
				currentAction = currentAction == null ? "" : currentAction;

				if (storyName.isEmpty()
						&& !currentAction.equals("Initialising")) {
					storyName = currentAction;
				}

				if (storyName.equals(currentAction)) {
					currentAction = "";
				}

				if (pg.getRelativeProgress() != progress
						|| !action.equals(currentAction)) {
					progress = pg.getRelativeProgress();
					action = currentAction;

					// The rest must be done in the UI thread
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							setDone(pg.isDone());
							labelName.setText(" " + getStoryName());
							labelAction.setText(" " + getAction());
							fireActionPerformed(CHANGE);
						}
					});
				}
			}
		});
	}

	@Override
	public void paint(Graphics g) {
		Rectangle clip = g.getClipBounds();
		if (!(clip == null || clip.getWidth() <= 0 || clip.getHeight() <= 0)) {
			g.setColor(new Color(200, 200, 255, 128));
			g.fillRect(clip.x, clip.y, (int) Math.round(clip.width * progress),
					clip.height);
		}

		super.paint(g);
	}
}