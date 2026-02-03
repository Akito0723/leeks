import com.intellij.ide.util.PropertiesComponent;
import handler.BaseTableRefreshHandler;
import handler.fund.impl.TianTianFundHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 *
 * @author akihiro
 * @date 2026/02/02.
 */
public class FundWindow extends BaseLeeksWindow {

	private static final String NAME = "Fund";

	public FundWindow() {
		super();
	}

	@Override
	protected BaseTableRefreshHandler factoryHandler() {
		return new TianTianFundHandler(table, refreshTimeLabel);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	protected String getCronExpression() {
		PropertiesComponent instance = PropertiesComponent.getInstance();
		String cronExpression = instance.getValue("key_cron_expression_fund");
		return StringUtils.defaultIfBlank(cronExpression, "0 * * * * ?");
	}

	@Override
	protected List<String> loadCodeConfig() {
		return SettingsWindow.getConfigList("key_funds");
	}
}
