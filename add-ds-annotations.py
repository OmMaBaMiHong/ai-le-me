#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
为 ai-le-me-shejiao-app 模块批量添加数据源注解
- admin 包 → @DS("master") 使用 xg_admin 库
- app 包 → @DS("app") 使用 bang_yi 库
"""

import os
import re

BASE_DIR = "/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix"

# 包与数据源映射
PACKAGE_DS_MAPPING = {
    "admin/service/impl": "master",  # 多租户后台 → xg_admin
    "app/service/impl": "app",       # 业务APP → bang_yi
}

def add_ds_annotation(file_path, ds_name):
    """为单个文件添加 @DS 注解"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 检查是否已有 @DS 注解
    if '@DS' in content:
        return False, "已包含 @DS 注解"
    
    # 检查是否有 @Service 注解
    if '@Service' not in content:
        return False, "未找到 @Service 注解"
    
    # 1. 添加 import
    if 'import com.baomidou.dynamic.datasource.annotation.DS;' not in content:
        # 在 package 后面添加 import
        lines = content.split('\n')
        new_lines = []
        import_added = False
        
        for line in lines:
            new_lines.append(line)
            # 在 package 声明后的第一个 import 前添加
            if line.startswith('package ') and not import_added:
                new_lines.append('import com.baomidou.dynamic.datasource.annotation.DS;')
                import_added = True
        
        content = '\n'.join(new_lines)
    
    # 2. 在 @Service 前添加 @DS
    service_pattern = r'(@Service)'
    content = re.sub(service_pattern, f'@DS("{ds_name}")\n\\1', content, count=1)
    
    # 写回文件
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    return True, "添加成功"

def process_package(package_path, ds_name):
    """处理单个包"""
    full_path = os.path.join(BASE_DIR, package_path)
    
    if not os.path.exists(full_path):
        print(f"  ✗ {package_path}: 路径不存在")
        return 0
    
    print(f"\n处理 {package_path} (数据源: {ds_name})...")
    
    success_count = 0
    skip_count = 0
    
    # 遍历所有 *ServiceImpl.java 文件
    for file in os.listdir(full_path):
        if file.endswith('ServiceImpl.java'):
            file_path = os.path.join(full_path, file)
            success, msg = add_ds_annotation(file_path, ds_name)
            
            if success:
                print(f"  ✓ {file}")
                success_count += 1
            else:
                skip_count += 1
    
    print(f"  完成: {success_count} 个文件已添加, {skip_count} 个文件跳过")
    return success_count

def main():
    """主函数"""
    print("=" * 70)
    print("为 ai-le-me-shejiao-app 批量添加数据源注解")
    print("=" * 70)
    
    total_success = 0
    
    for package_path, ds_name in PACKAGE_DS_MAPPING.items():
        count = process_package(package_path, ds_name)
        total_success += count
    
    print("\n" + "=" * 70)
    print(f"数据源注解添加完成！共添加 {total_success} 个文件")
    print("=" * 70)
    print("\n数据源分配：")
    print("  - admin 包 → @DS(\"master\") → xg_admin 库（多租户后台）")
    print("  - app 包   → @DS(\"app\")    → bang_yi 库（业务APP）")
    print("\n下一步操作：")
    print("  1. 检查代码是否正确")
    print("  2. 重启应用测试数据源切换")

if __name__ == "__main__":
    main()
