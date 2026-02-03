package handler.stock.impl;

import bean.BaseLeeksBean;
import bean.EastMoneyResponse;
import bean.StockBean;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import handler.stock.StockTableRefreshHandler;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import utils.HttpClientPool;
import utils.LogUtil;

import javax.swing.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * eastmoney.com 东方财富
 * @author akihiro
 * @date 2026/01/29.
 */
public class EastMoneyTableHandler extends StockTableRefreshHandler {

	private static final Logger log = Logger.getInstance(EastMoneyTableHandler.class);

	private final Gson gson = new Gson();

	/**
	 * key: 东财code: 1.000001, 0.399001
	 * value: sh000001,sz399001
	 */
	private final Map<String, String> eastMoneyCodeMap = new HashMap<>();

	public EastMoneyTableHandler(JTable table, JLabel refreshTimeLabel) {
		super(table, refreshTimeLabel);
	}

	@Override
	public void updateByAPI(Consumer<BaseLeeksBean> updateEachLineUI) {
		String resp = "";
		try {
			String urlFormat = "https://push2delay.eastmoney.com/api/qt/ulist.np/get?fltt=2"
					+ "&fields=f2,f3,f4,f12,f13,f14,f15,f16,f124"
					+ "&secids=%s";

			resp = HttpClientPool.getHttpClient().get(String.format(urlFormat, genSecids()));
		} catch (Exception e) {
			log.error("拉取东方财富eastmoney.com数据失败,原因:", e);
			LogUtil.notifyError(e.getMessage());
			return;
		}
		EastMoneyResponse eastMoneyResponse;
		try {
			eastMoneyResponse = gson.fromJson(resp, EastMoneyResponse.class);
		} catch (com.google.gson.JsonSyntaxException e) {
			log.error("解析东方财富eastmoney.com数据失败,原因:", e);
			LogUtil.notifyInfo(e.getMessage());
			return;
		}
		if (eastMoneyResponse.getData() == null || CollectionUtils.isEmpty(eastMoneyResponse.getData().getDiff())) {
			return;
		}
		for (EastMoneyResponse.Data.Diff diff : eastMoneyResponse.getData().getDiff()) {
			handleEachLine(diff, updateEachLineUI);
		}


	}

	private String genSecids() {
		List<String> secidList = Lists.newArrayList();
		for (String code : leeksBeanMap.keySet()) {
			String eastMoneyCode = "";
			if (StringUtils.startsWithIgnoreCase(code, "sh")) {
				// 上证
				eastMoneyCode = String.format(
						"1.%s", StringUtils.removeStartIgnoreCase(code, "sh"));
			} else if (StringUtils.startsWithIgnoreCase(code, "sz")) {
				// 深证
				eastMoneyCode = String.format(
						"0.%s", StringUtils.removeStartIgnoreCase(code, "sz"));
			} else if (StringUtils.startsWithIgnoreCase(code, "hk")) {
				// 港股
				eastMoneyCode = String.format(
						"100.%s", StringUtils.removeStartIgnoreCase(code, "hk"));

			} else {
				continue;
			}
			secidList.add(eastMoneyCode);
			eastMoneyCodeMap.put(eastMoneyCode, code);
		}
		return String.join(",", secidList);
	}

	/**
	 * 解析每一行
	 */
	private void handleEachLine(EastMoneyResponse.Data.Diff diff, Consumer<BaseLeeksBean> updateEachLineUI) {
		try {
			String eastMoneyCode = diff.getF13() + "." + diff.getF12();
			String code = eastMoneyCodeMap.get(eastMoneyCode);
			StockBean bean = (StockBean) leeksBeanMap.get(code);
			if (bean == null) {
				bean = new StockBean();
				bean.setCode(code);
				leeksBeanMap.put(code, bean);
				// 这里不应该走
				log.warn("股票代码:" + code + "对应行未初始化,请检查!");
			}

			bean.setName(diff.getF14());
			bean.setNow(diff.getF2());
			bean.setChange(diff.getF4());
			bean.setChangePercent(diff.getF3());


			try {
				// "f124": 1770010998,
				java.time.Instant instant = java.time.Instant.ofEpochSecond(Long.parseLong(diff.getF124()));
				Date date = java.util.Date.from(instant);
				bean.setTime(date);
			} catch (Exception e) {
				log.error("解析日期:" + diff.getF124() + "失败,原因:", e);
				bean.setTime(null);
			}

			bean.setMax(diff.getF15());
			bean.setMin(diff.getF16());

			// 成本价和持仓
			handlerCostAndBond(bean);
			// 处理完成一个更新一行
			updateEachLineUI.accept(bean);
		} catch (Exception e) {
			log.error("解析东方财富eastmoney.com数据失败,原因:", e);
			LogUtil.notifyInfo(e.getMessage());
		}
	}

	@Override
	public void stopHandle() {

	}
}
