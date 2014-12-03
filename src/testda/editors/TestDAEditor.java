package testda.editors;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

/**
 * An example showing how to create a multi-page editor.
 * This example has 3 pages:
 * <ul>
 * <li>page 0 contains a nested text editor.
 * <li>page 1 allows you to change the font used in page 2
 * <li>page 2 shows the words in page 0 in sorted order
 * </ul>
 */
public class TestDAEditor extends MultiPageEditorPart implements IResourceChangeListener{

	/** The text editor used in page 0. */
	private TextEditor editor;

	/** The font chosen in page 1. */
	private Font font;

	/** The text widget used in page 2. */
	private StyledText text;
	
	private ArrayList<ICompilationUnit> compilationUnits = new ArrayList<ICompilationUnit>();
	/**
	 * Creates a multi-page editor example.
	 */
	public TestDAEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}
	/**
	 * Creates page 0 of the multi-page editor,
	 * which contains a text editor.
	 */
	void createPage0() {
		try {
			editor = new TextEditor();
			int index = addPage(editor, getEditorInput());
			setPageText(index, editor.getTitle());
		} catch (PartInitException e) {
			ErrorDialog.openError(
				getSite().getShell(),
				"Error creating nested text editor",
				null,
				e.getStatus());
		}
	}
	/**
	 * Creates page 1 of the multi-page editor,
	 * which allows you to change the font used in page 2.
	 */
	void createPage1() {

		Composite composite = new Composite(getContainer(), SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 2;

		int index = addPage(composite);
		setPageText(index, "Properties");
	}
	/**
	 * Creates page 2 of the multi-page editor,
	 * which shows the sorted text.
	 */
	void createPage2() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		FillLayout layout = new FillLayout();
		composite.setLayout(layout);
		text = new StyledText(composite, SWT.H_SCROLL | SWT.V_SCROLL);
		text.setEditable(false);

		int index = addPage(composite);
		setPageText(index, "Preview");
	}
	/**
	 * Creates the pages of the multi-page editor.
	 */
	protected void createPages() {
		createPage0();
		createPage1();
		createPage2();
	}
	/**
	 * The <code>MultiPageEditorPart</code> implementation of this 
	 * <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}
	/**
	 * Saves the multi-page editor's document.
	 */
	public void doSave(IProgressMonitor monitor) {
		getEditor(0).doSave(monitor);
	}
	/**
	 * Saves the multi-page editor's document as another file.
	 * Also updates the text for page 0's tab, and updates this multi-page editor's input
	 * to correspond to the nested editor's.
	 */
	public void doSaveAs() {
		IEditorPart editor = getEditor(0);
		editor.doSaveAs();
		setPageText(0, editor.getTitle());
		setInput(editor.getEditorInput());
	}
	/* (non-Javadoc)
	 * Method declared on IEditorPart
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(0);
		IDE.gotoMarker(getEditor(0), marker);
	}
	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method
	 * checks that the input is an instance of <code>IFileEditorInput</code>.
	 */
	public void init(IEditorSite site, IEditorInput editorInput)
		throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		super.init(site, editorInput);
	}
	/* (non-Javadoc)
	 * Method declared on IEditorPart.
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}
	/**
	 * Calculates the contents of page 2 when the it is activated.
	 */
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		if (newPageIndex == 2) {
			sortWords();
		}
	}
	/**
	 * Closes all project files on project close.
	 */
	public void resourceChanged(final IResourceChangeEvent event){
		if(event.getType() == IResourceChangeEvent.PRE_CLOSE){
			Display.getDefault().asyncExec(new Runnable(){
				public void run(){
					IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
					for (int i = 0; i<pages.length; i++){
						if(((FileEditorInput)editor.getEditorInput()).getFile().getProject().equals(event.getResource())){
							IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
							pages[i].closeEditor(editorPart,true);
						}
					}
				}            
			});
		}
	}
	/**
	 * Sets the font related data to be applied to the text in page 2.
	 */
	void setFont() {
		FontDialog fontDialog = new FontDialog(getSite().getShell());
		fontDialog.setFontList(text.getFont().getFontData());
		FontData fontData = fontDialog.open();
		if (fontData != null) {
			if (font != null)
				font.dispose();
			font = new Font(text.getDisplay(), fontData);
			text.setFont(font);
		}
	}
	/**
	 * Sorts the words in page 0, and shows them in page 2.
	 */
	void sortWords() {
		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IJavaModel model = JavaCore.create(root);
		/* JavaModelあたりからcompilationunitを作らないとバインディング情報が手に入らない */
		
		/* ICompilationUnitを集める */
		visitChild(model);
		
		for (int i = 0; i < compilationUnits.size(); i++){
			ASTParser parser = ASTParser.newParser(AST.JLS4);
			parser.setSource(compilationUnits.get(i));
			parser.setResolveBindings(true);
//			parser.setEnvironment(, sourcepathEntries, null, true)
			ASTNode node = parser.createAST(new NullProgressMonitor());
			TestDAASTVisitor visitor = new TestDAASTVisitor();
			System.out.println(compilationUnits.get(i).getPath().toString());
			node.accept(visitor);
//			visitor.printTreeInfo();
			visitor.printTable();
			visitor.printBlockList();
//			visitor.printTreeInfo();
			System.out.println();
		}
		
/*		try {
//			SourceFile sourceFile = new SourceFile("src" + File.separator + "Something.java");
//			SourceFile sourceFile = new SourceFile(File.separator + "Users" + File.separator + "koyasukiichi" + File.separator + "Documents" + File.separator + "workspace" + File.separator + "testda" + File.separator + "src" + File.separator + "testda" + File.separator + "editors" + File.separator + "BasicLine.java");
			SourceFile sourceFile = new SourceFile(File.separator + "Users" + File.separator + "koyasukiichi" + File.separator + "Documents" + File.separator + "workspace" + File.separator + "HitAndBlow" + File.separator + "src" + File.separator + "Main.java");
			ASTParser parser = ASTParser.newParser(AST.JLS4);
			parser.setResolveBindings(true);
			parser.setEnvironment(Envs.getClasspath(), Envs.getSourcepath(), null, true);
			parser.setUnitName(sourceFile.getFilePath());
			parser.setSource(sourceFile.getSourceCode().toCharArray());
			ASTNode node = parser.createAST(new NullProgressMonitor());
			ASTVisitor visitor = new TestDAASTVisitor();
			node.accept(visitor);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		

		String source =
			editor.getDocumentProvider().getDocument(editor.getEditorInput()).get();
/*		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(source.toCharArray());
		ASTNode node = parser.createAST(new NullProgressMonitor());
		ASTVisitor visitor = new TestDAASTVisitor();
		node.accept(visitor);*/

		text.setText(source);
	}
	/**
	 * @param model
	 */
	private void visitChild(IJavaElement elem) {
		
//		System.out.println(elem.getElementType());
		switch(elem.getElementType()){
		case IJavaElement.JAVA_MODEL:
		case IJavaElement.JAVA_PROJECT:
		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			IParent parent = (IParent)elem;
			try{
				if(parent.hasChildren()){
					IJavaElement[] children = parent.getChildren();
					for(int i = 0; i < children.length; i++){
						visitChild(children[i]);
					}
				}
			} catch (JavaModelException e){
				e.printStackTrace();
			}
			break;
		case IJavaElement.PACKAGE_FRAGMENT:
			IPackageFragment fragment = (IPackageFragment)elem;
			try {
				ICompilationUnit[] units = fragment.getCompilationUnits();
				for(int i = 0; i < units.length; i++){
					compilationUnits.add(units[i]);
//					System.out.println(units[i].getElementName());
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
		
	}
}
