package org.aileme.shejiao.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP查询工具类
 * @author linfeng
 * @date 2022/2/7 19:18
 */
public class IPUtil {

    //填写腾讯地图申请的key
    private static final String key="";

    private static final String UNKNOWN = "unknown";
    // IP归属地查询
    public static final String IP_URL = "http://whois.pconline.com.cn/ipJson.jsp?ip=%s&json=true";
    /**
     * 获取ip地址
     */
    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        String comma = ",";
        String localhost = "127.0.0.1";
        if (ip.contains(comma)) {
            ip = ip.split(",")[0];
        }
        if  (localhost.equals(ip))  {
            // 获取本机真正的ip地址
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return ip;
    }

    /**
     * 根据ip获取详细地址
     */
    public static String getCityInfo(String ip) {
        try {
            String api = String.format(IP_URL,ip);
            String response = HttpUtil.get(api);
            if (StrUtil.isBlank(response)) {
                return "未知";
            }
            JSONObject object = JSONUtil.parseObj(response);
            String addr = object.get("addr", String.class);
            return StrUtil.isBlank(addr) ? "未知" : addr;
        } catch (Exception e) {
            // 记录日志但不抛出异常，避免影响主业务流程
            System.err.println("获取IP地址信息失败: " + e.getMessage());
            return "未知";
        }
    }

    /**
     * 根据ip获取干净的城市名（如"广州市"）
     * pconline接口返回的JSON中 pro=省份, city=城市
     */
    public static String[] getProvinceCityByIp(String ip) {
        try {
            String api = String.format(IP_URL, ip);
            String response = HttpUtil.get(api);
            if (StrUtil.isBlank(response)) {
                return null;
            }
            JSONObject object = JSONUtil.parseObj(response);
            String province = object.getStr("pro");
            String city = object.getStr("city");
            if (StrUtil.isAllNotBlank(province, city)) {
                return new String[]{province, city};
            }
        } catch (Exception e) {
            System.err.println("IP解析城市失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 根据ip获取归属地
     * @param ip 用户ip
     * @return 地址
     */
    public static String getAddress(String ip) {

        String url = "https://apis.map.qq.com/ws/location/v1/ip?ip="+ ip +"&key=" + key ;
        String str = HttpUtil.get(url);
        if (!StrUtil.hasBlank(str)) {
            com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(str);
            com.alibaba.fastjson.JSONObject res = jsonObject.getJSONObject("result");
            if(res!=null){
                com.alibaba.fastjson.JSONObject result = res.getJSONObject("ad_info");
                if(result!=null){
                    String province = result.getString("province");
                    if(province.equals("")){
                        return result.getString("nation");
                    }
                    String city = result.getString("city");
                    String district = result.getString("district");
                    if(province.equals(city)){
                        return province+district;
                    }
                    return province+city+district;
                }
            }
        }
        return "";
    }

}
