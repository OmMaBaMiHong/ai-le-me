package org.aileme.shejiao.common.utils;

import cn.hutool.core.util.RandomUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * 生成更像真人的默认用户名。
 */
public final class NaturalUsernameGenerator {

    private static final String[] SURNAMES = {
            "林", "苏", "周", "陈", "沈", "许", "顾", "程", "叶", "宋",
            "江", "陆", "韩", "谢", "梁", "温", "何", "白", "姜", "邵",
            "钟", "夏", "裴", "乔", "徐", "曹", "郑", "秦", "尤", "袁"
    };

    private static final String[] FEMALE_GIVEN = {
            "知夏", "晚晴", "可心", "予安", "念一", "书瑶", "星禾", "听雨", "若溪", "南栀",
            "初棠", "清妍", "语桐", "沐橙", "亦宁", "以沫", "昭月", "安冉", "静姝", "嘉宁"
    };

    private static final String[] MALE_GIVEN = {
            "景川", "予白", "言舟", "知远", "慕辰", "叙白", "时安", "泽言", "南山", "星野",
            "砚书", "怀川", "亦凡", "承泽", "嘉树", "知行", "云策", "柏川", "奕衡", "书临"
    };

    private static final String[] NEUTRAL_GIVEN = {
            "小满", "一诺", "阿宁", "向晚", "知遇", "拾光", "安可", "木子", "小鱼", "禾木",
            "青禾", "晚风", "星河", "川川", "阿九", "北栀", "初一", "夏木"
    };

    private static final String[] FEMALE_STYLE_SUFFIX = {"在杭州", "慢热中", "认真恋爱", "喜欢散步", "周末看展"};
    private static final String[] MALE_STYLE_SUFFIX = {"在上海", "真诚交友", "爱运动", "周末做饭", "认真相处"};
    private static final String[] NEUTRAL_STYLE_SUFFIX = {"认真生活", "想遇见你", "周末出门", "慢慢来", "同频优先"};

    private NaturalUsernameGenerator() {
    }

    public static String generate(Integer gender, String cityHint, String uniquenessSeed) {
        int safeGender = gender == null ? 0 : gender;
        String surname = RandomUtil.randomEle(SURNAMES);
        String given = switch (safeGender) {
            case 1 -> RandomUtil.randomEle(MALE_GIVEN);
            case 2 -> RandomUtil.randomEle(FEMALE_GIVEN);
            default -> RandomUtil.randomEle(NEUTRAL_GIVEN);
        };
        String base = surname + given;
        String city = normalizeCity(cityHint);
        String[] suffixPool = switch (safeGender) {
            case 1 -> MALE_STYLE_SUFFIX;
            case 2 -> FEMALE_STYLE_SUFFIX;
            default -> NEUTRAL_STYLE_SUFFIX;
        };
        int mode = RandomUtil.randomInt(100);
        if (mode < 58) {
            return base;
        }
        if (mode < 82) {
            String suffix = StringUtils.defaultIfBlank(city, RandomUtil.randomEle(suffixPool));
            return trimLength(base + suffix, 12);
        }
        String digits = StringUtils.right(StringUtils.defaultIfBlank(uniquenessSeed, RandomUtil.randomNumbers(4)), 2);
        return trimLength(base + digits, 12);
    }

    private static String normalizeCity(String cityHint) {
        if (StringUtils.isBlank(cityHint)) {
            return "";
        }
        String city = cityHint.trim();
        city = city.replace("市", "").replace("省", "").replace("自治区", "").replace("特别行政区", "");
        if (city.length() > 4) {
            city = city.substring(0, 4);
        }
        if (StringUtils.isBlank(city)) {
            return "";
        }
        return "在" + city;
    }

    private static String trimLength(String value, int maxLength) {
        if (StringUtils.length(value) <= maxLength) {
            return value;
        }
        return StringUtils.substring(value, 0, maxLength);
    }
}
