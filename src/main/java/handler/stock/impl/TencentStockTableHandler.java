package handler.stock.impl;

import bean.BaseLeeksBean;
import bean.StockBean;
import com.google.common.base.Joiner;
import com.intellij.openapi.diagnostic.Logger;
import handler.stock.StockTableRefreshHandler;
import org.apache.commons.lang3.time.DateUtils;
import utils.HttpClientPool;
import utils.LogUtil;

import javax.swing.*;
import java.util.Date;
import java.util.function.Consumer;

/**
 * qt.gtimg.cn 腾讯财经
 * @author akihiro
 * @date 2026/02/02.
 */
public class TencentStockTableHandler extends StockTableRefreshHandler {

	private static final Logger log = Logger.getInstance(TencentStockTableHandler.class);

    public TencentStockTableHandler(JTable table, JLabel label) {
        super(table, label);
    }

	@Override
	public void updateByAPI(Consumer<BaseLeeksBean> updateEachLineUI) {
		String params = Joiner.on(",").join(leeksBeanMap.keySet());
		String resp = "";
		try {
			resp = HttpClientPool.getHttpClient().get("http://qt.gtimg.cn/q=" + params);
		} catch (Exception e) {
			log.error("拉取腾讯qt.gtimg.cn数据失败,原因:", e);
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
			String code = line.substring(line.indexOf("_") + 1, line.indexOf("="));

			StockBean bean = (StockBean) leeksBeanMap.get(code);
			if (bean == null) {
				bean = new StockBean();
				bean.setCode(code);
				leeksBeanMap.put(code, bean);
				// 这里不应该走
				log.warn("股票代码:" + code + "对应行未初始化,请检查!");
			}


			String dataStr = line.substring(line.indexOf("=") + 2, line.length() - 2);
			String[] values = dataStr.split("~");

			bean.setName(values[1]);
			bean.setNow(values[3]);
			bean.setChange(values[31]);
			bean.setChangePercent(values[32]);


			try {
				// 20260128161413
				Date date = DateUtils.parseDate(values[30], "yyyyMMddHHmmss");
				bean.setTime(date);
			} catch (Exception e) {
				log.error("解析日期:" + values[30] + "失败,原因:", e);
				bean.setTime(null);
			}

			bean.setMax(values[33]);//33
			bean.setMin(values[34]);//34

			super.handlerCostAndBond(bean);
			// 处理完成一个更新一行
			updateEachLineUI.accept(bean);
		} catch (Exception e) {
			log.error("解析腾讯qt.gtimg.cn数据失败,原因:", e);
			LogUtil.notifyInfo(e.getMessage());
		}
	}

	@Override
	public void stopHandle() {
//		LogUtil.notifyInfo("停止更新Stock数据.");
	}
}
