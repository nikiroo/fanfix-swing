package be.nikiroo.fanfix_swing.gui.viewer;

import java.io.IOException;
import java.util.Arrays;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput;

/**
 * This class can export a chapter into HTML3 code ready for Java Swing support.
 * 
 * @author niki
 */
class ViewerTextOutput {
	private StringBuilder builder;
	private BasicOutput output;
	private Story fakeStory;
	private boolean chapterName;

	/**
	 * Create a new {@link ViewerTextOutput} that will convert a {@link Chapter}
	 * into HTML3 suited for Java Swing.
	 */
	public ViewerTextOutput() {
		builder = new StringBuilder();
		fakeStory = new Story();

		output = new BasicOutput() {
			private boolean paraInQuote;

			@Override
			protected void writeChapterHeader(Chapter chap) throws IOException {
				builder.append("<HTML style='line-height: normal;'>");

				if (chapterName) {
					builder.append("<H1>");
					builder.append("Chapter ");
					builder.append(chap.getNumber());
					if (chap.getName() != null
							&& !chap.getName().trim().isEmpty()) {
						builder.append(": ");
						builder.append(chap.getName());
					}
					builder.append("</H1>");
				}

				builder.append("<DIV align='justify'>");
			}

			@Override
			protected void writeChapterFooter(Chapter chap) throws IOException {
				if (paraInQuote) {
					builder.append("</DIV>");
				}
				paraInQuote = false;

				builder.append("</DIV>");
				builder.append("</HTML>");
			}

			@Override
			protected void writeParagraph(Paragraph para) throws IOException {
				if ((para.getType() == ParagraphType.QUOTE) == !paraInQuote) {
					paraInQuote = !paraInQuote;
					if (paraInQuote) {
						builder.append("<BR>");
						builder.append("<DIV>");
					} else {
						builder.append("</DIV>");
						builder.append("<BR>");
					}
				}

				switch (para.getType()) {
				case NORMAL:
					builder.append("&nbsp;&nbsp;&nbsp;&nbsp;");
					builder.append(decorateText(para.getContent()));
					builder.append("<BR>");
					break;
				case BLANK:
					builder.append("<BR><BR>");
					break;
				case BREAK:
					builder.append("<BR><P COLOR='#7777DD' ALIGN='CENTER'><B>");
					builder.append("* * *");
					builder.append("</B></P><BR><BR>");
					break;
				case QUOTE:
					builder.append("<DIV>");
					builder.append("&nbsp;&nbsp;&nbsp;&nbsp;");
					builder.append("&mdash;&nbsp;");
					builder.append(decorateText(para.getContent()));
					builder.append("</DIV>");

					break;
				case IMAGE:
				}
			}

			@Override
			protected String enbold(String word) {
				return "<B COLOR='#7777DD'>" + word + "</B>";
			}

			@Override
			protected String italize(String word) {
				return "<I COLOR='GRAY'>" + word + "</I>";
			}
		};
	}

	/**
	 * Convert the chapter into HTML3 code.
	 * 
	 * @param chap
	 *            the {@link Chapter} to convert
	 * @param chapterName
	 *            display the chapter name
	 * 
	 * @return HTML3 code tested with Java Swing
	 */
	public String convert(Chapter chap, boolean chapterName) {
		this.chapterName = chapterName;
		builder.setLength(0);
		try {
			fakeStory.setChapters(Arrays.asList(chap));
			output.process(fakeStory, null, null);
		} catch (IOException e) {
			Instance.getInstance().getTraceHandler().error(e);
		}
		return builder.toString();
	}
}
