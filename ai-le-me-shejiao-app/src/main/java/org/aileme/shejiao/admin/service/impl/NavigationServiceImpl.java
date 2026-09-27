package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import org.aileme.shejiao.common.utils.Constant;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;

import org.aileme.shejiao.admin.dao.NavigationDao;
import org.aileme.shejiao.domain.entity.admin.NavigationEntity;
import org.aileme.shejiao.api.service.NavigationService;


@DS("master")
@Service("navigationService")
public class NavigationServiceImpl extends ServiceImpl<NavigationDao, NavigationEntity> implements NavigationService {

    private static final List<NavigationEntity> DEFAULT_QUICK_ENTRIES = List.of(
        buildDefaultQuickEntry("性格测试", "/subpackages/tools/personality", 0),
        buildDefaultQuickEntry("星象占卜", "/subpackages/tools/constellation", 0),
        buildDefaultQuickEntry("老黄历", "/subpackages/tools/almanac", 0)
    );

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<NavigationEntity> page = this.page(
                new Query<NavigationEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<NavigationEntity> getNav() {
        List<NavigationEntity> list = this.lambdaQuery()
                .eq(NavigationEntity::getStatus, Constant.NORMAL)
                .orderByAsc(NavigationEntity::getId)
                .list();
        return mergeDefaultQuickEntries(list);
    }

    List<NavigationEntity> mergeDefaultQuickEntries(List<NavigationEntity> list) {
        List<NavigationEntity> merged = new ArrayList<>(list);
        Set<String> titles = new LinkedHashSet<>();
        for (NavigationEntity item : merged) {
            String title = normalizeTitle(item);
            if (!title.isEmpty()) {
                titles.add(title);
            }
        }
        for (NavigationEntity item : DEFAULT_QUICK_ENTRIES) {
            String title = normalizeTitle(item);
            if (title.isEmpty() || titles.contains(title)) {
                continue;
            }
            titles.add(title);
            merged.add(copyEntry(item));
        }
        return merged;
    }

    private static String normalizeTitle(NavigationEntity entity) {
        if (entity == null || entity.getTitle() == null) {
            return "";
        }
        return entity.getTitle().trim();
    }

    private static NavigationEntity copyEntry(NavigationEntity source) {
        NavigationEntity target = new NavigationEntity();
        target.setTitle(source.getTitle());
        target.setImg(source.getImg());
        target.setUrl(source.getUrl());
        target.setType(source.getType());
        target.setStatus(source.getStatus());
        return target;
    }

    private static NavigationEntity buildDefaultQuickEntry(String title, String url, Integer type) {
        NavigationEntity entity = new NavigationEntity();
        entity.setTitle(title);
        entity.setImg("");
        entity.setUrl(url);
        entity.setType(type);
        entity.setStatus(Constant.NORMAL);
        return entity;
    }
}
