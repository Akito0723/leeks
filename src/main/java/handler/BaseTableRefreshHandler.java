package handler;

import bean.BaseLeeksBean;
import bean.FundBean;
import bean.StockBean;
import com.google.common.collect.Lists;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import handler.stock.impl.EastMoneyTableHandler;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import utils.PinYinUtils;
import utils.WindowUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * 插件表格绘制
 * @author akihiro
 * @date 2026/01/28.
 */
public abstract class BaseTableRefreshHandler extends DefaultTableModel {

	private static final Logger log = Logger.getInstance(BaseTableRefreshHandler.class);

	private final static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	/**
	 * key: 中文表头
	 * value: 表头所在下标
	 */
	protected final Map<String, Integer> columnMaps = new LinkedHashMap<>();

	/**
	 * key: 编码
	 * value: BaseLeeksBean具体实现
	 */
	protected final Map<String, BaseLeeksBean> leeksBeanMap = new HashMap<>();

	/**
	 * 存放【编码】的位置，更新数据时用到
	 */
	@Getter
	protected int codeColumnIndex;

	protected JTable table;

	protected JLabel refreshTimeLabel;

	/**
	 *  是否彩色模式
	 */
	protected Boolean colorful;

	/**
	 *  是否开启表格条纹（斑马线）
	 */
	protected Boolean striped;

	/**
	 * 表头 在 com.intellij.ide.util.PropertiesComponent 的 key
	 */
	public abstract String getTableHeaderKey();

	/**
	 * 表头的值,使用,分割
	 */
	public abstract String getTableHeaderValue();

	/**
	 * 排序
	 */
	public abstract String[] sortColumns();

	/**
	 * instance 具体的 Bean
	 */
	public abstract BaseLeeksBean instanceLeeksBean();

	/**
	 * 从网络接口更新数据
	 */
	public abstract void updateByAPI(Consumer<BaseLeeksBean> updateEachLineUI);

	/**
	 * 停止从网络更新数据
	 */
	public abstract void stopHandle();


	private BaseTableRefreshHandler() {
		// 初始化表头
		PropertiesComponent instance = PropertiesComponent.getInstance();
		String tableHeaderValue = instance.getValue(this.getTableHeaderKey());
		if (StringUtils.isBlank(tableHeaderValue)) {
			instance.setValue(this.getTableHeaderKey(), this.getTableHeaderValue());
			tableHeaderValue = this.getTableHeaderValue();
		}
		String[] columns = StringUtils.split(tableHeaderValue, ",");
		for (int i = 0; i < columns.length; i++) {
			// 如果是非色彩模式,PropertiesComponent拿出来的就columns[i]可能是拼音,处理下
			String columnName = WindowUtils.getColumnPinYinMap().getOrDefault(columns[i], columns[i]);
			columnMaps.put(columnName, i);
			if (StringUtils.equals(columnName, "编码")) {
				this.codeColumnIndex = i;
			}
		}
	}

	public BaseTableRefreshHandler(JTable table, JLabel label) {
		this();
		this.table = table;
		this.refreshTimeLabel = label;
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		// Fix tree row height
		FontMetrics metrics = table.getFontMetrics(table.getFont());
		table.setRowHeight(Math.max(table.getRowHeight(), metrics.getHeight()));
		table.setModel(this);

		PropertiesComponent instance = PropertiesComponent.getInstance();
		this.colorful = instance.getBoolean("key_colorful");
		this.striped = instance.getBoolean("key_table_striped");

		// 刷新表头
		String[] columns = columnMaps.keySet().toArray(new String[0]);
		if (colorful != null && colorful) {
			setColumnIdentifiers(columns);
		} else {
			// 非彩色模式,把对应的表头转拼音
			setColumnIdentifiers(PinYinUtils.toPinYin((columns)));
		}
		// 处理 表格排序
		TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>(this);
		Comparator<Object> doubleComparator = (o1, o2) -> {
			Double v1 = NumberUtils.toDouble(StringUtils.remove((String) o1, '%'));
			Double v2 = NumberUtils.toDouble(StringUtils.remove((String) o2, '%'));
			return v1.compareTo(v2);
		};
		Arrays.stream(this.sortColumns())
				.map(columnMaps::get)
				.filter(index -> index != null && index >= 0)
				.forEach(index -> rowSorter.setComparator(index, doubleComparator));
		table.setRowSorter(rowSorter);

		if (table instanceof JBTable) {
			((JBTable) table).setStriped(false);
		}
		table.setShowGrid(false);
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(
					JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				// 表格条纹
				if (!isSelected && striped != null && striped) {
					setBackground(row % 2 == 0 ? JBColor.background(): JBColor.LIGHT_GRAY);
				}

				boolean b = Lists.newArrayList("涨跌", "涨跌幅", "收益率", "收益").contains(getColumnName(column));
				if (!b) {
					setForeground(JBColor.foreground());
					return c;
				}
				// 红张绿跌
				double temp = NumberUtils.toDouble(StringUtils.remove(Objects.toString(value), "%"));
				if (temp > 0) {
					if (colorful != null && colorful) {
						setForeground(JBColor.RED);
					} else {
						setForeground(JBColor.DARK_GRAY);
					}
				} else if (temp < 0) {
					if (colorful != null && colorful) {
						setForeground(JBColor.GREEN);
					} else {
						setForeground(JBColor.GRAY);
					}
				} else {
					Color origin = getForeground();
					setForeground(origin);
				}
				return c;
			}
		});
	}

	/**
	 * 刷新颜色模式
	 * @param colorful true: 彩色模式 false: 隐蔽模式
	 */
	public void setColorful(boolean colorful) {
		if (this.colorful != null && this.colorful == colorful) {
			return;
		}
		this.colorful = colorful;
		table.repaint();
	}

	/**
	 * 设置表格条纹（斑马线）
	 * @param striped true: 设置条纹
	 */
	public void setStriped(boolean striped) {
		if (!(table instanceof JBTable)) {
			log.warn("this.table不是 JBTable 类型，请自行实现setStriped");
			return;
		}
		if (this.striped != null && this.striped == striped) {
			return;
		}
		this.striped = striped;
		table.repaint();
	}

	/**
	 * 刷新 数据 和 ui
	 */
	public void refreshTableUIData(List<String> configCodeList) {
		if (CollectionUtils.isEmpty(configCodeList)) {
			return;
		}
		this.updateByAPI(this::updateEachLineUI);
		updateRefreshTimeLabelUI(LocalDateTime.now().format(timeFormatter));
	}

	/**
	 * 初始化表格,展示配置的code
	 * @param configCodeList @see SettingsWindow#getConfigList(java.lang.String)
	 */
	public void setupTable(List<String> configCodeList) {
		for (String configCode : configCodeList) {
			BaseLeeksBean bean = processConfigCodeList(configCode);
			this.leeksBeanMap.put(bean.getCode(), bean);
			updateEachLineUI(bean);
		}
	}

	private BaseLeeksBean processConfigCodeList(String configCode) {
		// 编码，英文分号分隔 成本价和成本接在编码后用逗号分隔
		BaseLeeksBean bean = instanceLeeksBean();

		// 兼容原有设置
		if (configCode.contains(",")) {
			// configCode包含,号说明有成本价和持仓
			String[] arr = configCode.split(",");
			bean.setCode(arr[0]);
			if (arr.length > 1) {
				bean.setCostPrice(arr[1]);
			}
			if (arr.length > 2) {
				bean.setBonds(arr[2]);
			}
		} else {
			// 直接是编码
			bean.setCode(configCode);
		}
		return bean;
	}

	/**
	 * 处理成本价和持仓
	 */
	protected void handlerCostAndBond(BaseLeeksBean bean) {
		String costPriceStr = bean.getCostPrice();
		if (StringUtils.isEmpty(costPriceStr)) {
			return;
		}

		BigDecimal now;
		if (bean instanceof StockBean) {
			now = new BigDecimal(((StockBean) bean).getNow());
		} else if (bean instanceof FundBean) {
			now = new BigDecimal(((FundBean) bean).getGsz());
		} else {
			return;
		}

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

	public void updateEachLineUI(BaseLeeksBean bean) {
		if (bean == null) {
			return;
		}
		if (bean.getCode() == null) {
			return;
		}
		Vector<Object> convertData = new Vector<>(columnMaps.size());
		for (String columnName : columnMaps.keySet()) {
			convertData.addElement(bean.getValueByColumn(columnName, colorful));
		}
		// 找到当前 bean.code 在的哪一行行
		int index = findRowIndex(codeColumnIndex, bean.getCode());
		if (index >= 0) {
			updateRow(index, convertData);
		} else {
			addRow(convertData);
		}
	}

	/**
	 * @param columnIndex 列号
	 * @param value       值
	 * @return 如果不存在返回-1
	 */
	private int findRowIndex(int columnIndex, String value) {
		int rowCount = getRowCount();
		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
			Object valueAt = getValueAt(rowIndex, columnIndex);
			if (valueAt != null && StringUtils.equalsIgnoreCase(value, valueAt.toString())) {
				return rowIndex;
			}
		}
		return -1;
	}

	/**
	 * 参考源码{@link DefaultTableModel#setValueAt}，此为直接更新行，提高点效率
	 */
	protected void updateRow(int rowIndex, Vector<Object> rowData) {
		dataVector.set(rowIndex, rowData);
		// 通知 listeners 刷新ui
		fireTableRowsUpdated(rowIndex, rowIndex);
	}

	/**
	 * 参考源码{@link DefaultTableModel#removeRow(int)}，此为直接清除全部行，提高点效率
	 */
	public void clearRow() {
		this.leeksBeanMap.clear();
		int size = dataVector.size();
		if (0 < size) {
			dataVector.clear();
			// 通知 listeners 刷新ui
			fireTableRowsDeleted(0, size - 1);
		}
	}

	public void updateRefreshTimeLabelUI(String text) {
		SwingUtilities.invokeLater(() -> {
			refreshTimeLabel.setText(text);
			refreshTimeLabel.setToolTipText("最后刷新时间");
		});
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}


}
