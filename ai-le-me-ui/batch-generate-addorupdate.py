#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量生成 shejiao 模块的 add-or-update.vue 组件
"""

import os
import json

# 模块配置映射
MODULE_CONFIGS = {
    "sensitive": {
        "title": "敏感词",
        "api_prefix": "sensitive",
        "response_key": "sensitive",
        "fields": [
            {"name": "sensitiveWord", "label": "敏感词库", "type": "textarea", "required": True, "tip": "敏感词请用英文逗号\u201c,\u201d隔开"},
            {"name": "state", "label": "是否开启", "type": "radio", "required": True, "options": [{"label": "是", "value": 1}, {"label": "否", "value": 0}]},
            {"name": "handleMeasures", "label": "处理措施", "type": "radio", "required": True, "options": [{"label": "禁止发布", "value": 1}, {"label": "需要审核", "value": 2}]}
        ]
    },
    "usermenu": {
        "title": "用户菜单",
        "api_prefix": "usermenu",
        "response_key": "userMenu",
        "fields": [
            {"name": "name", "label": "名称", "type": "input", "required": True},
            {"name": "url", "label": "跳转路径", "type": "input", "required": True},
            {"name": "img", "label": "图片", "type": "image", "required": True, "tip": "建议尺寸：200*200像素，jpg、png图片类型"},
            {"name": "sort", "label": "排序", "type": "number", "required": True, "tip": "按照降序排列，数字越大越靠前"},
            {"name": "status", "label": "状态", "type": "radio", "required": True, "options": [{"label": "显示", "value": 0}, {"label": "不显示", "value": 1}]}
        ]
    },
    "report": {
        "title": "举报",
        "api_prefix": "report",
        "response_key": "report",
        "fields": [
            {"name": "type", "label": "举报类型", "type": "select", "required": True, "options": [{"label": "违法违规", "value": 1}, {"label": "色情低俗", "value": 2}, {"label": "虚假信息", "value": 3}]},
            {"name": "status", "label": "处理状态", "type": "radio", "required": True, "options": [{"label": "待处理", "value": 0}, {"label": "已处理", "value": 1}]}
        ]
    },
    "post": {
        "title": "帖子",
        "api_prefix": "post",
        "response_key": "post",
        "fields": [
            {"name": "title", "label": "标题", "type": "input", "required": True},
            {"name": "content", "label": "内容", "type": "textarea", "required": True},
            {"name": "status", "label": "状态", "type": "radio", "required": True, "options": [{"label": "正常", "value": 0}, {"label": "禁用", "value": 1}]}
        ]
    },
    "xiangqin": {
        "title": "相亲",
        "api_prefix": "xiangqin",
        "response_key": "xiangqin",
        "fields": [
            {"name": "title", "label": "标题", "type": "input", "required": True},
            {"name": "content", "label": "内容", "type": "textarea", "required": True},
            {"name": "status", "label": "状态", "type": "radio", "required": True, "options": [{"label": "正常", "value": 0}, {"label": "禁用", "value": 1}]}
        ]
    },
    "vipoption": {
        "title": "VIP选项",
        "api_prefix": "vipoption",
        "response_key": "vipOption",
        "fields": [
            {"name": "name", "label": "名称", "type": "input", "required": True},
            {"name": "price", "label": "价格", "type": "number", "required": True},
            {"name": "days", "label": "天数", "type": "number", "required": True},
            {"name": "sort", "label": "排序", "type": "number", "required": True}
        ]
    },
    "usersign": {
        "title": "用户签到",
        "api_prefix": "usersign",
        "response_key": "userSign",
        "fields": [
            {"name": "signDay", "label": "签到天数", "type": "number", "required": True},
            {"name": "integral", "label": "积分", "type": "number", "required": True}
        ]
    },
    "tags": {
        "title": "标签",
        "api_prefix": "tags",
        "response_key": "tags",
        "fields": [
            {"name": "tagName", "label": "标签名称", "type": "input", "required": True},
            {"name": "sort", "label": "排序", "type": "number", "required": True},
            {"name": "status", "label": "状态", "type": "radio", "required": True, "options": [{"label": "启用", "value": 0}, {"label": "禁用", "value": 1}]}
        ]
    },
    "signconfig": {
        "title": "签到配置",
        "api_prefix": "signconfig",
        "response_key": "signConfig",
        "fields": [
            {"name": "signDay", "label": "签到天数", "type": "number", "required": True},
            {"name": "integral", "label": "积分奖励", "type": "number", "required": True}
        ]
    },
    "user": {
        "title": "用户",
        "api_prefix": "user",
        "response_key": "user",
        "fields": [
            {"name": "status", "label": "状态", "type": "radio", "required": True, "options": [{"label": "正常", "value": 0}, {"label": "禁用", "value": 1}]},
            {"name": "type", "label": "类型", "type": "radio", "required": True, "options": [{"label": "普通用户", "value": 0}, {"label": "官方账号", "value": 1}]}
        ]
    },
    "userrecharge": {
        "title": "用户充值",
        "api_prefix": "userrecharge",
        "response_key": "userRecharge",
        "fields": [
            {"name": "amount", "label": "充值金额", "type": "number", "required": True},
            {"name": "status", "label": "状态", "type": "radio", "required": True, "options": [{"label": "待支付", "value": 0}, {"label": "已支付", "value": 1}]}
        ]
    },
    "cashout": {
        "title": "提现",
        "api_prefix": "cashout",
        "response_key": "cashout",
        "fields": [
            {"name": "amount", "label": "提现金额", "type": "number", "required": True},
            {"name": "status", "label": "状态", "type": "radio", "required": True, "options": [{"label": "待审核", "value": 0}, {"label": "已通过", "value": 1}, {"label": "已拒绝", "value": 2}]}
        ]
    },
    "pay": {
        "title": "支付配置",
        "api_prefix": "pay",
        "response_key": "pay",
        "fields": [
            {"name": "name", "label": "名称", "type": "input", "required": True},
            {"name": "price", "label": "价格", "type": "number", "required": True},
            {"name": "status", "label": "状态", "type": "radio", "required": True, "options": [{"label": "启用", "value": 0}, {"label": "禁用", "value": 1}]}
        ]
    }
}

def generate_component(module_name, config):
    """生成单个组件的代码"""
    
    # 生成表单项
    form_items = []
    for field in config["fields"]:
        if field["type"] == "input":
            form_items.append(f'''      <el-form-item label="{field['label']}" prop="{field['name']}">
        <el-input
          v-model="form.{field['name']}"
          placeholder="请输入{field['label']}"
          clearable
        />
      </el-form-item>''')
        elif field["type"] == "textarea":
            tip = f'\n        <div class="el-upload__tip">{field["tip"]}</div>' if "tip" in field else ""
            form_items.append(f'''      <el-form-item label="{field['label']}" prop="{field['name']}">
        <el-input
          v-model="form.{field['name']}"
          type="textarea"
          :rows="3"
          placeholder="请输入{field['label']}"
          clearable
        />{tip}
      </el-form-item>''')
        elif field["type"] == "number":
            tip = f'\n        <div class="el-upload__tip">{field["tip"]}</div>' if "tip" in field else ""
            form_items.append(f'''      <el-form-item label="{field['label']}" prop="{field['name']}">
        <el-input-number
          v-model="form.{field['name']}"
          :min="0"
          controls-position="right"
        />{tip}
      </el-form-item>''')
        elif field["type"] == "image":
            tip = f'\n        <div class="el-upload__tip">{field["tip"]}</div>' if "tip" in field else ""
            form_items.append(f'''      <el-form-item label="{field['label']}" prop="{field['name']}">
        <image-upload
          v-model="form.{field['name']}"
          :limit="1"
        />{tip}
      </el-form-item>''')
        elif field["type"] == "radio":
            options = "\n          ".join([f'<el-radio :label="{opt["value"]}">{opt["label"]}</el-radio>' for opt in field["options"]])
            form_items.append(f'''      <el-form-item label="{field['label']}" prop="{field['name']}">
        <el-radio-group v-model="form.{field['name']}">
          {options}
        </el-radio-group>
      </el-form-item>''')
        elif field["type"] == "select":
            options = "\n          ".join([f'<el-option label="{opt["label"]}" :value="{opt["value"]}" />' for opt in field["options"]])
            form_items.append(f'''      <el-form-item label="{field['label']}" prop="{field['name']}">
        <el-select
          v-model="form.{field['name']}"
          placeholder="请选择{field['label']}"
          clearable
        >
          {options}
        </el-select>
      </el-form-item>''')
    
    form_items_str = "\n      \n".join(form_items)
    
    # 生成rules
    rules = []
    for field in config["fields"]:
        if field.get("required"):
            rules.append(f'''  {field['name']}: [
    {{ required: true, message: '{field['label']}不能为空', trigger: 'blur' }}
  ]''')
    
    rules_str = ",\n  ".join(rules)
    
    # 生成form字段初始化
    form_fields = []
    for field in config["fields"]:
        default_value = "0" if field["type"] in ["number", "radio", "select"] else "''"
        form_fields.append(f"  {field['name']}: {default_value}")
    
    form_fields_str = ",\n".join(form_fields)
    
    # 生成数据赋值
    field_assigns = []
    for field in config["fields"]:
        field_assigns.append(f"          {field['name']}: res.{config['response_key']}.{field['name']}")
    
    field_assigns_str = ",\n".join(field_assigns)
    
    # 生成重置表单
    form_resets = []
    for field in config["fields"]:
        default_value = "0" if field["type"] in ["number", "radio", "select"] else "''"
        form_resets.append(f"  form.{field['name']} = {default_value};")
    
    form_resets_str = "\n".join(form_resets)
    
    # 生成提交数据
    submit_data = []
    for field in config["fields"]:
        submit_data.append(f"          {field['name']}: form.{field['name']}")
    
    submit_data_str = ",\n".join(submit_data)
    
    template = f'''<template>
  <el-dialog
    :title="!form.id ? '新增{config['title']}' : '修改{config['title']}'"
    v-model="visible"
    width="600px"
    :close-on-click-modal="false"
    draggable
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      {form_items_str}
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="Shejiao{config['title'].replace(' ', '')}AddOrUpdate">
import {{ ref, reactive, getCurrentInstance }} from 'vue';
import {{ get{config['api_prefix'].capitalize()}, add{config['api_prefix'].capitalize()}, update{config['api_prefix'].capitalize()} }} from '@/api/shejiao/{config['api_prefix']}';
import {{ {config['api_prefix'].capitalize()}Form }} from '@/api/shejiao/{config['api_prefix']}/types';

const {{ proxy }} = getCurrentInstance() as ComponentInternalInstance;

const visible = ref(false);
const submitLoading = ref(false);
const formRef = ref();

const form = reactive<{config['api_prefix'].capitalize()}Form>({{
  id: undefined,
{form_fields_str}
}});

const rules = {{
{rules_str}
}};

/** 打开对话框 */
const open = async (id?: number) => {{
  visible.value = true;
  resetForm();
  
  if (id) {{
    try {{
      const res: any = await get{config['api_prefix'].capitalize()}(id);
      if (res.{config['response_key']}) {{
        Object.assign(form, {{
          id: res.{config['response_key']}.id,
{field_assigns_str}
        }});
      }}
    }} catch (error) {{
      proxy?.$modal.msgError('获取{config['title']}信息失败');
      visible.value = false;
    }}
  }}
}};

/** 重置表单 */
const resetForm = () => {{
  form.id = undefined;
{form_resets_str}
  formRef.value?.clearValidate();
}};

/** 提交表单 */
const handleSubmit = async () => {{
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid: boolean) => {{
    if (valid) {{
      submitLoading.value = true;
      try {{
        const data: {config['api_prefix'].capitalize()}Form = {{
          id: form.id,
{submit_data_str}
        }};
        
        if (form.id) {{
          await update{config['api_prefix'].capitalize()}(data);
          proxy?.$modal.msgSuccess('修改成功');
        }} else {{
          await add{config['api_prefix'].capitalize()}(data);
          proxy?.$modal.msgSuccess('新增成功');
        }}
        
        visible.value = false;
        emit('success');
      }} catch (error) {{
        console.error('提交失败:', error);
      }} finally {{
        submitLoading.value = false;
      }}
    }}
  }});
}};

/** 关闭对话框 */
const handleClose = () => {{
  visible.value = false;
  resetForm();
}};

const emit = defineEmits(['success']);

defineExpose({{
  open
}});
</script>

<style scoped lang="scss">
.el-upload__tip {{
  margin-top: 8px;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}}
</style>
'''
    
    return template

def main():
    """主函数"""
    base_dir = "/Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme/ai-le-me-ui/src/views/shejiao"
    
    print("开始批量生成 add-or-update.vue 组件...")
    
    for module_name, config in MODULE_CONFIGS.items():
        # 确定目标路径
        if module_name in ["vipoption", "usersign", "tags", "signconfig", "userrecharge"]:
            target_dir = os.path.join(base_dir, "user", module_name)
        elif module_name in ["cashout"]:
            target_dir = os.path.join(base_dir, "pay", module_name)
        elif module_name == "pay":
            target_dir = os.path.join(base_dir, "pay")
        else:
            target_dir = os.path.join(base_dir, module_name)
        
        target_file = os.path.join(target_dir, "add-or-update.vue")
        
        # 检查文件是否已存在
        if os.path.exists(target_file):
            print(f"  - {module_name}: 已存在，跳过")
            continue
        
        # 生成组件代码
        component_code = generate_component(module_name, config)
        
        # 写入文件
        with open(target_file, 'w', encoding='utf-8') as f:
            f.write(component_code)
        
        print(f"  ✓ {module_name}: 已生成 -> {target_file}")
    
    print("\n批量生成完成！")
    print(f"共处理 {len(MODULE_CONFIGS)} 个模块")

if __name__ == "__main__":
    main()
