package qyudy.workscript;

import qyudy.common.Utils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

enum VarPrototype {
	CLEAR {
		@Override
		String format(ScriptEnvironment environment, String... strs) {
			map.clear();
			return "清理成功";
		}
		@Override
		boolean isCommand() {
			return true;
		}
	}, TXT {
		@Override
		String format(ScriptEnvironment environment, String... strs) {
			return strs.length > 0 ? strs[0] : "";
		}

		@Override
		boolean isCommand() {
			return false;
		}
	}, INPUT {
		@Override
		String format(ScriptEnvironment environment, String... strs) {
			assertParamCount(strs, 1);
			String title = Utils.getFromArray(strs, 0), defaultText = Utils.getFromArray(strs, 0);
			var input = environment.openInput(title, defaultText);
			return input;
		}

		@Override
		boolean isCommand() {
			return false;
		}
	}, SELECT {
		@Override
		String format(ScriptEnvironment environment, String... strs) {
			assertParamCount(strs, 2);
			return environment.openSelect(strs[0], null, Arrays.copyOfRange(strs, 1, strs.length));
		}

		@Override
		boolean isCommand() {
			return false;
		}
	}, SET {
		@Override
		String format(ScriptEnvironment environment, String... strs) {
			assertParamCount(strs, 2);
			Map<String, String> setMap = get();
			if (setMap == null) {
				setMap = new ConcurrentHashMap<>();
				put(setMap);
			}
			setMap.put(strs[0], strs[1]);
			return "";
		}

		@Override
		boolean isCommand() {
			return false;
		}
	}, REF {
		@Override
		String format(ScriptEnvironment environment, String... strs) {
			assertParamCount(strs, 1);
			Map<String, String> setMap = get(SET);
			return Utils.trimToEmpty(setMap.get(strs[0]));
		}

		@Override
		boolean isCommand() {
			return false;
		}
	}, NOW {
		@Override
		String format(ScriptEnvironment environment, String... strs) {
			assertParamCount(strs, 0);
			String formatter;
			if (strs.length >= 1) {
				formatter = strs[0];
			} else {
				formatter = "yyyy-MM-dd HH:mm:ss";
			}
			return new SimpleDateFormat(formatter).format(new Date());
		}

		@Override
		boolean isCommand() {
			return false;
		}
	};
	private static Map<Object, Object> map = new ConcurrentHashMap<>();
	final protected <T> T get() {
		return get(this);
	}
	final protected <T> T get(Object key) {
		var t = map.get(key);
		return (T)t;
	}
	final protected void put(Object value) {
		put(this, value);
	}
	final protected void put(Object key, Object value) {
		map.put(key, value);
	}
	abstract String format(ScriptEnvironment environment, String...strs);
	abstract boolean isCommand();
	static VarPrototype forName(String name) {
		if (name == null || (name = name.trim()).length() == 0) return TXT;
		var prop = VarPrototype.valueOf(name.toUpperCase());
		if (prop != null) return prop;
		throw new RuntimeException("不支持的变量:" + name);
	}
	boolean assertParamCount(String[] params, int minCount) {
		if (params == null || params.length < minCount) throw new RuntimeException(name() + "参数不足" + minCount);
		return true;
	}
}
