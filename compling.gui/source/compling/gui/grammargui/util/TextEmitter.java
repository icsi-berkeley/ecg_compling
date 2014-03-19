package compling.gui.grammargui.util;

import java.util.Formatter;

public class TextEmitter {

	protected StringBuilder buffer;
	protected Formatter formatter;
	protected int ws;

	public TextEmitter(int ws) {
		this.ws = ws;
		reset();
	}

	public void emit(int level, String content) {
		buffer.append(ws(2 * level) + content);
	}

	public void say(int level, String format, Object... args) {
		buffer.append(ws(2 * level));
		formatter.format(format, args);
	}

	public void sayln(int level, String format, Object... args) {
		say(level, format, args);
		buffer.append('\n');
	}

	public void sayln(String format, Object... args) {
		sayln(0, format, args);
	}

	public void say(String format, Object... args) {
		say(0, format, args);
	}

	protected StringBuilder ws(int len) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			sb.append(" ");
		}
		return sb;
	}

	public String getOutput() {
		return buffer.toString();
	}

	public void reset() {
		buffer = new StringBuilder();
		formatter = new Formatter(buffer);
	}
}
