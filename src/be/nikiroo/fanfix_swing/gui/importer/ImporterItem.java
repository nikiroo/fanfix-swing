package be.nikiroo.fanfix_swing.gui.importer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix_swing.gui.utils.CoverImager;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Progress.ProgressListener;
import be.nikiroo.utils.ui.ListModel.Hoverable;
import be.nikiroo.utils.ui.ListenerPanel;

/**
 * The visual representation of a {@link Progress} used to download/convert a
 * book.
 * 
 * @author niki
 */
public class ImporterItem extends ListenerPanel implements Hoverable {
	private static final long serialVersionUID = 1L;

	/**
	 * The {@link ActionEvent} you receive from
	 * {@link ImporterItem#addActionListener(ActionListener)} can return this as
	 * a command (see {@link ActionEvent#getActionCommand()}) for change event
	 * on this {@link ImporterItem}.
	 */
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

	/**
	 * Create a new item for the given book conversion/download process.
	 * 
	 * @param pg
	 *            the {@link Progress} to track (must not be NULL)
	 * @param basename
	 *            the base name to use for the process, usually the web site it
	 *            is from (can be NULL)
	 * @param storyName
	 *            the name of the story (can be NULL)
	 */
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

	/**
	 * Return the full story name (including the base name, so the <i>source</i>
	 * of this download/conversion.
	 * 
	 * @return the story name (never NULL)
	 */
	public String getStoryName() {
		return basename + ": " + storyName;
	}

	/**
	 * Return the current action that is being done (more precise tha just
	 * "download" or "convert").
	 * 
	 * @return the current action
	 */
	public String getAction() {
		// space is for the default size
		if (done) {
			return "Done";
		}

		return action.isEmpty() ? " " : action;
	}

	/**
	 * This item is currently selected.
	 * 
	 * @return TRUE for selected, FALSE for unselected
	 */
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

	/**
	 * The element is currently under the mouse cursor.
	 * 
	 * @return TRUE if it is, FALSE if not
	 */
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

	/**
	 * The process is done.
	 * 
	 * @param acceptFailure
	 *            TRUE will also report "done" if we have a failure
	 * 
	 * @return TRUE if it is done
	 */
	public boolean isDone(boolean acceptFailure) {
		return done && (acceptFailure || !failed);
	}

	/**
	 * The process is done.
	 * 
	 * @param done
	 *            the new state
	 */
	public void setDone(boolean done) {
		if (this.done != done) {
			this.done = done;
			setHighlight();
		}
	}

	/**
	 * The process encountered an error.
	 * 
	 * @return TRUE if it has
	 */
	public boolean isFailed() {
		return failed;
	}

	/**
	 * The process encountered an error.
	 * 
	 * @param failed
	 *            the new value
	 */
	public void setFailed(boolean failed) {
		if (this.failed != failed) {
			this.failed = failed;
			setHighlight();
		}
	}

	/**
	 * Update the highlight of the action label depending upon the current state
	 * (normal, done, failed).
	 */
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

	/**
	 * Initialise the system and listen on the {@link Progress} events.
	 * 
	 * @param pg
	 *            the {@link Progress} (must not be NULL)
	 */
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
}