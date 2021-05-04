import java.io.File;
import java.io.IOException;
import java.util.Scanner;

// recursive top-down parser
public class CompilationEngine {

	private int currentLine; // good if want previous line
	private String[] Lines;
	private VMWriter vmw;
	private SymbolTable st1; // variables of class
	private SymbolTable st2; // variables of functions
	private String className;
	private int ifgotoIndex;
	private int whileIndex;

	public CompilationEngine(File input, File output) throws IOException {
		currentLine = 0;
		ifgotoIndex = 0;
		whileIndex = 0;
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
		st1 = new SymbolTable();
		st2 = new SymbolTable();
		vmw = new VMWriter(output);
	}

	public void compileClass() throws IOException {
		currentLine++; // class
		className = Lines[currentLine++]; // (ex: "<identifier> Main </identifier>")
		className = getContent(className); // (ex: "Main")
		currentLine++; // {
		String potentialVarDec = Lines[currentLine];
		while (potentialVarDec.equals("<keyword> static </keyword>")
				|| potentialVarDec.equals("<keyword> field </keyword>")) { // classVarDec*
			compileClassVarDec(); // WORK ON
			potentialVarDec = Lines[currentLine];
		}
		String potentialSubroutineDec = Lines[currentLine];
		while (potentialSubroutineDec.equals("<keyword> constructor </keyword>")
				|| potentialSubroutineDec.equals("<keyword> function </keyword>")
				|| potentialSubroutineDec.equals("<keyword> method </keyword>")) { // subroutineDec*
			st2.startSubroutine(); // new symbol table
			compileSubroutine(); // WORK ON
			potentialSubroutineDec = Lines[currentLine];
		}
		currentLine++; // }
		vmw.close(); // Don't forget to close or doesn't write to the file
	}

	public void compileClassVarDec() throws IOException { // put the variables into st1
		// (index of space + 1, index of / - 2) -> gets the "content" from "<tag>
		// content </tag>"
		String kind = getContent(Lines[currentLine++]); // static | field (KIND) (<KEYWORD>)
		String type = getContent(Lines[currentLine++]); // type (TYPE) (<IDENTIFIER>)
		String name = getContent(Lines[currentLine++]); // varName (NAME) (<IDENTIFIER>)
		st1.define(name, type, kind);
		String potentialComma = Lines[currentLine];
		while (potentialComma.equals("<symbol> , </symbol>")) { // (, varName)*
			currentLine++; // ,
			name = getContent(Lines[currentLine++]); // varName
			st1.define(name, type, kind);
			potentialComma = Lines[currentLine];
		}
		currentLine++; // ;
	}

	public void compileSubroutine() throws IOException { // method, function, or constructor
		// fileWriter.write("<subroutineDec>\n");
		currentLine++; // constructor | function | method
		currentLine++; // void | type
		String functionName = getContent(Lines[currentLine++]); // subroutineName
		currentLine++; // (
		int nArgs = compileParameterList();
		vmw.writeFunction(className + "." + functionName, nArgs);
		currentLine++; // )
		// fileWriter.write("<subroutineBody>\n");
		currentLine++; // {
		String potentialVar = Lines[currentLine];
		while (potentialVar.equals("<keyword> var </keyword>")) { // varDec*
			compileVarDec(); // LOCAL
			potentialVar = Lines[currentLine];
		}
		compileStatements();
		currentLine++; // }
		// fileWriter.write("</subroutineBody>\n");
		// fileWriter.write("</subroutineDec>\n");
	}

	public int compileParameterList() throws IOException { // ARGUMENTS
		int nArgs = 0;
		String potentialRightParenthesis = Lines[currentLine];
		if (!potentialRightParenthesis.equals("<symbol> ) </symbol>")) { // has parameters
			nArgs++;
			String kind = "argument";
			String type = getContent(Lines[currentLine++]); // type
			String name = getContent(Lines[currentLine++]); // varName
			st2.define(name, type, kind);
			String potentialComma = Lines[currentLine];
			while (potentialComma.equals("<symbol> , </symbol>")) { // extra vars
				nArgs++;
				currentLine++; // ,
				type = getContent(Lines[currentLine++]); // type
				name = getContent(Lines[currentLine++]); // varName
				st2.define(name, type, kind);
				potentialComma = Lines[currentLine];
			}
		}
		return nArgs;
	}

	public void compileVarDec() throws IOException { // (ex: var int i, j;) LOCALS
		String kind = "local";
		currentLine++; // var
		String type = getContent(Lines[currentLine++]); // type
		String name = getContent(Lines[currentLine++]); // varName;
		st2.define(name, type, kind);
		String potentialComma = Lines[currentLine];
		while (potentialComma.equals("<symbol> , </symbol>")) {
			currentLine++; // ,
			name = getContent(Lines[currentLine++]); // name
			st2.define(name, type, kind);
			potentialComma = Lines[currentLine];
		}
		currentLine++; // ;
	}

	public void compileStatements() throws IOException {
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
	}

	public void compileDo() throws IOException {
		currentLine++; // do
		int nLocals = 1;
		vmw.writePush("pointer", 0);
		String potentialPeriod = Lines[currentLine + 1];
		if (potentialPeriod.equals("<symbol> . </symbol>")) { // (ex: game.run(expressionList))
			String className = getContent(Lines[currentLine++]); // className | varName
			currentLine++; // .
			String functionName = getContent(Lines[currentLine++]); // subroutineName
			currentLine++; // (
			nLocals += compileExpressionList();
			vmw.writeCall(className + "." + functionName, nLocals);
			currentLine++; // )
		} else { // (ex: run(expressionList))
			String functionName = getContent(Lines[currentLine++]); // subroutineName
			currentLine++; // (
			nLocals += compileExpressionList();
			vmw.writeCall(functionName, nLocals);
			currentLine++; // )
		}
		vmw.writePop("temp", 0); // re-frame stack
		currentLine++; // ;
	}

	public void compileLet() throws IOException {
		currentLine++; // let
		String name = getContent(Lines[currentLine++]); // varName
		String kind;
		int index = st1.indexOf(name);
		if (index == -1) { // function scope (st2)
			index = st2.indexOf(name);
			kind = st2.kindOf(name);
			index = st2.varCount(kind);
		} else { // class scope (st1)
			kind = st1.kindOf(name);
			index = st1.varCount(kind);
		}
		String potentialBracket = Lines[currentLine];
		if (potentialBracket.equals("<symbol> [ </symbol>")) { // ARRAY
			currentLine++; // [
			compileExpression();
			vmw.writeCall("Memory.alloc", 1);
			currentLine++; // ]
		}
		currentLine++; // =
		compileExpression();
		vmw.writePop(kind, index);
		currentLine++; // ;
	}

	public void compileWhile() throws IOException {
		currentLine++; // while
		currentLine++; // (
		compileExpression();
		currentLine++; // )
		currentLine++; // {
		vmw.writeLabel("WHILE_EXP" + whileIndex);
		vmw.writeIf("WHILE_END" + whileIndex);
		compileStatements();
		vmw.writeGoto("WHILE_EXP" + whileIndex);
		vmw.writeLabel("WHILE_END" + whileIndex);
		whileIndex++;
		currentLine++; // }
	}

	public void compileReturn() throws IOException { // 'return' (expression?) ';' - if not ; then expr
		currentLine++; // return
		String potentialExpression = Lines[currentLine];
		if (!(potentialExpression.equals("<symbol> ; </symbol>"))) { // expression
			compileExpression();
		}
		vmw.writeReturn(); // write after so expression can be pushed
		currentLine++; // ;
	}

	public void compileIf() throws IOException { // 'if' '(' expression ')' '{' statements '}'
													// ('else' '{' statements '}')?
		currentLine++; // if
		currentLine++; // (
		compileExpression(); // expression
		currentLine++; // )
		currentLine++; // {
		vmw.writeIf("IF_TRUE" + ifgotoIndex);
		vmw.writeGoto("IF_FALSE" + ifgotoIndex);
		vmw.writeLabel("IF_TRUE" + ifgotoIndex);
		compileStatements(); // statements
		vmw.writeLabel("IF_FALSE" + ifgotoIndex);
		ifgotoIndex++;
		currentLine++; // }
		String potentialElse = Lines[currentLine];
		if (potentialElse.equals("<keyword> else </keyword>")) { // need to check if it checks the "\n" at the end ->
																	// there is no "\n" at the end
			currentLine++; // else
			currentLine++; // {
			compileStatements();
			currentLine++; // }
		}
	}

	public int compileExpression() throws IOException {
		int nLocals = 0;
		nLocals += compileTerm(); // push x
		// POTENTIAL OPERATOR
		char op = Lines[currentLine].charAt(9); // reference: <symbol> X </symbol>
		String content = "";
		if (op == '&') {
			content = getContent(Lines[currentLine]); // operator
		}
		// Note: '&' counts for <, >, and &
		while (op == '&' || op == '+' || op == '-' || op == '*' || op == '/' || op == '|' || op == '=') { // operator
																											// symbol
			currentLine++; // operator
			nLocals += compileTerm(); // push y
			switch (op) {
			case '&': // <, >, or &
				vmw.writeArithmetic(content.substring(1, content.length() - 1)); // "lt", "gt", "and"
				break;
			case '+':
				vmw.writeArithmetic("add");
				break;
			case '-':
				vmw.writeArithmetic("sub");
				break;
			case '*':
				vmw.writeArithmetic("call Math.multiply 2");
				break;
			case '/':
				vmw.writeArithmetic("call Math.divide 2");
				break;
			case '|':
				vmw.writeArithmetic("or");
				break;
			case '=':
				vmw.writeArithmetic("eq");
				break;
			}
			op = Lines[currentLine].charAt(9);
		}
		return nLocals;
	}

	public int compileTerm() throws IOException {
		int nLocals = 1;
		String firstToken = Lines[currentLine];
		if (firstToken.substring(0, 8).equals("<symbol>")) { // expression | unaryOp term
			if (firstToken.charAt(9) == '(') { // expression
				currentLine++; // (
				nLocals += compileExpression();
				currentLine++; // )
			} else { // unaryOp term
				currentLine++; // unaryOp
				nLocals += compileTerm();
				vmw.writeArithmetic("not"); // negation
			}
		} else if (firstToken.substring(0, 12).equals("<identifier>")) { // varName | varName [ exp ] | subroutineCall
			String secondToken = Lines[currentLine + 1];
			char symbol = secondToken.charAt(9);
			if (symbol == '(') { // subroutineCall "someMethod(expressionList)
				String functionName = getContent(Lines[currentLine++]); // subroutineName
				currentLine++; // (
				nLocals += compileExpressionList();
				vmw.writeCall(className + "." + functionName, nLocals);
				currentLine++; // )
			} else if (symbol == '.') { // subroutineCall "game.run(expressionList)"
				String className = getContent(Lines[currentLine++]); // className | varName
				currentLine++; // .
				String functionName = getContent(Lines[currentLine++]); // subroutineName
				currentLine++; // (
				nLocals += compileExpressionList();
				vmw.writeCall(className + "." + functionName, nLocals);
				currentLine++; // )
			} else if (symbol == '[') { // varName [ exp ]
				String name = getContent(Lines[currentLine++]); // varName
				currentLine++; // [
				nLocals += compileExpression();
				int[] index = getIndex(name);
				if (index[0] == 0) { // class
					String kind = st1.kindOf(name);
					vmw.writePop(kind, st1.varCount(kind));
				} else { // function
					String kind = st2.kindOf(name);
					vmw.writePop(kind, st2.varCount(kind));
				}
				currentLine++; // ]
			} else { // varName
				String name = getContent(Lines[currentLine++]); // varName
				int[] index = getIndex(name);
				if (index[0] == 0) { // class
					String kind = st1.kindOf(name);
					vmw.writePop(kind, st1.varCount(kind));
				} else { // function
					String kind = st2.kindOf(name);
					vmw.writePop(kind, st2.varCount(kind));
				}
			}
		} else { // keyword -> stringConstant | integerConstant | keywordConstant (this, true,
					// false, null)
			String keyword = getTag(Lines[currentLine]);
			switch (keyword) {
			case "stringConstant":
				vmw.writePush("this", nLocals);
				currentLine++;
				break;
			case "integerConstant":
				vmw.writePush("constant", Integer.valueOf(getContent(Lines[currentLine++])));
				break;
			default: // keywordConstant
				vmw.writePush("this", nLocals);
				currentLine++;
				break;
			}
		}
		return nLocals;
	}

	public int compileExpressionList() throws IOException {
		int nLocals = 0;
		String potentialRightParenthesis = Lines[currentLine];
		if (!potentialRightParenthesis.equals("<symbol> ) </symbol>")) { // has expressions

			nLocals += compileExpression();
			String potentialComma = Lines[currentLine];
			while (potentialComma.equals("<symbol> , </symbol>")) {
				currentLine++; // ,
				nLocals += compileExpression();
				potentialComma = Lines[currentLine];
			}
		}
		return nLocals;
	}

	private String getContent(String line) { // gets content from line: "<tag> content </tag>"
		return line.substring(line.indexOf(' ') + 1, line.indexOf('/') - 2);
	}

	private String getTag(String line) { // gets tag from line: "<tag> content </tag>"
		return line.substring(1, line.indexOf('>'));
	}

	private int[] getIndex(String name) {
		int index = st1.indexOf(name);
		if (index == -1) {
			return new int[] { 1, st2.indexOf(name) };
		} else {
			return new int[] { 0, index };
		}
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
