/**
 * 
 */
package testda.editors;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
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
	private Deque<StringBuilder> usingSBStack, finishSBStack;
	private Deque<Boolean> instanceFlagStack;
	
	public MethodVisitor(){
		this("output.txt");
	}
	
	public MethodVisitor(String outputFileName){
		super();
		this.outputFN = outputFileName;
		this.sb = new StringBuilder();
		this.isInInstanceMethod = false;
		this.usingSBStack = new ArrayDeque<StringBuilder>();
		this.finishSBStack = new ArrayDeque<StringBuilder>();
		this.instanceFlagStack = new ArrayDeque<Boolean>();
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		/* 今の状態を退避させる */
		this.instanceFlagStack.addFirst(this.isInInstanceMethod);
		this.usingSBStack.addFirst(this.sb);
		/* 新しく状態を作る */
		this.isInInstanceMethod = false;
		this.sb = new StringBuilder();
		return super.visit(node);
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		// TODO Auto-generated method stub
		return super.visit(node);
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
	public void endVisit(AnonymousClassDeclaration node) {
		this.finishSBStack.addFirst(this.sb);
		/* 匿名クラスに入る前の状態に戻す */
		this.isInInstanceMethod = this.instanceFlagStack.removeFirst();
		this.sb = this.usingSBStack.removeFirst();
		super.endVisit(node);
	}

	@Override
	public void endVisit(ClassInstanceCreation node) {
		if(this.isInInstanceMethod){
			ITypeBinding classBinding = node.resolveTypeBinding();
			// .new をつけるべき？本筋からはずれるので後回し
//			System.out.println(classBinding.getQualifiedName());
			if(classBinding.isAnonymous()){
				/* 無名クラスなので・・・ */
				ITypeBinding bind = classBinding.getSuperclass();
				if(bind != null){
					this.sb.append(bind.getQualifiedName() + " ");
				} else {
					bind = classBinding.getInterfaces()[0];
					this.sb.append(bind.getQualifiedName() + " ");
				}
			} else {
				this.sb.append(classBinding.getQualifiedName() + " ");
			}
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(ConstructorInvocation node) {
		if(this.isInInstanceMethod){
			IMethodBinding methodBinding = node.resolveConstructorBinding();
			ITypeBinding classBinding = methodBinding.getDeclaringClass();
//			System.out.println(classBinding.getQualifiedName() + "." +  methodBinding.getName());
			this.sb.append(classBinding.getQualifiedName() + "." + methodBinding.getName() + " ");
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		this.sb.append(">" + LINE_SEPARATOR);
		this.isInInstanceMethod = false;
		/* 今見ていたメソッド内に匿名クラスの中でメソッドがあったのであれば後にそれをつける */
		while(this.finishSBStack.peekFirst() != null){
			this.sb.append(this.finishSBStack.removeFirst());
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(MethodInvocation node) {
		if(this.isInInstanceMethod){
			IMethodBinding methodBinding = node.resolveMethodBinding();
			ITypeBinding classBinding = methodBinding.getDeclaringClass();
//			System.out.println(classBinding.getQualifiedName() + "." +  methodBinding.getName());
			this.sb.append(classBinding.getQualifiedName() + "." + methodBinding.getName() + " ");
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(SuperConstructorInvocation node) {
		if(this.isInInstanceMethod){
			IMethodBinding methodBinding = node.resolveConstructorBinding();
			ITypeBinding classBinding = methodBinding.getDeclaringClass();
//			System.out.println(classBinding.getQualifiedName() + "." +  methodBinding.getName());
			this.sb.append(classBinding.getQualifiedName() + "." + methodBinding.getName() + " ");
		}
		super.endVisit(node);
	}

	@Override
	public void endVisit(SuperMethodInvocation node) {
		if(this.isInInstanceMethod){
			IMethodBinding methodBinding = node.resolveMethodBinding();
			ITypeBinding classBinding = methodBinding.getDeclaringClass();
//			System.out.println(classBinding.getQualifiedName() + "." +  methodBinding.getName());
			this.sb.append(classBinding.getQualifiedName() + "." + methodBinding.getName() + " ");
		}
		super.endVisit(node);
	}
	
	public String getMethodInvocationList(){
		return this.sb.toString();
	}

}
