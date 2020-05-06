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

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

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

/**
 * An internal, Swing-based {@link Story} viewer.
 * <p>
 * Only useful for images document.
 * 
 * @author niki
 */
public class ViewerImages extends JFrame {
	private enum ZoomLevel {
		FIT_TO_WIDTH(-1, true), //
		FIT_TO_HEIGHT(-1, false), //
		ACTUAL_SIZE(1, true), //
		HALF_SIZE(0.5, true), //
		DOUBLE_SIZE(2, true),//
		;

		public final double zoom;
		public final boolean zoomSnapWidth;

		private ZoomLevel(double zoom, boolean zoomSnapWidth) {
			this.zoom = zoom;
			this.zoomSnapWidth = zoomSnapWidth;
		}

		@Override
		public String toString() {
			switch (this) {
			case FIT_TO_WIDTH:
				return "Fit to width";
			case FIT_TO_HEIGHT:
				return "Fit to height";
			case ACTUAL_SIZE:
				return "Actual size";
			case HALF_SIZE:
				return "Half size";
			case DOUBLE_SIZE:
				return "Double size";
			}
			return super.toString();
		}

		static ZoomLevel[] values(boolean orderedSelection) {
			if (orderedSelection) {
				return new ZoomLevel[] { //
						FIT_TO_WIDTH, //
						FIT_TO_HEIGHT, //
						ACTUAL_SIZE, //
						HALF_SIZE, //
						DOUBLE_SIZE,//
				};
			}

			return values();
		}
	}

	private static final long serialVersionUID = 1L;

	private List<Image> images;
	private int index;

	private double zoom = -1; // -1 = snap to width or height
	private double currentZoom = 1; // used to go from snap to + or -
	private Dimension currentImageSize;
	private boolean zoomSnapWidth = true;
	private Rotation rotation = Rotation.NONE;

	private NavBar navbar;
	private JLabel area;
	protected JScrollPane scroll;

	@SuppressWarnings("rawtypes") // JComboBox<?> not compatible java 1.6
	private DefaultComboBoxModel zoomBoxModel;

	private DelayWorker worker;

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

	protected void initGui() {
		this.setLayout(new BorderLayout());
		
		JToolBar toolbar = createToolBar();
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

	@SuppressWarnings({ "unchecked", "rawtypes" }) // JComboBox<?> not
													// compatible java 1.6
	protected JToolBar createToolBar() {
		JToolBar toolBar = new JToolBar();

		// Page navigation
		navbar = new NavBar(1, images.size());
		navbar.setIcons( //
				IconGenerator.get(Icon.arrow_double_left, Size.x32), //
				IconGenerator.get(Icon.arrow_left, Size.x32), //
				IconGenerator.get(Icon.arrow_right, Size.x32), //
				IconGenerator.get(Icon.arrow_double_right, Size.x32) //
		);

		// Rotate
		JButton left = new JButton(IconGenerator.get(Icon.turn_left, Size.x32));
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

		JButton right = new JButton(
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

		JButton zoomIn = new JButton(IconGenerator.get(Icon.zoom_in, Size.x32));
		zoomIn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				double newZoom = currentZoom + (currentZoom < 0.1 ? 0.01 : 0.1);
				if (newZoom > 0.1) {
					newZoom = Math.round(newZoom * 10.0) / 10.0; // snap to 10%
				} else {
					newZoom = Math.round(newZoom * 100.0) / 100.0; // snap to 1%
				}
				setZoom(newZoom, zoomSnapWidth, null);
			}
		});

		zoomBoxModel = new DefaultComboBoxModel(ZoomLevel.values(true));
		JComboBox zoomBox = new JComboBox(zoomBoxModel);
		zoomBox.setEditable(true);
		zoomBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object selected = zoomBoxModel.getSelectedItem();

				if (selected == null) {
					return;
				}

				if (selected instanceof ZoomLevel) {
					ZoomLevel selectedZoomLevel = (ZoomLevel) selected;
					setZoom(selectedZoomLevel.zoom,
							selectedZoomLevel.zoomSnapWidth, null);
					return;
				}

				String selectedString = selected.toString();
				selectedString = selectedString.trim();
				if (selectedString.endsWith("%")) {
					selectedString = selectedString
							.substring(0, selectedString.length() - 1).trim();
				}

				try {
					boolean newZoomSnapWidth = zoomSnapWidth;
					int pc = Integer.parseInt(selectedString);
					if (pc <= 0) {
						throw new NumberFormatException("invalid");
					}

					setZoom(pc / 100.0, newZoomSnapWidth, null);
				} catch (NumberFormatException nfe) {
				}
			}
		});

		JButton zoomOut = new JButton(
				IconGenerator.get(Icon.zoom_out, Size.x32));
		zoomOut.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				double newZoom = currentZoom
						- (currentZoom > 0.19 ? 0.1 : 0.01);
				if (newZoom < 0.01)
					newZoom = 0.01;
				if (newZoom > 0.1) {
					newZoom = Math.round(newZoom * 10.0) / 10.0; // snap to 10%
				} else {
					newZoom = Math.round(newZoom * 100.0) / 100.0; // snap to 1%
				}
				setZoom(newZoom, zoomSnapWidth, null);
			}

		});

		// Add to toolbar

		toolBar.add(navbar);

		toolBar.add(sep());

		toolBar.add(left);
		toolBar.add(right);

		toolBar.add(sep());

		toolBar.add(zoomIn);
		toolBar.add(zoomBox);
		toolBar.add(zoomOut);

		return toolBar;
	}

	private JComponent sep() {
		JComponent sep = new JPanel();
		sep.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		return sep;
	}

	private void setZoom(double newZoom, boolean newZoomSnapWidth,
			Point zoomCenterOffset) {
		if (newZoom > 0) {
			zoomBoxModel.setSelectedItem(
					Integer.toString((int) Math.round(newZoom * 100)) + " %");
		}

		zoom = newZoom;
		zoomSnapWidth = newZoomSnapWidth;
		display(index, rotation, false, zoomCenterOffset);
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
		final Dimension areaSize = new Dimension( //
				scroll.getViewport().getWidth(), //
				scroll.getViewport().getHeight()//
		);

		worker.delay("display:" + resetScroll,
				new SwingWorker<ImageIcon, Void>() {
					private double previousZoom;
					private Rectangle scrollTo;

					@Override
					protected ImageIcon doInBackground() throws Exception {
						Rotation rotation = ViewerImages.this.rotation;
						boolean turn = (rotation == Rotation.LEFT
								|| rotation == Rotation.RIGHT);

						BufferedImage image = ImageUtilsAwt.fromImage(img,
								rotation);
						BufferedImage resizedImage = ImageUtilsAwt.scaleImage(
								areaSize, image, zoom, zoomSnapWidth);

						Dimension previousImageSize = currentImageSize;
						currentImageSize = new Dimension(
								resizedImage.getWidth(),
								resizedImage.getHeight());

						previousZoom = currentZoom;
						if (!turn) {
							currentZoom = (1.0 * resizedImage.getWidth())
									/ image.getWidth();
						} else {
							currentZoom = (1.0 * resizedImage.getHeight())
									/ image.getWidth();
						}

						if (previousZoom != currentZoom
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
								double dzoom = currentZoom - previousZoom;
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

					double diff = -0.1 * e.getWheelRotation();
					setZoom(currentZoom + diff, zoomSnapWidth, new Point(x, y));
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
