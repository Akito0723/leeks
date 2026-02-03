package handler.stock;

import bean.BaseLeeksBean;
import bean.StockBean;
import handler.BaseTableRefreshHandler;
import utils.WindowUtils;

import javax.swing.*;

public abstract class StockTableRefreshHandler extends BaseTableRefreshHandler {

	@Override
	public String getTableHeaderKey() {
		return WindowUtils.STOCK_TABLE_HEADER_KEY;
	}

	@Override
	public String getTableHeaderValue() {
		return WindowUtils.STOCK_TABLE_HEADER_VALUE;
	}

	@Override
	public String[] sortColumns() {
		return new String[]{"当前价","涨跌","涨跌幅","最高价","最低价"};
	}

	public StockTableRefreshHandler(JTable table, JLabel refreshTimeLabel) {
		super(table, refreshTimeLabel);
    }

	@Override
	public BaseLeeksBean instanceLeeksBean() {
		return new StockBean();
	}

}
