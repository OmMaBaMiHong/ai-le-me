package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.app.SearchEntity;

import java.util.List;
import java.util.Map;

/**
 * 搜索服务 - 用于记录用户搜索历史
 *
 * @author linfeng
 */
public interface SearchService extends IService<SearchEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<String> selectHotSearch();

    void deleteSearchByUId(Integer uid);

    void setSearchContent(String keyword,Integer uid);

    List<SearchEntity> getSearchListByUid(Integer uid);
}
