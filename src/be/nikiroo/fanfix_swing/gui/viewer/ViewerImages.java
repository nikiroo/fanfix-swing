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
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix_swing.gui.MainFrame;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.ui.DelayWorker;
import be.nikiroo.utils.ui.ImageUtilsAwt;
import be.nikiroo.utils.ui.ImageUtilsAwt.Rotation;
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
	private boolean zoomSnapWidth = true;
	private Rotation rotation = Rotation.NONE;

	private JLabel area;
	private JScrollPane scroll;
	private JTextField page;
	private DefaultComboBoxModel<ZoomLevel> zoomBoxModel;

	private DelayWorker worker;

	/**
	 * Create a new {@link Story} viewer.
	 * 
	 * @param story
	 *            the {@link Story} to display
	 */
	public ViewerImages(Story story) {
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
		this.setLayout(new BorderLayout());
		this.add(createToolBar(), BorderLayout.NORTH);

		area = new JLabel();
		area.setHorizontalAlignment(JLabel.CENTER);
		area.setOpaque(false);
		area.setFocusable(true);

		scroll = UIUtils.scroll(area, true);
		this.add(scroll, BorderLayout.CENTER);

		area.requestFocus();

		listen();
	}

	private JToolBar createToolBar() {
		JToolBar toolBar = new JToolBar();

		// Page navigation
		JButton first = new JButton(
				IconGenerator.get(Icon.arrow_double_left, Size.x32));
		first.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				first();
			}
		});

		JButton previous = new JButton(
				IconGenerator.get(Icon.arrow_left, Size.x32));
		previous.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				previous();
			}
		});

		page = new JTextField("1");
		page.setPreferredSize(new Dimension(page.getPreferredSize().width * 2,
				page.getPreferredSize().height));
		page.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					int pageNb = Integer.parseInt(page.getText());
					pageNb--;
					if (pageNb < 0 || pageNb >= images.size()) {
						throw new NumberFormatException("invalid");
					}
					display(pageNb, rotation, true);
				} catch (NumberFormatException nfe) {
					page.setText(Integer.toString(index + 1));
				}
			}
		});

		JLabel maxPage = new JLabel(" of " + images.size());

		JButton next = new JButton(
				IconGenerator.get(Icon.arrow_right, Size.x32));
		next.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				next();
			}
		});

		JButton last = new JButton(
				IconGenerator.get(Icon.arrow_double_right, Size.x32));
		last.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				last();
			}
		});

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
				setZoom(newZoom, zoomSnapWidth);
			}
		});

		zoomBoxModel = new DefaultComboBoxModel<ZoomLevel>(
				ZoomLevel.values(true));
		JComboBox<ZoomLevel> zoomBox = new JComboBox<ZoomLevel>(zoomBoxModel);
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
							selectedZoomLevel.zoomSnapWidth);
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

					setZoom(pc / 100.0, newZoomSnapWidth);
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
				setZoom(newZoom, zoomSnapWidth);
			}

		});

		// Add to toolbar

		toolBar.add(first);
		toolBar.add(previous);
		toolBar.add(page);
		toolBar.add(maxPage);
		toolBar.add(next);
		toolBar.add(last);

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

	private void setZoom(double newZoom, boolean newZoomSnapWidth) {
		if (newZoom > 0) {
			zoomBoxModel.setSelectedItem(
					Integer.toString((int) Math.round(newZoom * 100)) + " %");
		}

		zoom = newZoom;
		zoomSnapWidth = newZoomSnapWidth;
		display(index, rotation, true);
	}

	private synchronized void display(int index, final Rotation rotation,
			final boolean resetScroll) {
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
					@Override
					protected ImageIcon doInBackground() throws Exception {
						BufferedImage image = ImageUtilsAwt.fromImage(img,
								rotation);
						BufferedImage resizedImage = ImageUtilsAwt.scaleImage(
								areaSize, image, zoom, zoomSnapWidth);

						currentZoom = (1.0 * resizedImage.getWidth())
								/ image.getWidth();

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
				area.requestFocus();
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
		this.addComponentListener(new ComponentAdapter() {
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
			updatePage();
			display(index, Rotation.NONE, true);
		}
	}

	private synchronized void previous() {
		index--;
		if (index < 0) {
			index = 0;
		} else {
			updatePage();
			display(index, Rotation.NONE, true);
		}
	}

	private synchronized void first() {
		index = 0;
		updatePage();
		display(index, Rotation.NONE, true);
	}

	private synchronized void last() {
		index = images.size() - 1;
		updatePage();
		display(index, Rotation.NONE, true);
	}

	private void updatePage() {
		page.setText(Integer.toString(index + 1));
	}
}
