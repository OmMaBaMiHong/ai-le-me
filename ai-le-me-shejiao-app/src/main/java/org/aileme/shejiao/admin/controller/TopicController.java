/**
 * -----------------------------------
 *  Copyright (c) 2021-2024
 *  All rights reserved, Designed By www.linfengtech.cn
 *  林风社交论坛商业版本请务必保留此注释头信息
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.admin.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.common.utils.ConfigConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
// import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.TopicEntity;
import org.aileme.shejiao.api.service.TopicService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-21 17:01:12
 */
@RestController
@RequestMapping("/admin/topic")
@Tag(name = "管理端——圈子管理")
public class TopicController {

    @Autowired
    private TopicService topicService;
    @GetMapping("/list")
    // @RequiresPermissions("admin:topic:list") // Temporarily removed due to Spring Boot 3.x compatibility
    @Operation(summary = "圈子列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = topicService.queryPage(params);

        return R.ok().put("page", page);
    }



    @GetMapping("/info/{id}")
    // @RequiresPermissions("admin:topic:info") // Temporarily removed due to Spring Boot 3.x compatibility
    @Operation(summary = "圈子详情")
    public R info(@PathVariable("id") Integer id){
		TopicEntity topic = topicService.getById(id);

        return R.ok().put("topic", topic);
    }


    @PostMapping("/save")
    // @RequiresPermissions("admin:topic:save") // Temporarily removed due to Spring Boot 3.x compatibility
    @Operation(summary = "保存圈子")
    public R save(@RequestBody TopicEntity topic){
		topicService.saveTopicByAdmin(topic);

        return R.ok();
    }


    @SysLog("修改圈子")
    @PostMapping("/update")
    // @RequiresPermissions("admin:topic:update") // Temporarily removed due to Spring Boot 3.x compatibility
    @Operation(summary = "修改圈子")
    public R update(@RequestBody TopicEntity topic){

		topicService.updateByAdmin(topic);
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.H5_TOPIC_POSTER_KEY + topic.getId());
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.WX_TOPIC_POSTER_KEY + topic.getId());
        return R.ok();
    }



    @SysLog("删除圈子")
    @PostMapping("/delete")
    // @RequiresPermissions("admin:topic:delete") // Temporarily removed due to Spring Boot 3.x compatibility
    @Operation(summary = "删除圈子")
    public R delete(@RequestBody Integer[] ids){
        topicService.topicDeleteByAdmin(Arrays.asList(ids));
        return R.ok();
    }


    @GetMapping("/getJoinTopicList/{id}")
    @Operation(summary = "查询用户加入的圈子")
    public R getJoinTopicList(@PathVariable("id") Integer id){
        List<TopicEntity> list = topicService.getJoinTopicList(id);

        return R.ok().put("result",list);
    }

}
