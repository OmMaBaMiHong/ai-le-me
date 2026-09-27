#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修正 ai-le-me-shejiao-app 模块的数据源注解
将所有 @DS("master") 改为 @DS("app")
因为整个 ai-le-me-shejiao-app 模块都是业务 APP，应该使用 bang_yi 库
"""

import os
import re

BASE_DIR = "/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-modules/ai-le-me-shejiao-app/src/main/java/td/matrix"

def fix_ds_annotation(file_path):
    """修正单个文件的 @DS 注解"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 检查是否包含 @DS("master")
    if '@DS("master")' not in content:
        return False, "未找到 @DS(\"master\")"
    
    # 替换为 @DS("app")
    content = content.replace('@DS("master")', '@DS("app")')
    
    # 写回文件
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    return True, "修正成功"

def process_directory(dir_path):
    """处理目录下的所有文件"""
    success_count = 0
    
    for root, dirs, files in os.walk(dir_path):
        for file in files:
            if file.endswith('ServiceImpl.java'):
                file_path = os.path.join(root, file)
                success, msg = fix_ds_annotation(file_path)
                
                if success:
                    print(f"  ✓ {file}")
                    success_count += 1
    
    return success_count

def main():
    """主函数"""
    print("=" * 70)
    print("修正 ai-le-me-shejiao-app 数据源注解")
    print("将所有 @DS(\"master\") 改为 @DS(\"app\")")
    print("=" * 70)
    
    admin_path = os.path.join(BASE_DIR, "admin/service/impl")
    
    if not os.path.exists(admin_path):
        print(f"✗ 路径不存在: {admin_path}")
        return
    
    print(f"\n处理 admin/service/impl ...")
    count = process_directory(admin_path)
    
    print("\n" + "=" * 70)
    print(f"修正完成！共修正 {count} 个文件")
    print("=" * 70)
    print("\n正确配置：")
    print("  - ai-le-me-shejiao-app 全部 → @DS(\"app\") → bang_yi 库（业务APP）")
    print("\n下一步操作：")
    print("  1. 检查代码是否正确")
    print("  2. 重启应用测试")

if __name__ == "__main__":
    main()
