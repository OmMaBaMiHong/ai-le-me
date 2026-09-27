package org.aileme.shejiao.admin.service.impl;

import org.junit.jupiter.api.Test;
import org.aileme.shejiao.domain.entity.admin.NavigationEntity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NavigationServiceImplTest {

    @Test
    @SuppressWarnings("unchecked")
    void mergeDefaultQuickEntriesAppendsMissingGamesWithoutDuplicatingExistingTitles() throws Exception {
        NavigationServiceImpl service = new NavigationServiceImpl();
        Method method = NavigationServiceImpl.class.getDeclaredMethod(
            "mergeDefaultQuickEntries", List.class
        );
        method.setAccessible(true);

        List<NavigationEntity> source = new ArrayList<>();
        source.add(buildEntry("算姻缘", "/pages/match/fate", 0));
        source.add(buildEntry("性格测试", "https://custom.example.com/mbti", 1));

        List<NavigationEntity> merged =
            (List<NavigationEntity>) method.invoke(service, source);

        assertEquals(
            List.of("算姻缘", "性格测试", "星象占卜", "老黄历"),
            merged.stream().map(NavigationEntity::getTitle).toList()
        );
        assertEquals(
            "https://custom.example.com/mbti",
            merged.get(1).getUrl()
        );
        assertEquals(1, merged.stream().filter(item -> "性格测试".equals(item.getTitle())).count());
        assertEquals("/subpackages/tools/constellation", merged.get(2).getUrl());
        assertEquals(0, merged.get(2).getType());
        assertEquals("/subpackages/tools/almanac", merged.get(3).getUrl());
        assertEquals(0, merged.get(3).getType());
    }

    private static NavigationEntity buildEntry(String title, String url, Integer type) {
        NavigationEntity entity = new NavigationEntity();
        entity.setTitle(title);
        entity.setUrl(url);
        entity.setType(type);
        entity.setStatus(0);
        return entity;
    }
}
