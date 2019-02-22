package qyudy.common;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public interface Utils {
	static <T> T getFromArray(T[] ts, int index) {
		return ts != null && index >= 0 && index < ts.length?ts[index]:null;
	}
	static String trimToEmpty(String s) {
		if (s == null) return "";
		return s.trim();
	}
	static boolean isEmpty(String s) {
		return s == null || s.isEmpty();
	}
	static <V> V getFieldValue(Object obj, String name) {
		try {
			var clazz = obj.getClass();
			var field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			return (V) field.get(obj);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 10, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(), (r, e) -> System.out.println("线程不足，抛弃任务"));
}
