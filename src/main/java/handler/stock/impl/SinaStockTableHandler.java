package handler.stock.impl;

import bean.BaseLeeksBean;
import bean.StockBean;
import com.google.common.base.Joiner;
import com.intellij.openapi.diagnostic.Logger;
import handler.stock.StockTableRefreshHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import utils.HttpClientPool;
import utils.LogUtil;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SinaStockTableHandler extends StockTableRefreshHandler {

	private static final Logger log = Logger.getInstance(SinaStockTableHandler.class);

	private final String url = "http://hq.sinajs.cn/list=";

	private final Pattern DEFAULT_STOCK_PATTERN = Pattern.compile("var hq_str_(\\w+?)=\"(.*?)\";");

    public SinaStockTableHandler(JTable table, JLabel label) {
		super(table, label);
    }

	@Override
	public void updateByAPI(Consumer<BaseLeeksBean> updateEachLineUI) {
		// 股票编码,号分割
		String params = Joiner.on(",").join(leeksBeanMap.keySet());
		String resp = "";
		try {
			resp = HttpClientPool.getHttpClient().get(url + params);
		} catch (Exception e) {
			log.error("拉取新浪hq.sinajs.cn数据失败,原因:", e);
			LogUtil.notifyError(e.getMessage());
			return;
		}
		for (String line : resp.split("\n")) {
			handleEachLine(line, updateEachLineUI);
		}
	}

	/**
	 * 解析每一行
	 */
    private void handleEachLine(String line, Consumer<BaseLeeksBean> updateEachLineUI) {
		try {
			Matcher matcher = DEFAULT_STOCK_PATTERN.matcher(line);
			if (!matcher.matches()) {
				return;
			}
			String code = matcher.group(1);
			String[] split = matcher.group(2).split(",");
			if (split.length < 32) {
				return;
			}
			StockBean bean = (StockBean) leeksBeanMap.get(code);
			if (bean == null) {
				bean = new StockBean();
				bean.setCode(code);
				leeksBeanMap.put(code, bean);
				// 这里不应该走
				log.warn("股票代码:" + code + "对应行未初始化,请检查!");
			}

			bean.setName(split[0]);
			BigDecimal now = new BigDecimal(split[3]);
			BigDecimal yesterday = new BigDecimal(split[2]);
			BigDecimal diff = now.add(yesterday.negate());

			bean.setNow(now.toString());
			bean.setChange(diff.toString());
			BigDecimal percent = diff.divide(yesterday, 4, RoundingMode.HALF_UP)
					.multiply(BigDecimal.TEN)
					.multiply(BigDecimal.TEN)
					.setScale(2, RoundingMode.HALF_UP);
			bean.setChangePercent(percent.toString());

			try {
				// 20260128161413
				Date date = DateUtils.parseDate(split[31], "yyyyMMddHHmmss");
				bean.setTime(date);
			} catch (Exception e) {
				log.error("解析日期:" + split[31]+ "失败,原因:", e);
				bean.setTime(null);
			}
			bean.setMax(split[4]);
			bean.setMin(split[5]);

			String costPriceStr = bean.getCostPrice();
			// 处理成本价和持仓
			if (StringUtils.isNotEmpty(costPriceStr)) {
				BigDecimal costPriceDec = new BigDecimal(costPriceStr);
				BigDecimal incomeDiff = now.add(costPriceDec.negate());
				BigDecimal incomePercentDec = incomeDiff.divide(costPriceDec, 5, RoundingMode.HALF_UP)
						.multiply(BigDecimal.TEN)
						.multiply(BigDecimal.TEN)
						.setScale(3, RoundingMode.HALF_UP);
				bean.setIncomePercent(incomePercentDec.toString());

				String bondStr = bean.getBonds();
				if (StringUtils.isNotEmpty(bondStr)) {
					BigDecimal bondDec = new BigDecimal(bondStr);
					BigDecimal incomeDec = incomeDiff.multiply(bondDec)
							.setScale(2, RoundingMode.HALF_UP);
					bean.setIncome(incomeDec.toString());
				}
			}
			// 处理完成一个更新一行
			updateEachLineUI.accept(bean);
		} catch (Exception e) {
			log.error("解析新浪hq.sinajs.cn数据失败,原因:", e);
			LogUtil.notifyError(e.getMessage());
		}
    }

    @Override
    public void stopHandle() {
    }
}
