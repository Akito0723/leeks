import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.table.JBTable;
import handler.fund.FundRefreshHandler;
import handler.fund.impl.TianTianFundHandler;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import quartz.HandlerJob;
import quartz.QuartzManager;
import utils.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.*;

public class FundWindow implements ToolWindowFactory {

	private static final Logger log = Logger.getInstance(FundWindow.class);

	/**
	 * key: project.getLocationHash()
	 * value: this
	 */
	private static final Map<Project, FundWindow> instances = new HashMap<>();

    public static final String NAME = "Fund";

	/**
	 * 停止刷新按钮是否可以点击
	 */
	private static boolean stop_action_enabled = true;

	@Getter
	private JPanel mPanel;

	private FundRefreshHandler fundRefreshHandler;

	@Getter
    private final StockWindow stockWindow = new StockWindow();

	@Getter
	private final CoinWindow coinWindow = new CoinWindow();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		instances.put(project, this);
        //先加载代理
        loadProxySetting();

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mPanel, NAME, false);
        //股票
        Content content_stock = contentFactory.createContent(stockWindow.getMPanel(), StockWindow.NAME, false);
        //虚拟货币
        Content content_coin = contentFactory.createContent(coinWindow.getMPanel(), CoinWindow.NAME, false);
        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(content);
        contentManager.addContent(content_stock);
        contentManager.addContent(content_coin);
        if (StringUtils.isEmpty(PropertiesComponent.getInstance().getValue("key_funds"))) {
            // 没有配置基金数据，选择展示股票
            contentManager.setSelectedContent(content_stock);
        }
		LogUtil.setNotifyProject(project);
    }

    private void loadProxySetting() {
        String proxyStr = PropertiesComponent.getInstance().getValue("key_proxy");
        HttpClientPool.getHttpClient().buildHttpClient(proxyStr);
    }

    @Override
    public void init(@NotNull ToolWindow window) {
        // 重要：由于idea项目窗口可多个，导致FundWindow#init方法被多次调用，出现UI和逻辑错误(bug #53)，故加此判断解决
		Project project = window.getProject();
		if (!instances.isEmpty()) {
			for (Project beforeProject : instances.keySet()) {
				log.info("UI 已在project[name]:" + beforeProject.getName() + " 中初始化");
			}
			log.debug("当前project[name]:" + project.getName());
			return;
		}
		JLabel refreshTimeLabel = new JLabel();
		refreshTimeLabel.setToolTipText("最后刷新时间");
		refreshTimeLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
		JBTable table = new JBTable();

		fundRefreshHandler = new TianTianFundHandler(table, refreshTimeLabel);

        // 列拖动事件
		WindowUtils.TableHeadChangeAdapter tableHeadChangeAdapter =
				new WindowUtils.TableHeadChangeAdapter(table, WindowUtils.FUND_TABLE_HEADER_KEY);
        table.getTableHeader().addMouseMotionListener(tableHeadChangeAdapter);

		// 行点击事件
		WindowUtils.TableRowMouseAdapter tableRowMouseAdapter =
				new WindowUtils.TableRowMouseAdapter(table, fundRefreshHandler);
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

		ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading...") {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				// 刷新一次数据
				apply();
				stockWindow.apply();
				coinWindow.apply();
			}
		});
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

	private static List<String> loadFunds() {
        return SettingsWindow.getConfigList("key_funds");
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }


    public void apply() {
        if (fundRefreshHandler != null) {
            PropertiesComponent instance = PropertiesComponent.getInstance();
            fundRefreshHandler.setStriped(instance.getBoolean("key_table_striped"));
            fundRefreshHandler.clearRow();
            fundRefreshHandler.setupTable(loadFunds());
            refresh();
        }
    }

    public void refresh() {
        if (fundRefreshHandler != null) {
            PropertiesComponent instance = PropertiesComponent.getInstance();
            boolean colorful = instance.getBoolean("key_colorful");
            fundRefreshHandler.refreshColorful(colorful);
            List<String> codes = loadFunds();
            if (CollectionUtils.isEmpty(codes)) {
				// 如果没有数据则不需要启动时钟任务浪费资源
                stop();
            } else {
                fundRefreshHandler.refreshTableUIData(codes);
                QuartzManager quartzManager = QuartzManager.getInstance(NAME); // 时钟任务
                HashMap<String, Object> dataMap = new HashMap<>();
                dataMap.put(HandlerJob.KEY_HANDLER, fundRefreshHandler);
                dataMap.put(HandlerJob.KEY_CODES, codes);
                String cronExpression = instance.getValue("key_cron_expression_fund");
                if (StringUtils.isEmpty(cronExpression)) {
                    cronExpression = "0 * * * * ?";
                }
                quartzManager.runJob(HandlerJob.class, cronExpression, dataMap);
            }
        }
    }

    public void stop() {
        QuartzManager.getInstance(NAME).stopJob();
        if (fundRefreshHandler != null) {
            fundRefreshHandler.stopHandle();
        }
    }

	public static FundWindow getInstance(Project project) {
		return instances.get(project);
	}
}
