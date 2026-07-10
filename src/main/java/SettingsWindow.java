import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import quartz.QuartzManager;
import utils.HttpClientManager;
import utils.LogUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SettingsWindow implements Configurable {

	private static final Logger log = Logger.getInstance(SettingsWindow.class);


	private JPanel panel;
    private JTextArea textAreaFund;
    private JTextArea textAreaStock;
    private JCheckBox checkbox;
    /**
     * 使用tab界面，方便不同的设置分开进行控制
     */
    private JTabbedPane tabbedPane;
    private JCheckBox checkBoxTableStriped;
    private JTextField cronExpressionFund;
    private JTextField cronExpressionStock;
    private JTextField cronExpressionCoin;

	private JComboBox stockComboBox;
	private JLabel stockComboBoxLabel;
//    private JCheckBox checkboxSina;

    private JCheckBox checkboxLog;

    private JTextArea textAreaCoin;

    private JLabel proxyLabel;

    private JTextField inputProxy;
    private JButton proxyTestButton;


	@Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Leeks";
    }

    @Override
    public @Nullable JComponent createComponent() {
        PropertiesComponent instance = PropertiesComponent.getInstance();
        String value = instance.getValue("key_funds");
        String value_stock = instance.getValue("key_stocks");
        String value_coin = instance.getValue("key_coins");
        boolean value_color = instance.getBoolean("key_colorful");
        textAreaFund.setText(value);
        textAreaStock.setText(value_stock);
        textAreaCoin.setText(value_coin);
        checkbox.setSelected(!value_color);
        checkBoxTableStriped.setSelected(instance.getBoolean("key_table_striped"));
		// 默认腾讯财经
		stockComboBox.setSelectedItem(instance.getValue("key_stock_api","腾讯财经"));
        checkboxLog.setSelected(instance.getBoolean("key_close_log"));
		// 默认每分钟执行
        cronExpressionFund.setText(instance.getValue("key_cron_expression_fund","0 * * * * ?"));
		// 默认每10秒执行
        cronExpressionStock.setText(instance.getValue("key_cron_expression_stock","*/10 * * * * ?"));
		// 默认每10秒执行
        cronExpressionCoin.setText(instance.getValue("key_cron_expression_coin","*/10 * * * * ?"));
        //代理设置
        inputProxy.setText(instance.getValue("key_proxy"));
        proxyTestButton.addActionListener(actionEvent -> {
			String proxy = inputProxy.getText().trim();
			testProxy(proxy);
		});
        return panel;
    }

    @Override
    public boolean isModified() {
        PropertiesComponent instance = PropertiesComponent.getInstance();
        return !StringUtils.equals(textAreaFund.getText(), instance.getValue("key_funds", ""))
                || !StringUtils.equals(textAreaStock.getText(), instance.getValue("key_stocks", ""))
                || !StringUtils.equals(textAreaCoin.getText(), instance.getValue("key_coins", ""))
                || checkbox.isSelected() == instance.getBoolean("key_colorful")
                || checkBoxTableStriped.isSelected() != instance.getBoolean("key_table_striped")
                || !StringUtils.equals(String.valueOf(stockComboBox.getSelectedItem()),
                        instance.getValue("key_stock_api", "腾讯财经"))
                || checkboxLog.isSelected() != instance.getBoolean("key_close_log")
                || !StringUtils.equals(cronExpressionFund.getText(),
                        instance.getValue("key_cron_expression_fund", "0 * * * * ?"))
                || !StringUtils.equals(cronExpressionStock.getText(),
                        instance.getValue("key_cron_expression_stock", "*/10 * * * * ?"))
                || !StringUtils.equals(cronExpressionCoin.getText(),
                        instance.getValue("key_cron_expression_coin", "*/10 * * * * ?"))
                || !StringUtils.equals(inputProxy.getText().trim(), instance.getValue("key_proxy", ""));
    }

    @Override
    public void apply() throws ConfigurationException {
        String errorMsg = checkConfig();
        if (StringUtils.isNotEmpty(errorMsg)) {
            throw new ConfigurationException(errorMsg);
        }
        String proxy = inputProxy.getText().trim();
        HttpClientManager.getInstance().configureProxy(proxy);

        PropertiesComponent instance = PropertiesComponent.getInstance();
        instance.setValue("key_funds", textAreaFund.getText());
        instance.setValue("key_stocks", textAreaStock.getText());
        instance.setValue("key_coins", textAreaCoin.getText());
        instance.setValue("key_colorful",!checkbox.isSelected());
        instance.setValue("key_cron_expression_fund", cronExpressionFund.getText());
        instance.setValue("key_cron_expression_stock", cronExpressionStock.getText());
        instance.setValue("key_cron_expression_coin", cronExpressionCoin.getText());
        instance.setValue("key_table_striped", checkBoxTableStriped.isSelected());
		instance.setValue("key_stock_api", String.valueOf(stockComboBox.getSelectedItem()));
        instance.setValue("key_close_log",checkboxLog.isSelected());
        instance.setValue("key_proxy",proxy);

		Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(panel));
		if (project != null) {
			ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("leeks");
			MainTableWindow mainTableWindow = MainTableWindow.getInstance(project);
			if (mainTableWindow != null) {
				mainTableWindow.getFundWindow().apply();
				mainTableWindow.getCoinWindow().apply();
				mainTableWindow.getStockWindow().apply();
			}
		}



    }


	private void testProxy(String proxy) {
		if (StringUtils.isBlank(proxy)) {
			LogUtil.notifyWarn("请输入代理地址");
			return;
		}
		try {
			HttpClientManager.validateProxy(proxy);
		} catch (IllegalArgumentException e) {
			LogUtil.notifyWarn(e.getMessage());
			return;
		}

		proxyTestButton.setEnabled(false);
		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			try {
				HttpClientManager.getInstance().getWithProxy("https://www.baidu.com", proxy);
				SwingUtilities.invokeLater(() -> {
					proxyTestButton.setEnabled(true);
					LogUtil.notifyInfo("代理测试成功!请保存");
				});
			} catch (Exception e) {
				log.error("代理测试异常,原因:", e);
				SwingUtilities.invokeLater(() -> {
					proxyTestButton.setEnabled(true);
					LogUtil.notifyWarn("测试代理异常!");
				});
			}
		});
	}

    public static List<String> getConfigList(String key, String split) {
        String value = PropertiesComponent.getInstance().getValue(key);
        return splitConfigValue(value, split);
    }

    private static List<String> splitConfigValue(String value, String split) {
        if (StringUtils.isEmpty(value)) {
            return new ArrayList<>();
        }
        Set<String> set = new LinkedHashSet<>();
        String[] codes = value.split(split);
        for (String code : codes) {
            String trimmedCode = StringUtils.trim(code);
            if (StringUtils.isNotEmpty(trimmedCode)) {
                set.add(trimmedCode);
            }
        }
        return new ArrayList<>(set);
    }

    public static List<String> getConfigList(String key) {
        String value = PropertiesComponent.getInstance().getValue(key);
        return parseInstrumentConfig(value);
    }

    static List<String> parseInstrumentConfig(String value) {
        if (StringUtils.isEmpty(value)) {
            return new ArrayList<>();
        }
        if (value.contains(";")) {//包含分号
            return splitConfigValue(value, ";");
        }
        if (isSinglePositionConfig(value)) {
            return List.of(StringUtils.trim(value));
        }
        return splitConfigValue(value, "[,，]");
    }

    private static boolean isSinglePositionConfig(String value) {
        String[] parts = StringUtils.splitPreserveAllTokens(value, ',');
        if (parts == null || parts.length != 3) {
            return false;
        }
        String code = StringUtils.trim(parts[0]);
        String costPrice = StringUtils.trim(parts[1]);
        String position = StringUtils.trim(parts[2]);
        boolean legacyFundCodeList = code.matches("\\d{6}")
                && costPrice.matches("\\d{6}") && position.matches("\\d{6}");
        return !legacyFundCodeList && StringUtils.isNotEmpty(code)
                && NumberUtils.isCreatable(costPrice) && NumberUtils.isCreatable(position);
    }

    /**
     * 检查配置项
     *
     * @return 返回提示的错误信息
     */
    private String checkConfig() {
        StringBuilder errorMsg = new StringBuilder();
		try {
			HttpClientManager.validateProxy(inputProxy.getText().trim());
		} catch (IllegalArgumentException e) {
			errorMsg.append(e.getMessage()).append('、');
		}
        errorMsg.append(splitConfigValue(cronExpressionFund.getText(), ";").stream().map(s -> {
            if (QuartzManager.checkCronExpression(s)) {
				return "";
            } else {
				return "Fund请配置正确的cron表达式[" + s + "]、";
            }
        }).collect(Collectors.joining())); errorMsg.append(splitConfigValue(cronExpressionStock.getText(), ";").stream().map(s -> {
            if (QuartzManager.checkCronExpression(s)) {
				return "";
            } else {
				return "Stock请配置正确的cron表达式[" + s + "]、";
            }
        }).collect(Collectors.joining()));
        errorMsg.append(splitConfigValue(cronExpressionCoin.getText(), ";").stream().map(s -> {
            if (QuartzManager.checkCronExpression(s)) {
				return "";
            } else {
				return "Coin请配置正确的cron表达式[" + s + "]、";
            }
        }).collect(Collectors.joining()));
        return errorMsg.toString();
    }
}
