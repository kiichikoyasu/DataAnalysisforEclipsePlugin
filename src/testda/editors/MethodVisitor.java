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
		/* ���̏�Ԃ�ޔ������� */
		this.instanceFlagStack.addFirst(this.isInInstanceMethod);
		this.usingSBStack.addFirst(this.sb);
		/* �V������Ԃ���� */
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
		/* �����N���X�ɓ���O�̏�Ԃɖ߂� */
		this.isInInstanceMethod = this.instanceFlagStack.removeFirst();
		this.sb = this.usingSBStack.removeFirst();
		super.endVisit(node);
	}

	@Override
	public void endVisit(ClassInstanceCreation node) {
		if(this.isInInstanceMethod){
			ITypeBinding classBind = node.resolveTypeBinding();
			// .new ������ׂ��H�{�؂���͂����̂Ō��
//			System.out.println(classBinding.getQualifiedName());
			if(classBind.isAnonymous()){
				/* �����N���X�Ȃ̂ŕK���X�[�p�[�N���X���C���^�[�t�F�[�X������ */
				ITypeBinding scBind = classBind.getSuperclass();
				if(scBind != null){
					classBind = scBind;
				} else {
					/* �����N���X�̓C���^�[�t�F�[�X��1����implement�ł��Ȃ��͂��E�E�E */
					classBind = classBind.getInterfaces()[0];
				}
			} 
			
			/* �C���X�^���X������N���X����肵���Ƃ���Ŏ����̏������R�[�h���ǂ����T�� */
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
			
			/* Object�N���X�܂Ŗ߂��Ă��܂����ꍇ�͌��̃N���X�̏��ɂ��� */
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
		/* �����Ă������\�b�h���ɓ����N���X�̒��Ń��\�b�h���������̂ł���Ό�ɂ�������� */
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
			
			/* �����ŏ����ĂȂ��R�[�h�܂ŒH�� */
			while(classBinding.isFromSource()){
				ITypeBinding superClassBinding = classBinding.getSuperclass();
				boolean isOverride = false;
				if(superClassBinding != null){
					IMethodBinding[] mBindings = superClassBinding.getDeclaredMethods();
					for(int i = 0; i < mBindings.length; ++i){
						isOverride |= methodBinding.overrides(mBindings[i]);
					}
				}
				
				/* �I�[�o�[���C�h�����������玟�̒i�K��T�� */
				if(isOverride){
					classBinding = superClassBinding;
					continue;
				}
				
				/* �X�[�p�[�N���X�ɃI�[�o�[���C�h��������΃C���^�[�t�F�[�X��T�� */
				ITypeBinding[] interfaceBindings = classBinding.getInterfaces();
				int j = 0;
				if(!isOverride){
					/* �I�[�o�[���C�h��������Ȃ���ΒT�������� */
					for(j = 0; j < interfaceBindings.length && !isOverride; ++j){
						IMethodBinding[] mBindings = interfaceBindings[j].getDeclaredMethods();
						for(int k = 0; k < mBindings.length; ++k){
							isOverride |= methodBinding.overrides(mBindings[k]);
						}
					}
				}
				
				/* �I�[�o�[���C�h�����������玟�̒i�K��T�� */
				if(isOverride){
					classBinding = interfaceBindings[j - 1];
					continue;
				}
				
				/* �I�[�o�[���C�h����Ė����ꍇ�͂����ŏI�� */
				/* �N���X�o�C���f�B���O�����ɖ߂��ăt�@�C���ɏ������� */
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

			/* �����ŏ����ĂȂ��R�[�h�܂ŒH�� */
			while(classBinding.isFromSource()){
				ITypeBinding superClassBinding = classBinding.getSuperclass();
				boolean isOverride = false;
				if(superClassBinding != null){
					IMethodBinding[] mBindings = superClassBinding.getDeclaredMethods();
					for(int i = 0; i < mBindings.length; ++i){
						isOverride |= methodBinding.overrides(mBindings[i]);
					}
				}
				
				/* �I�[�o�[���C�h�����������玟�̒i�K��T�� */
				if(isOverride){
					classBinding = superClassBinding;
					continue;
				}
				
				/* �X�[�p�[�N���X�ɃI�[�o�[���C�h��������΃C���^�[�t�F�[�X��T�� */
				ITypeBinding[] interfaceBindings = classBinding.getInterfaces();
				int j = 0;
				if(!isOverride){
					/* �I�[�o�[���C�h��������Ȃ���ΒT�������� */
					for(j = 0; j < interfaceBindings.length && !isOverride; ++j){
						IMethodBinding[] mBindings = interfaceBindings[j].getDeclaredMethods();
						for(int k = 0; k < mBindings.length; ++k){
							isOverride |= methodBinding.overrides(mBindings[k]);
						}
					}
				}
				
				/* �I�[�o�[���C�h�����������玟�̒i�K��T�� */
				if(isOverride){
					classBinding = interfaceBindings[j - 1];
					continue;
				}
				
				/* �I�[�o�[���C�h����Ė����ꍇ�͂����ŏI�� */
				/* �N���X�o�C���f�B���O�����ɖ߂��ăt�@�C���ɏ������� */
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
