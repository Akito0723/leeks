import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.table.JBTable;
import handler.stock.impl.SinaStockTableHandler;
import handler.stock.StockTableRefreshHandler;
import handler.stock.impl.TencentStockTableHandler;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import quartz.HandlerJob;
import quartz.QuartzManager;
import utils.LogUtil;
import utils.PopupsUiUtil;
import utils.WindowUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;

public class StockWindow {
    public static final String NAME = "Stock";

	@Getter
	private JPanel mPanel;

	private final JBTable table;

	private final JLabel refreshTimeLabel;
	/**
	 * 停止刷新按钮是否可以点击
	 */
	private static boolean stop_action_enabled = true;

    static StockTableRefreshHandler stockTableRefreshHandler;


    public StockWindow() {
		refreshTimeLabel = new JLabel();
		refreshTimeLabel.setToolTipText("最后刷新时间");
		refreshTimeLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
		table = new JBTable();

		// 切换接口
		stockTableRefreshHandler = factoryHandler();

		// 列拖动事件
		WindowUtils.TableHeadChangeAdapter tableHeadChangeAdapter =
				new WindowUtils.TableHeadChangeAdapter(table, WindowUtils.STOCK_TABLE_HEADER_KEY);
		table.getTableHeader().addMouseMotionListener(tableHeadChangeAdapter);

		// 行点击事件
		WindowUtils.TableRowMouseAdapter tableRowMouseAdapter =
				new WindowUtils.TableRowMouseAdapter(table, stockTableRefreshHandler);
		table.addMouseListener(tableRowMouseAdapter);

		// 工具栏 actionGroup
		DefaultActionGroup actionGroup = getActionGroup();

		// 创建 Toolbar
		ActionToolbar toolbar = ActionManager.getInstance()
				.createActionToolbar("TableToolbar", actionGroup, true);
		toolbar.setTargetComponent(table);

		// 顶部工具栏面板（可扩展右侧组件）
		JPanel toolbarPanel = new JPanel(new BorderLayout());
		toolbarPanel.add(toolbar.getComponent(), BorderLayout.CENTER);
		toolbarPanel.add(refreshTimeLabel, BorderLayout.EAST);

		// 主面板布局
		JPanel toolPanel = new JPanel(new BorderLayout());
		toolPanel.add(toolbarPanel, BorderLayout.NORTH);
		toolPanel.add(ScrollPaneFactory.createScrollPane(table), BorderLayout.CENTER);

		toolPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

		// 放入父容器
		mPanel.add(toolPanel, BorderLayout.CENTER);
    }

    private StockTableRefreshHandler factoryHandler() {
        boolean useSinaApi = PropertiesComponent.getInstance().getBoolean("key_stocks_sina");
        if (useSinaApi){
            if (stockTableRefreshHandler instanceof SinaStockTableHandler){
                return stockTableRefreshHandler;
            }
            return new SinaStockTableHandler(table, refreshTimeLabel);
        }
        if (stockTableRefreshHandler instanceof TencentStockTableHandler){
            return stockTableRefreshHandler;
        }
        return  new TencentStockTableHandler(table, refreshTimeLabel);
    }

	private static @NotNull DefaultActionGroup getActionGroup() {
		AnAction stopAction = new AnAction("停止刷新当前表格数据", "停止刷新当前表格数据", AllIcons.Actions.Pause) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				stop();
				stop_action_enabled = false;
			}

			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(stop_action_enabled);
			}
		};

		AnAction refreshAction = new AnAction("持续刷新当前表格数据", "持续刷新当前表格数据", AllIcons.Actions.Refresh) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				refresh();
				stop_action_enabled = true;
			}
		};

		// Action 组
		DefaultActionGroup actionGroup = new DefaultActionGroup();
		actionGroup.add(refreshAction);
		actionGroup.add(stopAction);
		return actionGroup;
	}

    public void apply() {
        if (stockTableRefreshHandler != null) {
			stockTableRefreshHandler = factoryHandler();
            PropertiesComponent instance = PropertiesComponent.getInstance();
			stockTableRefreshHandler.setStriped(instance.getBoolean("key_table_striped"));
			stockTableRefreshHandler.clearRow();
			stockTableRefreshHandler.setupTable(loadStocks());
            refresh();
        }
    }
    public static void refresh() {
        if (stockTableRefreshHandler != null) {
            PropertiesComponent instance = PropertiesComponent.getInstance();
			stockTableRefreshHandler.refreshColorful(instance.getBoolean("key_colorful"));
            List<String> codes = loadStocks();
            if (CollectionUtils.isEmpty(codes)) {
				// 如果没有数据则不需要启动时钟任务浪费资源
                stop();
            } else {
				stockTableRefreshHandler.refreshTableUIData(codes);
                QuartzManager quartzManager = QuartzManager.getInstance(NAME);
                HashMap<String, Object> dataMap = new HashMap<>();
                dataMap.put(HandlerJob.KEY_HANDLER, stockTableRefreshHandler);
                dataMap.put(HandlerJob.KEY_CODES, codes);
                String cronExpression = instance.getValue("key_cron_expression_stock");
                if (StringUtils.isEmpty(cronExpression)) {
                    cronExpression = "*/10 * * * * ?";
                }
                quartzManager.runJob(HandlerJob.class, cronExpression, dataMap);
            }
        }
    }

    public static void stop() {
        QuartzManager.getInstance(NAME).stopJob();
        if (stockTableRefreshHandler != null) {
			stockTableRefreshHandler.stopHandle();
        }
    }

    private static List<String> loadStocks(){
        return SettingsWindow.getConfigList("key_stocks");
    }

}
