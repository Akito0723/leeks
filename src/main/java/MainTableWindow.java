import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import utils.*;

import javax.swing.*;
import java.util.*;

public class MainTableWindow implements ToolWindowFactory {

	private static final Logger log = Logger.getInstance(MainTableWindow.class);

//	private JPanel mPanel;

	/**
	 * key: project.getLocationHash()
	 * value: this
	 */
	private static final Map<Project, MainTableWindow> instances = new HashMap<>();

	@Getter
	private FundWindow fundWindow;

	@Getter
    private StockWindow stockWindow;

	@Getter
	private CoinWindow coinWindow;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		instances.put(project, this);
        //先加载代理
        loadProxySetting();

        ContentFactory contentFactory = ContentFactory.getInstance();
		// 基金
        Content content_fund = contentFactory.createContent(fundWindow.getMPanel(), fundWindow.getName(), false);
        // 股票
        Content content_stock = contentFactory.createContent(stockWindow.getMPanel(), stockWindow.getName(), false);
        // 虚拟货币
        Content content_coin = contentFactory.createContent(coinWindow.getMPanel(), coinWindow.getName(), false);
        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(content_fund);
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

		this.fundWindow = new FundWindow();
		this.stockWindow = new StockWindow();
		this.coinWindow = new CoinWindow();

		ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading...") {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				// 刷新一次数据
				fundWindow.apply();
				stockWindow.apply();
				coinWindow.apply();
			}
		});
	}


	@Override
	public boolean shouldBeAvailable(@NotNull Project project) {
		return true;
	}


	public static MainTableWindow getInstance(Project project) {
		return instances.get(project);
	}
}
