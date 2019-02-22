package qyudy.common;

import java.io.File;

public abstract class RecurFileAction {
	protected abstract void doIfFile(File f);
	protected abstract void doIfDirectory(File f);
	protected abstract boolean filterFile(File f);
	protected abstract boolean filterDirectory(File f);
	protected abstract boolean doFirst();
	protected abstract void doLast();
	private void recur(File f, boolean recur, boolean first) {
		if (f.isFile() && filterFile(f)) {
			doIfFile(f);
		} else if (f.isDirectory() && filterDirectory(f)) {
			doIfDirectory(f);
			if (first || recur) {
                var fs = f.listFiles();
				if (fs != null) {
					for (File f_ : f.listFiles()) {
						recur(f_, recur, false);
					}
				}
			}
		}
	}
	public void recur(File f, boolean recur) {
		if (doFirst()) {
			recur(f, recur, true);
		}
		doLast();
	}
}
