
public class SymbolTable {

	String[][] Symbols;

	public SymbolTable() {
		Symbols = new String[0][3];
	}

	public void startSubroutine() { // new subroutine scope (reset symbol table)
		Symbols = new String[0][3];
	}

	public void define(String name, String type, String kind) {
		// increase size of Symbols Array
		// kind = "STATIC", "FIELD", "ARG", or "VAR"
		// Symbols[i][j] where j -> name = 0, type = 1, kind = 2
		String[][] temp = new String[Symbols.length + 1][3];
		for (int i = 0; i < Symbols.length; i++) {
			temp[i] = Symbols[i];
		}
		temp[temp.length - 1] = new String[] { name, type, kind };
		Symbols = temp;
	}

	public int varCount(String kind) {
		int varCount = 0;
		for (int i = 0; i < Symbols.length; i++) {
			if (kind.equals(Symbols[i][2])) {
				varCount++;
			}
		}
		return varCount; 
	}

	public String kindOf(String name) {
		for (int i = 0; i < Symbols.length; i++) {
			if (name.equals(Symbols[i][0])) {
				return Symbols[i][2]; // return kind
			}
		}
		return null; // error
	}

	public String typeOf(String name) {
		for (int i = 0; i < Symbols.length; i++) {
			if (name.equals(Symbols[i][0])) {
				return Symbols[i][1]; // return type
			}
		}
		return null; // error
	}

	public int indexOf(String name) {
		// INDEX OF SPECIFIC TYPE OR JUST INDEX IN SYMBOL TABLE?
		for (int i = 0; i < Symbols.length; i++) {
			if (name.equals(Symbols[i][0])) {
				return i; // return index
			}
		}
		return -1; // not found
	}
}
