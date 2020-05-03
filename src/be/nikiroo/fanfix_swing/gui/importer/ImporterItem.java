package be.nikiroo.fanfix_swing.gui.importer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix_swing.gui.utils.CoverImager;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Progress.ProgressListener;
import be.nikiroo.utils.ui.ListModel.Hoverable;
import be.nikiroo.utils.ui.ListenerPanel;

public class ImporterItem extends ListenerPanel implements Hoverable {
	static public final String CHANGE = "change";

	private String basename = "";
	private String storyName = "";
	private String action = "";
	private double progress = -1;

	private boolean hovered;
	private boolean selected;
	private boolean done;
	private boolean failed;

	private JLabel labelName;
	private JLabel labelAction;

	public ImporterItem(Progress pg, String basename, String storyName) {
		this.basename = basename == null ? "" : basename;
		this.storyName = storyName == null ? "" : storyName;

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

	@Override
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

	@Override
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

	public boolean isDone(boolean acceptFailure) {
		return done && (acceptFailure || !failed);
	}

	public void setDone(boolean done) {
		if (this.done != done) {
			this.done = done;
			setHighlight();
		}
	}

	public boolean isFailed() {
		return failed;
	}

	public void setFailed(boolean failed) {
		if (this.failed != failed) {
			this.failed = failed;
			setHighlight();
		}
	}

	private void setHighlight() {
		Color highlight = null;
		if (failed) {
			highlight = Color.red.darker();
		} else if (done) {
			highlight = Color.green.darker();
		}

		if (highlight != null) {
			labelAction.setForeground(highlight);
			labelAction.setFont(labelAction.getFont().deriveFont(Font.BOLD));
		} else {
			labelAction.setForeground(Color.gray);
			labelAction.setFont(labelAction.getFont().deriveFont(Font.PLAIN));
		}
	}

	private void init(final Progress pg) {
		pg.addProgressListener(new ProgressListener() {
			@Override
			public void progress(Progress notUsed, String currentAction) {
				currentAction = currentAction == null ? "" : currentAction;
				String currentStoryName = null;

				MetaData meta = (MetaData) pg.get("meta");
				if (meta != null) {
					currentStoryName = meta.getTitle();
				}

				if (pg.getRelativeProgress() != progress
						|| !action.equals(currentAction)
						|| !storyName.equals(currentStoryName)) {
					progress = pg.getRelativeProgress();
					action = currentAction;
					storyName = currentStoryName == null ? ""
							: currentStoryName;

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