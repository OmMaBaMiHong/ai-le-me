package org.aileme.shejiao.common.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class XinZuoUtil {
    /*** Java通过生日计算星座* * @param birthday 2021-09-02* @return*/
    public static String getConstellation(Date birthday) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(birthday);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int[] dayArr = new int[] { 20, 19, 21, 20, 21, 22, 23, 23, 23, 24, 23, 22 };
        String[] constellationArr = new String[] { "摩羯座", "水瓶座", "双鱼座", "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", "天秤座","天蝎座", "射手座", "摩羯座" };
        return day < dayArr[month - 1] ? constellationArr[month - 1] : constellationArr[month];
    }
    public static String getConstellationStr(String birthday) {
        DateTimeFormatter formatter=DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate parseDate=LocalDate.parse(birthday,formatter);
        int month = parseDate.getMonth().getValue();
        int day = parseDate.getDayOfMonth();
        int[] dayArr = new int[] { 20, 19, 21, 20, 21, 22, 23, 23, 23, 24, 23, 22 };
        String[] constellationArr = new String[] { "摩羯座", "水瓶座", "双鱼座", "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", "天秤座","天蝎座", "射手座", "摩羯座" };
        return day < dayArr[month - 1] ? constellationArr[month - 1] : constellationArr[month];
    }



    public static void main(String[] args) {
        LocalDate localDate=  LocalDate.of(1992,8,9);
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = localDate.atStartOfDay().atZone(zone).toInstant();
        java.util.Date da = Date.from(instant);

        System.out.println(getConstellationStr("1992-08-09"));
    }

}
