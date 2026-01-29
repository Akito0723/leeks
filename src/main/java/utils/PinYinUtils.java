package utils;

import com.github.promeg.pinyinhelper.Pinyin;
import org.apache.commons.lang3.StringUtils;

public class PinYinUtils {

    public static String toPinYin(String input) {
		if (StringUtils.isBlank(input)) {
			return null;
		}
        return Pinyin.toPinyin(input,"_").toLowerCase();
    }

    public static String[] toPinYin(String[] inputs) {
		if (inputs == null) {
			return null;
		}
        String[] result = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            result[i] = toPinYin(inputs[i]);
        }
        return result;
    }
}
