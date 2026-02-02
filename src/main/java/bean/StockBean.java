package bean;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.time.DateFormatUtils;
import utils.PinYinUtils;

import java.util.Date;

@Getter
@Setter
public class StockBean extends BaseLeeksBean{

	private String name;

	private String now;

	/**
	 * 涨跌
	 */
	private String change;

	private String changePercent;

	/**
	 * 更新时间
	 */
	private Date time;


	/**
     * 最高价
     */
    private String max;

	/**
     * 最低价
     */
    private String min;

    /**
     * 返回列名的VALUE 用作展示
     *
     * @param columnName   字段名
     * @param colorful 是否彩色模式
     * @return 对应列名的VALUE值 无法匹配返回""
     */
    public String getValueByColumn(String columnName, boolean colorful) {
        switch (columnName) {
            case "编码":
                return this.getCode();
            case "股票名称":
                return colorful ? this.getName() : PinYinUtils.toPinYin(this.getName());
            case "当前价":
                return this.getNow();
            case "涨跌":
                String changeStr = "--";
                if (this.getChange() != null) {
                    changeStr = this.getChange().startsWith("-") ? this.getChange() : "+" + this.getChange();
                }
                return changeStr;
            case "涨跌幅":
                String changePercentStr = "--";
                if (this.getChangePercent() != null) {
                    changePercentStr = this.getChangePercent().startsWith("-") ? this.getChangePercent() : "+" + this.getChangePercent();
                }
                return changePercentStr + "%";
            case "最高价":
                return this.getMax();
            case "最低价":
                return this.getMin();
            case "成本价":
                return this.getCostPrice();
            case "持仓":
                return this.getBonds();
            case "收益率":
                return this.getCostPrice() != null ? this.getIncomePercent() + "%" : this.getIncomePercent();
            case "收益":
                return this.getIncome();
            case "更新时间":
                String timeStr = "--";
                if (this.getTime() != null) {
					timeStr = DateFormatUtils.format(this.getTime(), "HH:mm:ss");
                }
                return timeStr;
            default:
                return "";

        }
    }
}
