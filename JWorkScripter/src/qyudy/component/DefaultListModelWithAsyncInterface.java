package qyudy.component;

import java.lang.reflect.InvocationTargetException;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import qyudy.common.Utils;

public class DefaultListModelWithAsyncInterface<E> extends DefaultListModel<E> implements AsyncDefaultListModel<E> {
	private static final long serialVersionUID = 7533973900921144999L;
	private DefaultListModelWithAsyncInterface() {
	}
	private DefaultListModelWithAsyncInterface<E> delegate;
	public DefaultListModelWithAsyncInterface<E> getDelegate() {
		return delegate;
	}
	public static <T> AsyncDefaultListModel<T> getProxy() {
		DefaultListModelWithAsyncInterface<T> instance = new DefaultListModelWithAsyncInterface<>();
		instance.delegate = new DefaultListModelWithAsyncInterface<>();
		AsyncDefaultListModel<T> listMod = (AsyncDefaultListModel<T>) java.lang.reflect.Proxy.newProxyInstance(AsyncDefaultListModel.class.getClassLoader(), new Class<?>[]{AsyncDefaultListModel.class}, (obj, method, args)->{
			if (method.getReturnType() == void.class) {
				if (SwingUtilities.isEventDispatchThread()) {
					Utils.threadPool.execute(() -> {
						try {
							SwingUtilities.invokeAndWait(() -> {
								try {
									method.invoke(instance.delegate, args);
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
							method.invoke(instance.delegate, args);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							e.printStackTrace();
						}
					});
				}
				return null;
			} else {
				return method.invoke(instance.delegate, args);
			}
		});
		return listMod;
	}
}
