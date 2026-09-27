package org.aileme.shejiao.app.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.vo.AppReccomentUserResponse;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.entity.app.RecommendLoveEntity;
import org.aileme.shejiao.domain.param.app.ReccomentLoveForm;
import org.aileme.shejiao.api.service.RecommendLoveService;
import org.aileme.shejiao.domain.vo.SmartMatchRecommendVo;

import java.util.List;

/**
 * <p>
 * 用户推荐设置 前端控制器
 * </p>
 *
 * @author lww
 * @since 2023-04-29
 */
@Tag(name = "移动端——推荐")
@RestController
@RequestMapping("/app/recommendLove")
public class AppRecommendLoveController {
    @Autowired
    private AppUserService appUserService;
    @Autowired
    private RecommendLoveService recommendLoveService;

    @Login
    @PostMapping("/setReccomentLoves")
    @Operation(summary = "推荐人设置")
    public  Result  setReccomentLoves(@RequestBody ReccomentLoveForm recommendLoveForm, @Parameter(hidden = true) @LoginUser AppUserEntity user){

        RecommendLoveEntity entity = new RecommendLoveEntity();
        BeanUtils.copyProperties(recommendLoveForm, entity);
        entity.setUid(user.getUid());
        entity.setCityids(joinList(recommendLoveForm.getCityidList()));
        entity.setEdu(joinList(recommendLoveForm.getEduList()));
        entity.setHeight(joinList(recommendLoveForm.getHeightList()));
        entity.setAge(joinList(recommendLoveForm.getAgeList()));

        return recommendLoveService.saveLove(entity,user);

    }
    @Login
    @GetMapping("/getMyReccomentLoves")
    @Operation(summary = "个人推荐设置信息")
    public  Result  getMyReccomentLoves( @Parameter(hidden = true) @LoginUser AppUserEntity user){

        RecommendLoveEntity recommendLoveEntity= recommendLoveService.getOne(new QueryWrapper<RecommendLoveEntity>().eq("uid", user.getUid()));

        return new Result().ok(recommendLoveEntity);

    }
    @Login
    @GetMapping("/getReccomentLoves")
    @Operation(summary = "推荐人详情")
    public Result getReccomentLoves( @Parameter(hidden = true) @LoginUser AppUserEntity user){
        List<AppReccomentUserResponse>  list= recommendLoveService.reccomentLoves(user);
        if(list != null && !list.isEmpty()){
            return new Result<>().ok(list.get(0));
        }
        return new Result().ok();
    }
    @Login
    @GetMapping("/getReccomentLoveLists")
    @Operation(summary = "推荐人详情列表")
    public Result<List<AppReccomentUserResponse>> getReccomentLoveLists( @Parameter(hidden = true) @LoginUser AppUserEntity user){
        List<AppReccomentUserResponse>  list= recommendLoveService.reccomentLoves(user);
        return new Result<List<AppReccomentUserResponse>>().ok(list);
    }
    @Login
    @GetMapping("/getLove")
    @Operation(summary = "喜欢")
    public R getLove(@RequestParam("recommendUid") Integer recommendUid, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        recommendLoveService.getLove(recommendUid,user);
        return R.ok();
    }
    @Login
    @GetMapping("/lossLove")
    @Operation(summary = "不喜欢")
    public R lossLove(@RequestParam("recommendUid") Integer recommendUid, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        recommendLoveService.lossLove(recommendUid,user);
        return R.ok();
    }
    @Login
    @GetMapping("/sendNots")
    @Operation(summary = "发送小纸条")
    public R sendNots(@RequestParam("recommendUid") Integer recommendUid,
                      @RequestParam("nots") String nots,
                      @Parameter(hidden = true) @LoginUser AppUserEntity user){
        recommendLoveService.sendNots(recommendUid,nots,user);
        return R.ok();
    }

    @Login
    @GetMapping("/sameCityList")
    @Operation(summary = "同城推荐列表")
    public Result<List<AppReccomentUserResponse>> sameCityList(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        List<AppReccomentUserResponse> list = recommendLoveService.sameCityLoves(user, city, page, size);
        return new Result<List<AppReccomentUserResponse>>().ok(list);
    }

    @Login
    @GetMapping("/smartMatches")
    @Operation(summary = "智能红娘推荐列表")
    public Result<SmartMatchRecommendVo> smartMatches(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "6") Integer size,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        SmartMatchRecommendVo result = recommendLoveService.smartMatchLoves(user, page, size);
        return new Result<SmartMatchRecommendVo>().ok(result);
    }
//    @Login
//    @GetMapping("/userRecommendList")
//    @ApiOperation("已推荐列表")
//    public R userRecommendList( @LoginUser AppUserEntity user){
//       // recommendLoveService.sendNots(recommendUid,nots,user);
//        return R.ok();
//    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }
}
