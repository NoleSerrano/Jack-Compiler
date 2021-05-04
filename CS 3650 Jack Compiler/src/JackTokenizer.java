import java.io.File;
import java.io.IOException;
import java.util.Scanner;

// tokenizer
public class JackTokenizer {

	private String token; // current token
	private String[] Tokens = new String[0]; // set of tokens
	private int n; // index of current token

	public JackTokenizer(File input) throws IOException { // refer to pg 208 and see Lexical Elements
		Scanner fileReader = new Scanner(input);
		char c;
		String line;
		int i; // used for keeping track of index of character in each line of the input file
		while (fileReader.hasNextLine()) {
			line = fileReader.nextLine();
			i = 0;
			int start;
			while (i < line.length()) {
				c = line.charAt(i);
				if ((c >= 48 && c <= 57) || (c >= 65 && c <= 90) || (c >= 97 && c <= 122)) { // number or letter
					start = i;
					i++; // next character
					while (i < line.length()) {
						c = line.charAt(i);
						if (!((c >= 48 && c <= 57) || (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || (c == '_'))) { // not
																														// number
																														// or
							// letter or underscore
							addEntry(line.substring(start, i)); // need to check if i is a symbol so don't increment
							break;
						}
						i++; // next character
					}
				} else if (c == ' ' || c == '\t') { // white space
					i++; // no token
				} else if (c == '/') { // comment
					String potentialComment = line.substring(i, i + 2);
					if (potentialComment.equals("//")) {
						break; // next line
					} else if (potentialComment.equals("/*")) { // comment until closing | API comment
						if (line.charAt(i + 2) == '*') { // API comment (tricky because can end in another line)
							i += 2; // here because it's "/**" thus ignore these tokens
							boolean endingFound = false;
							while (i < line.length() - 1) {
								if (line.charAt(i) == '*' && line.substring(i, i + 2).equals("*/")) {
									i += 2;
									endingFound = true;
									break;
								}
								i++;
							}
							while (!endingFound) {
								line = fileReader.nextLine();
								for (i = 0; i < line.length() - 1; i++) {
									if (line.charAt(i) == '*' && line.substring(i, i + 2).equals("*/")) {
										i += 2;
										endingFound = true;
										break;
									}
								}
							}
						} else { // comment until closing
							i += 2; // here because it's "/*"
							while (i < line.length() - 1) { // doesn't go entire length because "*/" has length 2
								if (line.charAt(i) == '*' && line.substring(i, i + 2).equals("*/")) {
									i += 2;
									break;
								}
								i++;
							}
						}
					} else { // division symbol -> "/"
						addEntry("/");
						i++;
					}
				} else if (c == '"') { // string (need to keep quotation for now to know it's a string)
					start = i;
					i++;
					while (i < line.length()) {
						if (line.charAt(i) == '"') {
							addEntry(line.substring(start, i + 1));
							i++; // i was quote so can increment;
							break;
						}
						i++;
					}
				} else { // has to be symbol
					addEntry(c + "");
					i++;
				}
			}
		}
		fileReader.close();
	}

	private void addEntry(String s) {
		String[] temp = new String[Tokens.length + 1];
		for (int i = 0; i < Tokens.length; i++) {
			temp[i] = Tokens[i];
		}
		temp[temp.length - 1] = s; // add new entry
		Tokens = temp;
	}

	public boolean hasMoreTokens() {
		return (n < Tokens.length);
	}

	public void advance() {
		token = Tokens[n++];
	}

	public String tokenType() {
		char c = token.charAt(0);
		if (c == '"') {
			return "STRING_CONST";
		} else if ((c >= 48) && (c <= 57)) { // number
			return "INT_CONST";
		} else if (!((c >= 65 && c <= 90) || (c >= 97 && c <= 122))) { // not letter so symbol
			return "SYMBOL";
		}
		switch (token) { // figure out if keyword
		case "class":
		case "constructor":
		case "function":
		case "method":
		case "field":
		case "static":
		case "var":
		case "int":
		case "char":
		case "boolean":
		case "void":
		case "true":
		case "false":
		case "null":
		case "this":
		case "let":
		case "do":
		case "if":
		case "else":
		case "while":
		case "return":
			return "KEYWORD";
		}
		return "IDENTIFIER"; // All other options exhausted, therefore by logical deduction -> identifier
	}

	public String keyWord() {
		return token;
	}

	public char symbol() {
		return token.charAt(0);
	}

	public String identifier() {
		return token;
	}

	public int intVal() {
		return Integer.valueOf(token);
	}

	public String stringVal() {
		return token.substring(1, token.length() - 1);
	}

	// TEST FUNCTIONS BELOW
	public void printTokens() {
		for (int i = 0; i < Tokens.length; i++) {
			println(Tokens[i]);
		}
	}

	public void printTokenTypes() {
		for (int i = 0; i < Tokens.length; i++) {
			token = Tokens[i];
			print(token + " -> " + tokenType());
			switch (tokenType()) {
			case "KEYWORD":
				print(" -> " + keyWord());
				break;
			case "SYMBOL":
				print(" -> " + symbol());
				break;
			case "IDENTIFIER":
				print(" -> " + identifier());
				break;
			case "INT_CONST":
				print(" -> " + intVal());
				break;
			case "STRING_CONST":
				print(" -> " + stringVal());
				break;
			default:
				print(" -> ERROR");
				break;
			}
			println("");
		}
	}

	public static void println(Object o) {
		System.out.println(o.toString());
	}

	public static void print(Object o) {
		System.out.print(o.toString());
	}

}
