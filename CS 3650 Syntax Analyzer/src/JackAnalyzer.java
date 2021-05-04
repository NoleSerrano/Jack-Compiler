import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

//  top-level driver that sets up and invokes the other modules
public class JackAnalyzer {
	public static void main(String[] args) throws IOException {
		Scanner sc = new Scanner(System.in);
		print("File/Directory: ");
		String fidi = sc.nextLine();
		if (fidi.charAt(0) == '"') { // removes quotations from paste from "Copy as Path" (Shift + RightClick)
			fidi = fidi.substring(1, fidi.length() - 1);
		}
		if (fidi.endsWith(".jack")) { // file (ex: "C:\PATH\MAIN.jack")
			compileFile(fidi);
		} else { // directory (ex: "C:\PATH\FOLDER" (no .jack extension))
			File directory = new File(fidi);
			for (File f : directory.listFiles()) {
				if (f.getName().endsWith(".jack")) {
					compileFile(f.getAbsolutePath());
				}
			}
		}

		sc.close();
	}

	public static void compileFile(String jackFileName) throws IOException {
		File jackFile = new File(jackFileName);
		String fileName = jackFile.getName(); // xxx.jack
		fileName = fileName.substring(0, fileName.length() - 5); // remove .jack extension
		JackTokenizer jt = new JackTokenizer(new File(jackFileName));
		File xxxT = tokenize(jt, jackFile.getParent() + "\\" + fileName + "T.xml");
		File xmlFile = new File(jackFile.getParent() + "\\" + fileName + ".xml");
		CompilationEngine ce = new CompilationEngine(xxxT, xmlFile);
		parse(ce);
		// indentation of xml file (not necessary)
		File tempFile = new File(jackFile.getParent() + "\\" + fileName + "I.xml");
		createIndentation(xmlFile, tempFile); 
		replaceFile(xmlFile, tempFile);
		tempFile.delete(); // remove the "...I.xml" file
	}

	public static File tokenize(JackTokenizer jt, String outFileName) throws IOException {
		FileWriter fileWriter = new FileWriter(new File(outFileName), false); // false to remove current file
		String type; // used for tag
		String tag = "";
		String content = ""; // content inside of tags
		fileWriter.write("<tokens>\n");
		while (jt.hasMoreTokens()) {
			jt.advance();
			type = jt.tokenType(); // used for tag
			switch (type) {
			case "KEYWORD":
				tag = "keyword";
				content = jt.keyWord();
				break;
			case "SYMBOL":
				tag = "symbol";
				switch (jt.symbol()) {
				case '<':
					content = "&lt;";
					break;
				case '>':
					content = "&gt;";
					break;
				case '"':
					content = "&quot;";
					break;
				case '&':
					content = "&amp;";
					break;
				default:
					content = "" + jt.symbol();
				}
				break;
			case "IDENTIFIER":
				tag = "identifier";
				content = jt.identifier();
				break;
			case "INT_CONST":
				tag = "integerConstant";
				content = "" + jt.intVal();
				break;
			case "STRING_CONST":
				tag = "stringConstant";
				content = jt.stringVal();
				break;
			} // end switch (content aquired)
			fileWriter.write("<" + tag + "> " + content + " </" + tag + ">\n");
		}
		fileWriter.write("</tokens>");
		fileWriter.close();
		return new File(outFileName); // NOT COMPLETE
	}

	public static void parse(CompilationEngine ce) throws IOException {
		ce.compileClass();
	}

	// replace file a with file b
	public static void replaceFile(File a, File b) throws IOException {
		FileWriter fileWriter = new FileWriter(new File(a.getAbsolutePath()), false);
		Scanner fileReader = new Scanner(b);
		String line;
		while (fileReader.hasNextLine()) {
			line = fileReader.nextLine();
			fileWriter.write(line + "\n");
		}
		fileReader.close();
		fileWriter.close();
	}

	// creates indentation for XML file
	public static void createIndentation(File input, File output) throws IOException {
		Scanner fileReader = new Scanner(input);
		FileWriter fileWriter = new FileWriter(output);
		String line;
		int indent = 0;
		while (fileReader.hasNextLine()) {
			line = fileReader.nextLine();
			if (line.charAt(1) == '/') { // non-terminal beginning tag
				indent--;
				fileWriter.write(indent(line, indent) + "\n");
			} else if (line.indexOf('>') + 1 == line.length()) { // non-terminal ending tag
				fileWriter.write(indent(line, indent) + "\n");
				indent++;
			} else {
				fileWriter.write(indent(line, indent) + "\n"); // terminal line
			}
		}
		fileWriter.close();
		fileReader.close();
	}

	public static String indent(String line, int indent) {
		String spaces = "";
		for (int i = 0; i < indent; i++) {
			spaces += "  "; // 2 spaces
		}
		return (spaces + line);
	}

	public static void println(Object o) {
		System.out.println(o.toString());
	}

	public static void print(Object o) {
		System.out.print(o.toString());
	}
}
