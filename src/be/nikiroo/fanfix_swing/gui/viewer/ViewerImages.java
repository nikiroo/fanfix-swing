package be.nikiroo.fanfix_swing.gui.viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix_swing.Main;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.ui.DelayWorker;
import be.nikiroo.utils.ui.ImageUtilsAwt;
import be.nikiroo.utils.ui.ImageUtilsAwt.Rotation;
import be.nikiroo.utils.ui.NavBar;
import be.nikiroo.utils.ui.UIUtils;
import be.nikiroo.utils.ui.ZoomBox;

/**
 * An internal, Swing-based {@link Story} viewer.
 * <p>
 * Only useful for images document.
 * 
 * @author niki
 */
public class ViewerImages extends JFrame {
	private static final long serialVersionUID = 1L;

	private List<Image> images;
	private int index;

	private Dimension currentImageSize;

	private Rotation rotation = Rotation.NONE;
	private Point zoomCenterOffset;
	private JLabel area;

	/** The navigation bar. */
	protected NavBar navbar;
	/** The zoom box. */
	protected ZoomBox zoombox;
	/** The main element of this viewer: the scrolled image. */
	protected JScrollPane scroll;

	private DelayWorker worker;
	private double previousZoom;

	/**
	 * Create a new {@link Story} viewer.
	 * 
	 * @param story
	 *            the {@link Story} to display
	 */
	public ViewerImages(Story story) {
		setTitle(Main.trans(StringIdGui.TITLE_STORY, story.getMeta().getLuid(),
				story.getMeta().getTitle()));

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

		// The first part of display() needs the scroll to be positioned
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// ...now it is
				display(index, Rotation.NONE, true);
			}
		});

		UiHelper.setFrameIcon(this, images.isEmpty() ? null : images.get(0));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	/**
	 * Initialise the GUI (after this call, all the graphical elements are in
	 * place).
	 */
	protected void initGui() {
		this.setLayout(new BorderLayout());

		JToolBar toolbar = createToolbar();
		if (toolbar != null) {
			this.add(toolbar, BorderLayout.NORTH);
		}

		area = new JLabel();
		area.setHorizontalAlignment(JLabel.CENTER);
		area.setOpaque(false);
		area.setFocusable(true);

		scroll = UIUtils.scroll(area, true);
		this.add(scroll, BorderLayout.CENTER);

		area.requestFocus();

		listen();
	}

	/**
	 * Create the main toolbar used for this viewer.
	 * 
	 * @return the toolbar, can be NULL
	 */
	protected JToolBar createToolbar() {
		final JToolBar toolbar = new JToolBar();

		// Page navigation
		navbar = new NavBar(1, images.size());
		navbar.setIcons( //
				IconGenerator.get(Icon.arrow_double_left, Size.x32), //
				IconGenerator.get(Icon.arrow_left, Size.x32), //
				IconGenerator.get(Icon.arrow_right, Size.x32), //
				IconGenerator.get(Icon.arrow_double_right, Size.x32) //
		);

		// Rotate
		final JButton left = new JButton(
				IconGenerator.get(Icon.turn_left, Size.x32));
		left.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch (rotation) {
				case NONE:
					display(index, Rotation.LEFT, true);
					break;
				case LEFT:
					display(index, Rotation.UTURN, true);
					break;
				case RIGHT:
					display(index, Rotation.NONE, true);
					break;
				case UTURN:
					display(index, Rotation.RIGHT, true);
					break;
				}
			}
		});

		final JButton right = new JButton(
				IconGenerator.get(Icon.turn_right, Size.x32));
		right.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch (rotation) {
				case NONE:
					display(index, Rotation.RIGHT, true);
					break;
				case LEFT:
					display(index, Rotation.NONE, true);
					break;
				case RIGHT:
					display(index, Rotation.UTURN, true);
					break;
				case UTURN:
					display(index, Rotation.LEFT, true);
					break;
				}
			}
		});

		// Zoom
		zoombox = new ZoomBox();
		zoombox.setIcons(//
				IconGenerator.get(Icon.zoom_in, Size.x32), //
				IconGenerator.get(Icon.zoom_out, Size.x32), //
				IconGenerator.get(Icon.fit_to_width, Size.x32), //
				IconGenerator.get(Icon.fit_to_height, Size.x32) //
		);
		zoombox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				display(index, rotation, false, zoomCenterOffset);
				zoomCenterOffset = null;
			}
		});

		// Add to toolbar

		toolbar.add(navbar);
		toolbar.add(Box.createRigidArea(new Dimension(10, 10)));
		toolbar.add(left);
		toolbar.add(right);
		toolbar.add(Box.createRigidArea(new Dimension(10, 10)));
		toolbar.add(zoombox);

		toolbar.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				boolean vertical = toolbar.getWidth() < toolbar.getHeight();
				if (navbar.setOrientation(vertical)
						|| zoombox.setOrientation(vertical)) {
					toolbar.revalidate();
					toolbar.repaint();
				}
			}
		});

		return toolbar;
	}

	private synchronized void display(int index, Rotation rotation,
			boolean resetScroll) {
		display(index, rotation, resetScroll, null);
	}

	private synchronized void display(int index, final Rotation rotation,
			final boolean resetScroll, final Point zoomCenterOffset) {
		if (images.isEmpty()) {
			return;
		}

		// So we can use the keyboard navigation even after a toolbar click
		area.requestFocus();

		this.rotation = rotation;

		final Image img = images.get(index);
		// TODO why 0?
		final int sz = UIManager.getInt("ScrollBar.width") == 0 ? 16
				: UIManager.getInt("ScrollBar.width");
		final Dimension areaSize = new Dimension( //
				scroll.getViewport().getWidth(), //
				scroll.getViewport().getHeight() //
		);

		worker.delay("display:" + resetScroll,
				new SwingWorker<ImageIcon, Void>() {
					private Rectangle scrollTo;

					@Override
					protected ImageIcon doInBackground() throws Exception {
						Rotation rotation = ViewerImages.this.rotation;
						boolean turn = (rotation == Rotation.LEFT
								|| rotation == Rotation.RIGHT);

						BufferedImage image = ImageUtilsAwt.fromImage(img,
								rotation);

						// If scrollbar needed, reserve space for it
						Dimension resizedArea = ImageUtilsAwt.scaleSize(
								new Dimension(image.getWidth(),
										image.getHeight()),
								areaSize, zoombox.getZoom(),
								zoombox.getSnapMode());
						// TODO: why +3 seems to work?
						if (resizedArea.width > areaSize.width) {
							areaSize.height -= sz + 3;
						}
						if (resizedArea.height > areaSize.height) {
							areaSize.width -= sz + 3;
							// Not needed locally, but needed remote..
						}
						//

						BufferedImage resizedImage = ImageUtilsAwt.scaleImage(
								image, areaSize, zoombox.getZoom(),
								zoombox.getSnapMode());

						Dimension previousImageSize = currentImageSize;
						currentImageSize = new Dimension(
								resizedImage.getWidth(),
								resizedImage.getHeight());

						zoombox.setZoom((1.0 * (turn ? resizedImage.getHeight()
								: resizedImage.getWidth())) / image.getWidth());

						if (previousZoom != zoombox.getZoom()
								&& previousImageSize != null) {
							Rectangle view = scroll.getViewport().getViewRect();

							double centerX = view.getCenterX();
							double centerY = view.getCenterY();
							double ratioW = centerX / previousImageSize.width;
							double ratioH = centerY / previousImageSize.height;

							if (turn) {
								double tmp = ratioW;
								ratioW = ratioH;
								ratioH = tmp;
							}

							centerX = currentImageSize.width * ratioW;
							centerY = currentImageSize.height * ratioH;
							if (zoomCenterOffset != null) {
								double dzoom = zoombox.getZoom() - previousZoom;
								centerX += zoomCenterOffset.x * dzoom * ratioW;
								centerY += zoomCenterOffset.y * dzoom * ratioH;
							}

							int x = (int) Math
									.round(centerX - (view.width / 2.0));
							int y = (int) Math
									.round(centerY - (view.height / 2.0));

							scrollTo = new Rectangle(x, y, //
									view.width, view.height);
						}

						previousZoom = zoombox.getZoom();
						return new ImageIcon(resizedImage);
					}

					@Override
					protected void done() {
						try {
							ImageIcon img = get();

							if (zoombox.getSnapMode() == null) {
								scroll.setHorizontalScrollBarPolicy(
										JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
								scroll.setVerticalScrollBarPolicy(
										JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
							} else if (zoombox.getSnapMode()) {
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
							} else if (scrollTo != null) {
								area.scrollRectToVisible(scrollTo);
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
		navbar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				index = navbar.getIndex() - 1;
				display(index, Rotation.NONE, true);
			}
		});
		area.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				boolean consume = true;
				switch (e.getKeyCode()) {
				case KeyEvent.VK_SPACE:
				case KeyEvent.VK_DOWN:
					if (!scroll(0,
							scroll.getViewport().getViewRect().height / 2)) {
						navbar.next();
					}
					break;
				case KeyEvent.VK_PAGE_DOWN:
					if (!scroll(0, scroll.getViewport().getViewRect().height)) {
						navbar.next();
					}
					break;
				case KeyEvent.VK_RIGHT:
					if (!scroll(scroll.getViewport().getViewRect().width / 2,
							0)) {
						navbar.next();
					}
					break;
				case KeyEvent.VK_LEFT:
					if (!scroll(-scroll.getViewport().getViewRect().width / 2,
							0)) {
						navbar.previous();
					}
					break;
				case KeyEvent.VK_BACK_SPACE:
				case KeyEvent.VK_UP:
					if (!scroll(0,
							-scroll.getViewport().getViewRect().width / 2)) {
						navbar.previous();
					}
					break;
				case KeyEvent.VK_PAGE_UP:
					if (!scroll(0, -scroll.getViewport().getViewRect().width)) {
						navbar.previous();
					}
					break;
				case KeyEvent.VK_HOME:
					navbar.first();
					break;
				case KeyEvent.VK_END:
					navbar.last();
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
		final MouseWheelListener wheeling = scroll.getMouseWheelListeners()[0];
		scroll.removeMouseWheelListener(wheeling);
		area.addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.isControlDown()) {
					Rectangle view = new Rectangle(scroll.getLocationOnScreen(),
							scroll.getViewport().getViewRect().getSize());
					int x = e.getLocationOnScreen().x - (int) view.getCenterX();
					int y = e.getLocationOnScreen().y - (int) view.getCenterY();

					zoomCenterOffset = new Point(x, y);
					zoombox.zoomOut(e.getWheelRotation());
					e.consume();
				} else {
					wheeling.mouseWheelMoved(e);
				}
				super.mouseWheelMoved(e);
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
				area.requestFocus();
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseReleased(e);
				if (e.getButton() == MouseEvent.BUTTON1) {
					navbar.next();
				} else {
					navbar.previous();
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
		scroll.addComponentListener(new ComponentAdapter() {
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
}
