package handler.coin.impl;

import bean.BaseLeeksBean;
import bean.CoinBean;
import bean.YahooResponse;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import handler.coin.CoinRefreshHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import utils.HttpClientPool;
import utils.LogUtil;

import javax.swing.*;
import java.util.Date;
import java.util.function.Consumer;

public class YahooCoinHandler extends CoinRefreshHandler {

	private static final Logger log = Logger.getInstance(YahooCoinHandler.class);

    private final String URL = "https://query1.finance.yahoo.com/v7/finance/quote?&symbols=";
    private final String KEYS = "&fields=regularMarketChange,regularMarketChangePercent,regularMarketPrice,regularMarketTime,regularMarketDayHigh,regularMarketDayLow";

    private final Gson gson = new Gson();

	public YahooCoinHandler(JTable table, JLabel label) {
		super(table, label);
	}



	@Override
	public void updateByAPI(Consumer<BaseLeeksBean> updateEachLineUI) {
		// 股票编码,号分割
		String params = Joiner.on(",").join(leeksBeanMap.keySet());
		String resp = "";
		try {
			resp = HttpClientPool.getHttpClient().get(URL + params + KEYS);
		} catch (Exception e) {
			log.error("拉取雅虎query1.finance.yahoo.com/v7数据失败,原因:", e);
			LogUtil.notifyError(e.getMessage());
			return;
		}
		if (StringUtils.contains(resp, "no longer be accessible from mainland China")) {
			LogUtil.notifyInfo("https://query1.finance.yahoo.com/v7/finance/quote no longer be accessible from mainland China");
			return;
		}
		YahooResponse yahooResponse = gson.fromJson(resp, YahooResponse.class);
		for (CoinBean coinBean : yahooResponse.getQuoteResponse().getResult()) {
			handleEachLine(coinBean, updateEachLineUI);
		}
	}

	/**
	 * 解析每一行
	 */
	private void handleEachLine(CoinBean coinBean, Consumer<BaseLeeksBean> updateEachLineUI) {
		try {
			String code = coinBean.getSymbol();
			CoinBean bean = (CoinBean) leeksBeanMap.get(code);
			if (bean == null) {
				bean = new CoinBean();
				bean.setCode(code);
				leeksBeanMap.put(code, bean);
				// 这里不应该走
				log.warn("虚拟币代码:" + code + "对应行未初始化,请检查!");
			}

			bean.setRegularMarketPrice(coinBean.getRegularMarketPrice());
			bean.setRegularMarketChange(coinBean.getRegularMarketChange());
			bean.setRegularMarketChangePercent(coinBean.getRegularMarketChangePercent());
			bean.setRegularMarketDayHigh(coinBean.getRegularMarketDayHigh());
			bean.setRegularMarketDayLow(coinBean.getRegularMarketDayLow());
			bean.setRegularMarketTime(coinBean.getRegularMarketTime());
			// 处理完成一个更新一行
			updateEachLineUI.accept(bean);
		} catch (Exception e) {
			log.error("解析雅虎query1.finance.yahoo.com/v7数据失败,原因:", e);
			LogUtil.notifyError(e.getMessage());
		}
	}



    @Override
    public void stopHandle() {
//		LogUtil.notifyInfo("停止更新Coin数据.");
    }
}
