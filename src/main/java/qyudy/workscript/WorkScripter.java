package qyudy.workscript;

import qyudy.common.RecurFileAction;
import qyudy.common.Utils;
import qyudy.component.AsyncDefaultListModel;
import qyudy.component.DefaultListModelWithAsyncInterface;
import qyudy.component.UndoableEditWithTime;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class WorkScripter implements ScriptEnvironment {

	private JFrame main;
	private JTextField pathIpt;
	private JTextField filenameIpt;
	private JComboBox<WorkMode> comboBox;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				var window = new WorkScripter();
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

		var groupLayout = new GroupLayout(main.getContentPane());
		
		var menuBar = new JMenuBar();
		main.setJMenuBar(menuBar);
		
		var propertyMeu = new JMenu("配置");
		menuBar.add(propertyMeu);

		var loadPropertyMeu = new JMenuItem("读取配置");
		propertyMeu.add(loadPropertyMeu);

		var savePropertyMeu = new JMenuItem("保存配置");
		propertyMeu.add(savePropertyMeu);

		var helpMeu = new JMenu("帮助");
		menuBar.add(helpMeu);

		var readmeMeu = new JMenuItem("readme");
		readmeMeu.addActionListener(e -> {
            try {
				var directory = System.getProperty("java.io.tmpdir");
				var file = Paths.get(directory, "readme.txt");
                Files.copy(WorkScripter.class.getResourceAsStream("/readme.txt"), file, StandardCopyOption.REPLACE_EXISTING);
                Runtime.getRuntime().exec("notepad " + file.toString());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
		helpMeu.add(readmeMeu);

		var aboutMeu = new JMenuItem("关于");
		aboutMeu.addActionListener(e -> JOptionPane.showMessageDialog(null, "JWorkScript" + Constants.version + " by qyudy\n2013.1-2018.8", "关于" , JOptionPane.INFORMATION_MESSAGE));
		helpMeu.add(aboutMeu);

		var pathLbl = new JLabel("目录");
		
		pathIpt = new JTextField();

		var filenameLbl = new JLabel("文件名");
		
		filenameIpt = new JTextField();

		var recurChk = new JCheckBox("递归");

		var searchBtn = new JToggleButton("搜索");
		// 按钮按下后在目录内搜索文件，文件列表只会展示文件不会展示文件夹
		searchBtn.addActionListener(e -> {
			if (searchBtn.isSelected()) {
				Utils.threadPool.execute(() -> {
					var path = getCanonicalPath();
					if (path != null) {
						var recur = recurChk.isSelected();
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
							protected boolean doFirst() {
								listMod.clear();
								searchBtn.setText("取消");
								return true;
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
		});

		var textAra = new JTextArea();
		textAra.setLineWrap(true);
		// undo管理器，这里实现了不超过1秒的相同操作合并的功能（比如两次输入或者两次删除操作间隔在1秒内，则视为同一个操作进行撤销或重做）
		var textAraUndo = new UndoManager() {
			private static final long serialVersionUID = -3080045939457797215L;
			@Override
			public synchronized boolean addEdit(UndoableEdit anEdit) {
				anEdit = new UndoableEditWithTime(anEdit, System.currentTimeMillis());
				return super.addEdit(anEdit);
			}
	        private static final long outtime = 1000;
			@Override
			protected UndoableEdit editToBeUndone() {
				int i = Utils.getFieldValue(this, "indexOfNextAdd");
		        if (i <= 0) {
		        	return null;
		        } else if (i == 1) {
					var edit = edits.elementAt(--i);
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
								var edit_ = edits.elementAt(--i);
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
		        int i = Utils.getFieldValue(this, "indexOfNextAdd"), count = edits.size();
		        if (i >= count) {
		        	return null;
		        } else if (i == count - 1) {
					var edit = edits.elementAt(i);
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
								var edit_ = edits.elementAt(i++);
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

		var textAraScroll = new JScrollPane(textAra);

		var relativeBtn = new JButton("获取相对路径");
		relativeBtn.addActionListener(e -> {
			File from = new File(pathIpt.getText());
			if (!from.exists()) {
				throw new RuntimeException("目录不存在");
			}
			var pathFrom = from.toPath();
			var str = new StringBuilder();
			listMod.elements().asIterator().forEachRemaining(f -> {
				if (f.toPath().startsWith(pathFrom)) {
					str.append(pathFrom.relativize(f.toPath())).append('\n');
				} else {
					str.append(f).append(" 不在目录中\n");
				}
			});
			textAra.setText(str.toString());
		});

		var jointBtn = new JButton("拼接路径");

		var formatBtn = new JButton("格式化");
		formatBtn.setEnabled(false);
		formatBtn.addActionListener(e -> textAra.setText(Script.format(textAra.getText(), WorkScripter.this)));
		textAra.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z) {// 撤销按键
					if (textAraUndo.canUndo()) {
						textAraUndo.undo();
					}
				} else if ((e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y) || (e.isControlDown() && e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_Z)) {// 重做按键
					if (textAraUndo.canRedo()) {
						textAraUndo.redo();
					}
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (getWorkMode() == WorkMode.COMMAND && e.isAltDown()) {// 命令行模式中alt+enter输入回车
//						e.setModifiers(e.getModifiers() & ~e.ALT_MASK);
						textAra.append("\n");
						e.consume();
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

		var operChk = new JCheckBox("不处理文件");
		operChk.setEnabled(false);

		var copytoBtn = new JButton("拷贝文件至");
		copytoBtn.addActionListener(e -> {
			var from = new File(pathIpt.getText());
			if (!from.exists()) {
				throw new RuntimeException("目录不存在");
			}
			var pathFrom = from.toPath();
			var chooser = new JFileChooser(from);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (chooser.showDialog(null,"拷贝至") == JFileChooser.APPROVE_OPTION) {
				var to = chooser.getSelectedFile();
				var pathTo = to.toPath();
				listMod.elements().asIterator().forEachRemaining(f -> {
					if (f.toPath().startsWith(pathFrom)) {
						try {
							var p = pathTo.resolve(pathFrom.relativize(f.toPath()));
							Files.createDirectories(p.getParent());
							Files.copy(f.toPath(), p, StandardCopyOption.REPLACE_EXISTING);
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
					}
				});
			}
		});

		var fileLst = new JList<>(listMod);
		fileLst.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
			if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {// 向文件列表粘贴文件或者
				try {
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
						var files = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
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
									protected boolean doFirst() {
										return true;
									}
									@Override
									protected void doLast() {
									}
								}.recur(f, true);
							}
						});
					} else if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
						var str = (String) clipboard.getData(DataFlavor.stringFlavor);
						var path = getCanonicalPath();
						for (String s : str.split("\r?\n")) {
							if (path == null) {
								listMod.addElement(new File(s).getCanonicalFile());
							} else {
								listMod.addElement(Paths.get(path.getPath(), s).toFile().getCanonicalFile());
							}
						}
					}
				} catch (UnsupportedFlavorException | IOException ex) {
					ex.printStackTrace();
				}
			}
			}
		});
		var fileLstScroll = new JScrollPane(fileLst);

		var commandLst = new JList<>();
		var commandScroll = new JScrollPane(commandLst);

		comboBox = new JComboBox<>();
		comboBox.addActionListener(e -> {
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
		});
		comboBox.setModel(new DefaultComboBoxModel<>(WorkMode.values()));

		var horizontalGlue = Box.createHorizontalGlue();
		
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
		var path = pathIpt.getText();
		if (Utils.isEmpty(path)) {
			return null;
		}
		try {
			var f = new File(path).getCanonicalFile();
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
	public boolean showConfirm(String title, String message) {
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