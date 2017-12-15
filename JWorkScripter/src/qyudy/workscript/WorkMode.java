package qyudy.workscript;
public enum WorkMode {
	NORMAL("普通模式"),SCRIPT("脚本模式"),COMMAND("命令行模式");
	private String name;
	WorkMode(String name) {
		this.name = name;
	}
	@Override
	public String toString() {
		return name;
	}
}