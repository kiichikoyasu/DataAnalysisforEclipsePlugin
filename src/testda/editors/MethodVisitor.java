/**
 * 
 */
package testda.editors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

/**
 * @author koyasukiichi
 *
 */
public class MethodVisitor extends ASTVisitor {
	
	private final static String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private String outputFN;
	private StringBuilder sb;
	private boolean isInInstanceMethod;
	
	public MethodVisitor(){
		this("output.txt");
	}
	
	public MethodVisitor(String outputFileName){
		super();
		this.outputFN = outputFileName;
		this.sb = new StringBuilder();
		this.isInInstanceMethod = false;
	}

	@Override
	public boolean visit(ConstructorInvocation node) {
		// TODO Auto-generated method stub
		return super.visit(node);
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		this.sb.append("< ");
		this.isInInstanceMethod = true;
		return super.visit(node);
	}

	@Override
	public boolean visit(MethodInvocation node) {
		// TODO Auto-generated method stub
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperConstructorInvocation node) {
		// TODO Auto-generated method stub
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		// TODO Auto-generated method stub
		return super.visit(node);
	}

	@Override
	public void endVisit(ConstructorInvocation node) {
		if(this.isInInstanceMethod){
			IMethodBinding methodBinding = node.resolveConstructorBinding();
			ITypeBinding classBinding = methodBinding.getDeclaringClass();
//			System.out.println(classBinding.getBinaryName() + "." +  methodBinding.getName());
			this.sb.append(classBinding.getBinaryName() + "." + methodBinding.getName() + " ");
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		this.sb.append(">" + LINE_SEPARATOR);
		this.isInInstanceMethod = false;
		super.endVisit(node);
	}

	@Override
	public void endVisit(MethodInvocation node) {
		if(this.isInInstanceMethod){
			IMethodBinding methodBinding = node.resolveMethodBinding();
			ITypeBinding classBinding = methodBinding.getDeclaringClass();
//			System.out.println(classBinding.getBinaryName() + "." +  methodBinding.getName());
			this.sb.append(classBinding.getBinaryName() + "." + methodBinding.getName() + " ");
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(SuperConstructorInvocation node) {
		if(this.isInInstanceMethod){
			IMethodBinding methodBinding = node.resolveConstructorBinding();
			ITypeBinding classBinding = methodBinding.getDeclaringClass();
//			System.out.println(classBinding.getBinaryName() + "." +  methodBinding.getName());
			this.sb.append(classBinding.getBinaryName() + "." + methodBinding.getName() + " ");
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(SuperMethodInvocation node) {
		if(this.isInInstanceMethod){
			IMethodBinding methodBinding = node.resolveMethodBinding();
			ITypeBinding classBinding = methodBinding.getDeclaringClass();
//			System.out.println(classBinding.getBinaryName() + "." +  methodBinding.getName());
			this.sb.append(classBinding.getBinaryName() + "." + methodBinding.getName() + " ");
		}
		super.endVisit(node);
	}
	
	public String getMethodInvocationList(){
		return this.sb.toString();
	}

}
