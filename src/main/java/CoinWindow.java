import com.intellij.ide.util.PropertiesComponent;
import handler.BaseTableRefreshHandler;
import handler.coin.impl.YahooCoinHandler;
import org.apache.commons.lang3.StringUtils;
import utils.WindowUtils;

import java.util.List;

public class CoinWindow extends BaseLeeksWindow {
    private static final String NAME = "Coin";

    public CoinWindow() {
		super();
		// 列拖动事件
		WindowUtils.TableHeadChangeAdapter tableHeadChangeAdapter =
				new WindowUtils.TableHeadChangeAdapter(table, WindowUtils.FUND_TABLE_HEADER_KEY);
		table.getTableHeader().addMouseMotionListener(tableHeadChangeAdapter);
    }

	@Override
	protected BaseTableRefreshHandler factoryHandler() {
		return new YahooCoinHandler(table, refreshTimeLabel);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	protected String getCronExpression() {
		PropertiesComponent instance = PropertiesComponent.getInstance();
		String cronExpression = instance.getValue("key_cron_expression_coin");
		return StringUtils.defaultIfBlank(cronExpression, "*/10 * * * * ?");
	}

	@Override
	protected List<String> loadCodeConfig() {
		return SettingsWindow.getConfigList("key_coins", "[,，]");
	}
}
