#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量更新 shejiao 模块的 index.vue，添加 add-or-update 组件引用和调用
"""

import os
import re

# 需要更新的模块列表 (路径, API名称)
MODULES = [
    ("sensitive", "Sensitive"),
    ("report", "Report"),
    ("post", "Post"),
    ("user/usermenu", "Usermenu"),
    ("user/vipoption", "Vipoption"),
    ("user/usersign", "Usersign"),
    ("user/tags", "Tags"),
    ("user/signconfig", "Signconfig"),
    ("user", "User"),
    ("pay/userrecharge", "Userrecharge"),
    ("pay/cashout", "Cashout"),
    ("pay", "Pay"),
]

BASE_DIR = "/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-ui/src/views/shejiao"

def update_index_vue(module_path, api_name):
    """更新单个模块的 index.vue"""
    index_file = os.path.join(BASE_DIR, module_path, "index.vue")
    
    if not os.path.exists(index_file):
        print(f"  ✗ {module_path}: index.vue 不存在")
        return False
    
    with open(index_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 检查是否已经包含 add-or-update 引用
    if 'add-or-update' in content.lower() and 'addOrUpdateRef' in content:
        print(f"  - {module_path}: 已包含 add-or-update，跳过")
        return True
    
    # 1. 在 script 顶部添加 import
    import_pattern = r"(<script setup[^>]*>[\s\S]*?import.*?from.*?types';)"
    import_addition = "\nimport AddOrUpdate from './add-or-update.vue';"
    
    if "import AddOrUpdate" not in content:
        content = re.sub(import_pattern, r"\1" + import_addition, content, count=1)
    
    # 2. 在变量声明区域添加 ref
    ref_pattern = r"(const total = ref\(0\);)"
    ref_addition = "\nconst addOrUpdateRef = ref();"
    
    if "addOrUpdateRef" not in content:
        content = re.sub(ref_pattern, r"\1" + ref_addition, content, count=1)
    
    # 3. 替换 handleAdd 函数
    handle_add_old = r"const handleAdd = \(\) => \{[\s\S]*?proxy\?\.\$modal\.msgWarning\('新增.*?功能待实现'\);[\s\S]*?\};"
    handle_add_new = """const handleAdd = () => {
  addOrUpdateRef.value?.open();
};"""
    
    content = re.sub(handle_add_old, handle_add_new, content)
    
    # 4. 替换 handleUpdate 函数
    handle_update_old = r"const handleUpdate = \(row\?: \w+VO\) => \{[\s\S]*?proxy\?\.\$modal\.msgWarning\('修改.*?功能待实现'\);[\s\S]*?\};"
    handle_update_new = f"""const handleUpdate = (row?: {api_name}VO) => {{
  const id = row?.id || ids.value[0];
  addOrUpdateRef.value?.open(id);
}};"""
    
    content = re.sub(handle_update_old, handle_update_new, content)
    
    # 5. 在 pagination 后添加组件引用
    pagination_pattern = r"(    <pagination[\s\S]*?@pagination=\"getList\"\s*/>\s*)(  </div>\s*</template>)"
    component_addition = "    \n    <!-- 新增/修改对话框 -->\n    <add-or-update ref=\"addOrUpdateRef\" @success=\"getList\" />\n"
    
    if '<add-or-update' not in content:
        content = re.sub(pagination_pattern, r"\1" + component_addition + r"\2", content)
    
    # 写回文件
    with open(index_file, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"  ✓ {module_path}: 已更新")
    return True

def main():
    """主函数"""
    print("开始批量更新 index.vue...")
    print(f"基础路径: {BASE_DIR}\n")
    
    success_count = 0
    fail_count = 0
    
    for module_path, api_name in MODULES:
        try:
            if update_index_vue(module_path, api_name):
                success_count += 1
            else:
                fail_count += 1
        except Exception as e:
            print(f"  ✗ {module_path}: 错误 - {str(e)}")
            fail_count += 1
    
    print(f"\n批量更新完成！")
    print(f"成功: {success_count} 个")
    print(f"失败: {fail_count} 个")
    print(f"共处理: {len(MODULES)} 个模块")

if __name__ == "__main__":
    main()
