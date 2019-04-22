package be.nikiroo.fanfix.library;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.server.ConnectActionClientObject;

/**
 * This {@link BasicLibrary} will access a remote server to list the available
 * stories, and download the ones you try to load to the local directory
 * specified in the configuration.
 * 
 * @author niki
 */
public class RemoteLibrary extends BasicLibrary {
	private String host;
	private int port;
	private final String key;

	/**
	 * Create a {@link RemoteLibrary} linked to the given server.
	 * 
	 * @param key
	 *            the key that will allow us to exchange information with the
	 *            server
	 * @param host
	 *            the host to contact or NULL for localhost
	 * @param port
	 *            the port to contact it on
	 */
	public RemoteLibrary(String key, String host, int port) {
		this.key = key;
		this.host = host;
		this.port = port;
	}

	@Override
	public String getLibraryName() {
		return host + ":" + port;
	}

	@Override
	public Status getStatus() {
		final Status[] result = new Status[1];

		result[0] = Status.INVALID;

		try {
			Instance.getTraceHandler().trace("Getting remote lib status...");
			new ConnectActionClientObject(host, port, false) {
				@Override
				public void action(Version serverVersion) throws Exception {
					try {
						Object rep = sendCmd(this, new Object[] { "PING" });

						if ("PONG".equals(rep)) {
							result[0] = Status.READY;
						} else {
							result[0] = Status.UNAUTORIZED;
						}
					} catch (IllegalArgumentException e) {
						result[0] = Status.UNAUTORIZED;
					}
				}

				@Override
				protected void onError(Exception e) {
					result[0] = Status.UNAVAILABLE;
				}
			}.connect();
		} catch (UnknownHostException e) {
			result[0] = Status.INVALID;
		} catch (IllegalArgumentException e) {
			result[0] = Status.INVALID;
		} catch (Exception e) {
			result[0] = Status.UNAVAILABLE;
		}

		Instance.getTraceHandler().trace("Remote lib status: " + result[0]);
		return result[0];
	}

	@Override
	public Image getCover(final String luid) {
		final Image[] result = new Image[1];

		try {
			new ConnectActionClientObject(host, port, false) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Object rep = sendCmd(this,
							new Object[] { "GET_COVER", luid });
					result[0] = (Image) rep;
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return result[0];
	}

	@Override
	public Image getCustomSourceCover(final String source) {
		return getCustomCover(source, "SOURCE");
	}

	@Override
	public Image getCustomAuthorCover(final String author) {
		return getCustomCover(author, "AUTHOR");
	}

	// type: "SOURCE" or "AUTHOR"
	private Image getCustomCover(final String source, final String type) {
		final Image[] result = new Image[1];

		try {
			new ConnectActionClientObject(host, port, false) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Object rep = sendCmd(this, new Object[] {
							"GET_CUSTOM_COVER", type, source });
					result[0] = (Image) rep;
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return result[0];
	}

	@Override
	public synchronized Story getStory(final String luid, Progress pg) {
		final Progress pgF = pg;
		final Story[] result = new Story[1];

		try {
			new ConnectActionClientObject(host, port, false) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Progress pg = pgF;
					if (pg == null) {
						pg = new Progress();
					}

					Object rep = sendCmd(this,
							new Object[] { "GET_STORY", luid });

					MetaData meta = null;
					if (rep instanceof MetaData) {
						meta = (MetaData) rep;
						if (meta.getWords() <= Integer.MAX_VALUE) {
							pg.setMinMax(0, (int) meta.getWords());
						}
					}

					List<Object> list = new ArrayList<Object>();
					for (Object obj = send(null); obj != null; obj = send(null)) {
						list.add(obj);
						pg.add(1);
					}

					result[0] = RemoteLibraryServer.rebuildStory(list);
					pg.done();
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return result[0];
	}

	@Override
	public synchronized Story save(final Story story, final String luid,
			Progress pg) throws IOException {
		final String[] luidSaved = new String[1];
		Progress pgSave = new Progress();
		Progress pgRefresh = new Progress();
		if (pg == null) {
			pg = new Progress();
		}

		pg.setMinMax(0, 10);
		pg.addProgress(pgSave, 9);
		pg.addProgress(pgRefresh, 1);

		final Progress pgF = pgSave;

		new ConnectActionClientObject(host, port, false) {
			@Override
			public void action(Version serverVersion) throws Exception {
				Progress pg = pgF;
				if (story.getMeta().getWords() <= Integer.MAX_VALUE) {
					pg.setMinMax(0, (int) story.getMeta().getWords());
				}

				sendCmd(this, new Object[] { "SAVE_STORY", luid });

				List<Object> list = RemoteLibraryServer.breakStory(story);
				for (Object obj : list) {
					send(obj);
					pg.add(1);
				}

				luidSaved[0] = (String) send(null);

				pg.done();
			}

			@Override
			protected void onError(Exception e) {
				Instance.getTraceHandler().error(e);
			}
		}.connect();

		// because the meta changed:
		MetaData meta = getInfo(luidSaved[0]);
		if (story.getMeta().getClass() != null) {
			// If already available locally:
			meta.setCover(story.getMeta().getCover());
		} else {
			// If required:
			meta.setCover(getCover(meta.getLuid()));
		}
		story.setMeta(meta);

		pg.done();

		return story;
	}

	@Override
	public synchronized void delete(final String luid) throws IOException {
		new ConnectActionClientObject(host, port, false) {
			@Override
			public void action(Version serverVersion) throws Exception {
				sendCmd(this, new Object[] { "DELETE_STORY", luid });
			}

			@Override
			protected void onError(Exception e) {
				Instance.getTraceHandler().error(e);
			}
		}.connect();
	}

	@Override
	public void setSourceCover(final String source, final String luid) {
		setCover(source, luid, "SOURCE");
	}

	@Override
	public void setAuthorCover(final String author, final String luid) {
		setCover(author, luid, "AUTHOR");
	}

	// type = "SOURCE" | "AUTHOR"
	private void setCover(final String value, final String luid,
			final String type) {
		try {
			new ConnectActionClientObject(host, port, false) {
				@Override
				public void action(Version serverVersion) throws Exception {
					sendCmd(this,
							new Object[] { "SET_COVER", type, value, luid });
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	@Override
	// Could work (more slowly) without it
	public Story imprt(final URL url, Progress pg) throws IOException {
		// Import the file locally if it is actually a file
		if (url == null || url.getProtocol().equalsIgnoreCase("file")) {
			return super.imprt(url, pg);
		}

		// Import it remotely if it is an URL

		if (pg == null) {
			pg = new Progress();
		}

		pg.setMinMax(0, 2);
		Progress pgImprt = new Progress();
		Progress pgGet = new Progress();
		pg.addProgress(pgImprt, 1);
		pg.addProgress(pgGet, 1);

		final Progress pgF = pgImprt;
		final String[] luid = new String[1];

		try {
			new ConnectActionClientObject(host, port, false) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Progress pg = pgF;

					Object rep = sendCmd(this,
							new Object[] { "IMPORT", url.toString() });

					while (true) {
						if (!RemoteLibraryServer.updateProgress(pg, rep)) {
							break;
						}

						rep = send(null);
					}

					pg.done();
					luid[0] = (String) rep;
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}

		if (luid[0] == null) {
			throw new IOException("Remote failure");
		}

		Story story = getStory(luid[0], pgGet);
		pgGet.done();

		pg.done();
		return story;
	}

	@Override
	// Could work (more slowly) without it
	protected synchronized void changeSTA(final String luid,
			final String newSource, final String newTitle,
			final String newAuthor, Progress pg) throws IOException {
		final Progress pgF = pg == null ? new Progress() : pg;

		try {
			new ConnectActionClientObject(host, port, false) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Progress pg = pgF;

					Object rep = sendCmd(this, new Object[] { "CHANGE_STA",
							luid, newSource, newTitle, newAuthor });
					while (true) {
						if (!RemoteLibraryServer.updateProgress(pg, rep)) {
							break;
						}

						rep = send(null);
					}
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	@Override
	public synchronized File getFile(final String luid, Progress pg) {
		throw new java.lang.InternalError(
				"Operation not supportorted on remote Libraries");
	}

	/**
	 * Stop the server.
	 */
	public void exit() {
		try {
			new ConnectActionClientObject(host, port, false) {
				@Override
				public void action(Version serverVersion) throws Exception {
					sendCmd(this, new Object[] { "EXIT" });
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	@Override
	public synchronized MetaData getInfo(String luid) {
		List<MetaData> metas = getMetasList(luid, null);
		if (!metas.isEmpty()) {
			return metas.get(0);
		}

		return null;
	}

	@Override
	protected List<MetaData> getMetas(Progress pg) {
		return getMetasList("*", pg);
	}

	@Override
	protected void updateInfo(MetaData meta) {
		// Will be taken care of directly server side
	}

	@Override
	protected void invalidateInfo(String luid) {
		// Will be taken care of directly server side
	}

	// The following methods are only used by Save and Delete in BasicLibrary:

	@Override
	protected int getNextId() {
		throw new java.lang.InternalError("Should not have been called");
	}

	@Override
	protected void doDelete(String luid) throws IOException {
		throw new java.lang.InternalError("Should not have been called");
	}

	@Override
	protected Story doSave(Story story, Progress pg) throws IOException {
		throw new java.lang.InternalError("Should not have been called");
	}

	//

	/**
	 * Return the meta of the given story or a list of all known metas if the
	 * luid is "*".
	 * <p>
	 * Will not get the covers.
	 * 
	 * @param luid
	 *            the luid of the story or *
	 * @param pg
	 *            the optional progress
	 * 
	 * 
	 * @return the metas
	 */
	private List<MetaData> getMetasList(final String luid, Progress pg) {
		final Progress pgF = pg;
		final List<MetaData> metas = new ArrayList<MetaData>();

		try {
			new ConnectActionClientObject(host, port, false) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Progress pg = pgF;
					if (pg == null) {
						pg = new Progress();
					}

					Object rep = sendCmd(this, new Object[] { "GET_METADATA",
							luid });

					while (true) {
						if (!RemoteLibraryServer.updateProgress(pg, rep)) {
							break;
						}

						rep = send(null);
					}

					if (rep instanceof MetaData[]) {
						for (MetaData meta : (MetaData[]) rep) {
							metas.add(meta);
						}
					} else if (rep != null) {
						metas.add((MetaData) rep);
					}
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return metas;
	}

	// IllegalArgumentException if key is bad
	private Object sendCmd(ConnectActionClientObject action, Object[] params)
			throws IOException, NoSuchFieldException, NoSuchMethodException,
			ClassNotFoundException {
		Object rep = action.send(params);

		String hash = hashKey(key, "" + rep);
		return action.send(hash);
	}

	/**
	 * Return a hash that corresponds to the given key and the given random
	 * value.
	 * 
	 * @param key
	 *            the key (the secret)
	 * 
	 * @param random
	 *            the random value
	 * 
	 * @return a hash that was computed using both
	 */
	static String hashKey(String key, String random) {
		return StringUtils.getMd5Hash(key + " <==> " + random);
	}
}
