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
public class BasicBlock {
	public AbstractSet<Integer> gen, kill;
	public ArrayList<BasicBlock> predecessor, successor;
	public ArrayList<BasicLine> lineElement;
	public int id;
	public int start, end;
	
	public BasicBlock(int startLineNumber){
		gen = new CopyOnWriteArraySet<Integer>();
		kill = new CopyOnWriteArraySet<Integer>();
		predecessor = new ArrayList<BasicBlock>();
		successor = new ArrayList<BasicBlock>();
		lineElement = new ArrayList<BasicLine>();
		start = startLineNumber;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("startLine:" + start + " ");
		sb.append("endLine:" + end + " ");
		
		sb.append("predecessor:[");
		for(BasicBlock pred : predecessor){
			if(pred.start == pred.end){
				sb.append(pred.start + ", ");
			}else{
				sb.append(pred.start + "-" + pred.end + ", ");
			}
		}
		if(predecessor.size() > 0){
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append("] ");
		
		sb.append("successor:[");
		for(BasicBlock succ : successor){
			if(succ.start == succ.end){
				sb.append(succ.start + ", ");
			}else{
				sb.append(succ.start + "-" + succ.end + ", ");
			}
		}
		if(successor.size() > 0){
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append("]");
		
		return sb.toString();
	}
}
