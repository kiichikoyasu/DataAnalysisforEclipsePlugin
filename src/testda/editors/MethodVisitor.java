/**
 * 
 */
package testda.editors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
	private String packagePath;
	private StringBuilder sb;
	private boolean isInInstanceMethod;
	private Deque<StringBuilder> usingSBStack, finishSBStack;
	private Deque<Boolean> instanceFlagStack;
	
	public MethodVisitor(String outputFileName, String packagePath){
		super();
		this.outputFN = outputFileName;
		this.packagePath = packagePath;
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
			ITypeBinding classBind = node.resolveTypeBinding();
			// .new をつけるべき？本筋からはずれるので後回し
//			System.out.println(classBinding.getQualifiedName());
			if(classBind.isAnonymous()){
				/* 匿名クラスなので必ずスーパークラスかインターフェースがある */
				ITypeBinding scBind = classBind.getSuperclass();
				if(scBind != null){
					classBind = scBind;
				} else {
					/* 匿名クラスはインターフェースを1つしかimplementできないはず・・・ */
					classBind = classBind.getInterfaces()[0];
				}
			} 
			
			/* インスタンス化するクラスを特定したところで自分の書いたコードかどうか探る */
			while(classBind.isFromSource()){
				ITypeBinding scBind = classBind.getSuperclass();
				if(scBind != null){
					classBind = scBind;
				}else{
					ITypeBinding[] iBinds = classBind.getInterfaces();
					for(int i = 0; i < iBinds.length; ++i){
						if(!iBinds[i].isFromSource()){
							classBind = iBinds[i];
							break;
						}
					}
				}
			}
			
			/* Objectクラスまで戻ってしまった場合は元のクラスの情報にする */
			if(classBind.getSuperclass() == null){
				classBind = node.resolveTypeBinding();
				if(classBind.isAnonymous()){
					ITypeBinding scBind = classBind.getSuperclass();
					if(scBind != null){
						classBind = scBind;
					}else{
						classBind = classBind.getInterfaces()[0];
					}
				}
			}
			
			this.sb.append(classBind.getQualifiedName() + ".new" + " ");
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
			
/*			if(classBinding.isFromSource()){
				classBinding = classBinding.getSuperclass();
				if(classBinding != null){
					IMethodBinding[] mBindings = classBinding.getDeclaredMethods();
					for(int i = 0; i < mBindings.length; ++i){
						System.out.println(methodBinding.overrides(mBindings[i]));
					}
				}
			}*/
			
			/* 自分で書いてないコードまで辿る */
			while(classBinding.isFromSource()){
				ITypeBinding superClassBinding = classBinding.getSuperclass();
				boolean isOverride = false;
				if(superClassBinding != null){
					IMethodBinding[] mBindings = superClassBinding.getDeclaredMethods();
					for(int i = 0; i < mBindings.length; ++i){
						isOverride |= methodBinding.overrides(mBindings[i]);
					}
				}
				
				/* オーバーライドが見つかったら次の段階を探る */
				if(isOverride){
					classBinding = superClassBinding;
					continue;
				}
				
				/* スーパークラスにオーバーライドが無ければインターフェースを探す */
				ITypeBinding[] interfaceBindings = classBinding.getInterfaces();
				int j = 0;
				if(!isOverride){
					/* オーバーライドが見つからなければ探し続ける */
					for(j = 0; j < interfaceBindings.length && !isOverride; ++j){
						IMethodBinding[] mBindings = interfaceBindings[j].getDeclaredMethods();
						for(int k = 0; k < mBindings.length; ++k){
							isOverride |= methodBinding.overrides(mBindings[k]);
						}
					}
				}
				
				/* オーバーライドが見つかったら次の段階を探る */
				if(isOverride){
					classBinding = interfaceBindings[j - 1];
					continue;
				}
				
				/* オーバーライドされて無い場合はそこで終了 */
				/* クラスバインディングを元に戻してファイルに書き込む */
				if(!isOverride){
					classBinding = methodBinding.getDeclaringClass();
					break;
				}
			}
//			System.out.println(classBinding.isFromSource() + " " + classBinding.getBinaryName());
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

			/* 自分で書いてないコードまで辿る */
			while(classBinding.isFromSource()){
				ITypeBinding superClassBinding = classBinding.getSuperclass();
				boolean isOverride = false;
				if(superClassBinding != null){
					IMethodBinding[] mBindings = superClassBinding.getDeclaredMethods();
					for(int i = 0; i < mBindings.length; ++i){
						isOverride |= methodBinding.overrides(mBindings[i]);
					}
				}
				
				/* オーバーライドが見つかったら次の段階を探る */
				if(isOverride){
					classBinding = superClassBinding;
					continue;
				}
				
				/* スーパークラスにオーバーライドが無ければインターフェースを探す */
				ITypeBinding[] interfaceBindings = classBinding.getInterfaces();
				int j = 0;
				if(!isOverride){
					/* オーバーライドが見つからなければ探し続ける */
					for(j = 0; j < interfaceBindings.length && !isOverride; ++j){
						IMethodBinding[] mBindings = interfaceBindings[j].getDeclaredMethods();
						for(int k = 0; k < mBindings.length; ++k){
							isOverride |= methodBinding.overrides(mBindings[k]);
						}
					}
				}
				
				/* オーバーライドが見つかったら次の段階を探る */
				if(isOverride){
					classBinding = interfaceBindings[j - 1];
					continue;
				}
				
				/* オーバーライドされて無い場合はそこで終了 */
				/* クラスバインディングを元に戻してファイルに書き込む */
				if(!isOverride){
					classBinding = methodBinding.getDeclaringClass();
					break;
				}
			}
//			System.out.println(classBinding.getQualifiedName() + "." +  methodBinding.getName());
			this.sb.append(classBinding.getQualifiedName() + "." + methodBinding.getName() + " ");
		}
		super.endVisit(node);
	}
	
	public String getMethodInvocationList(){
		return this.sb.toString();
	}
	
	public void outputMethodInvocationList(){
		File file = new File(this.outputFN);
		
		try {
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write(this.sb.toString());
			
			bw.close();
			fw.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}

}
