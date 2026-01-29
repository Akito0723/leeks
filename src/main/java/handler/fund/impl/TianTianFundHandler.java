package handler.fund.impl;

import bean.BaseLeeksBean;
import bean.FundBean;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import handler.fund.FundRefreshHandler;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import utils.HttpClientPool;
import utils.LogUtil;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TianTianFundHandler extends FundRefreshHandler {

    private static final Logger log = Logger.getInstance(TianTianFundHandler.class);

    private static final Gson gson = new Gson();

    private static final ThreadPoolExecutor FETCH_EXECUTOR = new ThreadPoolExecutor(
            2,
            8,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(256),
            new ThreadFactory() {
                private final AtomicInteger index = new AtomicInteger(1);

                @Override
                public Thread newThread(@NotNull Runnable runnable) {
                    Thread thread = new Thread(runnable, "leeks-fund-refresh-" + index.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public TianTianFundHandler(JTable table, JLabel refreshTimeLabel) {
        super(table, refreshTimeLabel);
    }

    @Override
    public void updateByAPI(Consumer<BaseLeeksBean> updateEachLineUI) {
        List<String> codes;
        synchronized (leeksBeanMap) {
            codes = new ArrayList<>(leeksBeanMap.keySet());
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>(codes.size());
        for (String code : codes) {
            futures.add(CompletableFuture.runAsync(() -> {
                String result;
                try {
                    result = HttpClientPool.getHttpClient()
                            .get("http://fundgz.1234567.com.cn/js/" + code + ".js?rt=" + System.currentTimeMillis());
                } catch (Exception e) {
                    log.error("拉取天天基金fundgz.1234567.com.cn数据失败,原因:", e);
                    LogUtil.notifyError(e.getMessage());
                    return;
                }
                String json = result.substring(8, result.length() - 2);
                if (json.isEmpty()) {
                    LogUtil.notifyInfo("Fund编码:[" + code + "]无法获取数据");
                    return;
                }
                FundBean freshBean = gson.fromJson(json, FundBean.class);
                handleEachLine(freshBean, updateEachLineUI);
            }, FETCH_EXECUTOR));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Override
    public void stopHandle() {
//        LogUtil.notifyInfo("停止更新Fund数据.");
    }

    private void handleEachLine(FundBean fundBean, Consumer<BaseLeeksBean> updateEachLineUI) {
        try {
			String code = fundBean.getFundCode();
			FundBean bean;
			synchronized (leeksBeanMap) {
				bean = (FundBean) leeksBeanMap.get(code);
				if (bean == null) {
					bean = new FundBean();
					bean.setCode(code);
					bean.setFundCode(code);
					leeksBeanMap.put(code, bean);
					log.warn("基金代码:" + code + "对应行未初始化,请检查!");
				}
			}

            bean.setFundName(fundBean.getFundName());
            bean.setJzrq(fundBean.getJzrq());
            bean.setDwjz(fundBean.getDwjz());
            bean.setGsz(fundBean.getGsz());
            bean.setGszzl(fundBean.getGszzl());
            bean.setGztime(fundBean.getGztime());

            BigDecimal now = new BigDecimal(bean.getGsz());
            String costPriceStr = bean.getCostPrice();
            if (StringUtils.isNotEmpty(costPriceStr)) {
                BigDecimal costPriceDec = new BigDecimal(costPriceStr);
                BigDecimal incomeDiff = now.add(costPriceDec.negate());
                if (costPriceDec.compareTo(BigDecimal.ZERO) <= 0) {
                    bean.setIncomePercent("0");
                } else {
                    BigDecimal incomePercentDec = incomeDiff.divide(costPriceDec, 8, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.TEN)
                            .multiply(BigDecimal.TEN)
                            .setScale(3, RoundingMode.HALF_UP);
                    bean.setIncomePercent(incomePercentDec.toString());
                }

                String bondStr = bean.getBonds();
                if (StringUtils.isNotEmpty(bondStr)) {
                    BigDecimal bondDec = new BigDecimal(bondStr);
                    BigDecimal incomeDec = incomeDiff.multiply(bondDec)
                            .setScale(2, RoundingMode.HALF_UP);
                    bean.setIncome(incomeDec.toString());
                }
            }
            updateEachLineUI.accept(bean);
        } catch (Exception e) {
            log.error("解析基金数据失败,原因:", e);
            LogUtil.notifyError(e.getMessage());
        }
    }
}
