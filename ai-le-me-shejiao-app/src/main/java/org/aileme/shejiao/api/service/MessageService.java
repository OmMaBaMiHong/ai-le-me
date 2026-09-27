package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.vo.MessageNumberResponse;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.MessageEntity;
import org.aileme.shejiao.domain.param.app.MessageReadForm;
import org.aileme.shejiao.domain.param.app.UpdateChatStatusForm;
import org.aileme.shejiao.domain.param.app.UpdateSystemNoticeStatusForm;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;

/**
 * 
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-26 13:15:30
 */
public interface MessageService extends IService<MessageEntity> {

    PageUtils queryPage(Map<String, Object> params);

    MessageNumberResponse getMessageNumber();

    AppPageUtils queryMessageList(Integer type, Integer page, AppUserEntity user);

    @Async
    void sendMessage(Integer fromUid,Integer toUid,Integer postId,Integer type,String content,String title);

    void sendMessageNotAsync(Integer fromUid,Integer toUid,Integer postId,Integer type,String content,String title);

    Boolean status(Integer type, Integer uid);

    Boolean articleMsgState(UpdateSystemNoticeStatusForm request, Integer uid);

    void readMessage(MessageReadForm request, Integer uid);

    void readAllWatchInfo(Integer uid);

    void deleteMessageByMonth(Integer month);

    void deleteSomeMessageByDay(Integer day);

    AppPageUtils getSystemList(Integer toUid, Integer fromUid, Integer page);

    void updateSystemStatus(UpdateChatStatusForm request, Integer uid);

    void delSystemMsg(Integer uid);

    boolean deleteMessageById(Integer mid, Integer uid);
}
