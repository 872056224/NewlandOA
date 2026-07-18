package com.oa2.util;

import com.alibaba.fastjson.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * 地址工具类 - 使用腾讯地图API根据坐标获取地址
 */
public class LocationUtil {

    // 腾讯地图API Key
    private static final String TENCENT_MAP_KEY = "JSTBZ-UEPWK-SEMJX-APSBD-YRJFK-IMBIM";
    
    // 腾讯地图逆地理编码API URL
    private static final String TENCENT_GEOCODER_URL = "https://apis.map.qq.com/ws/geocoder/v1/";

    /**
     * 根据坐标获取地址信息
     * @param coordinates 坐标字符串，格式："纬度,经度" 例如："39.908692,116.397477"
     * @return 地址字符串
     */
    public static String getAddressFromCoordinates(String coordinates) {
        if (coordinates == null || coordinates.trim().isEmpty()) {
            return "未知位置";
        }

        try {
            // 验证坐标格式
            String[] coords = coordinates.split(",");
            if (coords.length != 2) {
                return "坐标格式错误";
            }

            double lat = Double.parseDouble(coords[0].trim());
            double lng = Double.parseDouble(coords[1].trim());
            
            // 验证坐标范围
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                return "坐标范围错误";
            }

            // 调用腾讯地图API进行逆地理编码
            String address = callTencentGeocoderAPI(coordinates);
            
            if (address != null && !address.trim().isEmpty()) {
                return address;
            } else {
                return "位置解析失败";
            }
            
        } catch (NumberFormatException e) {
            return "坐标格式错误：" + e.getMessage();
        } catch (Exception e) {
            System.err.println("获取地址信息时发生错误：" + e.getMessage());
            e.printStackTrace();
            return "位置解析异常";
        }
    }

    /**
     * 调用腾讯地图逆地理编码API
     * @param coordinates 坐标字符串 "纬度,经度"
     * @return 地址字符串
     */
    private static String callTencentGeocoderAPI(String coordinates) {
        BufferedReader in = null;
        try {
            // 构建请求URL（对坐标进行URL编码）
            String encodedCoordinates = URLEncoder.encode(coordinates, "UTF-8");
            String urlString = TENCENT_GEOCODER_URL + "?location=" + encodedCoordinates + 
                              "&key=" + TENCENT_MAP_KEY + "&get_poi=0&poi_options=radius=1";
            
            System.out.println("请求URL：" + urlString);
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            
            // 设置请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", 
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            
            // 建立连接
            connection.connect();
            
            // 读取响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            
            // 解析响应JSON
            return parseGeocoderResponse(result.toString());
            
        } catch (Exception e) {
            System.err.println("调用腾讯地图API时发生错误：" + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 解析腾讯地图API响应
     * @param response API响应JSON字符串
     * @return 地址字符串
     */
    private static String parseGeocoderResponse(String response) {
        try {
            // 打印完整响应用于调试
            System.out.println("腾讯地图API响应：" + response);
            
            Map<String, Object> responseMap = JSONObject.parseObject(response, Map.class);
            
            // 检查状态码
            Object statusObj = responseMap.get("status");
            Integer status = null;
            if (statusObj instanceof Integer) {
                status = (Integer) statusObj;
            } else if (statusObj instanceof String) {
                try {
                    status = Integer.parseInt((String) statusObj);
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
            
            String message = (String) responseMap.get("message");
            System.out.println("API状态码：" + status + "，消息：" + message);
            
            // 检查多种成功状态
            if ((status != null && status == 0) || 
                "query ok".equals(message) || 
                "Success".equals(message)) {
                
                Map<String, Object> result = (Map<String, Object>) responseMap.get("result");
                if (result != null) {
                    String address = (String) result.get("address");
                    System.out.println("解析到的地址：" + address);
                    return address;
                }
            } else {
                System.err.println("腾讯地图API返回错误 - 状态码：" + status + "，消息：" + message);
            }
        } catch (Exception e) {
            System.err.println("解析腾讯地图API响应时发生错误：" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 验证坐标格式是否正确
     * @param coordinates 坐标字符串
     * @return 是否有效
     */
    public static boolean isValidCoordinates(String coordinates) {
        if (coordinates == null || coordinates.trim().isEmpty()) {
            return false;
        }
        
        try {
            String[] coords = coordinates.split(",");
            if (coords.length != 2) {
                return false;
            }
            
            double lat = Double.parseDouble(coords[0].trim());
            double lng = Double.parseDouble(coords[1].trim());
            
            return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
