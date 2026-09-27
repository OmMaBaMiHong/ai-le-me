package org.aileme.shejiao.common.utils;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;

/**
 * @author linfeng
 * @date 2022/1/25 15:37
 */
public  class JsonUtils {


    public static List<String> JsonToList(String jsonString){
        List<String> result = new ArrayList<>();
        flattenValue(jsonString, result);
        return result;
    }

    public static String normalizeUrlValue(String value) {
        List<String> result = new ArrayList<>();
        flattenValue(value, result);
        return result.isEmpty() ? "" : result.get(0);
    }

    private static void flattenValue(Object value, List<String> result) {
        if (value == null) {
            return;
        }
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                flattenValue(item, result);
            }
            return;
        }

        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            return;
        }

        if ((raw.startsWith("[") && raw.endsWith("]"))
                || (raw.startsWith("{") && raw.endsWith("}"))
                || (raw.startsWith("\"") && raw.endsWith("\""))) {
            try {
                Object parsed = JSON.parse(raw);
                if (parsed instanceof List<?>) {
                    flattenValue(parsed, result);
                    return;
                }
                if (parsed instanceof String && !raw.equals(parsed)) {
                    flattenValue(parsed, result);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        String normalized = raw
                .replaceFirst("^/+(?=https?://)", "")
                .replaceAll("^[\"']+|[\"']+$", "");
        if (!normalized.isEmpty()) {
            result.add(normalized);
        }
    }
}
