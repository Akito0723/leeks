package utils;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.table.JBTable;
import handler.BaseTableRefreshHandler;
import handler.fund.FundRefreshHandler;
import handler.stock.StockTableRefreshHandler;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @Created by DAIE
 * @Date 2021/3/8 20:26
 * @Description Leeks 面板TABLE工具类
 */
public class WindowUtils {
	// 基金表头
	public static final String FUND_TABLE_HEADER_KEY = "fund_table_header_key2";
	public static final String FUND_TABLE_HEADER_VALUE = "编码,基金名称,估算涨跌,当日净值,估算净值,持仓成本价,持有份额,收益率,收益,更新时间";

	// 股票表头
	public static final String STOCK_TABLE_HEADER_KEY = "stock_table_header_key2";
	public static final String STOCK_TABLE_HEADER_VALUE = "编码,股票名称,涨跌,涨跌幅,当前价,最高价,最低价,成本价,持仓,收益率,收益,更新时间";

	// 货币表头
	public static final String COIN_TABLE_HEADER_KEY = "coin_table_header_key2";
	public static final String COIN_TABLE_HEADER_VALUE = "编码,当前价,涨跌,涨跌幅,最高价,最低价,更新时间";

	@Getter
	private final static HashMap<String,String> columnPinYinMap = new HashMap<>();

	static {
		Arrays.stream(FUND_TABLE_HEADER_VALUE.split(",")).forEach(one -> {
			columnPinYinMap.put(PinYinUtils.toPinYin(one), one);
		});
		Arrays.stream(STOCK_TABLE_HEADER_VALUE.split(",")).forEach(one -> {
			columnPinYinMap.put(PinYinUtils.toPinYin(one), one);
		});
		Arrays.stream(COIN_TABLE_HEADER_VALUE.split(",")).forEach(one -> {
			columnPinYinMap.put(PinYinUtils.toPinYin(one), one);
		});
	}

	/**
	 * 表头拖动监听事件
	 */
	public static class TableHeadChangeAdapter extends MouseMotionAdapter {

		private final JBTable table;

		private final String tableHeaderKey;

		public TableHeadChangeAdapter(JBTable table, String tableHeaderKey) {
			super();
			this.table = table;
			this.tableHeaderKey = tableHeaderKey;

		}

		public void mouseDragged(MouseEvent e) {
			if (table == null || StringUtils.isBlank(tableHeaderKey)) {
				return;
			}
			StringBuilder tableHeadChange = new StringBuilder();
			for (int i = 0; i < table.getColumnCount(); i++) {
				tableHeadChange.append(table.getColumnName(i)).append(",");
			}
			String value = StringUtils.removeEnd(tableHeadChange.toString(), ",");
			PropertiesComponent instance = PropertiesComponent.getInstance();
			// 将列名的修改放入环境中 key:stock_table_header_key
			instance.setValue(tableHeaderKey, value);
		}
	}

	/**
	 * 行鼠标监听事件
	 */
	public static class TableRowMouseAdapter extends MouseAdapter {

		private static final Logger log = Logger.getInstance(TableRowMouseAdapter.class);

		private final JBTable table;

		private final BaseTableRefreshHandler handler;

		public TableRowMouseAdapter(JBTable table, BaseTableRefreshHandler handler) {
			super();
			this.table = table;
			this.handler = handler;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (table.getSelectedRow() < 0)
				return;
			String code = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), handler.getCodeColumnIndex()));//FIX 移动列导致的BUG
			if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
				// 鼠标左键双击
				if (handler instanceof StockTableRefreshHandler) {
					// 股票
					PopupsUiUtil.showImageByStockCode(code, PopupsUiUtil.StockShowType.min, new Point(e.getXOnScreen(), e.getYOnScreen()));
				}
				if (handler instanceof FundRefreshHandler) {
					// 基金
					PopupsUiUtil.showImageByFundCode(code, PopupsUiUtil.FundShowType.gsz, new Point(e.getXOnScreen(), e.getYOnScreen()));
				}
			} else if (SwingUtilities.isRightMouseButton(e)) {
				// 鼠标右键
				PopupsUiUtil.BaseShowType[] values = new PopupsUiUtil.BaseShowType[0];
				if (handler instanceof StockTableRefreshHandler) {
					// 股票
					values = PopupsUiUtil.StockShowType.values();
				}
				if (handler instanceof FundRefreshHandler) {
					// 基金
					values = PopupsUiUtil.FundShowType.values();
				}

				JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<PopupsUiUtil.BaseShowType>("", values) {
					@Override
					public @NotNull String getTextFor(PopupsUiUtil.BaseShowType showType) {
						return showType.getDesc();
					}

					@Override
					public @Nullable PopupStep<?> onChosen(PopupsUiUtil.BaseShowType selectedValue, boolean finalChoice) {
						if (handler instanceof StockTableRefreshHandler) {
							// 股票
							PopupsUiUtil.showImageByStockCode(code,
									(PopupsUiUtil.StockShowType) selectedValue,
									new Point(e.getXOnScreen(), e.getYOnScreen()));
						}
						if (handler instanceof FundRefreshHandler) {
							// 基金
							PopupsUiUtil.showImageByFundCode(code,
									(PopupsUiUtil.FundShowType) selectedValue,
									new Point(e.getXOnScreen(), e.getYOnScreen()));
						}
						return super.onChosen(selectedValue, finalChoice);
					}
				}).show(RelativePoint.fromScreen(new Point(e.getXOnScreen(), e.getYOnScreen())));
			}
		}
	}

}

