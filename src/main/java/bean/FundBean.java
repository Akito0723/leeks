package bean;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import utils.PinYinUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class FundBean extends BaseLeeksBean {

    @SerializedName("fundcode")
    private String fundCode;

    @SerializedName("name")
    private String fundName;

	/**
	 * 净值日期
	 */
    private String jzrq;

	/**
	 * 当日净值
	 */
    private String dwjz;

	/**
	 * 估算净值
	 */
    private String gsz;

	/**
	 * 估算涨跌百分比 eg.-0.42%
	 */
    private String gszzl;

	/**
	 * gztime估值时间
	 */
    private String gztime;

    /**
     * 返回列名的VALUE 用作展示
     *
     * @param columnName   字段名
     * @param colorful 隐蔽模式
     * @return 对应列名的VALUE值 无法匹配返回""
     */
    public String getValueByColumn(String columnName, boolean colorful) {
        switch (columnName) {
            case "编码":
                return this.getCode();
            case "基金名称":
                return colorful ? this.getFundName() : PinYinUtils.toPinYin(this.getFundName());
            case "估算净值":
                return this.getGsz();
            case "估算涨跌":
                String gszzlStr = "--";
                String gszzl = this.getGszzl();
                if (gszzl != null) {
                    gszzlStr = gszzl.startsWith("-") ? gszzl : "+" + gszzl;
                }
                return gszzlStr + "%";
            case "更新时间":
                String timeStr = this.getGztime();
                if (timeStr == null) {
                    timeStr = "--";
                }
                String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
                if (timeStr.startsWith(today)) {
                    timeStr = timeStr.substring(timeStr.indexOf(" "));
                }
                return timeStr;
            case "当日净值":
                return this.getDwjz() + "[" + this.getJzrq() + "]";
            case "持仓成本价":
                return this.getCostPrice();
            case "持有份额":
                return this.getBonds();
            case "收益率":
                return this.getCostPrice() != null ? this.getIncomePercent() + "%" : this.getIncomePercent();
            case "收益":
                return this.getIncome();
            default:
                return "";

        }
    }
}
