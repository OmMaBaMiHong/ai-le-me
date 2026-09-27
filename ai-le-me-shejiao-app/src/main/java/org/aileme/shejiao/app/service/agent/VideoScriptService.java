package org.aileme.shejiao.app.service.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.vo.AgentSuggestionVo;

import java.util.ArrayList;
import java.util.List;

/**
 * 求爱视频脚本建议服务
 */
@Service
public class VideoScriptService {

    @Autowired
    private AgentSafetyService safetyService;

    public List<AgentSuggestionVo> suggest(AppUserEntity me,
                                           AppUserEntity target,
                                           String templateCode,
                                           String personaSummary) {
        List<AgentSuggestionVo> result = new ArrayList<>();

        String targetName = StringUtils.defaultIfBlank(target.getUsername(), "你");
        String myCity = firstNonBlank(me.getLocationCity(), me.getCity(), me.getAbodeCity());
        String targetCity = firstNonBlank(target.getLocationCity(), target.getCity(), target.getAbodeCity());

        result.add(build(
                "大家好，我是" + StringUtils.defaultIfBlank(me.getUsername(), "一个认真找对象的人")
                        + "。我希望通过这段视频让" + targetName + "看到真实的我：稳定、真诚，也愿意长期投入。",
                "opening"
        ));

        if (StringUtils.isNotBlank(myCity) || StringUtils.isNotBlank(targetCity)) {
            result.add(build(
                    "我们都在" + StringUtils.defaultIfBlank(targetCity, myCity)
                            + "生活，我期待的是一起把平凡日子过得有温度，而不是短暂热闹。",
                    "life"
            ));
        }

        String summary = safetyService.sanitizeText(personaSummary, 26);
        if (StringUtils.isNotBlank(summary)) {
            result.add(build(
                    "我从你的 AI 画像里看到“" + summary + "”，这正是我欣赏的特质。"
                            + "如果你愿意，我们可以从一次轻松见面开始。",
                    "targeted"
            ));
        }

        result.add(build(
                "如果你也在找稳定真诚的关系，欢迎给我一个回应，我们慢慢来，认真来。",
                "closing"
        ));

        return result;
    }

    private AgentSuggestionVo build(String text, String styleTag) {
        return AgentSuggestionVo.builder()
                .suggestionId(safetyService.newSuggestionId())
                .text(text)
                .styleTag(styleTag)
                .riskLevel("low")
                .reason("结合个人资料与关系目标生成")
                .nextAction("jump_ai_video")
                .build();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }
}
