import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
import handler.BaseTableRefreshHandler;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import quartz.HandlerJob;
import quartz.QuartzManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author akihiro
 * @date 2026/02/02.
 */
public abstract class BaseLeeksWindow {

	@Getter
	protected JPanel mPanel;

	protected final JBTable table;

	protected final JLabel refreshTimeLabel;

	/**
	 * 停止刷新按钮是否可以点击
	 */
	private boolean stop_action_enabled = true;

	protected BaseTableRefreshHandler baseTableRefreshHandler;

	protected abstract BaseTableRefreshHandler factoryHandler();

	public abstract String getName();

	public BaseLeeksWindow() {
		this.mPanel = new JPanel(new BorderLayout());
		refreshTimeLabel = new JLabel();
		refreshTimeLabel.setToolTipText("最后刷新时间");
		refreshTimeLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
		table = new JBTable();
		baseTableRefreshHandler = factoryHandler();

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
		this.mPanel.add(toolPanel, BorderLayout.CENTER);
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

	public void apply() {
		if (baseTableRefreshHandler == null) {
			return;
		}
		baseTableRefreshHandler = factoryHandler();
		PropertiesComponent instance = PropertiesComponent.getInstance();
		baseTableRefreshHandler.setStriped(instance.getBoolean("key_table_striped"));
		baseTableRefreshHandler.clearRow();
		baseTableRefreshHandler.setupTable(this.loadCodeConfig());
		refresh();
	}

	public void refresh() {
		if (baseTableRefreshHandler == null) {
			return;
		}
		PropertiesComponent instance = PropertiesComponent.getInstance();
		baseTableRefreshHandler.setColorful(instance.getBoolean("key_colorful"));
		List<String> codes = this.loadCodeConfig();
		if (CollectionUtils.isEmpty(codes)) {
			// 如果没有数据则不需要启动时钟任务浪费资源
			stop();
		} else {
			baseTableRefreshHandler.refreshTableUIData(codes);
			QuartzManager quartzManager = QuartzManager.getInstance(getName());
			HashMap<String, Object> dataMap = new HashMap<>();
			dataMap.put(HandlerJob.KEY_HANDLER, baseTableRefreshHandler);
			dataMap.put(HandlerJob.KEY_CODES, codes);
			String cronExpression = getCronExpression();
			quartzManager.runJob(HandlerJob.class, cronExpression, dataMap);
		}
	}

	public void stop() {
		QuartzManager.getInstance(getName()).stopJob();
		if (baseTableRefreshHandler != null) {
			baseTableRefreshHandler.stopHandle();
		}
	}

	protected abstract String getCronExpression();

	protected abstract List<String> loadCodeConfig();
}
