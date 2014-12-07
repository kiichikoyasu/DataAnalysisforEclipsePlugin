/**
 * 
 */
package testda.editors;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

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
	
	/* 各ノードに入ったときにそのノードが何行目かを更新 */
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
		
		/* 最後におかしなデータを修正 */
		/* ループが連続するものはstartとendが逆行するブロックがある（イメージとしてはループ同士の間の無の行のブロック） */
		boolean removeStrainData = false;
		if(removeStrainData)
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
			/* 親ノードもNameのインスタンスの場合は親ノードが処理をすでにしているはず */
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
				/* フィールド変数などで解析中のクラスメソッド等で宣言してない変数が出る場合もあるのでTableにないものは新たに作る */
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
		return super.visit(node);
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
	

	public void printTreeInfo(){
		System.out.println(treeInfo.toString());
	}

	public void printBlockList(){
		for(BasicBlock block : blockList){
			System.out.println(block.toString());
		}
	}

}
