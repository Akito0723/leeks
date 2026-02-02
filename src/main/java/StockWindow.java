import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import handler.BaseTableRefreshHandler;
import handler.stock.impl.EastMoneyTableHandler;
import handler.stock.impl.TencentStockTableHandler;
import org.apache.commons.lang3.StringUtils;
import utils.WindowUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class StockWindow extends BaseLeeksWindow {

	private static final String NAME = "Stock";


    public StockWindow() {
		super();

		// 列拖动事件
		WindowUtils.TableHeadChangeAdapter tableHeadChangeAdapter =
				new WindowUtils.TableHeadChangeAdapter(table, WindowUtils.STOCK_TABLE_HEADER_KEY);
		table.getTableHeader().addMouseMotionListener(tableHeadChangeAdapter);

		// 行点击事件
		WindowUtils.TableRowMouseAdapter tableRowMouseAdapter =
				new WindowUtils.TableRowMouseAdapter(table, baseTableRefreshHandler);
		table.addMouseListener(tableRowMouseAdapter);
    }

	@Override
    public BaseTableRefreshHandler factoryHandler() {

		String stockApi = PropertiesComponent.getInstance().getValue("key_stock_api");
		if (StringUtils.equals(stockApi, "腾讯财经")) {
			if (baseTableRefreshHandler != null && baseTableRefreshHandler instanceof TencentStockTableHandler) {
				return baseTableRefreshHandler;
			}
			return new TencentStockTableHandler(table, refreshTimeLabel);
		} else if (StringUtils.equals(stockApi, "东方财富")) {
			if (baseTableRefreshHandler != null && baseTableRefreshHandler instanceof EastMoneyTableHandler) {
				return baseTableRefreshHandler;
			}
			return new EastMoneyTableHandler(table, refreshTimeLabel);

		}
		// 默认用qt.gtimg.cn
		if (baseTableRefreshHandler != null && baseTableRefreshHandler instanceof TencentStockTableHandler) {
			return baseTableRefreshHandler;
		}
		return new TencentStockTableHandler(table, refreshTimeLabel);
    }

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	protected String getCronExpression() {
		PropertiesComponent instance = PropertiesComponent.getInstance();
		String cronExpression = instance.getValue("key_cron_expression_stock");
		return StringUtils.defaultIfBlank(cronExpression,  "*/10 * * * * ?");
	}

	@Override
	protected List<String> loadCodeConfig() {
		return SettingsWindow.getConfigList("key_stocks");
	}

}
