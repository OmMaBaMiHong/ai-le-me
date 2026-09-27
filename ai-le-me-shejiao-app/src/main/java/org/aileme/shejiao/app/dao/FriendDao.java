package org.aileme.shejiao.app.dao;

import cn.hutool.json.JSONObject;
import org.apache.ibatis.annotations.*;
import org.aileme.shejiao.domain.entity.app.FriendEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
/**
 *
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-11-16 14:05:06
 */
@Mapper
public interface FriendDao extends BaseMapper<FriendEntity> {


    @Update("update friend " +
            "set last_message=#{lastMessage},unread=unread+1,update_time=NOW() " +
            "where session_id=#{sessionId}")
    void updateInfo(@Param("sessionId") String sessionId, @Param("lastMessage") String lastMessage);

    @Update("update friend " +
            "set last_message=#{lastMessage},update_time=NOW() " +
            "where session_id=#{sessionId}")
    void updateWithdrawInfo(@Param("sessionId") String sessionId, @Param("lastMessage") String lastMessage);

    @Select("SELECT f.id,f.friend_id,u.username,f.session_id,f.last_message,f.unread,f.is_hidden,f.update_time,u.avatar " +
            "FROM friend f,user u " +
            "where u.uid=f.friend_id and f.my_id=#{myId}")
    List<JSONObject> getFriendList(Integer uid);

    @Delete("delete from friend where (u.uid=${uid} and friend_id=${friend_id}) or (u.uid=${friend_id} and friend_id=${uid})")
    void removeFriends(@Param("uid") Integer uid, @Param("friend_id")Integer friend_id);
}
