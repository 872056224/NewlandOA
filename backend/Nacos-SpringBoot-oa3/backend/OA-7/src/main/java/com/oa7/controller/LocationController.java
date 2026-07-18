package com.oa7.controller;

import com.oa7.util.LocationUtil;
import com.oa7.util.RESP;
import org.springframework.web.bind.annotation.*;

/**
 * 位置服务控制器
 */
@RestController
@RequestMapping("/location")
@CrossOrigin
public class LocationController {

    /**
     * 根据经纬度获取地址信息
     * @param coordinates 坐标字符串，格式："纬度,经度"
     * @return 地址信息
     */
    @GetMapping("/address")
    public RESP getAddressByCoordinates(@RequestParam("coordinates") String coordinates) {
        try {
            // 验证坐标格式
            if (!LocationUtil.isValidCoordinates(coordinates)) {
                return RESP.error("坐标格式错误");
            }
            
            // 获取地址信息
            String address = LocationUtil.getAddressFromCoordinates(coordinates);
            
            if (address != null && !address.contains("错误") && !address.contains("失败")) {
                return RESP.ok(address);
            } else {
                return RESP.error(address != null ? address : "位置解析失败");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("获取地址信息失败：" + e.getMessage());
        }
    }

    /**
     * 验证坐标格式是否正确
     * @param coordinates 坐标字符串
     * @return 验证结果
     */
    @GetMapping("/validate")
    public RESP validateCoordinates(@RequestParam("coordinates") String coordinates) {
        try {
            boolean isValid = LocationUtil.isValidCoordinates(coordinates);
            return RESP.ok(isValid);
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("坐标验证失败：" + e.getMessage());
        }
    }
} 