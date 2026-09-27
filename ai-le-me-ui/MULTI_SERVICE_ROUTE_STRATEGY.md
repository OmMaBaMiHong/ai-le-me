# 多服务路由策略

## 概述
本项目采用多服务架构，主要包含两个核心服务：
1. **ai-le-me-admin** - 主后端服务，提供系统管理、用户权限等基础功能
2. **3d-matrix-world (shejiao)** - 社交后端服务，提供红娘、用户关系等业务功能

## 前端架构设计

### 1. 多服务HTTP客户端配置
在 `src/utils/request.ts` 中定义了两个独立的axios实例：
- `yueLaoService`: 主后端服务，用于系统管理功能
- `shejiaoService`: 社交后端服务，用于业务功能

环境变量配置：
- `VITE_APP_BASE_API`: 主后端API地址
- `VITE_APP_SHEJIAO_API`: 社交后端API地址

### 2. 路由加载机制
- 前端路由由后端菜单动态生成
- 菜单信息从 `/system/menu/getRouters` 接口获取
- 根据用户权限动态加载相应路由

### 3. API调用策略
- 基础系统功能（用户管理、角色管理、菜单管理等）使用主后端服务
- 业务功能（红娘管理、用户关系管理等）使用社交后端服务
- 在API层面对不同服务进行区分，避免混淆

### 4. 具体实现示例
以 `hongniang-user` 模块为例：
- 视图组件位于 `src/views/hongniang/hongniang-user/index.vue`
- API接口位于 `src/api/hongniang/hongniangUser.ts`，使用 `shejiaoService` 实例
- 数据类型定义位于 `src/api/hongniang/types.ts`

### 5. 跨服务数据处理
- 统一认证：所有服务共享JWT Token
- 数据格式适配：统一数据结构，兼容不同服务的返回格式
- 错误处理：统一错误处理机制，对不同服务的错误进行统一处理

## 配置文件

### 环境变量
```bash
# 主后端API
VITE_APP_BASE_API = '/prod-api'

# 社交后端API
VITE_APP_SHEJIAO_API = 'http://localhost:8080'
```

### HTTP客户端配置
```typescript
// 创建主后端 axios 实例 (ai-le-me-admin)
const yueLaoService = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/prod-api',
  timeout: 50000
});

// 创建社交后端 axios 实例 (3d-matrix-world)
const shejiaoService = axios.create({
  baseURL: import.meta.env.VITE_APP_SHEJIAO_API || 'http://localhost:8080',
  timeout: 50000
});
```

## 最佳实践

1. **API分离**：按功能领域分离API调用，明确各服务职责
2. **类型安全**：定义清晰的TypeScript接口，保证跨服务数据一致性
3. **错误处理**：统一错误处理机制，提供良好的用户体验
4. **认证同步**：确保所有服务使用相同的认证机制
5. **监控调试**：提供清晰的日志输出，便于调试跨服务请求