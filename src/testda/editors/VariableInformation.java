/**
 * 
 */
package testda.editors;

import java.util.AbstractSet;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * @author koyasukiichi
 *
 */
public class VariableInformation {
	private ITypeBinding binding;
	private AbstractSet<Integer> definition;
	private AbstractSet<Integer> using;
	
	public VariableInformation(ITypeBinding binding){
		this.binding = binding;
		definition = new CopyOnWriteArraySet<Integer>();
		using = new CopyOnWriteArraySet<Integer>();
	}
	
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		builder.append("Type:");
		builder.append(binding.getQualifiedName());
		builder.append(" Def:");
		builder.append(definition.toString());
		builder.append(" Use:");
		builder.append(using.toString());
		return builder.toString();
	}
	
	public void addToDef(int lineNumber){
		definition.add(lineNumber);
	}
	
	public void addToUse(int lineNumber){
		using.add(lineNumber);
	}
}
