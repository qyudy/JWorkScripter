package qyudy.workscript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Script {
	private static final char[] scriptChar = "{(')}".toCharArray();
	private static final char varStartChar = scriptChar[0];
	private static final char varEndChar = scriptChar[4];
	private static final char paramStartChar = scriptChar[1];
	private static final char paramEndChar = scriptChar[3];
	private static final char commentChar = scriptChar[2];
	protected String script;
	protected List<Var> vars = new ArrayList<>();
	public Script(String script) {
		this.script = script;
		parse();
	}
	protected void parse() {
		if (script == null) return;
		int index = -1, varStart, varEnd = -1, paramStart, commentStart, commentEnd;
		while(index < script.length()) {
			varStart = index = findOptional(script, index, varStartChar);
			
			if(varEnd + 1 < varStart) vars.add(new Var(VarPrototype.TXT, script.substring(varEnd + 1, varStart)));
			if (varStart >= script.length()) break;

			var params = new ArrayList<String>();

			index = findNecessary(script, index, paramStartChar, varEndChar);
			var name = script.substring(varStart + 1, index);
			while (index < script.length()) {
				if (script.charAt(index) == paramStartChar) {
					paramStart = index;
					var param = new StringBuilder();
					commentEnd = paramStart;
					while (index < script.length()) {
						index = findNecessary(script, index, commentChar, paramEndChar);
						param.append(script.substring(commentEnd + 1, index));
						if (script.charAt(index) == commentChar) {
							commentStart = index;
							commentEnd = index = findNecessary(script, index, commentChar);
							if (commentStart + 1 == commentEnd) {
								param.append(commentChar);
							} else {
								param.append(script.substring(commentStart + 1, commentEnd));
							}
						} else {
							break;
						}
					}
					params.add(param.toString());
					index = findNecessary(script, index, paramStartChar, varEndChar);
				} else {
					varEnd = index;
					vars.add(new Var(name, params.toArray(new String[params.size()])));
					break;
				}
			}
		}
	}
	public String format(ScriptEnvironment environment) {
		var formatted = new StringBuilder();
		for (Var v : vars) {
			formatted.append(v.format(environment));
		}
		return formatted.toString();
	}
	public static String format(String str, ScriptEnvironment environment) {
		return new Script(str).format(environment);
	}
	private static int findNecessary(String str, int indexFrom, char...tofind) {
		for (var i = indexFrom + 1; i < str.length(); i++) {
			var c = str.charAt(i);
			for (var charToFind : tofind) {
				if (c == charToFind) {
					return i;
				}
			}
		}
		throw new RuntimeException("无法找到匹配符号:" + Arrays.toString(tofind));
	}
	private static int findOptional(String str, int indexFrom, char...tofind) {
		int i;
		for (i = indexFrom + 1; i < str.length(); i++) {
			var c = str.charAt(i);
			for (char charToFind : tofind) {
				if (c == charToFind) {
					return i;
				}
			}
		}
		return i;
	}
}
