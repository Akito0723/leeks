import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
import handler.coin.CoinRefreshHandler;
import handler.coin.impl.YahooCoinHandler;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import quartz.HandlerJob;
import quartz.QuartzManager;
import utils.WindowUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class CoinWindow {
    public static final String NAME = "Coin";

	@Getter
	private JPanel mPanel;

    private CoinRefreshHandler coinRefreshHandler;

	/**
	 * 停止刷新按钮是否可以点击
	 */
	private static boolean stop_action_enabled = true;

    public CoinWindow() {

		JLabel refreshTimeLabel = new JLabel();
		refreshTimeLabel.setToolTipText("最后刷新时间");
		refreshTimeLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
		JBTable table = new JBTable();

		coinRefreshHandler = new YahooCoinHandler(table, refreshTimeLabel);

		// 列拖动事件
		WindowUtils.TableHeadChangeAdapter tableHeadChangeAdapter =
				new WindowUtils.TableHeadChangeAdapter(table, WindowUtils.FUND_TABLE_HEADER_KEY);
		table.getTableHeader().addMouseMotionListener(tableHeadChangeAdapter);

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



    public void apply() {
        if (coinRefreshHandler != null) {
            PropertiesComponent instance = PropertiesComponent.getInstance();
			coinRefreshHandler.setStriped(instance.getBoolean("key_table_striped"));
			coinRefreshHandler.clearRow();
			coinRefreshHandler.setupTable(loadCoins());
            refresh();
        }
    }
    public void refresh() {
        if (coinRefreshHandler != null) {
            PropertiesComponent instance = PropertiesComponent.getInstance();
			coinRefreshHandler.refreshColorful(instance.getBoolean("key_colorful"));
            List<String> codes = loadCoins();
            if (CollectionUtils.isEmpty(codes)) {
				// 如果没有数据则不需要启动时钟任务浪费资源
                stop();
            } else {
				coinRefreshHandler.refreshTableUIData(codes);
                QuartzManager quartzManager = QuartzManager.getInstance(NAME);
                HashMap<String, Object> dataMap = new HashMap<>();
                dataMap.put(HandlerJob.KEY_HANDLER, coinRefreshHandler);
                dataMap.put(HandlerJob.KEY_CODES, codes);
                String cronExpression = instance.getValue("key_cron_expression_coin");
                if (StringUtils.isEmpty(cronExpression)) {
                    cronExpression = "*/10 * * * * ?";
                }
                quartzManager.runJob(HandlerJob.class, cronExpression, dataMap);
            }
        }
    }

    public void stop() {
        QuartzManager.getInstance(NAME).stopJob();
        if (coinRefreshHandler != null) {
			coinRefreshHandler.stopHandle();
        }
    }

    private List<String> loadCoins(){
        return SettingsWindow.getConfigList("key_coins", "[,，]");
    }

	private @NotNull DefaultActionGroup getActionGroup() {
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

}
