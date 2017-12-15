package qyudy.component;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

public class UndoableEditWithTime implements UndoableEdit {
	private UndoableEdit edit;
	private long time;
	public UndoableEditWithTime(UndoableEdit edit, long time) {
		this.edit = edit;
		this.time = time;
	}
	
	public long getTime() {
		return time;
	}

	@Override
	public void undo() throws CannotUndoException {
		edit.undo();
	}

	@Override
	public boolean canUndo() {
		return edit.canUndo();
	}

	@Override
	public void redo() throws CannotRedoException {
		edit.redo();
	}

	@Override
	public boolean canRedo() {
		return edit.canRedo();
	}

	@Override
	public void die() {
		edit.die();
	}

	@Override
	public boolean addEdit(UndoableEdit anEdit) {
		return edit.addEdit(anEdit);
	}

	@Override
	public boolean replaceEdit(UndoableEdit anEdit) {
		return edit.replaceEdit(anEdit);
	}

	private boolean significant = true;
	public void setSignificant(boolean significant) {
		this.significant = significant;
	}
	@Override
	public boolean isSignificant() {
		return significant;
	}

	@Override
	public String getPresentationName() {
		return edit.getPresentationName();
	}

	@Override
	public String getUndoPresentationName() {
		return edit.getUndoPresentationName();
	}

	@Override
	public String getRedoPresentationName() {
		return edit.getRedoPresentationName();
	}

}
