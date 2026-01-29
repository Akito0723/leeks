package bean;

import lombok.Getter;
import lombok.Setter;

/**
 * @author akihiro
 * @date 2026/01/28.
 */
@Getter
@Setter
public abstract class BaseLeeksBean {
	// 编码
	protected String code;

	// 成本价
	protected String costPrice;

	// 持仓
	protected String bonds;

	protected BaseLeeksBean() {}

	public abstract String getValueByColumn(String columnName, boolean colorful);
}
