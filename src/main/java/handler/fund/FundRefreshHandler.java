package handler.fund;

import bean.BaseLeeksBean;
import bean.FundBean;
import handler.BaseTableRefreshHandler;
import utils.WindowUtils;

import javax.swing.*;

public abstract class FundRefreshHandler extends BaseTableRefreshHandler {

	@Override
	public String getTableHeaderKey() {
		return WindowUtils.FUND_TABLE_HEADER_KEY;
	}

	@Override
	public String getTableHeaderValue() {
		return WindowUtils.FUND_TABLE_HEADER_VALUE;
	}

	@Override
	public String[] sortColumns() {
		return new String[]{"估算净值", "估算涨跌", "收益率", "收益"};
	}

	public FundRefreshHandler(JTable table, JLabel refreshTimeLabel) {
		super(table, refreshTimeLabel);
	}

	@Override
	public BaseLeeksBean instanceLeeksBean() {
		return new FundBean();
	}
}
