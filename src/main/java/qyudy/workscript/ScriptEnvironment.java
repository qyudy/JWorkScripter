package qyudy.workscript;

public interface ScriptEnvironment {
	String openInput(String title, String defaultText);
	<T> T openSelect(String title, String message, T[] selects);
	boolean showConfirm(String title, String message);
	void showMessage(String title, String message);
	
	WorkMode getWorkMode();
}
