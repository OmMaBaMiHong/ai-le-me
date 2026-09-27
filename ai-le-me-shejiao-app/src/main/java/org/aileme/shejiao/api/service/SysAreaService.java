package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.sys.SysArea;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SysAreaService extends IService<SysArea> {

      Map<String, Map<String, List<SysArea>>> cityMap=new HashMap<>();
      static Map<String, Map<String, List<SysArea>>> provincesMap() {
            return cityMap;
      }
}
