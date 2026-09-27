package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.api.service.ChatMessageService;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.app.dao.FriendDao;
import org.aileme.shejiao.app.websocket.constant.MessageConstant;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.common.utils.SnowFlakeUtil;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.FriendEntity;
import org.aileme.shejiao.domain.param.app.ClearChatMessageUnreadForm;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@DS("master")
@Service("friendService")
public class FriendServiceImpl extends ServiceImpl<FriendDao, FriendEntity> implements FriendService {

    @Resource
    private FriendDao friendDao;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private FriendService friendService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<FriendEntity> page = this.page(
                new Query<FriendEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public List<JSONObject> getFriendList(Integer uid) {
        return friendDao.getFriendList(uid);
    }

    @Override
    public void clearUnread(ClearChatMessageUnreadForm param) {
        this.lambdaUpdate().set(FriendEntity::getUnread,0)
            .eq(FriendEntity::getFriendId,param.getFriendId())
            .eq(FriendEntity::getMyId,param.getMyId())
            .update();
    }

    @Override
    public Boolean checkIsFriend(Integer uid, Integer uid1) {
        long count = this.lambdaQuery()
                .eq(FriendEntity::getMyId, uid)
                .eq(FriendEntity::getFriendId, uid1)
                .eq(FriendEntity::getIsHidden, false)
                .count();
        return count > 0;
    }


    @Override
    public void doFirendsEach(AppUserEntity user, long recommendUid) {
        if(checkIsFriend(user.getUid(), (int) recommendUid)){
            return;
        }

        Long sessionId = Math.abs(SnowFlakeUtil.getSnowFlakeId());
        //生成好友关系
        FriendEntity friend1 = FriendEntity.builder()
            .myId(user.getUid())
            .friendId((int) recommendUid)
            .notation(user.getUsername())
            .sessionId(sessionId)
            .lastMessage(MessageConstant.DEFAULT_LAST_MESSAGE)
            .unread(MessageConstant.NOT_READ)
            .isHidden(false)
            .createTime(DateUtil.nowDateTime())
            .updateTime(DateUtil.nowDateTime())
            .build();
        FriendEntity friend2 = FriendEntity.builder()
            .myId((int) recommendUid)
            .friendId(user.getUid())
            //.notation(jsonObject.getStr("senderName"))
            .sessionId(sessionId)
            .lastMessage(MessageConstant.DEFAULT_LAST_MESSAGE)
            .unread(MessageConstant.NOT_READ)
            .isHidden(false)
            .createTime(DateUtil.nowDateTime())
            .updateTime(DateUtil.nowDateTime())
            .build();
        friendDao.insert(friend1);
        friendDao.insert(friend2);
    }


    @Override
    public void removeFriends(Integer uid, Integer id) {
        this.lambdaUpdate()
                .eq(FriendEntity::getMyId, uid)
                .eq(FriendEntity::getFriendId, id)
                .remove();
        this.lambdaUpdate()
                .eq(FriendEntity::getMyId, id)
                .eq(FriendEntity::getFriendId, uid)
                .remove();
    }

    @Override
    @DSTransactional
    public void deleteFriend(Integer friendId, Integer myId) {
        LambdaQueryWrapper<FriendEntity> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(FriendEntity::getFriendId, friendId)
                .eq(FriendEntity::getMyId, myId);
        boolean remove1 = this.remove(wrapper1);

        LambdaQueryWrapper<FriendEntity> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(FriendEntity::getFriendId, myId)
                .eq(FriendEntity::getMyId, friendId);
        boolean remove2 = this.remove(wrapper2);

        if (!remove1 || !remove2) {
            throw new RuntimeException("删除好友失败");
        }
        // 清理两人之间的聊天记录
        chatMessageService.deleteChatMessage(friendId, myId);
    }

    @Override
    @DSTransactional
    public void applyFriend(Object dataInfo) {
        // 保存好友申请到通知表（简化实现，实际项目中可能需要 NoticeService）
        // 这里暂时只打印日志，具体实现需要看 Notice 表结构
        System.out.println("收到好友申请：" + dataInfo);
    }

    @Override
    @DSTransactional
    public void agreePersonApply(Integer id) {
        // 同意好友申请的逻辑
        // 由于缺少 Notice 表，这里暂时无法完整实现
        System.out.println("同意好友申请 ID: " + id);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        LambdaQueryWrapper<FriendEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendEntity::getSessionId, Long.valueOf(sessionId));
        this.remove(wrapper);
    }

    @Override
    @DSTransactional
    public Long getOrCreateSession(Integer myId, Integer friendId) {
        // 查找已有好友关系
        FriendEntity existing = this.lambdaQuery()
                .eq(FriendEntity::getMyId, myId)
                .eq(FriendEntity::getFriendId, friendId)
                .one();
        if (existing != null && existing.getSessionId() != null) {
            return existing.getSessionId();
        }
        // 创建双向好友关系（带新会话）
        Long sessionId = Math.abs(SnowFlakeUtil.getSnowFlakeId());
        FriendEntity friend1 = FriendEntity.builder()
                .myId(myId)
                .friendId(friendId)
                .sessionId(sessionId)
                .lastMessage("")
                .unread(0)
                .isHidden(true)
                .createTime(DateUtil.nowDateTime())
                .updateTime(DateUtil.nowDateTime())
                .build();
        FriendEntity friend2 = FriendEntity.builder()
                .myId(friendId)
                .friendId(myId)
                .sessionId(sessionId)
                .lastMessage("")
                .unread(0)
                .isHidden(true)
                .createTime(DateUtil.nowDateTime())
                .updateTime(DateUtil.nowDateTime())
                .build();
        friendDao.insert(friend1);
        friendDao.insert(friend2);
        return sessionId;
    }

    @Override
    @DSTransactional
    public Long activateFriendship(Integer myId, Integer friendId) {
        FriendEntity direct = this.lambdaQuery()
                .eq(FriendEntity::getMyId, myId)
                .eq(FriendEntity::getFriendId, friendId)
                .one();
        FriendEntity reverse = this.lambdaQuery()
                .eq(FriendEntity::getMyId, friendId)
                .eq(FriendEntity::getFriendId, myId)
                .one();

        Long sessionId = null;
        if (direct != null && direct.getSessionId() != null) {
            sessionId = direct.getSessionId();
        } else if (reverse != null && reverse.getSessionId() != null) {
            sessionId = reverse.getSessionId();
        } else {
            sessionId = Math.abs(SnowFlakeUtil.getSnowFlakeId());
        }

        if (direct == null) {
            direct = FriendEntity.builder()
                    .myId(myId)
                    .friendId(friendId)
                    .sessionId(sessionId)
                    .lastMessage("")
                    .unread(0)
                    .isHidden(false)
                    .createTime(DateUtil.nowDateTime())
                    .updateTime(DateUtil.nowDateTime())
                    .build();
            friendDao.insert(direct);
        } else {
            direct.setSessionId(sessionId);
            direct.setIsHidden(false);
            direct.setUpdateTime(DateUtil.nowDateTime());
            this.updateById(direct);
        }

        if (reverse == null) {
            reverse = FriendEntity.builder()
                    .myId(friendId)
                    .friendId(myId)
                    .sessionId(sessionId)
                    .lastMessage("")
                    .unread(0)
                    .isHidden(false)
                    .createTime(DateUtil.nowDateTime())
                    .updateTime(DateUtil.nowDateTime())
                    .build();
            friendDao.insert(reverse);
        } else {
            reverse.setSessionId(sessionId);
            reverse.setIsHidden(false);
            reverse.setUpdateTime(DateUtil.nowDateTime());
            this.updateById(reverse);
        }

        return sessionId;
    }
}
