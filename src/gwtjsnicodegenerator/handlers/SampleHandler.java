package gwtjsnicodegenerator.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jface.text.Document;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SampleHandler extends AbstractHandler
{
	private static final String JDT_NATURE = "org.eclipse.jdt.core.javanature";

	public Object execute( ExecutionEvent event ) throws ExecutionException
	{
		// Get the root of the workspace
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Get all projects in the workspace
		IProject[] projects = root.getProjects();
		// Loop over all projects
		for ( IProject project : projects )
		{
			try
			{
				printProjectInfo( project );
				if ( project.isNatureEnabled( JDT_NATURE ) )
				{
					analyseMethods( project );
				}
			}
			catch ( CoreException e )
			{
				e.printStackTrace();
			}
		}

		writeCode();
		return null;
	}

	private void writeCode()
	{
		AST ast = AST.newAST( AST.JLS3 );
		CompilationUnit cu = ast.newCompilationUnit();

		//com.amcharts.jso
		PackageDeclaration p1 = ast.newPackageDeclaration();
		p1.setName( ast.newSimpleName( "jso" ) );
		cu.setPackage( p1 );

		//com.amcharts.api.IsChartCursor
		//com.google.gwt.core.client.JavaScriptObject
		ImportDeclaration id = ast.newImportDeclaration();
		id.setName( ast.newName( new String[]
		{ "java", "util", "Set" , "IsChartCursor" ,"JavaScriptObject"} ) );
		cu.imports().add( id );

		TypeDeclaration td = ast.newTypeDeclaration();
		td.setName( ast.newSimpleName( "ChartCursorJSO" ) );
		td.setSuperclassType( ast.newSimpleType( ast.newSimpleName( "JavaScriptObject" ) ) );
		td.superInterfaceTypes().add(ast.newSimpleType(ast.newSimpleName("IsChartCursor")));
		cu.types().add( td );

		MethodDeclaration md = ast.newMethodDeclaration();
		md.setName( ast.newSimpleName( "getAdjustment" ) );
		md.setReturnType2(ast.newPrimitiveType(PrimitiveType.BOOLEAN));
		td.bodyDeclarations().add( md );

		Block newBody=ast.newBlock();
		md.setBody(newBody);
		Block block = md.getBody();
		ReturnStatement returnStatement = ast.newReturnStatement();
		FieldAccess fieldAccess=ast.newFieldAccess();

		SimpleName name = ast.newSimpleName("adjustment");
		returnStatement.setExpression(name);
		block.statements().add(returnStatement);
		
		System.out.println( cu );
	}

	private void analyseMethods( IProject project ) throws JavaModelException
	{
		IPackageFragment[] packages = JavaCore.create( project )
				.getPackageFragments();
		// parse(JavaCore.create(project));
		for ( IPackageFragment mypackage : packages )
		{
			if ( mypackage.getKind() == IPackageFragmentRoot.K_SOURCE )
			{
				createAST( mypackage );
			}

		}
	}

	private void createAST( IPackageFragment mypackage ) throws JavaModelException
	{
		for ( ICompilationUnit unit : mypackage.getCompilationUnits() )
		{
			// now create the AST for the ICompilationUnits
			CompilationUnit parse = parse( unit );
			MethodVisitor visitor = new MethodVisitor();
			parse.accept( visitor );

			for ( MethodDeclaration method : visitor.getMethods() )
			{
				System.out
						.println( "Method name: " + method.getName() + " Return type: " + method
								.getReturnType2() );
			}

		}
	}

	/**
	 * Reads a ICompilationUnit and creates the AST DOM for manipulating the Java source file
	 * 
	 * @param unit
	 * @return
	 */

	private static CompilationUnit parse( ICompilationUnit unit )
	{
		ASTParser parser = ASTParser.newParser( AST.JLS3 );
		parser.setKind( ASTParser.K_COMPILATION_UNIT );
		parser.setSource( unit );
		parser.setResolveBindings( true );
		return ( CompilationUnit ) parser.createAST( null ); // parse
	}

	private void printProjectInfo( IProject project ) throws CoreException, JavaModelException
	{
		System.out.println( "Working in project " + project.getName() );
		// check if we have a Java project
		if ( project.isNatureEnabled( "org.eclipse.jdt.core.javanature" ) )
		{
			IJavaProject javaProject = JavaCore.create( project );
			printPackageInfos( javaProject );
		}
	}

	private void printPackageInfos( IJavaProject javaProject ) throws JavaModelException
	{
		IPackageFragment[] packages = javaProject.getPackageFragments();
		for ( IPackageFragment mypackage : packages )
		{
			// Package fragments include all packages in the
			// classpath
			// We will only look at the package from the source
			// folder
			// K_BINARY would include also included JARS, e.g.
			// rt.jar
			if ( mypackage.getKind() == IPackageFragmentRoot.K_SOURCE )
			{
				System.out.println( "Package " + mypackage.getElementName() );
				printICompilationUnitInfo( mypackage );

			}

		}
	}

	private void printICompilationUnitInfo( IPackageFragment mypackage ) throws JavaModelException
	{
		for ( ICompilationUnit unit : mypackage.getCompilationUnits() )
		{
			printCompilationUnitDetails( unit );

		}
	}

	private void printIMethods( ICompilationUnit unit ) throws JavaModelException
	{
		IType[] allTypes = unit.getAllTypes();
		for ( IType type : allTypes )
		{
			printIMethodDetails( type );
		}
	}

	private void printCompilationUnitDetails( ICompilationUnit unit ) throws JavaModelException
	{
		System.out.println( "Source file " + unit.getElementName() );
		Document doc = new Document( unit.getSource() );
		System.out.println( "Has number of lines: " + doc.getNumberOfLines() );
		printIMethods( unit );
	}

	private void printIMethodDetails( IType type ) throws JavaModelException
	{
		IMethod[] methods = type.getMethods();
		for ( IMethod method : methods )
		{

			System.out.println( "Method name " + method.getElementName() );
			System.out.println( "Signature " + method.getSignature() );
			System.out.println( "Return Type " + method.getReturnType() );

		}
	}
}