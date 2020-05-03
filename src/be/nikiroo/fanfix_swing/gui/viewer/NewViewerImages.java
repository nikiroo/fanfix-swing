package be.nikiroo.fanfix_swing.gui.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix_swing.gui.MainFrame;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.ui.DelayWorker;
import be.nikiroo.utils.ui.ImageUtilsAwt;
import be.nikiroo.utils.ui.ImageUtilsAwt.Rotation;
import be.nikiroo.utils.ui.UIUtils;

/**
 * An internal, Swing-based {@link Story} viewer.
 * <p>
 * Works on both text and image document (see {@link MetaData#isImageDocument()}
 * ).
 * 
 * @author niki
 */
public class NewViewerImages extends JFrame {
	private static final long serialVersionUID = 1L;

	private List<Image> images;
	private int index;

	private double zoom = -1; // -1 = snap to width or height
	private boolean zoomSnapWidth = true;
	private Rotation rotation = Rotation.NONE;

	private JLabel area;
	private JScrollPane scroll;

	private DelayWorker worker;

	/**
	 * Create a new {@link Story} viewer.
	 * 
	 * @param story
	 *            the {@link Story} to display
	 */
	public NewViewerImages(Story story) {
		setTitle(MainFrame.trans(StringIdGui.TITLE_STORY,
				story.getMeta().getLuid(), story.getMeta().getTitle()));

		setSize(800, 600);

		images = new ArrayList<Image>();
		if (!story.getMeta().isFakeCover()) {
			images.add(story.getMeta().getCover());
		}
		for (Chapter chap : story) {
			for (Paragraph para : chap) {
				Image img = para.getContentImage();
				if (img != null) {
					images.add(img);
				}
			}
		}

		worker = new DelayWorker(100);
		worker.start();

		initGui();
		display(index, Rotation.NONE, true);

		if (!images.isEmpty()) {
			UiHelper.setFrameIcon(this, images.get(0));
		}
	}

	private void initGui() {
		// TODO: menu + bar with all the options

		this.setLayout(new BorderLayout());

		area = new JLabel();
		area.setOpaque(false);
		area.setFocusable(true);
		area.requestFocus();

		scroll = UIUtils.scroll(area, true);
		this.add(scroll, BorderLayout.CENTER);

		listen();
	}

	private synchronized void display(int index, final Rotation rotation,
			final boolean resetScroll) {
		if (images.isEmpty()) {
			return;
		}

		this.rotation = rotation;

		final Image img = images.get(index);
		final Dimension areaSize = new Dimension( //
				scroll.getViewport().getWidth(), //
				scroll.getViewport().getHeight()//
		);

		worker.delay("display:" + resetScroll,
				new SwingWorker<ImageIcon, Void>() {
					@Override
					protected ImageIcon doInBackground() throws Exception {
						BufferedImage image = ImageUtilsAwt.fromImage(img,
								rotation);
						BufferedImage resizedImage = ImageUtilsAwt.scaleImage(
								areaSize, image, zoom, zoomSnapWidth);

						return new ImageIcon(resizedImage);
					}

					@Override
					protected void done() {
						try {
							ImageIcon img = get();

							if (zoom > 0) {
								scroll.setHorizontalScrollBarPolicy(
										JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
								scroll.setVerticalScrollBarPolicy(
										JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
							} else if (zoomSnapWidth) {
								scroll.setHorizontalScrollBarPolicy(
										JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
								scroll.setVerticalScrollBarPolicy(
										JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
							} else {
								scroll.setHorizontalScrollBarPolicy(
										JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
								scroll.setVerticalScrollBarPolicy(
										JScrollPane.VERTICAL_SCROLLBAR_NEVER);
							}

							area.setSize(scroll.getViewport().getSize());
							area.setIcon(img);
							if (resetScroll) {
								area.scrollRectToVisible(new Rectangle());
							}
						} catch (InterruptedException e) {
							Instance.getInstance().getTraceHandler().error(e);
						} catch (ExecutionException e) {
							Instance.getInstance().getTraceHandler().error(e);
						}
					}
				});
	}

	private void listen() {
		area.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				boolean consume = true;
				switch (e.getKeyCode()) {
				case KeyEvent.VK_SPACE:
				case KeyEvent.VK_DOWN:
					if (!scroll(0,
							scroll.getViewport().getViewRect().height / 2)) {
						next();
					}
					break;
				case KeyEvent.VK_PAGE_DOWN:
					if (!scroll(0, scroll.getViewport().getViewRect().height)) {
						next();
					}
					break;
				case KeyEvent.VK_RIGHT:
					if (!scroll(scroll.getViewport().getViewRect().width / 2,
							0)) {
						next();
					}
					break;
				case KeyEvent.VK_LEFT:
					if (!scroll(-scroll.getViewport().getViewRect().width / 2,
							0)) {
						previous();
					}
					break;
				case KeyEvent.VK_BACK_SPACE:
				case KeyEvent.VK_UP:
					if (!scroll(0,
							-scroll.getViewport().getViewRect().width / 2)) {
						previous();
					}
					break;
				case KeyEvent.VK_PAGE_UP:
					if (!scroll(0, -scroll.getViewport().getViewRect().width)) {
						previous();
					}
					break;
				case KeyEvent.VK_HOME:
					first();
					break;
				case KeyEvent.VK_END:
					last();
					break;
				default:
					consume = false;
					break;
				}

				if (consume) {
					e.consume();
				}
			}
		});
		final Point origin[] = new Point[1];
		area.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				origin[0] = e.getPoint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				origin[0] = null;
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseReleased(e);
				if (e.getButton() == MouseEvent.BUTTON1) {
					next();
				} else {
					previous();
				}
			}
		});
		area.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				Point from = origin[0];
				if (from != null) {
					int deltaX = from.x - e.getX();
					int deltaY = from.y - e.getY();

					scroll(deltaX, deltaY);
				}
			}
		});
		area.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				display(index, rotation, false);
			}
		});
	}

	private boolean scroll(int deltaX, int deltaY) {
		Rectangle before = scroll.getViewport().getViewRect();
		Rectangle target = (Rectangle) before.clone();
		target.x += deltaX;
		target.y += deltaY;

		area.scrollRectToVisible(target);

		return !scroll.getViewport().getViewRect().equals(before);
	}

	private synchronized void next() {
		index++;
		if (index >= images.size()) {
			index = images.size() - 1;
		} else {
			display(index, Rotation.NONE, true);
		}
	}

	private synchronized void previous() {
		index--;
		if (index < 0) {
			index = 0;
		} else {
			display(index, Rotation.NONE, true);
		}
	}

	private synchronized void first() {
		index = 0;
		display(index, Rotation.NONE, true);
	}

	private synchronized void last() {
		index = images.size() - 1;
		display(index, Rotation.NONE, true);
	}
}
