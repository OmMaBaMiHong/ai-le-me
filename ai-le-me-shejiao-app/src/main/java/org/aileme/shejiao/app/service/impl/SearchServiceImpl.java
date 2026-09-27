package org.aileme.shejiao.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.SearchService;
import org.aileme.shejiao.app.dao.SearchDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.SearchEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 用户搜索历史服务实现
 * 
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2023-04-26 20:13:08
 */
@Service("searchService")
public class SearchServiceImpl extends ServiceImpl<SearchDao, SearchEntity> implements SearchService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SearchEntity> page = this.page(
                new Query<SearchEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public List<SearchEntity> getSearchListByUid(Integer uid) {
        QueryWrapper<SearchEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("uid", uid).orderByDesc("update_time");
        return this.list(wrapper);
    }

    @Override
    public List<String> selectHotSearch() {
        // 热门搜索逻辑，这里简化实现返回空列表
        // 实际项目中可以根据搜索次数统计热门内容
        return new ArrayList<>();
    }

    @Override
    public void deleteSearchByUId(Integer uid) {
        QueryWrapper<SearchEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("uid", uid);
        this.remove(wrapper);
    }

    @Override
    public void setSearchContent(String keyword, Integer uid) {
        List<SearchEntity> list = this.lambdaQuery()
                .eq(SearchEntity::getUid, uid)
                .eq(SearchEntity::getContent, keyword)
                .list();
        if(list.size()==0){
            SearchEntity search=new SearchEntity();
            search.setContent(keyword);
            search.setUid(uid);
            search.setUpdateTime(new Date());
            this.save(search);
        }else{
            SearchEntity search = list.get(0);
            search.setUpdateTime(new Date());
            this.updateById(search);
        }
    }
}
