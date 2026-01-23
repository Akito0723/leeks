package utils;

import java.util.HashMap;

/**
 * @Created by DAIE
 * @Date 2021/3/8 20:26
 * @Description leek面板TABLE工具类
 */
public class WindowUtils {
    //基金表头
    public static final String FUND_TABLE_HEADER_KEY = "fund_table_header_key2"; //移动表头时存储的key
    public static final String FUND_TABLE_HEADER_VALUE = "编码,基金名称,估算涨跌,当日净值,估算净值,持仓成本价,持有份额,收益率,收益,更新时间";
    //股票表头
    public static final String STOCK_TABLE_HEADER_KEY = "stock_table_header_key2"; //移动表头时存储的key
    public static final String STOCK_TABLE_HEADER_VALUE = "编码,股票名称,涨跌,涨跌幅,最高价,最低价,当前价,成本价,持仓,收益率,收益,更新时间";
    //货币表头
    public static final String COIN_TABLE_HEADER_KEY = "coin_table_header_key2"; //移动表头时存储的key
    public static final String COIN_TABLE_HEADER_VALUE = "编码,当前价,涨跌,涨跌幅,最高价,最低价,更新时间";

    private static HashMap<String,String> remapPinYinMap = new HashMap<>();

    static {
        addPinyinMapping("编码");
        addPinyinMapping("基金名称");
        addPinyinMapping("估算净值");
        addPinyinMapping("估算涨跌");
        addPinyinMapping("更新时间");
        addPinyinMapping("当日净值");
        addPinyinMapping("股票名称");
        addPinyinMapping("当前价");
        addPinyinMapping("涨跌");
        addPinyinMapping("涨跌幅");
        addPinyinMapping("最高价");
        addPinyinMapping("最低价");
        addPinyinMapping("名称");

        addPinyinMapping("成本价");
        addPinyinMapping("持仓");
        addPinyinMapping("收益率");
        addPinyinMapping("收益");

        addPinyinMapping("持仓成本价");
        addPinyinMapping("持有份额");
    }


    /**
     * 通过列名 获取该TABLE的列的数组下标
     *
     * @param columnNames 列名数组
     * @param columnName  要获取的列名
     * @return 返回给出列名的数组下标 匹配失败返回-1
     */
    public static int getColumnIndexByName(String[] columnNames, String columnName) {
        for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i].equals(columnName)) {
                return i;
            }
        }
        //考虑拼音编码

        return -1;
    }

    public static String remapPinYin(String pinyin) {
        if (pinyin == null) {
            return null;
        }
        String normalized = pinyin.toLowerCase();
        String mapped = remapPinYinMap.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        String noUnderscore = normalized.replace("_", "");
        return remapPinYinMap.getOrDefault(noUnderscore, pinyin);
    }

    private static void addPinyinMapping(String chinese) {
        String pinyin = PinYinUtils.toPinYin(chinese);
        remapPinYinMap.put(pinyin, chinese);
        remapPinYinMap.put(pinyin.replace("_", ""), chinese);
    }


}
