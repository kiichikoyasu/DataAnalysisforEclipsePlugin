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
	public boolean visit(WhileStatement node) {
		/* whileの前で切る */
		currentBlock.end = currentLineNumber - 1;
		blockList.add(currentBlock);
		BasicBlock whileBlock = new BasicBlock(currentLineNumber);
		makeBlockDependence(currentBlock, whileBlock);
		/* 条件の終わりの次の行で切る  */
		Expression condition = node.getExpression();
		whileBlock.end = unit.getLineNumber(condition.getStartPosition() + condition.getLength());
		blockList.add(whileBlock);
		/* while文はendvisitで使うのでスタックにpush（while文が終わったら条件に戻ってくるのを制御構造で表すため） */
		blockStack.addFirst(whileBlock);
		/* whileの中身のブロック */
		BasicBlock bodyBlock = new BasicBlock(unit.getLineNumber(condition.getStartPosition() + condition.getLength()) + 1);
		makeBlockDependence(whileBlock, bodyBlock);
		currentBlock = bodyBlock;
		return super.visit(node);
	}

	@Override
	public void endVisit(WhileStatement node) {
		/* ブロックの終わりで切る */
		currentBlock.end = unit.getLineNumber(node.getStartPosition() + node.getLength());
		blockList.add(currentBlock);
		/* 制御フローの依存関係を作るためにスタックからpop */
		BasicBlock whileBlock = blockStack.removeFirst();
		makeBlockDependence(currentBlock, whileBlock);
		/* ブロックの終わりの次の行から新しいブロックを始める */
		BasicBlock newBlock = new BasicBlock(unit.getLineNumber(node.getStartPosition() + node.getLength()) + 1);
		makeBlockDependence(whileBlock, newBlock);
		currentBlock = newBlock;
		super.endVisit(node);
	}

	@Override
	public boolean visit(ForStatement node) {
		/* 基本whileと一緒だが、条件や更新文がない可能性もあるのでその辺を気をつけて処理する */
		
		/* forの前で切る */
		currentBlock.end = currentLineNumber - 1;
		blockList.add(currentBlock);
		BasicBlock forBlock = new BasicBlock(currentLineNumber);
		makeBlockDependence(currentBlock, forBlock);
		/* for文の()が終わる行で切る */
		int endLineNumber;
		@SuppressWarnings("unchecked")
		List<Expression> updList = node.updaters();
		if(updList != null && updList.size() > 0){
			Expression lastUpd = updList.get(updList.size() - 1);
			endLineNumber = unit.getLineNumber(lastUpd.getStartPosition() + lastUpd.getLength());
		}else{
			Expression expression = node.getExpression();
			if(expression != null){
				endLineNumber = unit.getLineNumber(expression.getStartPosition() + expression.getLength());
			}else{
				@SuppressWarnings("unchecked")
				List<Expression> initList = node.initializers();
				if(initList != null && initList.size() > 0){
					Expression lastInit = initList.get(initList.size() - 1);
					endLineNumber = unit.getLineNumber(lastInit.getStartPosition() + lastInit.getLength());
				}else{
					endLineNumber = unit.getLineNumber(node.getStartPosition());
				}
			}
		}
		
		forBlock.end = endLineNumber;
		blockList.add(forBlock);
		blockStack.addFirst(forBlock);
		/* ブロックの最初（がわからないのでfor文の()の終わりの次の行）から始める */
		BasicBlock bodyBlock = new BasicBlock(endLineNumber + 1);
		makeBlockDependence(forBlock, bodyBlock);
		currentBlock = bodyBlock;
		return super.visit(node);
	}

	@Override
	public void endVisit(ForStatement node) {
		/* ブロックの終わりで切る */
		currentBlock.end = unit.getLineNumber(node.getStartPosition() + node.getLength());
		blockList.add(currentBlock);
		/* 制御フローの依存関係を作るためにstackからpopする */
		BasicBlock forBlock = blockStack.removeFirst();
		makeBlockDependence(currentBlock, forBlock);
		/* ブロックの終わりから新しいブロックを作る */
		BasicBlock newBlock = new BasicBlock(unit.getLineNumber(node.getStartPosition() + node.getLength() + 1));
		makeBlockDependence(forBlock, newBlock);
		currentBlock = newBlock;
		super.endVisit(node);
	}

	
	@Override
	public boolean visit(Block node) {
/*		ASTNode parent = node.getParent();
		BasicBlock newBlock;
		switch(parent.getNodeType()){
		case ASTNode.FOR_STATEMENT:
			/* forとブロックの改行がどうなってるか考えようとしたけど性善説でそんな見づらいコードはほとんどないと考えよう */
			/* 追記:eclipseでcommand + shift + f で整形できるのでそれを施してからにする */
			
			/* for文の手前で区切る */
/*			currentBlock.end = currentLineNumber - 1;
			newBlock = new BasicBlock(currentLineNumber);
			makeBlockDependence(currentBlock, newBlock);
			blockList.add(currentBlock);
			currentBlock = newBlock;
			
			/* for文の後で区切る */
/*			currentBlock.end = currentLineNumber;
			newBlock = new BasicBlock(currentLineNumber + 1);
			makeBlockDependence(currentBlock, newBlock);
			blockStack.addFirst(currentBlock);			
			blockList.add(currentBlock);
			currentBlock = newBlock;
			break;
		case ASTNode.WHILE_STATEMENT:
			/* 基本的にはfor文と同じはず */
			/* while文の手前で区切る */
/*			currentBlock.end = currentLineNumber - 1;
			newBlock = new BasicBlock(currentLineNumber);
			makeBlockDependence(currentBlock, newBlock);
			blockList.add(currentBlock);
			currentBlock = newBlock;
			
			/* while文の後で区切る */
/*			currentBlock.end = currentLineNumber;
			newBlock = new BasicBlock(currentLineNumber + 1);
			makeBlockDependence(currentBlock, newBlock);
			blockStack.addFirst(currentBlock);			
			blockList.add(currentBlock);
			currentBlock = newBlock;
			break;
		default:
			break;
		}*/
		return super.visit(node);
	}

/*	@Override
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
	}*/

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
			System.out.println("オブジェクト:" + expression);
		}
		List arguments = node.arguments();
		if(arguments != null){
			System.out.println("引数:" + arguments.toString());
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
