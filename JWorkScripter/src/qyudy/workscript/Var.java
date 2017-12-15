package qyudy.workscript;

class Var {
	VarPrototype prototype;
	String[] params;
	Var(String name, String...params) {
		this.prototype = VarPrototype.forName(name);
		this.params = params;
	}
	Var(VarPrototype prototype, String...params) {
		this.prototype = prototype;
		this.params = params;
	}
	String format(ScriptEnvironment environment) {
		String s = prototype.format(environment, params);
		if (environment.getWorkMode() == WorkMode.COMMAND || !prototype.isCommand()) {
			return s;
		} else {
			return "";
		}
	}
}
