/**
 * 
 */
package testda.editors;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

/**
 * @author koyasukiichi
 *
 */
public class TestDAASTVisitor extends ASTVisitor {
	
	private int ASTDepth = 0;
	private StringBuilder treeInfo;
//	private int NodeId = 0;
	private CompilationUnit unit;
	private ArrayList<BasicLine> lineList;
	private ArrayList<BasicBlock> blockList;
	
	private Deque<BasicLine> lineStack;
	private Deque<BasicBlock> blockStack;
	
	/* �e�m�[�h�ɓ������Ƃ��ɂ��̃m�[�h�����s�ڂ����X�V */
	private int currentLineNumber;
	private BasicLine currentLine;
	private BasicBlock currentBlock;
	
	private HashMap<IVariableBinding, VariableInformation> variableTable;
	
	public TestDAASTVisitor(){
		treeInfo = new StringBuilder();
		lineList = new ArrayList<BasicLine>();
		blockList = new ArrayList<BasicBlock>();
		lineStack = new ArrayDeque<BasicLine>();
		blockStack = new ArrayDeque<BasicBlock>();
		variableTable = new HashMap<IVariableBinding, VariableInformation>();
	}
	
	@Override
	public void preVisit(ASTNode node) {
		ASTDepth++;
		if(unit != null){
			treeInfo.append(unit.getLineNumber(node.getStartPosition()));
			for(int i = 0; i < ASTDepth; i++){
				treeInfo.append("  ");
			}
			treeInfo.append(ASTNode.nodeClassForType(node.getNodeType()).getSimpleName() + "\n");
			currentLineNumber = unit.getLineNumber(node.getStartPosition());
		}
	}

	@Override
	public void postVisit(ASTNode node) {
		ASTDepth--;
		currentLineNumber = unit.getLineNumber(node.getStartPosition() + node.getLength());
	}

	@Override
	public boolean visit(CompilationUnit node) {
		unit = node;
		treeInfo.append(unit.getJavaElement().getElementName() + "\n");
		treeInfo.append(unit.getLineNumber(node.getStartPosition()));
		treeInfo.append(ASTNode.nodeClassForType(node.getNodeType()).getSimpleName() + "\n");
		return super.visit(node);
	}

	@Override
	public void endVisit(CompilationUnit node) {
		
		/* �Ō�ɂ������ȃf�[�^���C�� */
		/* ���[�v���A��������̂�start��end���t�s����u���b�N������i�C���[�W�Ƃ��Ă̓��[�v���m�̊Ԃ̖��̍s�̃u���b�N�j */
		for(int i = 0; i < blockList.size(); ++i){
			BasicBlock block = blockList.get(i);
			if(block.predecessor.size() == 1
					&& block.successor.size() == 1
					&& block.predecessor.get(0).end < block.successor.get(0).start){
					block.predecessor.get(0).successor.add(block.successor.get(0));
					block.successor.get(0).predecessor.add(block.predecessor.get(0));
					block.predecessor.get(0).successor.remove(block);
					block.successor.get(0).predecessor.remove(block);
					blockList.remove(block);
					i--;
			}
		}
/*		for(BasicBlock block : blockList){
			if(block.start > block.end){
				block.predecessor.get(0).successor.add(block.successor.get(0));
				block.successor.get(0).predecessor.add(block.predecessor.get(0));
				block.predecessor.get(0).successor.remove(block);
				block.successor.get(0).predecessor.remove(block);
				//concurrent error
//				blockList.remove(block);
				}
		}*/
		super.endVisit(node);
	}

	@Override
	public boolean visit(QualifiedName node) {
		IBinding binding = node.resolveBinding();
		switch(binding.getKind()){
		case IBinding.VARIABLE:
			nameExpression(node, (IVariableBinding)binding);
			break;
		default:
			break;	
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(SimpleName node) {
		IBinding binding = node.resolveBinding();
		switch(binding.getKind()){
		case IBinding.VARIABLE:
			nameExpression(node, (IVariableBinding)binding);
			break;
		default:
			break;	
		}
		return super.visit(node);
	}

	private void nameExpression(Name node, IVariableBinding variableBinding) {
		int lineNumber = unit.getLineNumber(node.getStartPosition());
		ASTNode parent = node.getParent();
		if(parent instanceof Name){
			/* �e�m�[�h��Name�̃C���X�^���X�̏ꍇ�͐e�m�[�h�����������łɂ��Ă���͂� */
		}else if(parent instanceof VariableDeclaration){
			if(!variableTable.containsKey(variableBinding)){
				VariableInformation info = new VariableInformation(node.resolveTypeBinding());
				if(((VariableDeclaration) parent).getInitializer() != null){
					info.addToDef(lineNumber);
				}
				variableTable.put(variableBinding, info);
			}
		}else if(parent instanceof Assignment){
			Expression left = ((Assignment) parent).getLeftHandSide();
			VariableInformation info = variableTable.get(variableBinding);
			if(info == null){
				/* �t�B�[���h�ϐ��Ȃǂŉ�͒��̃N���X���\�b�h���Ő錾���ĂȂ��ϐ����o��ꍇ������̂�Table�ɂȂ����̂͐V���ɍ�� */
				info = new VariableInformation(node.resolveTypeBinding());
				variableTable.put(variableBinding, info);
			}
			if(((Assignment) parent).getOperator() == Assignment.Operator.ASSIGN){
				if(node.equals(left)){
					info.addToDef(lineNumber);
				}else{
					info.addToUse(lineNumber);
				}
			}else{
				info.addToDef(lineNumber);
				info.addToUse(lineNumber);
			}
		}else if(parent instanceof PrefixExpression){
			VariableInformation info = variableTable.get(variableBinding);
			if(info == null){
				info = new VariableInformation(node.resolveTypeBinding());
				variableTable.put(variableBinding, info);
			}
			if(((PrefixExpression) parent).getOperator() == PrefixExpression.Operator.INCREMENT ||
					((PrefixExpression) parent).getOperator() == PrefixExpression.Operator.DECREMENT){
				info.addToDef(lineNumber);
				info.addToUse(lineNumber);
			}else{
				info.addToUse(lineNumber);
			}
		}else{
			VariableInformation info = variableTable.get(variableBinding);
			if(info == null){
				info = new VariableInformation(node.resolveTypeBinding());
				variableTable.put(variableBinding, info);
			}
			info.addToUse(lineNumber);
		}
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		currentLine = new BasicLine(unit.getLineNumber(node.getStartPosition()));
		currentBlock = new BasicBlock(currentLine.getLineNumber());
		return super.visit(node);
	}

	@Override
	public void endVisit(MethodDeclaration node) {
//		currentLineNumber = unit.getLineNumber(node.getStartPosition() + node.getLength());
		currentBlock.end = currentLineNumber;
		blockList.add(currentBlock);
		super.endVisit(node);
	}

	@Override
	public boolean visit(Block node) {
		ASTNode parent = node.getParent();
		BasicBlock newBlock;
		switch(parent.getNodeType()){
		case ASTNode.FOR_STATEMENT:
			/* for�ƃu���b�N�̉��s���ǂ��Ȃ��Ă邩�l���悤�Ƃ������ǐ��P���ł���Ȍ��Â炢�R�[�h�͂قƂ�ǂȂ��ƍl���悤 */
			/* �ǋL:eclipse��command + shift + f �Ő��`�ł���̂ł�����{���Ă���ɂ��� */
			
			/* for���̎�O�ŋ�؂� */
			currentBlock.end = currentLineNumber - 1;
			newBlock = new BasicBlock(currentLineNumber);
			makeBlockDependence(currentBlock, newBlock);
			blockList.add(currentBlock);
			currentBlock = newBlock;
			
			/* for���̌�ŋ�؂� */
			currentBlock.end = currentLineNumber;
			newBlock = new BasicBlock(currentLineNumber + 1);
			makeBlockDependence(currentBlock, newBlock);
			blockStack.addFirst(currentBlock);			
			blockList.add(currentBlock);
			currentBlock = newBlock;
			break;
		case ASTNode.WHILE_STATEMENT:
			/* ��{�I�ɂ�for���Ɠ����͂� */
			/* while���̎�O�ŋ�؂� */
			currentBlock.end = currentLineNumber - 1;
			newBlock = new BasicBlock(currentLineNumber);
			makeBlockDependence(currentBlock, newBlock);
			blockList.add(currentBlock);
			currentBlock = newBlock;
			
			/* while���̌�ŋ�؂� */
			currentBlock.end = currentLineNumber;
			newBlock = new BasicBlock(currentLineNumber + 1);
			makeBlockDependence(currentBlock, newBlock);
			blockStack.addFirst(currentBlock);			
			blockList.add(currentBlock);
			currentBlock = newBlock;
			break;
		default:
			break;
		}
		return super.visit(node);
	}

	@Override
	public void endVisit(Block node) {
		ASTNode parent = node.getParent();
		BasicBlock newBlock, stackedBlock;
		switch(parent.getNodeType()){
		case ASTNode.FOR_STATEMENT:
			currentLineNumber++;
			currentBlock.end = currentLineNumber;
			newBlock = new BasicBlock(currentLineNumber + 1);
			stackedBlock = blockStack.removeFirst();
			makeBlockDependence(currentBlock, stackedBlock);
			makeBlockDependence(stackedBlock, newBlock);
			
			blockList.add(currentBlock);
			currentBlock = newBlock;
			break;
		case ASTNode.WHILE_STATEMENT:
			currentLineNumber++;
			currentBlock.end = currentLineNumber;
			newBlock = new BasicBlock(currentLineNumber + 1);
			stackedBlock = blockStack.removeFirst();
			makeBlockDependence(currentBlock, stackedBlock);
			makeBlockDependence(stackedBlock, newBlock);
			
			blockList.add(currentBlock);
			currentBlock = newBlock;
			break;
		default:
			break;
		}
		super.endVisit(node);
	}

	private void makeBlockDependence(BasicBlock predBlock, BasicBlock succBlock){
		predBlock.successor.add(succBlock);
		succBlock.predecessor.add(predBlock);
	}
	
	public void printTable(){
		for(IVariableBinding key : variableTable.keySet()){
			System.out.println(key.getName() + " = {" + variableTable.get(key).toString() + "}");
		}
	}
	
	@Override
	public boolean visit(MethodInvocation node) {
/*		ASTNode expression = node.getExpression();
		if(expression != null){
			System.out.println("�I�u�W�F�N�g:" + expression);
		}
		List arguments = node.arguments();
		if(arguments != null){
			System.out.println("����:" + arguments.toString());
		}*/
		return super.visit(node);
	}

	public void printTreeInfo(){
		System.out.println(treeInfo.toString());
	}

	public void printBlockList(){
		for(BasicBlock block : blockList){
			System.out.println(block.toString());
		}
	}

}
