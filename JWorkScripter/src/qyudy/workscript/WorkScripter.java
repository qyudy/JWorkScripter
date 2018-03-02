package qyudy.workscript;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import qyudy.common.RecurFileAction;
import qyudy.common.Utils;
import qyudy.component.AsyncDefaultListModel;
import qyudy.component.DefaultListModelWithAsyncInterface;
import qyudy.component.UndoableEditWithTime;

public class WorkScripter implements ScriptEnvironment {

	private JFrame main;
	private JTextField pathIpt;
	private JTextField filenameIpt;
	private JComboBox<WorkMode> comboBox;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WorkScripter window = new WorkScripter();
					window.main.setVisible(true);
					Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
						if (!Utils.isEmpty(e.getMessage())) {
							window.showMessage("错误", e.getMessage());
						} else {
							e.printStackTrace();
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public WorkScripter() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		main = new JFrame();
		main.setTitle("WorkScripter" + Constants.version);
		main.setMinimumSize(new Dimension(660, 550));
		main.setLocationRelativeTo(null);
		main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		GroupLayout groupLayout = new GroupLayout(main.getContentPane());
		
		JMenuBar menuBar = new JMenuBar();
		main.setJMenuBar(menuBar);
		
		JMenu propertyMeu = new JMenu("配置");
		menuBar.add(propertyMeu);
		
		JMenuItem loadPropertyMeu = new JMenuItem("读取配置");
		propertyMeu.add(loadPropertyMeu);
		
		JMenuItem savePropertyMeu = new JMenuItem("保存配置");
		propertyMeu.add(savePropertyMeu);
		
		JMenu helpMeu = new JMenu("帮助");
		menuBar.add(helpMeu);
		
		JMenuItem versionMeu = new JMenuItem("版本记录");
		helpMeu.add(versionMeu);
		
		JMenuItem aboutMeu = new JMenuItem("关于");
		helpMeu.add(aboutMeu);
		
		JLabel pathLbl = new JLabel("目录");
		
		pathIpt = new JTextField();
		
		JLabel filenameLbl = new JLabel("文件名");
		
		filenameIpt = new JTextField();
		
		JCheckBox recurChk = new JCheckBox("递归搜索");
		
		JToggleButton searchBtn = new JToggleButton("搜索");
		// 按钮按下后在目录内搜索文件，文件列表只会展示文件不会展示文件夹
		searchBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (searchBtn.isSelected()) {
					Utils.threadPool.execute(() -> {
						File path = getCanonicalPath();
						if (path != null) {
							boolean recur = recurChk.isSelected();
							new RecurFileAction() {
								@Override
								protected boolean filterFile(File f) {
									return searchBtn.isSelected() && !f.isHidden();
								}
								@Override
								protected boolean filterDirectory(File f) {
									return searchBtn.isSelected() && (!f.isHidden() || f.toPath().getRoot().equals(f.toPath()));
								}
								@Override
								protected void doIfFile(File f) {
									listMod.addElement(f);// 注意listMod是异步处理过的代理，如果这里直接使用同步处理，会有跨线程异常
								}
								@Override
								protected void doIfDirectory(File f) {
								}
								@Override
								protected void doFirst() {
									listMod.clear();
									searchBtn.setText("取消");
								}
								@Override
								protected void doLast() {
									searchBtn.setText("搜索");
									searchBtn.setSelected(false);
								}
							}.recur(path, recur);
						} else {
							showMessage("提示", "设置目录为空");
							searchBtn.setSelected(false);
						}
					});
				}
			}
		});
		
		JTextArea textAra = new JTextArea();
		textAra.setLineWrap(true);
		// undo管理器，这里实现了不超过1秒的相同操作合并的功能（比如两次输入或者两次删除操作间隔在1秒内，则视为同一个操作进行撤销或重做）
		UndoManager textAraUndo = new UndoManager() {
			private static final long serialVersionUID = -3080045939457797215L;
			@Override
			public synchronized boolean addEdit(UndoableEdit anEdit) {
				anEdit = new UndoableEditWithTime(anEdit, System.currentTimeMillis());
				return super.addEdit(anEdit);
			}
	        private static final long outtime = 1000;
			@Override
			protected UndoableEdit editToBeUndone() {
		        int i = Utils.getFieldValue(UndoManager.class, "indexOfNextAdd", this);
		        if (i <= 0) {
		        	return null;
		        } else if (i == 1) {
		        	UndoableEdit edit = edits.elementAt(--i);
		        	return edit.isSignificant() ? edit : null;
		        }
		        
	        	UndoableEditWithTime edit = (UndoableEditWithTime) edits.elementAt(--i), editPrev = edit, editIsSignificant = null;
		        while (i > 0) {
		        	edit = editPrev;
		        	editPrev = (UndoableEditWithTime) edits.elementAt(--i);
		            if (edit.isSignificant()) {
		            	editIsSignificant = edit;
		            }
		            if (edit.getTime() - editPrev.getTime() >= outtime || edit.getPresentationName() != editPrev.getPresentationName()) {
		            	if (edit.isSignificant()) {
		            		return edit;
		            	} else if (editIsSignificant != null) {
		            		return editIsSignificant;
		            	} else if (editPrev.isSignificant()) {
		            		return editPrev;
		            	} else {
		            		while (i > 0) {
		                        UndoableEdit edit_ = edits.elementAt(--i);
		                        if (edit_.isSignificant()) {
		                            return edit_;
		                        }
		            		}
		            		return null;
		            	}
		        	} else if (i == 0) {
		            	return editPrev.isSignificant() ? editPrev : editIsSignificant;
		            }
		        }
		        
		        return null;
			}
			@Override
			protected UndoableEdit editToBeRedone() {
		        int i = Utils.getFieldValue(UndoManager.class, "indexOfNextAdd", this), count = edits.size();
		        if (i >= count) {
		        	return null;
		        } else if (i == count - 1) {
		        	UndoableEdit edit = edits.elementAt(i);
		        	return edit.isSignificant() ? edit : null;
		        }
		        
	        	UndoableEditWithTime edit = (UndoableEditWithTime) edits.elementAt(i++), editNext = edit, editIsSignificant = null;
		        while (i < count) {
		        	edit = editNext;
		        	editNext = (UndoableEditWithTime) edits.elementAt(i++);
		            if (edit.isSignificant()) {
		            	editIsSignificant = edit;
		            }
		            if (editNext.getTime() - edit.getTime() >= outtime || edit.getPresentationName() != editNext.getPresentationName()) {
		            	if (edit.isSignificant()) {
		            		return edit;
		            	} else if (editIsSignificant != null) {
		            		return editIsSignificant;
		            	} else if (editNext.isSignificant()) {
		            		return editNext;
		            	} else {
		            		while (i < count) {
		                        UndoableEdit edit_ = edits.elementAt(i++);
		                        if (edit_.isSignificant()) {
		                            return edit_;
		                        }
		            		}
		            		return null;
		            	}
		        	} else if (i == count) {
		            	return editNext.isSignificant() ? editNext : editIsSignificant;
		            }
		        }
		        
		        return null;
			}
		};
		textAra.getDocument().addUndoableEditListener(textAraUndo);
		
		JScrollPane textAraScroll = new JScrollPane(textAra);
		
		JButton relativeBtn = new JButton("获取相对路径");
		
		JButton jointBtn = new JButton("拼接路径");
		
		JButton formatBtn = new JButton("格式化");
		formatBtn.setEnabled(false);
		formatBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textAra.setText(Script.format(textAra.getText(), WorkScripter.this));
			}
		});
		textAra.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z) {// 撤销按键
					if (textAraUndo.canUndo()) {
						textAraUndo.undo();
					}
				} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y) {// 重做按键
					if (textAraUndo.canRedo()) {
						textAraUndo.redo();
					}
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (getWorkMode() == WorkMode.COMMAND && e.isAltDown()) {// 命令行模式中alt+enter输入回车
						e.setModifiers(e.getModifiers() & ~e.ALT_MASK);
					} else if (getWorkMode() == WorkMode.COMMAND && !e.isAltDown()) {// enter进行格式化
						formatBtn.doClick();
						e.consume();
					} else if (getWorkMode() == WorkMode.SCRIPT && e.isAltDown()) {// 脚本模式中enter输入回车，alt+enter进行格式化
						formatBtn.doClick();
						e.consume();
					}
				}
			}
		});
		
		JCheckBox operChk = new JCheckBox("不处理文件");
		operChk.setEnabled(false);
		
		JButton copytoBtn = new JButton("拷贝文件至");
		
		JList<File> fileLst = new JList<File>(listMod);
		fileLst.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {// 向文件列表粘贴文件或者
					try {
						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
							List<File> files = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
							Utils.threadPool.execute(() -> {
								for (File f : files) {
									new RecurFileAction() {
										@Override
										protected boolean filterFile(File f) {
											return !f.isHidden();
										}
										@Override
										protected boolean filterDirectory(File f) {
											return !f.isHidden() || f.toPath().getRoot().equals(f.toPath());
										}
										@Override
										protected void doIfFile(File f) {
											listMod.addElement(f);// 注意listMod是异步处理过的代理，如果这里直接使用同步处理，会有跨线程异常
										}
										@Override
										protected void doIfDirectory(File f) {
										}
										@Override
										protected void doFirst() {
										}
										@Override
										protected void doLast() {
										}
									}.recur(f, true);
								}
							});
						} else if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
							String str = (String) clipboard.getData(DataFlavor.stringFlavor);
							File path = getCanonicalPath();
							for (String s : str.split("\r?\n")) {
								if (path == null) {
									listMod.addElement(new File(s).getCanonicalFile());
								} else {
									listMod.addElement(Paths.get(path.getPath(), s).toFile().getCanonicalFile());
								}
							}
						}
					} catch (UnsupportedFlavorException | IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		JScrollPane fileLstScroll = new JScrollPane(fileLst);
		
		JList<String> commandLst = new JList<>();
		JScrollPane commandScroll = new JScrollPane(commandLst);

		comboBox = new JComboBox<>();
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (comboBox.getSelectedItem() == WorkMode.COMMAND && fileLstScroll.isVisible()) {
					groupLayout.replace(fileLstScroll, commandScroll);
					fileLstScroll.setVisible(false);
					relativeBtn.setEnabled(false);
					jointBtn.setEnabled(false);
					copytoBtn.setEnabled(false);
					filenameIpt.setEnabled(false);
					recurChk.setEnabled(false);
					searchBtn.setEnabled(false);
				} else if (comboBox.getSelectedItem() != WorkMode.COMMAND && !fileLstScroll.isVisible()) {
					groupLayout.replace(commandScroll, fileLstScroll);
					fileLstScroll.setVisible(true);
					relativeBtn.setEnabled(true);
					jointBtn.setEnabled(true);
					copytoBtn.setEnabled(true);
					filenameIpt.setEnabled(true);
					recurChk.setEnabled(true);
					searchBtn.setEnabled(true);
				}
				if (comboBox.getSelectedItem() == WorkMode.SCRIPT) {
					operChk.setEnabled(true);
				} else {
					operChk.setEnabled(false);
				}
				if (comboBox.getSelectedItem() == WorkMode.NORMAL) {
					formatBtn.setEnabled(false);
				} else {
					formatBtn.setEnabled(true);
				}
			}
		});
		comboBox.setModel(new DefaultComboBoxModel<>(WorkMode.values()));
		
		Component horizontalGlue = Box.createHorizontalGlue();
		
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addComponent(pathLbl)
						.addComponent(pathIpt, 0, 150, Integer.MAX_VALUE)
						.addComponent(filenameLbl)
						.addComponent(filenameIpt, 0, 10, Integer.MAX_VALUE)
						.addComponent(recurChk)
						.addComponent(searchBtn))
				.addGroup(groupLayout.createParallelGroup(Alignment.CENTER)
						.addComponent(fileLstScroll, 0, 0, Integer.MAX_VALUE)
						.addComponent(textAraScroll, 0, 0, Integer.MAX_VALUE))
				.addGroup(groupLayout.createSequentialGroup()
						.addComponent(relativeBtn)
						.addComponent(jointBtn)
						.addComponent(copytoBtn)
						.addComponent(operChk)
						.addComponent(horizontalGlue)
						.addComponent(comboBox, 0, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(formatBtn))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.CENTER)
				.addGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(pathLbl)
						.addComponent(pathIpt)
						.addComponent(filenameLbl)
						.addComponent(filenameIpt)
						.addComponent(recurChk)
						.addComponent(searchBtn))
					.addComponent(fileLstScroll, 0, 0, Integer.MAX_VALUE)
					.addComponent(textAraScroll, 0, 0, Integer.MAX_VALUE)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(relativeBtn)
						.addComponent(jointBtn)
						.addComponent(copytoBtn)
						.addComponent(operChk)
						.addComponent(horizontalGlue)
						.addComponent(comboBox)
						.addComponent(formatBtn)))
		);
		groupLayout.setAutoCreateGaps(true);
		groupLayout.setAutoCreateContainerGaps(true);
		main.getContentPane().setLayout(groupLayout);
	}
	
	private File getCanonicalPath() {
		String path = pathIpt.getText();
		if (Utils.isEmpty(path)) {
			return null;
		}
		try {
			File f = new File(path).getCanonicalFile();
			if (f.exists() && f.isDirectory()) {
				pathIpt.setText(f.getPath());
				return f;
			} else {
				showMessage("错误", "设置目录不存在");
				return null;
			}
		} catch (IOException e) {
			showMessage("错误", "设置目录不正确");
			return null;
		}
	}
	
//	private DefaultListModel<File> listMod = new DefaultListModel<>();
	
	private AsyncDefaultListModel<File> listMod = DefaultListModelWithAsyncInterface.getProxy();

	@Override
	public String openInput(String title, String defaultText) {
		return JOptionPane.showInputDialog(null, defaultText, title, JOptionPane.PLAIN_MESSAGE);
	}

	@Override
	public <T> T openSelect(String title, String message, T[] selects) {
		return (T)JOptionPane.showInputDialog(null, message, title, JOptionPane.PLAIN_MESSAGE, null, selects, selects[0]);
	}

	@Override
	public boolean showConfirmDialog(String title, String message) {
		return JOptionPane.showConfirmDialog(null, message, title,JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION;
	}

	@Override
	public void showMessage(String title, String message) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public WorkMode getWorkMode() {
		return (WorkMode) comboBox.getSelectedItem();
	}
}