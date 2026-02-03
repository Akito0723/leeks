package handler.coin;

import bean.BaseLeeksBean;
import handler.BaseTableRefreshHandler;
import bean.CoinBean;
import utils.WindowUtils;

import javax.swing.*;

public abstract class CoinRefreshHandler extends BaseTableRefreshHandler {

	@Override
	public String getTableHeaderKey() {
		return WindowUtils.COIN_TABLE_HEADER_KEY;
	}

	@Override
	public String getTableHeaderValue() {
		return  WindowUtils.COIN_TABLE_HEADER_VALUE;
	}

	@Override
	public String[] sortColumns() {
		return new String[]{"涨跌幅"};
	}

	public CoinRefreshHandler(JTable table, JLabel refreshTimeLabel) {
		super(table, refreshTimeLabel);
	}

	@Override
	public BaseLeeksBean instanceLeeksBean() {
		return new CoinBean();
	}

}
