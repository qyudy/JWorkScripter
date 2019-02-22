package qyudy.component;

import qyudy.common.Utils;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class DefaultListModelWithAsyncInterface<E> extends DefaultListModel<E> implements AsyncDefaultListModel<E> {
	private static final long serialVersionUID = 7533973900921144999L;
	private DefaultListModelWithAsyncInterface() {
	}
	public static <T> AsyncDefaultListModel<T> getProxy() {
		var instance = new DefaultListModelWithAsyncInterface<T>();
		var listMod = (AsyncDefaultListModel<T>) java.lang.reflect.Proxy.newProxyInstance(AsyncDefaultListModel.class.getClassLoader(), new Class<?>[]{AsyncDefaultListModel.class}, (obj, method, args)->{
			if (method.getReturnType() == void.class) {
				if (SwingUtilities.isEventDispatchThread()) {
					Utils.threadPool.execute(() -> {
						try {
							SwingUtilities.invokeAndWait(() -> {
								try {
									method.invoke(instance, args);
								} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
									e.printStackTrace();
								}
							});
						} catch (InvocationTargetException | InterruptedException e) {
							e.printStackTrace();
						}
					});
				} else {
					SwingUtilities.invokeAndWait(() -> {
						try {
							method.invoke(instance, args);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							e.printStackTrace();
						}
					});
				}
				return null;
			} else {
				return method.invoke(instance, args);
			}
		});
		return listMod;
	}
}
