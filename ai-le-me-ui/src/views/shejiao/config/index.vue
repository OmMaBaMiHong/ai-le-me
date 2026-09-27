<template>
  <div class="topPadding">
    <div class="config-bridge">
      <div class="bridge-title">业务配置中心</div>
      <div class="bridge-desc">
        这里仅保留平台业务规则与前台展示配置。微信、短信、支付、存储、AI 模型等渠道参数已统一迁移到第三方服务配置。
      </div>
      <div class="bridge-actions">
        <el-button type="primary" @click="openThirdparty('video')">打开第三方服务配置</el-button>
        <el-button plain @click="openThirdparty('payment')">支付配置</el-button>
        <el-button plain @click="openThirdparty('sms')">短信配置</el-button>
      </div>
    </div>
    <el-tabs v-model="activeName">
      <el-tab-pane label="帖子设置" name="post">
        <div class="app-container">
          <el-form
            ref="form"
            :model="form"
            :rules="rules"
            size="small"
            label-width="150px"
          >
            <el-form-item label="普通贴人工审核">
              <el-radio v-model="form.normalPost" :label="0">人工审核</el-radio>
              <el-radio v-model="form.normalPost" :label="1">自动过审</el-radio>
            </el-form-item>
            <el-form-item label="付费贴人工审核">
              <el-radio v-model="form.vipPost" :label="0">人工审核</el-radio>
              <el-radio v-model="form.vipPost" :label="1">自动过审</el-radio>
            </el-form-item>

            <el-form-item label="付费贴抽成">
              <el-input
                v-model="form.postPrice"
                style="width: 370px"
                type="number"
              />
            </el-form-item>
            <el-form-item label="">
              <el-button type="primary" @click="doSubmit(form)">提交</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane label="前台设置" name="frontend">
        <div class="app-container">
          <el-form
            ref="form"
            :model="form2"
            :rules="rules"
            size="small"
            label-width="150px"
          >
            <el-form-item label="视频入口开关">
              <el-radio v-model="form2.isOpen" :label="0">开启</el-radio>
              <el-radio v-model="form2.isOpen" :label="1">关闭</el-radio>
            </el-form-item>
            <el-form-item label="充值开关">
              <el-radio v-model="form2.chargeIsOpen" :label="0">开启</el-radio>
              <el-radio v-model="form2.chargeIsOpen" :label="1">关闭</el-radio>
            </el-form-item>
            <el-form-item label="提现开关">
              <el-radio v-model="form2.canCashOut" :label="1">开启</el-radio>
              <el-radio v-model="form2.canCashOut" :label="0">关闭</el-radio>
            </el-form-item>
            <el-form-item label="积分兑换余额开关">
              <el-radio v-model="form2.exchange" :label="0">开启</el-radio>
              <el-radio v-model="form2.exchange" :label="1">关闭</el-radio>
            </el-form-item>

            <el-form-item label="兑换比例">
              <el-input
                v-model="form2.integral"
                style="width: 370px"
                type="number"
              />
              <p style="color: red; line-height: 0px">
                兑换一块钱需要的积分数 必须为整数
              </p>
            </el-form-item>
            <el-form-item label="圈子页公告">
              <el-input v-model="form2.noticeContent" style="width: 370px" />
            </el-form-item>
            <el-form-item label="项目logo" prop="coverImage">
              <el-upload
                class="avatar-uploader"
                :action="uploadUrl"
                :show-file-list="false"
                :on-success="handleIconSuccess"
              >
                <img v-if="form2.img" :src="form2.img" class="avatar" />
                <i v-else class="el-icon-plus avatar-uploader-icon" />
              </el-upload>
              <p class="formInfo">建议尺寸：200*200像素，jpg、png图片类型</p>
            </el-form-item>
            <el-form-item label="用户个人页背景图" prop="coverImage">
              <el-upload
                class="avatar-uploader"
                :action="uploadUrl"
                :show-file-list="false"
                :on-success="handleIconSuccess3"
              >
                <img v-if="form2.bgImg" :src="form2.bgImg" class="avatar2" />
                <i v-else class="el-icon-plus avatar-uploader-icon" />
              </el-upload>
              <p class="formInfo">建议尺寸：750*400像素，jpg、png图片类型</p>
            </el-form-item>
            <el-form-item label="">
              <el-button type="primary" @click="doSubmit(form2)">提交</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="客服设置" name="service">
        <div class="app-container">
          <el-form
            ref="form3"
            :model="form2"
            :rules="rules"
            size="small"
            label-width="150px"
          >
            <el-form-item label="客服工作时间">
              <el-input v-model="form3.contactTime" style="width: 370px" />
            </el-form-item>
            <el-form-item label="客服微信号">
              <el-input v-model="form3.contactWechat" style="width: 370px" />
            </el-form-item>
            <el-form-item label="客服电话">
              <el-input v-model="form3.contactPhone" style="width: 370px" />
            </el-form-item>
            <el-form-item label="客服微信二维码" prop="contactWechatQr">
              <el-upload
                class="avatar-uploader"
                :action="uploadUrl"
                :show-file-list="false"
                :on-success="handleIconSuccess2"
              >
                <img
                  v-if="form3.contactWechatQr"
                  :src="form3.contactWechatQr"
                  class="avatar"
                />
                <i v-else class="el-icon-plus avatar-uploader-icon" />
              </el-upload>
              <p class="formInfo">建议尺寸：430*430像素，jpg、png图片类型</p>
            </el-form-item>
            <el-form-item label="">
              <el-button type="primary" @click="doSubmit(form3)">提交</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="抽奖设置" name="luckdraw">
        <div class="app-container">
          <el-form
            ref="form4"
            :model="form4"
            :rules="rules"
            size="small"
            label-width="150px"
          >
            <el-form-item label="抽奖是否开启">
              <el-radio v-model="form4.luckDrawStatus" :label="1"
                >开启</el-radio
              >
              <el-radio v-model="form4.luckDrawStatus" :label="0"
                >关闭</el-radio
              >
            </el-form-item>
            <el-form-item label="每次抽奖消耗积分数">
              <el-input
                v-model="form4.luckDrawIntegral"
                style="width: 370px"
                type="number"
              />
            </el-form-item>

            <el-form-item label="每天抽奖次数">
              <el-input
                v-model="form4.surplus"
                style="width: 370px"
                type="number"
              />
            </el-form-item>

            <el-form-item label="抽奖规则">
              <el-input
                v-model="form4.luckDrawRule"
                style="width: 500px"
                type="textarea"
                :autosize="{ minRows: 5, maxRows: 50}"
              />
            </el-form-item>
            <el-form-item label="">
              <el-button type="primary" @click="doSubmit(form4)">提交</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="注册设置" name="register">
        <div class="app-container">
          <el-form
            ref="form5"
            :model="form4"
            :rules="rules"
            size="small"
            label-width="150px"
          >
          <el-form-item label="邮箱登录是否开启">
              <el-radio v-model="form5.emailLogin" :label="1"
                >开启</el-radio
              >
              <el-radio v-model="form5.emailLogin" :label="0"
                >关闭</el-radio
              >
            <p class="formInfo">开启后请在后端yml文件中配置邮箱相关参数</p>
          </el-form-item>
          <el-form-item label="个人隐私协议">
              <el-input
                v-model="form5.privacy"
                style="width: 500px"
                :autosize="{ minRows: 10, maxRows: 50}"
                type="textarea"
              />
          </el-form-item>
          <el-form-item label="用户服务协议">
              <el-input
                v-model="form5.protocol"
                style="width: 500px"
                type="textarea"
                autosize
              />
            <p class="formInfo">内容不要超过1500字符</p>
          </el-form-item>
            <el-form-item label="">
              <el-button type="primary" @click="doSubmit(form5)">提交</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="会员设置" name="vip">
        <div class="app-container">
          <el-form
            ref="form6"
            :model="form4"
            :rules="rules"
            size="small"
            label-width="150px"
          >
          <el-form-item label="会员充值协议">
              <el-input
                v-model="form6.vipAgreeContent"
                style="width: 500px"
                type="textarea"
                autosize
              />
            <p class="formInfo">内容不要超过1500字符</p>
          </el-form-item>
            <el-form-item label="会员积分奖励翻倍数">
              <el-input
                v-model="form6.vipIntegral"
                style="width: 370px"
                type="number"
              />
            </el-form-item>
            <el-form-item label="发帖积分奖励">
              <el-input
                v-model="form6.addPostIntegral"
                style="width: 370px"
                type="number"
              />
            </el-form-item>
            <el-form-item label="会员每月改名次数">
              <el-input
                v-model="form6.vipRename"
                style="width: 370px"
                type="number"
              />
            </el-form-item>
            <el-form-item label="普通用户每月改名数">
              <el-input
                v-model="form6.commonRename"
                style="width: 370px"
                type="number"
              />
            </el-form-item>
            <el-form-item label="会员可创建圈子数">
              <el-input
                v-model="form6.vipTopicNumber"
                style="width: 370px"
                type="number"
              />
            </el-form-item>
            <el-form-item label="普通用户可创建圈子">
              <el-input
                v-model="form6.commonTopicNumber"
                style="width: 370px"
                type="number"
              />
            </el-form-item>
<!--            <el-form-item label="会员广告屏蔽">-->
<!--              <el-input-->
<!--                v-model="form6.vipAdBlock"-->
<!--                style="width: 370px"-->
<!--                type="number"-->
<!--              />-->
<!--            </el-form-item>-->
<!--            <el-form-item label="会员支持付费帖子">-->
<!--              <el-input-->
<!--                v-model="form6.vipPaidPost"-->
<!--                style="width: 370px"-->
<!--                type="number"-->
<!--              />-->
<!--              <el-form-item label="会员课创建ai分身数">-->
<!--                <el-input-->
<!--                  v-model="form6.aiApp"-->
<!--                  style="width: 370px"-->
<!--                  type="number"-->
<!--                />-->
<!--            </el-form-item>-->
            <el-form-item label="">
              <el-button type="primary" @click="doSubmit(form6)">提交</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts" name="ShejiaoConfig">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { listConfig, updateConfigBatch } from '@/api/system/config';
import { getToken } from '@/utils/auth';

const router = useRouter();
const uploadUrl = ref("");
const activeName = ref("post");

const form = reactive({
  normalPost: 0,
  vipPost: 0,
  postPrice: 0,
});

const form2 = reactive({
  isOpen: 0,
  chargeIsOpen: 0,
  canCashOut: 0,
  exchange: 0,
  integral: 0,
  noticeContent: "",
  img: "",
  bgImg: "",
});

const form3 = reactive({
  contactTime: "",
  contactPhone: "",
  contactWechatQr: "",
  contactWechat: "",
});

const form4 = reactive({
  surplus: 0,
  luckDrawStatus: 0,
  luckDrawRule: "",
  luckDrawIntegral: 0,
});

const form5 = reactive({
  protocol: "",
  privacy: "",
  emailLogin: 0
});

const form6 = reactive({
  vipAgreeContent: "",
  vipIntegral: 0,
  addPostIntegral: 0,
  vipRename: 0,
  commonRename: 0,
  vipTopicNumber: 0,
  commonTopicNumber: 0,
});

const rules = reactive({});

const openThirdparty = (tab: string) => {
  router.push({
    path: '/system/thirdparty',
    query: { tab }
  });
};

// 获取数据列表
const getDataList = async () => {
  // 设置上传 URL (使用 shejiao 后端)
  uploadUrl.value = import.meta.env.VITE_APP_SHEJIAO_API + `/admin/oss/upload?token=${getToken()}`;
  
  try {
    const res: any = await listConfig({
      pageNum: 1,
      pageSize: 1000
    });

    const list = res.rows || [];

    list.forEach((item: any) => {
      const keyName = item.configKey;
      let newValue: any = item.configValue;

      const numberFields = [
        'normalPost', 'vipPost', 'postPrice',
        'isOpen', 'chargeIsOpen', 'canCashOut', 'exchange', 'integral',
        'surplus', 'luckDrawStatus', 'luckDrawIntegral',
        'emailLogin',
        'vipIntegral', 'addPostIntegral', 'vipRename', 'commonRename', 'vipTopicNumber', 'commonTopicNumber'
      ];

      if (numberFields.includes(keyName)) {
        newValue = parseInt(newValue) || 0;
      }

      if (keyName in form) {
        (form as any)[keyName] = newValue;
      } else if (keyName in form2) {
        (form2 as any)[keyName] = newValue;
      } else if (keyName in form3) {
        (form3 as any)[keyName] = newValue;
      } else if (keyName in form4) {
        (form4 as any)[keyName] = newValue;
      } else if (keyName in form5) {
        (form5 as any)[keyName] = newValue;
      } else if (keyName in form6) {
        (form6 as any)[keyName] = newValue;
      }
    });
  } catch (error) {
    console.error('获取配置列表失败:', error);
    ElMessage.error('获取配置列表失败');
  }
};

const doSubmit = async (formData: any) => {
  try {
    await updateConfigBatch(formData);
    ElMessage({
      message: "设置成功",
      type: "success",
      duration: 1500,
    });
  } catch (error) {
    console.error('保存配置失败:', error);
  }
};

const handleIconSuccess = (response: any) => {
  form2.img = response.url;
};

const handleIconSuccess2 = (response: any) => {
  form3.contactWechatQr = response.url;
};

const handleIconSuccess3 = (response: any) => {
  form2.bgImg = response.url;
};

onMounted(() => {
  getDataList();
});
</script>

<style scoped>
.app-container {
  padding: 20px 20px 45px 20px;
}
.topPadding {
  padding-left: 30px;
}

.config-bridge {
  margin: 0 30px 20px 0;
  padding: 18px 20px;
  border-radius: 16px;
  border: 1px solid #e4e7ed;
  background: linear-gradient(135deg, #fff8ef 0%, #f6fbff 58%, #f4fff8 100%);
}

.bridge-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.bridge-desc {
  margin-top: 8px;
  max-width: 760px;
  line-height: 1.7;
  color: #5b6475;
}

.bridge-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.formInfo {
  line-height: 0px;
  color: #999999;
  font-size: 12px;
}
.notice {
  line-height: 0px;
  color: #656161;
}
.avatar-uploader .el-upload {
  border: 3px dashed #979494;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.avatar-uploader .el-upload:hover {
  border-color: #409eff;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}
.avatar {
  width: 100px;
  height: 100px;
  display: block;
}
.avatar2 {
  width: 200px;
  height: 80px;
  display: block;
}
</style>
