<template>
  <div class="app-container home">
    <el-card shadow="never" class="hero-card">
      <div class="eyebrow">AiAi 后台运营中心</div>
      <h2>正式发布只保留主链能力</h2>
      <p class="hero-copy">
        当前上线面收敛为 H5、微信小程序、Java 单体服务和管理后台四条主链，其余历史模块不纳入本轮发布。
      </p>
      <div class="hero-actions">
        <el-button type="primary" @click="goTarget('https://plus-doc.dromara.org/#/ruoyi-vue-plus/changlog')">查看基础框架变更</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="overview-grid">
      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="overview-card">
          <div class="overview-label">用户端 H5</div>
          <div class="overview-value">multi-platform-app</div>
          <div class="overview-tip">生产构建输出 H5 发布包</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="overview-card">
          <div class="overview-label">微信小程序</div>
          <div class="overview-value">mp-weixin</div>
          <div class="overview-tip">同仓多端构建，需保持页面行为一致</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="overview-card">
          <div class="overview-label">Java 单体</div>
          <div class="overview-value">ai-le-me-admin.jar</div>
          <div class="overview-tip">统一承载业务、Quartz 和三方能力</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="overview-card">
          <div class="overview-label">管理后台</div>
          <div class="overview-value">ai-le-me-ui/dist</div>
          <div class="overview-tip">运营、配置、任务与业务运营入口</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="section-grid">
      <el-col :xs="24" :xl="12">
        <el-card shadow="never" class="section-card">
          <template #header>上线前必查</template>
          <ul class="bullet-list">
            <li>三方密钥只允许存放在三方配置中心，不再散落在业务配置里。</li>
            <li>Quartz 是唯一保留的任务体系，历史工作流和旧任务命名只做下线清理。</li>
            <li>H5 与小程序需共用同一套核心链路：登录、消息、活动、支付、AI 创作。</li>
          </ul>
        </el-card>
      </el-col>
      <el-col :xs="24" :xl="12">
        <el-card shadow="never" class="section-card">
          <template #header>当前目标</template>
          <ul class="bullet-list">
            <li>清理历史菜单与残留文案，避免后台出现不再发布的模块影子。</li>
            <li>统一产物、环境变量、依赖服务与回滚步骤，形成正式发布清单。</li>
            <li>完成一轮四链构建验证，确保构建告警和明显阻断项被收口。</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index" lang="ts">
const goTarget = (url: string) => {
  window.open(url, '__blank');
};
</script>

<style lang="scss" scoped>
.home {
  color: #334155;
}

.hero-card {
  margin-bottom: 16px;
  border: 1px solid #dbeafe;
  background: linear-gradient(135deg, #eff6ff 0%, #ecfeff 100%);
}

.eyebrow {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #0f766e;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h2 {
  margin: 0;
  font-size: 28px;
  color: #0f172a;
}

.hero-copy {
  max-width: 760px;
  margin: 12px 0 0;
  line-height: 1.7;
  color: #475569;
}

.hero-actions {
  margin-top: 18px;
}

.overview-grid,
.section-grid {
  margin-top: 16px;
}

.overview-card,
.section-card {
  height: 100%;
}

.overview-label {
  font-size: 13px;
  color: #64748b;
}

.overview-value {
  margin-top: 10px;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.overview-tip {
  margin-top: 8px;
  line-height: 1.6;
  color: #475569;
}

.bullet-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.9;
  color: #475569;
}
</style>
