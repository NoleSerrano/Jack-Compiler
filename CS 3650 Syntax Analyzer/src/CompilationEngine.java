import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

// recursive top-down parser
public class CompilationEngine {

	private int currentLine; // good if want previous line
	private FileWriter fileWriter;
	private String[] Lines;

	public CompilationEngine(File input, File output) throws IOException {
		currentLine = 0;
		Scanner fileReader = new Scanner(input);
		int i = 0;
		while (fileReader.hasNextLine()) {
			fileReader.nextLine();
			i++; // get size so don't have to create a temp array each time
		}
		Lines = new String[i - 2]; // dont want the "token" stuff at start and end of xxxT.xml file
		fileReader.close();
		fileReader = new Scanner(input);
		fileReader.nextLine(); // "<tokens>"
		for (i = 0; i < Lines.length; i++) { // don't want the <tokens> or </tokens>
			Lines[i] = fileReader.nextLine();
		}
		fileReader.close();
		fileWriter = new FileWriter(output, false);
	}

	public void compileClass() throws IOException {
		fileWriter.write("<class>\n");
		fileWriter.write(Lines[currentLine++] + "\n"); // class
		fileWriter.write(Lines[currentLine++] + "\n"); // className
		fileWriter.write(Lines[currentLine++] + "\n"); // {
		String potentialVarDec = Lines[currentLine];
		while (potentialVarDec.equals("<keyword> static </keyword>")
				|| potentialVarDec.equals("<keyword> field </keyword>")) { // classVarDec*
			compileClassVarDec(); // SURROUND ENTIRE BLOCK OF STATIC/FIELD VARIABLES OR SURROUND EACH ONE?
			potentialVarDec = Lines[currentLine];
		}
		String potentialSubroutineDec = Lines[currentLine];
		while (potentialSubroutineDec.equals("<keyword> constructor </keyword>")
				|| potentialSubroutineDec.equals("<keyword> function </keyword>")
				|| potentialSubroutineDec.equals("<keyword> method </keyword>")) { // subroutineDec*
			compileSubroutine();
			potentialSubroutineDec = Lines[currentLine];
		}
		fileWriter.write(Lines[currentLine++] + "\n"); // }
		fileWriter.write("</class>");
		fileWriter.close(); // Don't forget to close or doesn't write to the file
	}

	public void compileClassVarDec() throws IOException {
		fileWriter.write("<classVarDec>\n");
		fileWriter.write(Lines[currentLine++] + "\n"); // static | field
		fileWriter.write(Lines[currentLine++] + "\n"); // type
		fileWriter.write(Lines[currentLine++] + "\n"); // var name
		String potentialComma = Lines[currentLine];
		while (potentialComma.equals("<symbol> , </symbol>")) { // (, varName)*
			fileWriter.write(Lines[currentLine++] + "\n");
			fileWriter.write(Lines[currentLine++] + "\n");
			potentialComma = Lines[currentLine];
		}
		fileWriter.write(Lines[currentLine++] + "\n"); // ;
		fileWriter.write("</classVarDec>\n");
	}

	public void compileSubroutine() throws IOException { // method, function, or constructor
		fileWriter.write("<subroutineDec>\n");
		fileWriter.write(Lines[currentLine++] + "\n"); // constructor | function | method
		fileWriter.write(Lines[currentLine++] + "\n"); // void | type
		fileWriter.write(Lines[currentLine++] + "\n"); // subroutineName
		fileWriter.write(Lines[currentLine++] + "\n"); // (
		compileParameterList();
		fileWriter.write(Lines[currentLine++] + "\n"); // )
		fileWriter.write("<subroutineBody>\n");
		fileWriter.write(Lines[currentLine++] + "\n"); // {
		String potentialVar = Lines[currentLine];
		while (potentialVar.equals("<keyword> var </keyword>")) { // varDec*
			compileVarDec();
			potentialVar = Lines[currentLine];
		}
		compileStatements();
		fileWriter.write(Lines[currentLine++] + "\n"); // }
		fileWriter.write("</subroutineBody>\n");
		fileWriter.write("</subroutineDec>\n");
	}

	public void compileParameterList() throws IOException {
		fileWriter.write("<parameterList>\n");
		String potentialRightParenthesis = Lines[currentLine];
		if (!potentialRightParenthesis.equals("<symbol> ) </symbol>")) { // has parameters
			fileWriter.write(Lines[currentLine++] + "\n"); // type
			fileWriter.write(Lines[currentLine++] + "\n"); // varName
			String potentialComma = Lines[currentLine];
			while (potentialComma.equals("<symbol> , </symbol>")) { // extra vars
				fileWriter.write(Lines[currentLine++] + "\n"); // ,
				fileWriter.write(Lines[currentLine++] + "\n"); // type
				fileWriter.write(Lines[currentLine++] + "\n"); // varName
				potentialComma = Lines[currentLine];
			}
		}
		fileWriter.write("</parameterList>\n");
	}

	public void compileVarDec() throws IOException { // (ex: var int i, j;)
		fileWriter.write("<varDec>\n");
		fileWriter.write(Lines[currentLine++] + "\n"); // var
		fileWriter.write(Lines[currentLine++] + "\n"); // type
		fileWriter.write(Lines[currentLine++] + "\n"); // varName
		String potentialComma = Lines[currentLine];
		while (potentialComma.equals("<symbol> , </symbol>")) {
			fileWriter.write(Lines[currentLine++] + "\n"); // ,
			fileWriter.write(Lines[currentLine++] + "\n"); // varName
			potentialComma = Lines[currentLine];
		}
		fileWriter.write(Lines[currentLine++] + "\n"); // ;
		fileWriter.write("</varDec>\n");
	}

	public void compileStatements() throws IOException {
		fileWriter.write("<statements>\n");
		String potentialEnd = Lines[currentLine];
		while (!potentialEnd.equals("<symbol> } </symbol>")) {
			switch (potentialEnd) {
			case "<keyword> let </keyword>":
				compileLet();
				break;
			case "<keyword> if </keyword>":
				compileIf();
				break;
			case "<keyword> while </keyword>":
				compileWhile();
				break;
			case "<keyword> do </keyword>":
				compileDo();
				break;
			case "<keyword> return </keyword>":
				compileReturn();
				break;
			}
			potentialEnd = Lines[currentLine];
		}
		fileWriter.write("</statements>\n");
	}

	public void compileDo() throws IOException {
		fileWriter.write("<doStatement>\n");
		fileWriter.write(Lines[currentLine++] + "\n"); // do
		String potentialPeriod = Lines[currentLine + 1];
		if (potentialPeriod.equals("<symbol> . </symbol>")) { // (ex: game.run(expressionList))
			fileWriter.write(Lines[currentLine++] + "\n"); // className | varName
			fileWriter.write(Lines[currentLine++] + "\n"); // .
			fileWriter.write(Lines[currentLine++] + "\n"); // subroutineName
			fileWriter.write(Lines[currentLine++] + "\n"); // (
			compileExpressionList();
			fileWriter.write(Lines[currentLine++] + "\n"); // )
		} else { // (ex: run(expressionList))
			fileWriter.write(Lines[currentLine++] + "\n"); // subroutineName
			fileWriter.write(Lines[currentLine++] + "\n"); // (
			compileExpressionList();
			fileWriter.write(Lines[currentLine++] + "\n"); // )
		}
		fileWriter.write(Lines[currentLine++] + "\n"); // ;
		fileWriter.write("</doStatement>\n");
	}

	public void compileLet() throws IOException {
		fileWriter.write("<letStatement>\n");
		fileWriter.write(Lines[currentLine++] + "\n"); // let
		fileWriter.write(Lines[currentLine++] + "\n"); // varName
		String potentialBracket = Lines[currentLine];
		if (potentialBracket.equals("<symbol> [ </symbol>")) {
			fileWriter.write(Lines[currentLine++] + "\n"); // [
			compileExpression();
			fileWriter.write(Lines[currentLine++] + "\n"); // ]
		}
		fileWriter.write(Lines[currentLine++] + "\n"); // =
		compileExpression();
		fileWriter.write(Lines[currentLine++] + "\n"); // ;
		fileWriter.write("</letStatement>\n");
	}

	public void compileWhile() throws IOException {
		fileWriter.write("<whileStatement>\n");
		fileWriter.write(Lines[currentLine++] + "\n"); // while
		fileWriter.write(Lines[currentLine++] + "\n"); // (
		compileExpression();
		fileWriter.write(Lines[currentLine++] + "\n"); // )
		fileWriter.write(Lines[currentLine++] + "\n"); // {
		compileStatements();
		fileWriter.write(Lines[currentLine++] + "\n"); // }
		fileWriter.write("</whileStatement>\n");
	}

	public void compileReturn() throws IOException { // 'return' (expression?) ';' - if not ; then expr
		fileWriter.write("<returnStatement>\n");
		fileWriter.write(Lines[currentLine++] + "\n"); // return
		String potentialExpression = Lines[currentLine];
		if (!(potentialExpression.equals("<symbol> ; </symbol>"))) { // expression
			compileExpression();
		}
		fileWriter.write(Lines[currentLine++] + "\n"); // ;
		fileWriter.write("</returnStatement>\n");
	}

	public void compileIf() throws IOException { // 'if' '(' expression ')' '{' statements '}'
													// ('else' '{' statements '}')?
		fileWriter.write("<ifStatement>\n");
		fileWriter.write(Lines[currentLine++] + "\n"); // if
		fileWriter.write(Lines[currentLine++] + "\n"); // (
		compileExpression(); // expression
		fileWriter.write(Lines[currentLine++] + "\n"); // )
		fileWriter.write(Lines[currentLine++] + "\n"); // {
		compileStatements(); // statements
		fileWriter.write(Lines[currentLine++] + "\n"); // }
		String potentialElse = Lines[currentLine];
		if (potentialElse.equals("<keyword> else </keyword>")) { // need to check if it checks the "\n" at the end ->
																	// there is no "\n" at the end
			fileWriter.write(Lines[currentLine++] + "\n"); // else
			fileWriter.write(Lines[currentLine++] + "\n"); // {
			compileStatements();
			fileWriter.write(Lines[currentLine++] + "\n"); // }
		}
		fileWriter.write("</ifStatement>\n");
	}

	public void compileExpression() throws IOException {
		fileWriter.write("<expression>\n");
		compileTerm();
		// POTENTIAL OPERATOR
		char op = Lines[currentLine].charAt(9); // refernce: <symbol> X </symbol>
		// Note: '&' counts for <, >, and &
		while (op == '&' || op == '+' || op == '-' || op == '*' || op == '/' || op == '|' || op == '=') { // operator
																											// symbol
			fileWriter.write(Lines[currentLine++] + "\n"); // operator
			compileTerm();
			op = Lines[currentLine].charAt(9);
		}
		fileWriter.write("</expression>\n");
	}

	public void compileTerm() throws IOException {
		fileWriter.write("<term>\n");
		String firstToken = Lines[currentLine];
		if (firstToken.substring(0, 8).equals("<symbol>")) { // expression | unaryOp term
			if (firstToken.charAt(9) == '(') { // expression
				fileWriter.write(Lines[currentLine++] + "\n"); // (
				compileExpression();
				fileWriter.write(Lines[currentLine++] + "\n"); // )
			} else { // unaryOp term
				fileWriter.write(Lines[currentLine++] + "\n"); // unaryOp
				compileTerm();
			}
		} else if (firstToken.substring(0, 12).equals("<identifier>")) { // varName | varName [ exp ] | subroutineCall
			String secondToken = Lines[currentLine + 1];
			char symbol = secondToken.charAt(9);
			if (symbol == '(') { // subroutineCall "someMethod(expressionList)
				fileWriter.write(Lines[currentLine++] + "\n"); // subroutineName
				fileWriter.write(Lines[currentLine++] + "\n"); // (
				compileExpressionList();
				fileWriter.write(Lines[currentLine++] + "\n"); // )
			} else if (symbol == '.') { // subroutineCall "game.run(expressionList)"
				fileWriter.write(Lines[currentLine++] + "\n"); // className | varName
				fileWriter.write(Lines[currentLine++] + "\n"); // .
				fileWriter.write(Lines[currentLine++] + "\n"); // subroutineName
				fileWriter.write(Lines[currentLine++] + "\n"); // (
				compileExpressionList();
				fileWriter.write(Lines[currentLine++] + "\n"); // )
			} else if (symbol == '[') { // varName [ exp ]
				fileWriter.write(Lines[currentLine++] + "\n"); // varName
				fileWriter.write(Lines[currentLine++] + "\n"); // [
				compileExpression();
				fileWriter.write(Lines[currentLine++] + "\n"); // ]
			} else { // varName
				fileWriter.write(Lines[currentLine++] + "\n"); // varName
			}
		} else { // keyword -> stringConstant | integerConstant | keywordConstant (this, true,
					// false, null)
			fileWriter.write(Lines[currentLine++] + "\n"); // whatever the keyword is
		}
		fileWriter.write("</term>\n");
	}

	public void compileExpressionList() throws IOException {
		fileWriter.write("<expressionList>\n");
		String potentialRightParenthesis = Lines[currentLine];
		if (!potentialRightParenthesis.equals("<symbol> ) </symbol>")) { // has expressions
			compileExpression();
			String potentialComma = Lines[currentLine];
			while (potentialComma.equals("<symbol> , </symbol>")) {
				fileWriter.write(Lines[currentLine++] + "\n"); // ,
				compileExpression();
				potentialComma = Lines[currentLine];
			}
		}
		fileWriter.write("</expressionList>\n");
	}

	// TEST FUNCTIONS BELOW
	public void printLines() {
		for (int i = 0; i < Lines.length; i++) {
			println(Lines[i]);
		}
	}

	public void println(Object o) {
		System.out.println(o.toString());
	}
}
