/**
 * 
 */
package testda.editors;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author koyasukiichi
 * 
 */
public class BasicLine {
	public AbstractSet<Integer> gen, kill;
	public ArrayList<BasicLine> predecessor, successor;
	/* 行番号 */
	private int lineNumber;

	/* elementをもつフィールドがいるかな？ */

	public BasicLine(int lineNumber) {
		gen = new CopyOnWriteArraySet<Integer>();
		kill = new CopyOnWriteArraySet<Integer>();
		predecessor = new ArrayList<BasicLine>();
		successor = new ArrayList<BasicLine>();
		this.lineNumber = lineNumber;
	}

	public int getLineNumber() {
		return lineNumber;
	}
}
