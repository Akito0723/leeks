//import com.intellij.icons.AllIcons;
//import com.intellij.ide.util.PropertiesComponent;
//import com.intellij.openapi.actionSystem.*;
//import com.intellij.openapi.diagnostic.Logger;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.ui.popup.JBPopupFactory;
//import com.intellij.openapi.ui.popup.PopupStep;
//import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
//import com.intellij.openapi.wm.ToolWindowAnchor;
//import com.intellij.openapi.wm.ToolWindowFactory;
//import com.intellij.openapi.wm.ToolWindowManager;
//import com.intellij.ui.ScrollPaneFactory;
//import com.intellij.ui.awt.RelativePoint;
//import com.intellij.ui.content.Content;
//import com.intellij.ui.content.ContentFactory;
//import com.intellij.ui.content.ContentManager;
//import com.intellij.ui.table.JBTable;
//import handler.fund.impl.TianTianFundHandler;
//import kotlin.Unit;
//import kotlin.coroutines.Continuation;
//import org.apache.commons.lang3.StringUtils;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import utils.HttpClientPool;
//import utils.LogUtil;
//import utils.PopupsUiUtil;
//import utils.WindowUtils;
//
//import javax.swing.*;
//import javax.swing.border.EmptyBorder;
//import java.awt.*;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.awt.event.MouseMotionAdapter;
//import java.net.MalformedURLException;
//import java.util.Objects;
//
///**
// *
// * @author sunjunyi
// * @date 2026/01/29.
// */
//public class ToolWindow implements ToolWindowFactory {
//
//	private static final Logger log = Logger.getInstance(FundWindow.class);
//
//	public static final String NAME = "Fund";
//
//	/**
//	 * 停止刷新按钮是否可以点击
//	 */
//	private static boolean stop_action_enabled = true;
//
//	private JPanel mPanel;
//
//	static TianTianFundHandler fundRefreshHandler;
//
//	private final StockWindow stockWindow = new StockWindow();
//	private final CoinWindow coinWindow = new CoinWindow();
//	private final FundWindow fundWindow = new FundWindow();
//
//
//	@Override
//	public void createToolWindowContent(@NotNull Project project, @NotNull com.intellij.openapi.wm.ToolWindow toolWindow) {
//		//先加载代理
//		loadProxySetting();
//
//		ContentFactory contentFactory = ContentFactory.getInstance();
//		// 基金
//		Content content_fund = contentFactory.createContent(fundWindow.getMPanel(), NAME, false);
//		//股票
//		Content content_stock = contentFactory.createContent(stockWindow.getMPanel(), StockWindow.NAME, false);
//		//虚拟货币
//		Content content_coin = contentFactory.createContent(coinWindow.getMPanel(), CoinWindow.NAME, false);
//		ContentManager contentManager = toolWindow.getContentManager();
//		contentManager.addContent(content_fund);
//		contentManager.addContent(content_stock);
//		contentManager.addContent(content_coin);
//		if (StringUtils.isEmpty(PropertiesComponent.getInstance().getValue("key_funds"))) {
//			// 没有配置基金数据，选择展示股票
//			contentManager.setSelectedContent(content_stock);
//		}
//		LogUtil.setProject(project);
//	}
//
//	private void loadProxySetting() {
//		String proxyStr = PropertiesComponent.getInstance().getValue("key_proxy");
//		HttpClientPool.getHttpClient().buildHttpClient(proxyStr);
//	}
//
//	@Override
//	public void init(@NotNull com.intellij.openapi.wm.ToolWindow window) {
//		// 重要：由于idea项目窗口可多个，导致FundWindow#init方法被多次调用，出现UI和逻辑错误(bug #53)，故加此判断解决
//		if (Objects.nonNull(fundRefreshHandler)) {
//			LogUtil.notifyInfo("Leeks UI已初始化");
//			return;
//		}
//
//		JLabel refreshTimeLabel = new JLabel();
//		refreshTimeLabel.setToolTipText("最后刷新时间");
//		refreshTimeLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
//		JBTable table = new JBTable();
//		//记录列名的变化
//		table.getTableHeader().addMouseMotionListener(new MouseMotionAdapter() {
//			@Override
//			public void mouseDragged(MouseEvent e) {
//				StringBuilder tableHeadChange = new StringBuilder();
//				for (int i = 0; i < table.getColumnCount(); i++) {
//					tableHeadChange.append(table.getColumnName(i)).append(",");
//				}
//				PropertiesComponent instance = PropertiesComponent.getInstance();
//				//将列名的修改放入环境中 key:fund_table_header_key
//				instance.setValue(WindowUtils.FUND_TABLE_HEADER_KEY, tableHeadChange
//						.substring(0, !tableHeadChange.isEmpty() ? tableHeadChange.length() - 1 : 0));
//
//			}
//
//		});
//		table.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mousePressed(MouseEvent e) {
//				if (table.getSelectedRow() < 0)
//					return;
//				String code = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), fundRefreshHandler.getCodeColumnIndex()));//FIX 移动列导致的BUG
//				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
//					// 鼠标左键双击
//					try {
//						PopupsUiUtil.showImageByFundCode(code, PopupsUiUtil.FundShowType.gsz, new Point(e.getXOnScreen(), e.getYOnScreen()));
//					} catch (MalformedURLException ex) {
//						log.error("showImageByFundCode出现异常,原因:", ex);
//					}
//				} else if (SwingUtilities.isRightMouseButton(e)) {
//					//鼠标右键
//					JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<PopupsUiUtil.FundShowType>("",
//							PopupsUiUtil.FundShowType.values()) {
//						@Override
//						public @NotNull String getTextFor(PopupsUiUtil.FundShowType value) {
//							return value.getDesc();
//						}
//
//						@Override
//						public @Nullable PopupStep onChosen(PopupsUiUtil.FundShowType selectedValue, boolean finalChoice) {
//							try {
//								PopupsUiUtil.showImageByFundCode(code, selectedValue, new Point(e.getXOnScreen(), e.getYOnScreen()));
//							} catch (MalformedURLException ex) {
//								log.error("showImageByFundCode出现异常,原因:", ex);
//							}
//							return super.onChosen(selectedValue, finalChoice);
//						}
//					}).show(RelativePoint.fromScreen(new Point(e.getXOnScreen(), e.getYOnScreen())));
//				}
//			}
//		});
//		fundRefreshHandler = new TianTianFundHandler(table, refreshTimeLabel);
//
//
//		AnAction stopAction = new AnAction(
//				"停止刷新当前表格数据", "停止刷新当前表格数据", AllIcons.Actions.Pause) {
//			@Override
//			public void actionPerformed(@NotNull AnActionEvent e) {
//				stop();
//				stop_action_enabled = false;
//			}
//
//			@Override
//			public void update(@NotNull AnActionEvent e) {
//				e.getPresentation().setEnabled(stop_action_enabled);
//			}
//		};
//
//		AnAction refreshAction = new AnAction("持续刷新当前表格数据", "持续刷新当前表格数据", AllIcons.Actions.Refresh) {
//			@Override
//			public void actionPerformed(@NotNull AnActionEvent e) {
//				refresh();
//				stop_action_enabled = true;
//			}
//		};
//
//		// Action 组
//		DefaultActionGroup actionGroup = new DefaultActionGroup();
//		actionGroup.add(refreshAction);
//		actionGroup.add(stopAction);
//
//		// 创建 Toolbar
//		ActionToolbar toolbar = ActionManager.getInstance()
//				.createActionToolbar("TableToolbar", actionGroup, true);
//		toolbar.setTargetComponent(table);
//
//		// 顶部工具栏面板（可扩展右侧组件）
//		JPanel toolbarPanel = new JPanel(new BorderLayout());
//		toolbarPanel.add(toolbar.getComponent(), BorderLayout.CENTER);
//		toolbarPanel.add(refreshTimeLabel, BorderLayout.EAST);
//
//		// 主面板布局
//		JPanel toolPanel = new JPanel(new BorderLayout());
//		toolPanel.add(toolbarPanel, BorderLayout.NORTH);
//		toolPanel.add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER);
//
//		toolPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
//
//		// 放入父容器
//		mPanel.add(toolPanel, BorderLayout.CENTER);
//		// 刷新一次数据
//		apply();
//	}
//}
