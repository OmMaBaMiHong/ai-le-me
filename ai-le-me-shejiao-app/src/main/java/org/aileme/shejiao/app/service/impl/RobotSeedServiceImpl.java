package org.aileme.shejiao.app.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.app.oss.factory.RuoyiSysClound;
import org.aileme.shejiao.app.service.ai.image.AIImageStrategyManager;
import org.aileme.shejiao.api.service.*;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.common.utils.NaturalUsernameGenerator;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.domain.entity.admin.TopicEntity;
import org.aileme.shejiao.domain.entity.admin.VoteOptionEntity;
import org.aileme.shejiao.domain.entity.admin.VoteSubjectEntity;
import org.aileme.shejiao.domain.entity.app.RecommendLoveEntity;
import org.aileme.shejiao.domain.entity.app.UserVideoEntity;
import org.aileme.shejiao.domain.entity.app.UserSettingEntity;
import org.aileme.shejiao.domain.entity.job.SysQuartzJob;
import org.aileme.shejiao.domain.param.app.GenerateVideoForm;
import org.aileme.shejiao.gateway.chat.ChatModelGatewayService;

import java.math.BigDecimal;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

@Slf4j
@Service
@DS("master")
public class RobotSeedServiceImpl implements RobotSeedService {

    private static final AtomicLong MOBILE_SEQUENCE = new AtomicLong(System.currentTimeMillis() % 1_000_000_000L);
    private static final AtomicLong CONTENT_SEQUENCE = new AtomicLong(System.currentTimeMillis() % 10_000);
    private static final String ROBOT_JOB_CODE = "robot_single_post_generation";
    private static final String ROBOT_AI_FACTORY_JOB_CODE = "robot_ai_factory_generation";
    private static final String ROBOT_AI_FACTORY_SCENE = "robot_seed_factory";
    private static final String ROBOT_AI_FACTORY_PROVIDER = "doubao";
    private static final DateTimeFormatter BIRTHDAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final long GENERATED_IMAGE_UPLOAD_TIMEOUT_SECONDS = 45L;
    private static final int LOCAL_FIGURE_WIDTH = 1080;
    private static final int LOCAL_FIGURE_HEIGHT = 1440;

    private static final String[] CITIES = {
            "杭州", "上海", "苏州", "南京", "宁波", "深圳", "广州", "成都", "武汉", "厦门",
            "长沙", "青岛", "天津", "西安", "郑州", "合肥", "重庆", "福州", "珠海", "无锡"
    };

    private static final String[] PROVINCES = {
            "浙江省", "上海市", "江苏省", "江苏省", "浙江省", "广东省", "广东省", "四川省", "湖北省", "福建省",
            "湖南省", "山东省", "天津市", "陕西省", "河南省", "安徽省", "重庆市", "福建省", "广东省", "江苏省"
    };

    private static final String[] FEMALE_JOBS = {
            "产品经理", "品牌策划", "小学老师", "运营经理", "插画师", "新媒体编辑", "室内设计师", "咖啡店主理人",
            "护士", "财务主管", "健身教练", "民宿主理人", "心理咨询师", "婚礼策划", "摄影师",
            "服装买手", "舞蹈老师", "珠宝顾问", "宠物美容师", "私募IR", "皮肤管理师", "空乘"
    };

    private static final String[] MALE_JOBS = {
            "后端工程师", "建筑设计师", "短视频导演", "新能源项目经理", "律师", "宠物医生", "广告创意总监", "外贸经理",
            "金融分析师", "飞盘教练", "纪录片摄影师", "咖啡烘焙师", "工业设计师", "数据产品经理", "民谣乐手",
            "航空机械师", "室内灯光设计师", "投行顾问", "健身工作室主理人", "汽车评测博主", "口腔医生", "香水品牌运营"
    };

    private static final String[] FEMALE_INTERESTS = {
            "健身", "普拉提", "瑜伽", "爬山", "徒步", "美食探店", "手冲咖啡", "拍照", "旅行", "Citywalk",
            "烘焙", "看展", "骑行", "跑步", "游泳", "露营", "舞蹈", "羽毛球", "逛书店", "滑雪",
            "撸猫", "香薰", "电影", "飞盘", "皮划艇", "花艺", "吉他"
    };

    private static final String[] MALE_INTERESTS = {
            "撸铁", "健身", "跑步", "篮球", "羽毛球", "超跑", "自驾", "咖啡", "摄影", "露营",
            "徒步", "爬山", "游泳", "骑行", "做饭", "桌游", "看展", "电影", "滑雪", "Citywalk",
            "拳击", "路亚", "威士忌", "吉他", "桨板", "宠物", "探店"
    };

    private static final String[] TEMPERAMENT_LABELS = {
            "慢热但熟了会很主动", "情绪稳定，讲话不绕弯", "外表清冷，熟人面前很会接梗", "做事利落，生活里反而很软",
            "社交不算闹腾，但会认真回应", "有自己的节奏，不喜欢用试探推进关系", "看起来松弛，其实对关系挺上心",
            "有一点表达欲，但不喜欢空泛营业", "日常温和，遇到在意的人会很有行动力", "不擅长热场，但很会照顾相处感受"
    };

    private static final String[] SOCIAL_STYLE_LABELS = {
            "更适合一对一慢慢聊", "小范围社交会比热闹局更自在", "第一次见面会先观察细节，再慢慢放开",
            "面对面会比线上消息更有状态", "属于越聊越有意思的类型", "不爱场面话，喜欢真实交换近况",
            "适合边走边聊，或者一起做点具体事情", "如果氛围松弛，会比想象中健谈很多"
    };

    private static final String[] EMOTIONAL_STYLE_LABELS = {
            "会通过陪伴和落实细节表达喜欢", "不太会嘴上轰炸，但行动很稳定", "更在意回应速度和情绪边界",
            "喜欢有回音的交流，不爱冷处理", "愿意把时间留给在意的人", "喜欢把喜欢说清楚，也希望被认真接住",
            "对关系有耐心，但不想反复拉扯", "更看重长期舒服，不追求短促上头"
    };

    private static final String[] LIFE_RHYTHM_LABELS = {
            "工作日节奏紧，周末会认真回血", "平时习惯早起，把生活收拾得比较清爽", "白天投入工作，晚上切回生活模式很快",
            "不追求满档安排，更喜欢有呼吸感的日程", "喜欢把运动、吃饭、见朋友分得比较清楚", "最近在把作息重新调顺",
            "属于工作认真、休息也不想敷衍自己的那种节奏", "会给自己留固定的独处时间和出门时间"
    };

    private static final String[] WEEKEND_STYLE_LABELS = {
            "周末愿意早起出门", "更喜欢上午运动、下午探店", "有空会约朋友爬山或者看展", "会把一天拆成运动、吃饭、散步三段",
            "喜欢边走边拍，顺手把生活留住", "偏爱短途自驾或者近郊出走", "偶尔宅家做饭，也能过得很舒服", "喜欢先把家里收拾好再出门"
    };

    private static final String[] COMMUNICATION_STYLE_LABELS = {
            "不太喜欢嗯嗯哦哦式聊天", "会更喜欢有信息量的交流", "回复不一定秒回，但会认真接住话题",
            "比起查户口，更喜欢从日常切进去认识人", "不太能接受忽冷忽热", "聊得来以后会分享很多生活碎片",
            "更适合真实直接一点的表达", "不太擅长暧昧拉扯，更喜欢明确推进"
    };

    private static final String[] RELATIONSHIP_GOAL_LABELS = {
            "认真交往并自然走向长期关系", "先从稳定认识开始，再看能不能走进彼此生活", "希望是能落到现实安排里的恋爱",
            "想找能一起过好日子的人", "更想建立舒服、有回应、可持续的关系", "希望认识以后可以尽快见面，不只停留在线上",
            "比起热闹开始，更看重稳定推进", "想谈那种能互相偏爱的长期关系"
    };

    private static final String[] AESTHETIC_STYLE_LABELS = {
            "干净利落", "松弛轻熟", "自然有力量感", "带一点都市感", "更偏生活化质感", "清爽克制", "有点镜头感但不过度摆拍", "真实比精修重要"
    };

    private static final String[] FAMILY_VISION_LABELS = {
            "两个人一起把普通日子过顺", "忙完也愿意留时间给彼此", "家里有烟火气，也有各自空间", "能一起安排旅行、做饭和节日",
            "不是互相消耗，而是一起变得更稳定", "有事情能第一时间沟通", "既能并肩，也能互相照顾", "关系里有偏爱，也有分寸"
    };

    private static final String[] FEMALE_DECLARATIONS = {
            "想找一个情绪稳定、愿意把日子慢慢过好的人。",
            "比起轰轰烈烈，我更喜欢被认真回应。",
            "希望我们都是真诚的成年人，能把喜欢说清楚。",
            "理想关系是一起出门，也一起回家。"
    };

    private static final String[] MALE_DECLARATIONS = {
            "想遇到一个能互相偏爱，也能一起规划未来的人。",
            "我更看重相处舒服，愿意把细节做到位。",
            "不喜欢试探，喜欢坦诚和稳定的双向奔赴。",
            "希望以后忙完一天，回家还能有人分享日常。"
    };

    private static final String[] FEMALE_INTROS = {
            "慢热但不冷淡，熟起来以后很会照顾人。",
            "平时节奏不快，喜欢把生活过得有点仪式感。",
            "工作认真，休息时很爱到处走走看看。",
            "不太擅长社交场面，更喜欢真实一点的相处。"
    };

    private static final String[] MALE_INTROS = {
            "不太会花言巧语，但答应的事会认真做到。",
            "喜欢稳定的节奏，也保留一点少年感。",
            "工作之外会尽量给生活留出空间和兴趣。",
            "属于相处起来越来越有意思的那一类。"
    };

    private static final String[] IMAGE_SELF_TITLES = {
            "今天把自己还给生活",
            "周末状态不错，留一张给自己",
            "认真更新一下最近的我",
            "最近这张状态，想分享出来",
            "下班后拍的一组生活切片",
            "刚运动完的状态，顺手记录一下",
            "今天是很适合出片的我",
            "留一组更贴近生活的近况",
            "最近喜欢这种清爽有力量感的状态"
    };

    private static final String[] IMAGE_SHOWCASE_TITLES = {
            "最近喜欢这种干净直接的状态",
            "认真出片，也认真找对象",
            "不想只做资料卡，想让你看见真实的我",
            "如果你也喜欢有生活感的人，可以认识一下",
            "今天这组更像我本人一点",
            "练完以后拍一组，很像我平时的状态",
            "这组有点生活里的高级感",
            "不是模板自拍，是我最近真实的样子",
            "顺手留一组更有氛围的近照"
    };

    private static final String[] COUPLE_MOOD_TITLES = {
            "我理想里的恋爱，是一起把普通日子过漂亮",
            "想把恋爱谈成舒服又稳定的日常",
            "更喜欢两个人慢慢靠近的感觉",
            "比起热闹，我更想认真喜欢一个人",
            "如果是双向偏爱，日子会很有意思"
    };

    private static final String[] ACTIVITY_GLAMOUR_TITLES = {
            "今晚这场局，帅哥靓女都挺在线",
            "这类活动最打动我的，是颜值之外的分寸感",
            "现场比照片里更有氛围",
            "认真社交的人聚在一起，状态会很好看",
            "这一场活动的氛围感和人都很加分"
    };

    private static final String[] VIDEO_SELF_TITLES = {
            "简单介绍一下现在的我",
            "想认真认识人，所以做了一条口播版名片",
            "如果你也在认真找对象，可以看看这条",
            "把自己的状态拍成一条小视频",
            "不是模板感自我介绍，是我现在真实的样子",
            "运动完顺手录一条，算是正式打个招呼",
            "今天用口播版近况介绍一下自己",
            "不是照本宣科，是更接近真实生活的一条视频"
    };

    private static final String[] ARTICLE_RELATIONSHIP_TITLES = {
            "我现在更喜欢怎样的关系",
            "认真找对象以后，我更在意这些",
            "比起心动，我更想要稳定回应",
            "成年人的喜欢，应该是轻松但有分量的",
            "把自己的恋爱观认真说一次",
            "把最近的生活节奏和恋爱观一起说清楚",
            "现在的我，更适合怎样的人和关系"
    };

    private static final String[] VOTE_DATING_TITLES = {
            "第一次见面，你更在意哪种感觉",
            "认真恋爱里，你最看重什么",
            "你会被哪种相处细节打动",
            "线下见面你更喜欢什么氛围",
            "开始一段关系前，你会先观察什么",
            "你更容易被哪种生活状态吸引",
            "相亲社交里，你会先注意对方哪一点"
    };

    private static final String[] SELF_SHARE_OPENERS = {
            "今天的状态比较松弛，想把这种真实生活感发出来。",
            "最近会更愿意记录自己，而不是只把生活过掉。",
            "不是刻意营业，就是想认真留一条近况。",
            "这一组没有太多修饰，比较像我平时真实的样子。",
            "下班去运动以后整个人都轻了一点，顺手记录一下。",
            "周末出去爬山、拍照、喝咖啡，这种节奏会让我觉得很舒服。"
    };

    private static final String[] SHOWCASE_OPENERS = {
            "如果要认识一个人，我更希望先让对方看到我本来的状态。",
            "认真出片不是为了摆拍，是想把自己的气质和生活感留下来。",
            "与其只放资料，不如让人看见我平时的真实样子。",
            "我喜欢干净、自洽、有一点光感的自己。",
            "比起精修模板，我更想保留一点真实的身材线条和生活质感。",
            "偶尔会认真出片，但更想让人看到我平时真实的状态。"
    };

    private static final String[] COUPLE_VISION_LINES = {
            "我理想里的关系，是一起散步、一起买菜、一起把很普通的日子过出温度。",
            "比起轰轰烈烈，我更喜欢两个人稳定靠近、慢慢熟悉、慢慢偏爱。",
            "想要的恋爱不是消耗，而是两个人在一起以后都更舒服。",
            "如果以后是双向喜欢，我希望我们能把生活过得轻松又有回应。"
    };

    private static final String[] ACTIVITY_GLAMOUR_LINES = {
            "这种局最吸引我的，不是热闹，而是现场真的会出现气质很好的帅哥靓女。",
            "这场更像高质量社交，不会硬尬聊，大家都在认真展示自己。",
            "现场状态比线上更有说服力，很多人都是清爽、真诚、能聊天的类型。",
            "认真交友的人聚在一起，整个场子的颜值和氛围都会自然抬起来。"
    };

    private static final String[] VIDEO_MONOLOGUE_LINES = {
            "平时是认真工作、也认真生活的人，想找一个能真诚靠近的人。",
            "如果你也在找稳定关系，这条口播版的自我介绍就当我们先认识一下。",
            "不太想在聊天框里反复试探，所以更想直接一点地介绍自己。",
            "我现在更适合那种舒服、踏实、有回音的关系。",
            "平时会健身、跑步，也会认真安排约会和生活节奏。",
            "工作之外我很重视生活质感，也希望以后是两个人一起把日子过好。"
    };

    private static final String[] ACTIVITY_SCENARIOS = {
            "周六咖啡破冰局",
            "下班后微醺聊天局",
            "春日晚风散步局",
            "认真交友桌游夜",
            "周末书店轻聊天",
            "城市天台观景轻社交"
    };

    private static final String[] CONTENT_TONE_SEEDS = {
            "像真实朋友圈近况，不像文案样板",
            "偏生活切片，口吻自然，不装成熟",
            "像认真找对象的人在做自我展示",
            "有轻微表达欲，但不油腻不端着",
            "像下班后随手发的近况，带一点真诚介绍",
            "像社交平台里会让人点开主页继续看的内容"
    };

    private static final String[] RELATIONSHIP_ANGLES = {
            "更看重回应感和稳定推进",
            "更喜欢有来有回、能落到日常的关系",
            "不想停留在表面聊天，希望能往线下认真认识",
            "比起条件罗列，更在意相处里的松弛和偏爱",
            "希望两个人都保留自己，但也愿意一起安排生活"
    };

    private static final String[] VISUAL_DIRECTIONS = {
            "清爽自然光", "城市通勤感", "健身后松弛感", "周末户外感", "咖啡馆生活感", "轻熟氛围感"
    };

    private static final String[] VIDEO_DIRECTIONS = {
            "下班后口播版自我介绍",
            "运动结束后的生活化口播",
            "周末出门前的镜头自述",
            "工作切换到生活状态的短视频口播",
            "真实相亲社交向的近况介绍"
    };

    private static final String[] ARTICLE_TITLES = {
            "最近想明白的一件小事", "认真恋爱前，我更想先做好自己", "三十岁以后，我开始喜欢稳定的关系",
            "比起条件，我更在意相处的松弛感", "一个人也很好，但两个人会更有意思"
    };

    private static final String[] FEMALE_SCENE_HIGHLIGHTS = {
            "健身房练背后的清爽状态", "瑜伽结束以后那种松弛感", "周末爬山回来的自然状态",
            "探店吃饭时的生活感", "拍一组腿部线条和整体状态都在线的照片", "下班后去普拉提的日常节奏"
    };

    private static final String[] MALE_SCENE_HIGHLIGHTS = {
            "刚练完胸背肩的状态", "周末开车跑山顺手拍的一组", "运动完八块腹肌比较在线的时候",
            "自驾去看日落的生活感", "认真生活也认真出片的状态", "超跑和城市夜景一起出现的那种氛围"
    };

    private static final String[] VOTE_TITLES = {
            "第一次见面更想去哪", "你更在意哪种安全感", "周末约会你会选什么",
            "认真恋爱里最重要的是什么", "你会被哪种细节打动"
    };

    private static final String[] COMMUNITY_TOPIC_SUFFIXES = {
            "认真相处局", "下班后慢聊局", "同城见面局", "生活分享局", "周末松弛局", "真诚脱单局"
    };

    private static final String[] HONGNIANG_COMPANIES = {
            "煊光同频社", "城市见面研究所", "认真恋爱计划", "同城慢聊俱乐部", "关系推进工作室", "线下社交事务局"
    };

    private final AppUserService appUserService;
    private final TopicService topicService;
    private final HongniangService hongniangService;
    private final PostService postService;
    private final VoteSubjectService voteSubjectService;
    private final VoteOptionService voteOptionService;
    private final UserSettingService userSettingService;
    private final RecommendLoveService recommendLoveService;
    private final SysQuartzJobService sysQuartzJobService;
    private final UserVideoService userVideoService;
    private final AIImageStrategyManager imageStrategyManager;
    private final ChatModelGatewayService chatModelGatewayService;
    private final ObjectMapper objectMapper;
    private final HttpClient imageDownloadClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public RobotSeedServiceImpl(AppUserService appUserService,
                                TopicService topicService,
                                HongniangService hongniangService,
                                PostService postService,
                                VoteSubjectService voteSubjectService,
                                VoteOptionService voteOptionService,
                                UserSettingService userSettingService,
                                RecommendLoveService recommendLoveService,
                                @Lazy SysQuartzJobService sysQuartzJobService,
                                UserVideoService userVideoService,
                                @Lazy AIImageStrategyManager imageStrategyManager,
                                @Lazy ChatModelGatewayService chatModelGatewayService,
                                ObjectMapper objectMapper) {
        this.appUserService = appUserService;
        this.topicService = topicService;
        this.hongniangService = hongniangService;
        this.postService = postService;
        this.voteSubjectService = voteSubjectService;
        this.voteOptionService = voteOptionService;
        this.userSettingService = userSettingService;
        this.recommendLoveService = recommendLoveService;
        this.sysQuartzJobService = sysQuartzJobService;
        this.userVideoService = userVideoService;
        this.imageStrategyManager = imageStrategyManager;
        this.chatModelGatewayService = chatModelGatewayService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> generateBatchUsersWithPosts(int userCount, int minPostsPerUser, int maxPostsPerUser) {
        if (userCount <= 0) {
            throw new IllegalArgumentException("userCount 必须大于 0");
        }
        if (minPostsPerUser <= 0 || maxPostsPerUser < minPostsPerUser) {
            throw new IllegalArgumentException("帖子数量范围不合法");
        }
        GenerationOptions generationOptions = new GenerationOptions(true, "hybrid", ROBOT_AI_FACTORY_PROVIDER, "");
        List<TopicEntity> topicPool = loadPublicTopics();
        int createdUsers = 0;
        int createdPosts = 0;
        int createdVideos = 0;
        int failedCount = 0;
        List<Map<String, Object>> samples = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            try {
                RobotProfile profile = createProfile((int) (System.currentTimeMillis() % Integer.MAX_VALUE) + i, topicPool);
                AppUserEntity user = createRobotUser(profile, generationOptions);
                createdUsers++;
                int contentCount = RandomUtil.randomInt(minPostsPerUser, maxPostsPerUser + 1);
                Set<ContentLane> usedLanes = new LinkedHashSet<>();
                for (int j = 0; j < contentCount; j++) {
                    ContentCreationResult result = createContentForUser(
                            user,
                            RobotProfile.fromExisting(user, resolvePreferredTopicForExistingUser(user)),
                            topicPool,
                            true,
                            j,
                            usedLanes,
                            generationOptions
                    );
                    if (result.postId() != null) {
                        createdPosts++;
                    }
                    if (result.videoId() != null) {
                        createdVideos++;
                    }
                }
                if (samples.size() < 20) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("uid", user.getUid());
                    item.put("username", user.getUsername());
                    item.put("city", user.getCity());
                    item.put("avatar", user.getAvatar());
                    item.put("figur", user.getFigur());
                    samples.add(item);
                }
            } catch (Exception ex) {
                failedCount++;
                log.warn("[robot-seed] create new robot user failed, index={}, reason={}", i, ex.getMessage(), ex);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "new_users_with_posts");
        result.put("usersRequested", userCount);
        result.put("usersCreated", createdUsers);
        result.put("postsCreated", createdPosts);
        result.put("videoTasksCreated", createdVideos);
        result.put("failedCount", failedCount);
        result.put("contentFactoryMode", generationOptions.contentFactoryMode());
        result.put("preferredProvider", generationOptions.preferredProvider());
        result.put("samples", samples);
        return result;
    }

    @Override
    public Map<String, Object> generatePostsForExistingUsers(Map<String, Object> options) {
        ExistingUserBatchOptions batchOptions = parseExistingUserBatchOptions(options);
        List<AppUserEntity> targetUsers = loadExistingSeedUsers(batchOptions);
        if (targetUsers.isEmpty()) {
            throw new IllegalStateException("未找到符合条件的现有用户，无法生成动态");
        }
        Map<String, Object> communityResult = ensureExistingUserCommunityScaffold(targetUsers);
        List<TopicEntity> topicPool = loadPublicTopics();

        int postCount = 0;
        int videoTaskCount = 0;
        int failedCount = 0;
        List<Integer> touchedUserIds = new ArrayList<>();
        List<Map<String, Object>> samples = new ArrayList<>();
        List<Map<String, Object>> failedSamples = new ArrayList<>();
        for (AppUserEntity user : targetUsers) {
            RobotProfile profile = RobotProfile.fromExisting(user, resolvePreferredTopicForExistingUser(user));
            touchedUserIds.add(user.getUid());
            int contentCount = RandomUtil.randomInt(batchOptions.minPostsPerUser(), batchOptions.maxPostsPerUser() + 1);
            Set<ContentLane> usedLanes = new LinkedHashSet<>();
            for (int j = 0; j < contentCount; j++) {
                try {
                    ContentCreationResult result = createContentForUser(
                            user,
                            profile,
                            topicPool,
                            true,
                            j,
                            usedLanes,
                            batchOptions.generationOptions()
                    );
                    if (result.postId() != null) {
                        postCount++;
                    }
                    if (result.videoId() != null) {
                        videoTaskCount++;
                    }
                    if (samples.size() < 24) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("uid", user.getUid());
                        item.put("username", user.getUsername());
                        item.put("lane", result.lane().name());
                        item.put("postId", result.postId());
                        item.put("videoId", result.videoId());
                        item.put("status", result.status());
                        samples.add(item);
                    }
                } catch (Exception ex) {
                    failedCount++;
                    log.warn("[robot-seed] create content failed for existing user. uid={}, index={}, reason={}",
                            user.getUid(), j, ex.getMessage(), ex);
                    if (failedSamples.size() < 24) {
                        Map<String, Object> failedItem = new LinkedHashMap<>();
                        failedItem.put("uid", user.getUid());
                        failedItem.put("username", user.getUsername());
                        failedItem.put("contentIndex", j);
                        failedItem.put("reason", ex.getMessage());
                        failedSamples.add(failedItem);
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "existing_users_posts");
        result.put("scope", batchOptions.userScope());
        result.put("usersSelected", targetUsers.size());
        result.put("postsCreated", postCount);
        result.put("videoTasksCreated", videoTaskCount);
        result.put("failedCount", failedCount);
        result.put("contentFactoryMode", batchOptions.generationOptions().contentFactoryMode());
        result.put("preferredProvider", batchOptions.generationOptions().preferredProvider());
        result.put("preferredModel", batchOptions.generationOptions().preferredModel());
        result.put("userIds", touchedUserIds);
        result.put("sampleResults", samples);
        result.put("failedSamples", failedSamples);
        result.put("communityScaffold", communityResult);
        return result;
    }

    @Override
    public Map<String, Object> refreshExistingUserFigures(Map<String, Object> options) {
        Map<String, Object> safeOptions = options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
        ExistingUserBatchOptions batchOptions = parseExistingUserBatchOptions(safeOptions);
        List<AppUserEntity> targetUsers = loadExistingSeedUsers(batchOptions);
        if (targetUsers.isEmpty()) {
            throw new IllegalStateException("未找到符合条件的现有用户，无法刷新形象图");
        }

        int minImages = parseIntOption(safeOptions.get("minImages"), 1, 1, 3);
        int maxImages = parseIntOption(safeOptions.get("maxImages"), 3, minImages, 3);
        boolean onlyDirty = parseBooleanOption(safeOptions.get("onlyDirty"), true);
        Set<String> duplicateFigureMedia = findDuplicateFigureMedia(targetUsers);

        int refreshedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        List<Map<String, Object>> samples = new ArrayList<>();

        for (AppUserEntity user : targetUsers) {
            boolean dirtyBefore = hasDirtyOrDuplicateFigure(user, duplicateFigureMedia);
            if (onlyDirty && !dirtyBefore) {
                skippedCount++;
                continue;
            }
            List<String> references = collectAvatarOnlyReferenceMedia(user);
            if (references.isEmpty()) {
                failedCount++;
                continue;
            }

            RobotProfile profile = RobotProfile.fromExisting(user, resolvePreferredTopicForExistingUser(user));
            int desiredCount = RandomUtil.randomInt(minImages, maxImages + 1);
            LinkedHashSet<String> generated = new LinkedHashSet<>();
            for (int i = 0; i < desiredCount; i++) {
                ContentLane lane = pickFigureRefreshLane(profile, i);
                if (batchOptions.generationOptions().aiEnabled()) {
                    generated.addAll(generateAiSceneMediaByReference(
                            user,
                            references,
                            profile,
                            lane,
                            i,
                            1,
                            batchOptions.generationOptions()
                    ));
                } else {
                    generated.addAll(generateLocalSceneMediaByReference(
                            user,
                            references,
                            profile,
                            lane,
                            i,
                            1
                    ));
                }
            }

            List<String> finalMedia = generated.stream()
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .limit(maxImages)
                    .collect(Collectors.toList());
            if (finalMedia.isEmpty()) {
                failedCount++;
                continue;
            }

            user.setFigur(String.join(",", finalMedia));
            user.setUpdateTime(new Date());
            appUserService.updateById(user);
            refreshedCount++;

            if (samples.size() < 20) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("uid", user.getUid());
                item.put("username", user.getUsername());
                item.put("dirtyBefore", dirtyBefore);
                item.put("imageCount", finalMedia.size());
                item.put("figur", finalMedia);
                samples.add(item);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "refresh_existing_user_figures");
        result.put("usersSelected", targetUsers.size());
        result.put("onlyDirty", onlyDirty);
        result.put("refreshedCount", refreshedCount);
        result.put("skippedCount", skippedCount);
        result.put("failedCount", failedCount);
        result.put("sampleResults", samples);
        return result;
    }

    @Override
    @DSTransactional
    public Map<String, Object> generateSinglePost(boolean createUserIfNeeded) {
        return generateSinglePostInternal(GenerationOptions.aiDefault(createUserIfNeeded));
    }

    @Override
    @DSTransactional
    public Map<String, Object> generateSinglePostWithOptions(Map<String, Object> options) {
        return generateSinglePostInternal(parseGenerationOptions(options));
    }

    private Map<String, Object> generateSinglePostInternal(GenerationOptions generationOptions) {
        List<TopicEntity> topicPool = loadPublicTopics();
        AppUserEntity robotUser = pickRandomRobotUser();
        RobotProfile profile;
        boolean createdUser = false;
        if (robotUser == null && generationOptions.createUserIfNeeded()) {
            throw new IllegalStateException("老图池已删除，当前没有可直接发内容的机器人用户，请先准备带真人头像/形象图的用户。");
        } else if (robotUser == null) {
            throw new IllegalStateException("当前没有可用机器人用户");
        } else {
            profile = RobotProfile.fromExisting(robotUser, resolvePreferredTopicForExistingUser(robotUser));
        }
        ContentCreationResult created = createContentForUser(robotUser, profile, topicPool, false, 0, new LinkedHashSet<>(), generationOptions);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "single_post");
        result.put("createdUser", createdUser);
        result.put("uid", robotUser.getUid());
        result.put("postId", created.postId());
        result.put("videoId", created.videoId());
        result.put("contentLane", created.lane().name());
        result.put("status", created.status());
        result.put("username", robotUser.getUsername());
        result.put("contentFactoryMode", generationOptions.contentFactoryMode());
        result.put("preferredProvider", generationOptions.preferredProvider());
        result.put("preferredModel", generationOptions.preferredModel());
        return result;
    }

    @Override
    @DSTransactional
    public Map<String, Object> ensureDefaultSinglePostQuartzJob() {
        SysQuartzJob job = sysQuartzJobService.lambdaQuery()
                .eq(SysQuartzJob::getDeleted, 0)
                .eq(SysQuartzJob::getJobCode, ROBOT_JOB_CODE)
                .last("limit 1")
                .one();
        boolean created = false;
        if (job == null) {
            job = new SysQuartzJob();
            job.setJobName("机器人单条内容生成");
            job.setJobGroup("ROBOT");
            job.setJobCode(ROBOT_JOB_CODE);
            job.setCronExpression("0 0 0/2 * * ?");
            job.setJobParams("{\"createUserIfNeeded\":true}");
            job.setAllowConcurrent(0);
            job.setStatus(SysQuartzJob.STATUS_PAUSED);
            job.setRemark("默认关闭，每两小时生成一条机器人内容，可在 Quartz 后台手动启用。");
            sysQuartzJobService.saveJob(job);
            created = true;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("jobId", job.getId());
        result.put("jobCode", job.getJobCode());
        result.put("status", job.getStatus());
        result.put("cronExpression", job.getCronExpression());
        return result;
    }

    @Override
    @DSTransactional
    public Map<String, Object> ensureDefaultAiFactoryQuartzJob() {
        SysQuartzJob job = sysQuartzJobService.lambdaQuery()
                .eq(SysQuartzJob::getDeleted, 0)
                .eq(SysQuartzJob::getJobCode, ROBOT_AI_FACTORY_JOB_CODE)
                .last("limit 1")
                .one();
        String defaultJobName = "机器人内容工厂";
        String defaultJobParams = "{\"createUserIfNeeded\":true,\"contentFactoryMode\":\"ai\",\"preferredProvider\":\"doubao\",\"preferredModel\":\"\"}";
        String defaultRemark = "默认关闭，每两小时生成一条机器人内容。内容只走模型动态生成，支持通过 jobParams 传 preferredProvider / preferredModel。";
        boolean created = false;
        if (job == null) {
            job = new SysQuartzJob();
            job.setJobName(defaultJobName);
            job.setJobGroup("ROBOT");
            job.setJobCode(ROBOT_AI_FACTORY_JOB_CODE);
            job.setCronExpression("0 0 0/2 * * ?");
            job.setJobParams(defaultJobParams);
            job.setAllowConcurrent(0);
            job.setStatus(SysQuartzJob.STATUS_PAUSED);
            job.setRemark(defaultRemark);
            sysQuartzJobService.saveJob(job);
            created = true;
        } else {
            job.setJobName(defaultJobName);
            job.setJobGroup("ROBOT");
            job.setCronExpression("0 0 0/2 * * ?");
            job.setJobParams(defaultJobParams);
            job.setAllowConcurrent(0);
            job.setRemark(defaultRemark);
            sysQuartzJobService.updateJob(job);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("jobId", job.getId());
        result.put("jobCode", job.getJobCode());
        result.put("status", job.getStatus());
        result.put("cronExpression", job.getCronExpression());
        return result;
    }

    private AppUserEntity pickRandomRobotUser() {
        List<AppUserEntity> robots = appUserService.lambdaQuery()
                .eq(AppUserEntity::getType, 2)
                .eq(AppUserEntity::getStatus, 0)
                .orderByDesc(AppUserEntity::getUid)
                .last("limit 80")
                .list();
        if (robots == null || robots.isEmpty()) {
            return null;
        }
        return RandomUtil.randomEle(robots);
    }

    private List<AppUserEntity> loadExistingSeedUsers(ExistingUserBatchOptions options) {
        List<Integer> explicitUidList = options.uidList() == null ? List.of() : options.uidList();
        if (!explicitUidList.isEmpty()) {
            List<AppUserEntity> users = appUserService.lambdaQuery()
                    .eq(AppUserEntity::getStatus, 0)
                    .in(AppUserEntity::getUid, explicitUidList)
                    .list();
            if (users == null || users.isEmpty()) {
                return List.of();
            }
            Map<Integer, AppUserEntity> userMap = users.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> item.getUid() != null)
                    .collect(Collectors.toMap(AppUserEntity::getUid, item -> item, (left, right) -> left, LinkedHashMap::new));
            List<AppUserEntity> ordered = explicitUidList.stream()
                    .map(userMap::get)
                    .filter(Objects::nonNull)
                    .filter(this::hasUsableSeedMedia)
                    .collect(Collectors.toCollection(ArrayList::new));
            if (Boolean.TRUE.equals(options.shuffle())) {
                Collections.shuffle(ordered);
            }
            return ordered;
        }
        int safeLimit = Math.max(1, Math.min(options.userLimit(), 200));
        String scope = options.userScope();
        var query = appUserService.lambdaQuery()
                .eq(AppUserEntity::getStatus, 0);
        if ("robot".equals(scope)) {
            query.eq(AppUserEntity::getType, 2);
        } else if ("legacy".equals(scope)) {
            query.lt(AppUserEntity::getUid, 106);
        } else if (!"all".equals(scope)) {
            query.and(wrapper -> wrapper.lt(AppUserEntity::getUid, 106).or().eq(AppUserEntity::getType, 2));
        }
        List<AppUserEntity> users = query
                .orderByDesc(AppUserEntity::getUid)
                .last("limit " + Math.max(safeLimit * 4, 120))
                .list();
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        List<AppUserEntity> filtered = users.stream()
                .filter(this::hasUsableSeedMedia)
                .collect(Collectors.toCollection(ArrayList::new));
        if (Boolean.TRUE.equals(options.shuffle())) {
            Collections.shuffle(filtered);
        }
        return filtered.stream().limit(safeLimit).collect(Collectors.toList());
    }

    private boolean hasUsableSeedMedia(AppUserEntity user) {
        if (user == null || user.getUid() == null) {
            return false;
        }
        return !collectReferenceMedia(user).isEmpty();
    }

    private List<TopicEntity> loadPublicTopics() {
        List<TopicEntity> topics = topicService.lambdaQuery()
                .eq(TopicEntity::getStatus, 0)
                .eq(TopicEntity::getIsPrivacy, 0)
                .eq(TopicEntity::getRest, 0)
                .orderByDesc(TopicEntity::getUserNum)
                .last("limit 12")
                .list();
        if (topics == null || topics.isEmpty()) {
            TopicEntity fallback = topicService.getById(Constant.OFFICIAL_TOPIC_ID);
            if (fallback == null) {
                throw new IllegalStateException("未找到可用于机器人发帖的公开圈子");
            }
            return List.of(fallback);
        }
        return topics;
    }

    private Map<String, Object> ensureExistingUserCommunityScaffold(List<AppUserEntity> targetUsers) {
        List<AppUserEntity> scaffoldUsers = new ArrayList<>();
        if (targetUsers != null) {
            scaffoldUsers.addAll(targetUsers);
        }
        if (scaffoldUsers.size() < 24) {
            List<AppUserEntity> extraUsers = appUserService.lambdaQuery()
                    .eq(AppUserEntity::getStatus, 0)
                    .and(wrapper -> wrapper.lt(AppUserEntity::getUid, 106).or().eq(AppUserEntity::getType, 2))
                    .orderByDesc(AppUserEntity::getUid)
                    .last("limit 160")
                    .list();
            if (extraUsers != null) {
                for (AppUserEntity user : extraUsers) {
                    if (user == null || user.getUid() == null || !hasUsableSeedMedia(user)) {
                        continue;
                    }
                    boolean exists = scaffoldUsers.stream().anyMatch(item -> Objects.equals(item.getUid(), user.getUid()));
                    if (!exists) {
                        scaffoldUsers.add(user);
                    }
                }
            }
        }
        Map<Integer, HongniangInfoEntity> hongniangMap = ensureHongniangProfiles(scaffoldUsers);
        Map<String, Integer> topicStats = ensureTopicScaffold(scaffoldUsers, hongniangMap);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("candidateUsers", scaffoldUsers.size());
        result.put("hongniangCount", hongniangMap.size());
        result.putAll(topicStats);
        return result;
    }

    private Map<Integer, HongniangInfoEntity> ensureHongniangProfiles(List<AppUserEntity> users) {
        Map<Integer, HongniangInfoEntity> hongniangMap = new LinkedHashMap<>();
        if (users == null || users.isEmpty()) {
            return hongniangMap;
        }
        for (AppUserEntity user : users) {
            if (user == null || user.getUid() == null) {
                continue;
            }
            HongniangInfoEntity hongniang = hongniangService.getByUserId(user.getUid());
            if (hongniang != null && Objects.equals(hongniang.getStatus(), 1)) {
                hongniangMap.put(user.getUid(), hongniang);
            }
        }

        int targetCount = Math.min(8, Math.max(4, users.size() / 6));
        if (hongniangMap.size() >= targetCount) {
            return hongniangMap;
        }

        List<AppUserEntity> candidates = users.stream()
                .filter(this::isEligibleHongniangUser)
                .sorted(Comparator.comparing((AppUserEntity user) -> user.getGender() == null ? 9 : user.getGender())
                        .thenComparing(AppUserEntity::getUid, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        for (AppUserEntity user : candidates) {
            if (hongniangMap.size() >= targetCount) {
                break;
            }
            if (hongniangMap.containsKey(user.getUid())) {
                continue;
            }
            HongniangInfoEntity hongniang = buildSeedHongniang(user, hongniangMap.size());
            hongniangService.save(hongniang);
            hongniangMap.put(user.getUid(), hongniang);
            user.setHongniangId(hongniang.getId());
            appUserService.updateById(user);
        }
        return hongniangMap;
    }

    private Map<String, Integer> ensureTopicScaffold(List<AppUserEntity> users,
                                                     Map<Integer, HongniangInfoEntity> hongniangMap) {
        Map<String, Integer> result = new LinkedHashMap<>();
        List<TopicEntity> existingTopics = topicService.lambdaQuery()
                .eq(TopicEntity::getStatus, 0)
                .eq(TopicEntity::getIsPrivacy, 0)
                .orderByAsc(TopicEntity::getId)
                .list();
        Map<Integer, TopicEntity> hongniangTopics = new LinkedHashMap<>();
        Map<Integer, TopicEntity> userTopics = new LinkedHashMap<>();
        if (existingTopics != null) {
            for (TopicEntity topic : existingTopics) {
                if (topic == null) {
                    continue;
                }
                if (topic.getHongniangId() != null && topic.getHongniangId() > 0) {
                    hongniangTopics.putIfAbsent(topic.getHongniangId(), topic);
                }
                if (topic.getUid() != null && topic.getUid() > 0) {
                    userTopics.putIfAbsent(topic.getUid(), topic);
                }
            }
        }

        int createdHongniangTopics = 0;
        for (Map.Entry<Integer, HongniangInfoEntity> entry : hongniangMap.entrySet()) {
            if (hongniangTopics.containsKey(entry.getValue().getId())) {
                continue;
            }
            AppUserEntity owner = users.stream()
                    .filter(user -> Objects.equals(user.getUid(), entry.getKey()))
                    .findFirst()
                    .orElse(null);
            if (owner == null) {
                continue;
            }
            createSeedTopic(owner, entry.getValue(), true, createdHongniangTopics, users);
            createdHongniangTopics++;
        }

        int targetUserTopics = Math.min(10, Math.max(4, users.size() / 5));
        int createdUserTopics = 0;
        for (AppUserEntity owner : users) {
            if (owner == null || owner.getUid() == null || userTopics.containsKey(owner.getUid())) {
                continue;
            }
            if (hongniangMap.containsKey(owner.getUid())) {
                continue;
            }
            if (createdUserTopics + userTopics.size() >= targetUserTopics) {
                break;
            }
            createSeedTopic(owner, null, false, createdUserTopics, users);
            createdUserTopics++;
        }

        int publicTopicCount = topicService.lambdaQuery()
                .eq(TopicEntity::getStatus, 0)
                .eq(TopicEntity::getIsPrivacy, 0)
                .count().intValue();
        result.put("publicTopicCount", publicTopicCount);
        result.put("createdHongniangTopics", createdHongniangTopics);
        result.put("createdUserTopics", createdUserTopics);
        return result;
    }

    private boolean isEligibleHongniangUser(AppUserEntity user) {
        if (user == null || user.getUid() == null || !hasUsableSeedMedia(user)) {
            return false;
        }
        Integer gender = user.getGender();
        return gender == null || gender == 2 || user.getVip() != null && user.getVip() > 0;
    }

    private HongniangInfoEntity buildSeedHongniang(AppUserEntity user, int index) {
        HongniangInfoEntity hongniang = new HongniangInfoEntity();
        hongniang.setUserId(user.getUid());
        hongniang.setHongniangName(StringUtils.defaultIfBlank(user.getUsername(), "爱情主理人"));
        hongniang.setAvatar(firstReferenceMedia(user));
        hongniang.setPhone(user.getMobile());
        hongniang.setWechat("hn_" + user.getUid());
        hongniang.setCompanyName(user.getCity() + RandomUtil.randomEle(HONGNIANG_COMPANIES));
        hongniang.setCertificationStatus(1);
        hongniang.setCertificationImg(firstReferenceMedia(user));
        hongniang.setIntro(trim(StringUtils.defaultIfBlank(user.getSelfIntroduction(), user.getIntro()), 180));
        hongniang.setServiceArea(StringUtils.defaultIfBlank(user.getCity(), "杭州"));
        hongniang.setLevel(Math.max(1, Math.min(3, 1 + index % 3)));
        hongniang.setTotalUsers(0);
        hongniang.setTotalActivities(0);
        hongniang.setSuccessCount(0);
        hongniang.setStatus(1);
        hongniang.setTenantId("0");
        hongniang.setCreateTime(new Date());
        hongniang.setUpdateTime(new Date());
        return hongniang;
    }

    private void createSeedTopic(AppUserEntity owner,
                                 HongniangInfoEntity hongniang,
                                 boolean hongniangTopic,
                                 int index,
                                 List<AppUserEntity> users) {
        TopicEntity topic = new TopicEntity();
        topic.setUid(owner.getUid());
        topic.setCateId(resolveTopicCateId(owner));
        topic.setChannelType(hongniangTopic ? 2 : 3);
        topic.setHongniangId(hongniangTopic && hongniang != null ? hongniang.getId() : null);
        topic.setTopicName(buildSeedTopicName(owner, hongniangTopic, index));
        topic.setDescription(buildSeedTopicDescription(owner, hongniangTopic));
        topic.setCoverImage(firstReferenceMedia(owner));
        topic.setBgImage(firstReferenceMedia(owner));
        topic.setTopType(hongniangTopic ? 1 : 0);
        topic.setStatus(0);
        topic.setIndexRecommend(1);
        topic.setUserNum(0);
        topic.setCreateTime(randomRecentDate(20));
        topic.setRest(0);
        topic.setQuestion("");
        topic.setIsPrivacy(0);
        topicService.save(topic);

        try {
            topicService.joinTopic(topic.getId(), owner);
        } catch (Exception ex) {
            log.debug("圈子创建后加入圈子失败，uid={}, topicId={}, reason={}", owner.getUid(), topic.getId(), ex.getMessage());
        }
        seedTopicMembers(topic.getId(), owner, users, hongniangTopic ? 8 : 5);
    }

    private void seedTopicMembers(Integer topicId, AppUserEntity owner, List<AppUserEntity> users, int maxJoinCount) {
        if (topicId == null || users == null || users.isEmpty()) {
            return;
        }
        List<AppUserEntity> candidates = users.stream()
                .filter(user -> user != null && user.getUid() != null)
                .filter(user -> !Objects.equals(user.getUid(), owner.getUid()))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(candidates);
        int joinCount = 0;
        for (AppUserEntity candidate : candidates) {
            if (joinCount >= maxJoinCount) {
                break;
            }
            try {
                topicService.joinTopic(topicId, candidate);
                joinCount++;
            } catch (Exception ignored) {
                // 已在圈子里或被限制时直接忽略，避免影响主流程
            }
        }
    }

    private String buildSeedTopicName(AppUserEntity owner, boolean hongniangTopic, int index) {
        String city = StringUtils.defaultIfBlank(owner.getCity(), "同城");
        String focus = firstNonBlank(extractPrimaryInterest(owner), StringUtils.defaultIfBlank(owner.getJob(), "认真相处"));
        String suffix = COMMUNITY_TOPIC_SUFFIXES[Math.floorMod(owner.getUid() + index, COMMUNITY_TOPIC_SUFFIXES.length)];
        if (hongniangTopic) {
            return trim(city + focus + "主理人" + suffix, 18);
        }
        return trim(city + focus + suffix, 18);
    }

    private String buildSeedTopicDescription(AppUserEntity owner, boolean hongniangTopic) {
        String intro = StringUtils.defaultIfBlank(owner.getSelfIntroduction(), owner.getIntro());
        String prefix = hongniangTopic ? "这是一个偏认真相处和线下见面的主理人圈子，" : "这是一个偏真实聊天和同城见面的用户圈子，";
        return trim(prefix + intro, 180);
    }

    private Integer resolveTopicCateId(AppUserEntity owner) {
        String interest = extractPrimaryInterest(owner);
        if (StringUtils.containsAny(interest, "健身", "撸铁", "跑步", "羽毛球", "瑜伽", "游泳")) {
            return 5;
        }
        if (StringUtils.containsAny(interest, "旅行", "徒步", "露营", "爬山", "自驾", "滑雪")) {
            return 6;
        }
        if (StringUtils.containsAny(interest, "音乐", "吉他", "Livehouse")) {
            return 2;
        }
        if (StringUtils.containsAny(interest, "二次元", "动漫")) {
            return 8;
        }
        if (StringUtils.containsAny(interest, "摄影", "拍照", "看展", "Citywalk")) {
            return 4;
        }
        return 9;
    }

    private TopicEntity resolvePreferredTopicForExistingUser(AppUserEntity user) {
        if (user == null || user.getUid() == null) {
            return null;
        }
        Integer hongniangId = user.getHongniangId();
        if (hongniangId == null || hongniangId <= 0) {
            HongniangInfoEntity hongniang = hongniangService.getByUserId(user.getUid());
            if (hongniang != null && hongniang.getId() != null) {
                hongniangId = hongniang.getId();
            }
        }
        if (hongniangId != null && hongniangId > 0) {
            TopicEntity hongniangTopic = topicService.lambdaQuery()
                    .eq(TopicEntity::getStatus, 0)
                    .eq(TopicEntity::getIsPrivacy, 0)
                    .eq(TopicEntity::getHongniangId, hongniangId)
                    .orderByDesc(TopicEntity::getUserNum)
                    .orderByAsc(TopicEntity::getId)
                    .last("limit 1")
                    .one();
            if (hongniangTopic != null) {
                return hongniangTopic;
            }
        }
        TopicEntity ownTopic = topicService.lambdaQuery()
                .eq(TopicEntity::getStatus, 0)
                .eq(TopicEntity::getIsPrivacy, 0)
                .eq(TopicEntity::getUid, user.getUid())
                .orderByDesc(TopicEntity::getChannelType)
                .orderByDesc(TopicEntity::getUserNum)
                .orderByAsc(TopicEntity::getId)
                .last("limit 1")
                .one();
        if (ownTopic != null) {
            return ownTopic;
        }
        return topicService.getById(Constant.OFFICIAL_TOPIC_ID);
    }

    private String extractPrimaryInterest(AppUserEntity owner) {
        String interest = StringUtils.defaultIfBlank(owner.getInterest(), "");
        if (StringUtils.isBlank(interest)) {
            return "";
        }
        return Arrays.stream(interest.split("[、,，]"))
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse("");
    }

    private String firstReferenceMedia(AppUserEntity user) {
        List<String> media = collectReferenceMedia(user);
        if (media != null && !media.isEmpty()) {
            return media.get(0);
        }
        return StringUtils.defaultIfBlank(user == null ? "" : user.getAvatar(), "");
    }

    private RobotProfile createProfile(int seed, List<TopicEntity> topicPool) {
        int gender = RandomUtil.randomBoolean() ? 1 : 2;
        int cityIndex = Math.floorMod(seed + RandomUtil.randomInt(CITIES.length), CITIES.length);
        String city = CITIES[cityIndex];
        String province = PROVINCES[cityIndex];
        int age = gender == 2 ? RandomUtil.randomInt(23, 34) : RandomUtil.randomInt(24, 36);
        int education = RandomUtil.randomInt(2, 6);
        String job = gender == 2 ? RandomUtil.randomEle(FEMALE_JOBS) : RandomUtil.randomEle(MALE_JOBS);
        List<String> interests = RandomUtil.randomEleList(Arrays.asList(gender == 2 ? FEMALE_INTERESTS : MALE_INTERESTS), 3);
        String backgroundTheme = normalizeTheme(job) + "-" + normalizeTheme(interests.get(0));
        String intro = RandomUtil.randomEle(gender == 2 ? FEMALE_INTROS : MALE_INTROS);
        String declaration = RandomUtil.randomEle(gender == 2 ? FEMALE_DECLARATIONS : MALE_DECLARATIONS);
        String temperament = RandomUtil.randomEle(TEMPERAMENT_LABELS);
        String socialStyle = RandomUtil.randomEle(SOCIAL_STYLE_LABELS);
        String emotionalStyle = RandomUtil.randomEle(EMOTIONAL_STYLE_LABELS);
        String lifeRhythm = RandomUtil.randomEle(LIFE_RHYTHM_LABELS);
        String weekendStyle = RandomUtil.randomEle(WEEKEND_STYLE_LABELS);
        String communicationStyle = RandomUtil.randomEle(COMMUNICATION_STYLE_LABELS);
        String relationshipGoal = RandomUtil.randomEle(RELATIONSHIP_GOAL_LABELS);
        String aestheticStyle = RandomUtil.randomEle(AESTHETIC_STYLE_LABELS);
        String familyVision = RandomUtil.randomEle(FAMILY_VISION_LABELS);
        TopicEntity preferredTopic = RandomUtil.randomEle(topicPool);
        return new RobotProfile(seed, gender, age, city, province, education, job, interests, intro, declaration,
                temperament, socialStyle, emotionalStyle, lifeRhythm, weekendStyle, communicationStyle,
                relationshipGoal, aestheticStyle, familyVision, backgroundTheme, preferredTopic);
    }

    private AppUserEntity createRobotUser(RobotProfile profile, GenerationOptions generationOptions) {
        SeedMediaBundle mediaBundle = generateSeedMediaBundle(profile, generationOptions);
        AppUserEntity entity = new AppUserEntity();
        entity.setMobile(nextUniqueMobile());
        entity.setEmail("robot_" + System.currentTimeMillis() + "_" + RandomUtil.randomInt(1000, 9999) + "@ai-ni.store");
        entity.setGender(profile.gender());
        entity.setUsername(nextUniqueUsername(profile.gender(), profile.city(), entity.getMobile()));
        entity.setAvatar(mediaBundle.avatar());
        entity.setFigur(mediaBundle.figur());
        entity.setProvince(profile.province());
        entity.setCity(profile.city());
        entity.setHomeCity(profile.city());
        entity.setAbodeCity(profile.city());
        entity.setLocationCity(profile.city());
        entity.setLocationUpdateTime(new Date());
        entity.setStatus(0);
        entity.setVip(RandomUtil.randomInt(100) < 18 ? Constant.VIP_USER : Constant.COMMON_USER);
        entity.setVipExpireTime(entity.getVip().equals(Constant.VIP_USER) ? DateUtil.offsetDay(new Date(), RandomUtil.randomInt(30, 240)) : null);
        entity.setType(2);
        entity.setGroupId(1);
        entity.setLevel(RandomUtil.randomInt(0, 5));
        entity.setAge(profile.age());
        entity.setBirthday(LocalDate.now().minusYears(profile.age()).minusDays(RandomUtil.randomInt(0, 365)).format(BIRTHDAY_FORMATTER));
        entity.setHeight(profile.gender() == 2 ? String.valueOf(RandomUtil.randomInt(158, 173)) : String.valueOf(RandomUtil.randomInt(170, 188)));
        entity.setEducation(profile.education());
        entity.setSchool(resolveSchool(profile.education()));
        entity.setEduCertifStatus(profile.education() >= 3 ? 1 : 0);
        entity.setIdentyCertifStatus(RandomUtil.randomInt(100) < 72 ? 1 : 0);
        entity.setJob(profile.job());
        entity.setIncome(RandomUtil.randomInt(1, 6));
        entity.setMarryStatus(0);
        entity.setIntro(trim(profile.job() + "，" + profile.temperament(), 120));
        entity.setSelfIntroduction(trim(profile.intro() + "，平时喜欢" + String.join("、", profile.interests()) + "，" + profile.lifeRhythm() + "。", 240));
        entity.setLoveDeclaration(trim(profile.declaration() + "，也更适合" + profile.relationshipGoal() + "。", 240));
        entity.setInterest(String.join("、", profile.interests()));
        entity.setTagStr(Arrays.asList(profile.city(), profile.job(), profile.interests().get(0), "真诚交友").toString());
        entity.setMoney(BigDecimal.ZERO);
        entity.setIntegral(RandomUtil.randomInt(8, 120));
        entity.setSignNum(RandomUtil.randomInt(0, 18));
        entity.setCreateTime(randomRecentDate(45));
        entity.setUpdateTime(new Date());
        appUserService.save(entity);

        UserSettingEntity setting = new UserSettingEntity();
        setting.setUid(entity.getUid());
        setting.setIsFollow(0);
        setting.setIsPost(0);
        setting.setIsWatch(0);
        userSettingService.save(setting);

        RecommendLoveEntity recommend = new RecommendLoveEntity();
        recommend.setUid(entity.getUid());
        recommend.setRecommendUid(0);
        recommend.setRecNum(50);
        recommend.setHasRecNums(0);
        recommend.setVip(entity.getVip());
        recommend.setCreateTime(entity.getCreateTime());
        recommendLoveService.save(recommend);

        topicService.joinTopic(Constant.OFFICIAL_TOPIC_ID, entity);
        if (profile.preferredTopic() != null && !Objects.equals(profile.preferredTopic().getId(), Constant.OFFICIAL_TOPIC_ID)) {
            try {
                topicService.joinTopic(profile.preferredTopic().getId(), entity);
            } catch (Exception ignored) {
                log.debug("机器人加入扩展圈子失败，已忽略。uid={}, topicId={}", entity.getUid(), profile.preferredTopic().getId());
            }
        }
        return entity;
    }

    private SeedMediaBundle generateSeedMediaBundle(RobotProfile profile, GenerationOptions generationOptions) {
        if (profile == null) {
            throw new IllegalArgumentException("profile 不能为空");
        }
        if (!generationOptions.aiEnabled() || imageStrategyManager == null) {
            throw new IllegalStateException("当前未启用 AI 图片生成，无法创建新用户真人头像");
        }
        String avatar = generateSeedAvatar(profile);
        List<String> figurList = generateSeedFigures(profile, avatar, generationOptions);
        return new SeedMediaBundle(avatar, figurList.isEmpty() ? "" : String.join(",", figurList));
    }

    private String generateSeedAvatar(RobotProfile profile) {
        String prompt = buildSeedAvatarPrompt(profile);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("function_type", "robot_seed_avatar");
        params.put("scene_code", "robot_seed_avatar");
        params.put("size", "2K");
        params.put("response_format", "url");
        params.put("watermark", false);
        params.put("max_images", 1);
        List<String> generatedUrls = imageStrategyManager.generateImages(prompt, List.of(), params);
        List<String> persisted = persistGeneratedImages(generatedUrls, 1);
        if (persisted.isEmpty()) {
            throw new IllegalStateException("新用户头像生成失败");
        }
        return persisted.get(0);
    }

    private List<String> generateSeedFigures(RobotProfile profile,
                                             String avatar,
                                             GenerationOptions generationOptions) {
        if (StringUtils.isBlank(avatar)) {
            return List.of();
        }
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("function_type", "robot_seed_figur");
            params.put("scene_code", "robot_seed_figur");
            params.put("size", "2K");
            params.put("response_format", "url");
            params.put("watermark", false);
            params.put("max_images", 1);
            params.put("sequential_image_generation", "auto");
            List<String> generatedUrls = imageStrategyManager.generateImages(
                    buildSeedFigurePrompt(profile),
                    List.of(avatar),
                    params
            );
            return persistGeneratedImages(generatedUrls, 1);
        } catch (Exception ex) {
            log.warn("[robot-seed] new user figur generation skipped, reason={}", ex.getMessage());
            return List.of();
        }
    }

    private String buildSeedAvatarPrompt(RobotProfile profile) {
        return "生成一个婚恋社交平台新用户头像。"
                + "人物要求："
                + (profile.gender() == 2 ? "亚洲女性" : "亚洲男性")
                + "，" + profile.age() + "岁，常住" + profile.city() + "，职业是" + profile.job() + "。"
                + "头像要求：真人摄影、亚洲面孔、单人、近景半身或头像、五官自然、真实皮肤质感、自然光、干净背景。"
                + "风格要贴合" + profile.job() + "和" + String.join("、", profile.interests()) + "的生活气质，"
                + "不要卡通，不要欧美脸，不要夸张美颜，不要文字水印，不要多人。";
    }

    private String buildSeedFigurePrompt(RobotProfile profile) {
        ContentLane lane = pickFigureRefreshLane(profile, 0);
        return "参考图中的同一个人，保持五官、脸型、年龄感和个人辨识度一致。"
                + (profile.gender() == 2 ? "亚洲女性" : "亚洲男性")
                + "，" + profile.age() + "岁，常住" + profile.city() + "，职业是" + profile.job() + "。"
                + "人物画像：" + buildFigurePersonaPack(profile, lane, 0) + "。"
                + "场景要求：" + buildLifeScene(profile, lane, 0) + "，贴近" + String.join("、", profile.interests()) + "和真实相亲社交氛围。"
                + "镜头指令：" + buildFigureShotDirective(profile, lane, 0) + "。"
                + "随机风格包：" + buildPromptFlavorPack(profile, lane, 0) + "。"
                + "画面要求：真人写实、自然光、生活化、不同场景、不同姿态、不要棚拍模板、不要多人、不要文字水印。"
                + "必须像这个人真实会拍出来的形象照，不要和其他用户共用同一套背景、构图、服装和动作。";
    }

    private ContentCreationResult createContentForUser(AppUserEntity user,
                                                       RobotProfile profile,
                                                       List<TopicEntity> topicPool,
                                                       boolean allowRichMix,
                                                       int contentIndex,
                                                       Set<ContentLane> usedLanes,
                                                       GenerationOptions generationOptions) {
        TopicEntity topic = profile.preferredTopic() != null ? profile.preferredTopic() : RandomUtil.randomEle(topicPool);
        ContentLane lane = pickContentLane(profile, allowRichMix, contentIndex, usedLanes, generationOptions);
        SeedContentBundle aiBundle = buildAiContentBundle(profile, lane, contentIndex, generationOptions);
        ensureModelGeneratedBundle(aiBundle, lane, profile, generationOptions);
        return switch (lane) {
            case IMAGE_SELF, IMAGE_SHOWCASE, IMAGE_COUPLE, IMAGE_ACTIVITY -> createImagePost(user, profile, topic, lane, contentIndex, aiBundle, generationOptions);
            case ARTICLE_RELATIONSHIP -> createArticlePost(user, profile, topic, contentIndex, aiBundle, generationOptions);
            case VOTE_DATING -> createVotePost(user, profile, topic, contentIndex, aiBundle, generationOptions);
            case VIDEO_MONOLOGUE -> createVideoPost(user, profile, topic, contentIndex, aiBundle, generationOptions);
        };
    }

    private ContentCreationResult createImagePost(AppUserEntity user,
                                                  RobotProfile profile,
                                                  TopicEntity topic,
                                                  ContentLane lane,
                                                  int contentIndex,
                                                  SeedContentBundle aiBundle,
                                                  GenerationOptions generationOptions) {
        List<String> media = resolveImageMedia(user, profile, lane, contentIndex, generationOptions);
        if ((generationOptions.strictModelContent() || requireFreshSceneMedia()) && media.isEmpty()) {
            throw new IllegalStateException("模型图文生成成功，但缺少可用媒体");
        }
        PostEntity post = buildBasePost(user, topic, 1);
        post.setTitle(trim(aiBundle.title(), 28));
        post.setContent(trim(aiBundle.content(), 220));
        post.setMedia(JSON.toJSONString(media));
        postService.save(post);
        return ContentCreationResult.post(post.getId(), lane);
    }

    private ContentCreationResult createArticlePost(AppUserEntity user,
                                                    RobotProfile profile,
                                                    TopicEntity topic,
                                                    int contentIndex,
                                                    SeedContentBundle aiBundle,
                                                    GenerationOptions generationOptions) {
        List<String> media = resolveArticleMedia(user, profile, contentIndex, generationOptions);
        if ((generationOptions.strictModelContent() || requireFreshSceneMedia()) && media.isEmpty()) {
            throw new IllegalStateException("模型长文生成成功，但缺少可用封面");
        }
        PostEntity post = buildBasePost(user, topic, 3);
        post.setTitle(trim(aiBundle.title(), 28));
        post.setContent(aiBundle.content());
        post.setMedia(JSON.toJSONString(media));
        postService.save(post);
        return ContentCreationResult.post(post.getId(), ContentLane.ARTICLE_RELATIONSHIP);
    }

    private ContentCreationResult createVideoPost(AppUserEntity user,
                                                  RobotProfile profile,
                                                  TopicEntity topic,
                                                  int contentIndex,
                                                  SeedContentBundle aiBundle,
                                                  GenerationOptions generationOptions) {
        ContentCreationResult aiVideoResult = tryCreateAiVideoPost(user, profile, topic, contentIndex, aiBundle, generationOptions);
        if (aiVideoResult != null) {
            return aiVideoResult;
        }
        throw new IllegalStateException("模型视频生成失败，已停止，未再回退旧视频素材");
    }

    private ContentCreationResult createVotePost(AppUserEntity user,
                                                 RobotProfile profile,
                                                 TopicEntity topic,
                                                 int contentIndex,
                                                 SeedContentBundle aiBundle,
                                                 GenerationOptions generationOptions) {
        VoteSubjectEntity voteSubject = new VoteSubjectEntity();
        voteSubject.setCreateTime(new Date());
        String voteTitle = trim(aiBundle.voteTitle(), 28);
        voteSubject.setTitle(voteTitle);
        voteSubject.setType(RandomUtil.randomBoolean() ? 1 : 2);
        voteSubject.setExpireTime(DateUtil.offsetDay(new Date(), RandomUtil.randomInt(7, 31)));
        voteSubjectService.save(voteSubject);

        List<String> options = normalizeVoteOptions(aiBundle.voteOptions(), List.of());
        if (generationOptions.strictModelContent() && options.size() < 4) {
            throw new IllegalStateException("模型投票选项不足 4 个");
        }
        for (String option : options) {
            VoteOptionEntity entity = new VoteOptionEntity();
            entity.setVoteId(voteSubject.getId());
            entity.setContent(option);
            entity.setTicketNum(RandomUtil.randomInt(0, 18));
            voteOptionService.save(entity);
        }

        PostEntity post = buildBasePost(user, topic, 4);
        post.setVoteId(voteSubject.getId());
        post.setTitle(null);
        post.setContent(trim(aiBundle.content(), 220));
        List<String> media = resolveVoteMedia(user, profile, contentIndex, generationOptions);
        if ((generationOptions.strictModelContent() || requireFreshSceneMedia()) && media.isEmpty()) {
            throw new IllegalStateException("模型投票生成成功，但缺少可用封面");
        }
        post.setMedia(JSON.toJSONString(media));
        postService.save(post);
        return ContentCreationResult.post(post.getId(), ContentLane.VOTE_DATING);
    }

    private void ensureModelGeneratedBundle(SeedContentBundle aiBundle,
                                            ContentLane lane,
                                            RobotProfile profile,
                                            GenerationOptions generationOptions) {
        if (!generationOptions.strictModelContent()) {
            return;
        }
        if (aiBundle == null) {
            throw new IllegalStateException("模型未返回内容");
        }
        if (StringUtils.isBlank(aiBundle.title()) || StringUtils.isBlank(aiBundle.content())) {
            throw new IllegalStateException("模型内容不完整，用户=" + profile.seed() + "，类型=" + lane.name());
        }
        if (lane == ContentLane.VOTE_DATING) {
            if (StringUtils.isBlank(aiBundle.voteTitle()) || aiBundle.voteOptions() == null || aiBundle.voteOptions().size() < 4) {
                throw new IllegalStateException("模型投票内容不完整，用户=" + profile.seed());
            }
        }
    }

    private List<String> resolveImageMedia(AppUserEntity user,
                                           RobotProfile profile,
                                           ContentLane lane,
                                           int contentIndex,
                                           GenerationOptions generationOptions) {
        List<String> generated = generateAiSceneMedia(user, profile, lane, contentIndex, 3, generationOptions);
        if (!generated.isEmpty()) {
            return generated;
        }
        if (requireFreshSceneMedia()) {
            return List.of();
        }
        return resolveReferenceMediaFallback(user, 3);
    }

    private List<String> resolveArticleMedia(AppUserEntity user,
                                             RobotProfile profile,
                                             int contentIndex,
                                             GenerationOptions generationOptions) {
        List<String> generated = generateAiSceneMedia(user, profile, ContentLane.ARTICLE_RELATIONSHIP, contentIndex, 1, generationOptions);
        if (!generated.isEmpty()) {
            return generated;
        }
        if (requireFreshSceneMedia()) {
            return List.of();
        }
        return resolveReferenceMediaFallback(user, 1);
    }

    private List<String> resolveVoteMedia(AppUserEntity user,
                                          RobotProfile profile,
                                          int contentIndex,
                                          GenerationOptions generationOptions) {
        List<String> generated = generateAiSceneMedia(user, profile, ContentLane.VOTE_DATING, contentIndex, 1, generationOptions);
        if (!generated.isEmpty()) {
            return generated;
        }
        if (requireFreshSceneMedia()) {
            return List.of();
        }
        return resolveReferenceMediaFallback(user, 1);
    }

    private boolean requireFreshSceneMedia() {
        return Boolean.getBoolean("robot.seed.requireFreshSceneMedia");
    }

    private List<String> resolveReferenceMediaFallback(AppUserEntity user, int desiredCount) {
        List<String> referenceMedia = collectReferenceMedia(user);
        if (referenceMedia.isEmpty()) {
            return List.of();
        }
        return referenceMedia.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .limit(Math.max(1, desiredCount))
                .collect(Collectors.toList());
    }

    private ContentCreationResult tryCreateAiVideoPost(AppUserEntity user,
                                                       RobotProfile profile,
                                                       TopicEntity topic,
                                                       int contentIndex,
                                                       SeedContentBundle aiBundle,
                                                       GenerationOptions generationOptions) {
        if (!generationOptions.aiEnabled()) {
            return null;
        }
        List<String> referenceMedia = collectReferenceMedia(user);
        if (referenceMedia.isEmpty()) {
            return null;
        }
        try {
            GenerateVideoForm form = new GenerateVideoForm();
            form.setTemplateCode(pickVideoTemplateCode(profile, contentIndex));
            form.setCustomPrompt(buildVideoGenerationPrompt(profile, contentIndex, aiBundle));
            form.setPostContent(trim(firstNonBlank(aiBundle.content(), buildVideoCaption(profile, contentIndex)), 220));
            form.setAutoPublish(1);
            form.setMedia(referenceMedia);
            var generateResult = userVideoService.generateVideo(user.getUid(), form);
            if (generateResult == null || generateResult.getVideoId() == null) {
                return null;
            }
            UserVideoEntity videoEntity = userVideoService.getById(generateResult.getVideoId());
            Integer postId = videoEntity != null ? videoEntity.getPostId() : null;
            return new ContentCreationResult(postId, generateResult.getVideoId(), ContentLane.VIDEO_MONOLOGUE, "video_generating");
        } catch (Exception ex) {
            log.warn("[robot-seed] ai video generation fallback, uid={}, reason={}", user.getUid(), ex.getMessage());
            return null;
        }
    }

    private List<String> generateAiSceneMedia(AppUserEntity user,
                                              RobotProfile profile,
                                              ContentLane lane,
                                              int contentIndex,
                                              int desiredCount,
                                              GenerationOptions generationOptions) {
        if (!generationOptions.aiEnabled() || imageStrategyManager == null || Boolean.getBoolean("robot.seed.skipAiSceneMedia")) {
            return List.of();
        }
        List<String> referenceMedia = collectReferenceMedia(user);
        if (referenceMedia.isEmpty()) {
            return List.of();
        }
        String prompt = buildSceneImagePrompt(profile, lane, contentIndex);
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("function_type", "robot_seed_scene_image");
            params.put("scene_code", "robot_seed_" + lane.name().toLowerCase(Locale.ROOT));
            params.put("size", "2K");
            params.put("response_format", "url");
            params.put("watermark", false);
            params.put("max_images", Math.max(1, desiredCount));
            params.put("sequential_image_generation", "auto");
            List<String> generatedUrls = imageStrategyManager.generateImages(prompt, referenceMedia, params);
            return persistGeneratedImages(generatedUrls, desiredCount);
        } catch (Exception ex) {
            log.warn("[robot-seed] scene image generation fallback, uid={}, lane={}, reason={}",
                    user.getUid(),
                    lane,
                    ex.getMessage());
            return List.of();
        }
    }

    private List<String> collectReferenceMedia(AppUserEntity user) {
        LinkedHashSet<String> media = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(user.getAvatar()) && StringUtils.startsWithIgnoreCase(StringUtils.trim(user.getAvatar()), "http")) {
            media.add(StringUtils.trim(user.getAvatar()));
        }
        if (StringUtils.isNotBlank(user.getFigur())) {
            for (String item : StringUtils.split(user.getFigur(), ",")) {
                if (StringUtils.isNotBlank(item) && StringUtils.startsWithIgnoreCase(StringUtils.trim(item), "http")) {
                    media.add(StringUtils.trim(item));
                }
                if (media.size() >= 3) {
                    break;
                }
            }
        }
        return new ArrayList<>(media);
    }

    private List<String> collectAvatarOnlyReferenceMedia(AppUserEntity user) {
        if (user == null) {
            return List.of();
        }
        String avatar = StringUtils.trimToEmpty(user.getAvatar());
        if (StringUtils.isBlank(avatar) || !StringUtils.startsWithIgnoreCase(avatar, "http")) {
            return List.of();
        }
        return List.of(avatar);
    }

    private List<String> generateAiSceneMediaByReference(AppUserEntity user,
                                                         List<String> referenceMedia,
                                                         RobotProfile profile,
                                                         ContentLane lane,
                                                         int contentIndex,
                                                         int desiredCount,
                                                         GenerationOptions generationOptions) {
        if (!generationOptions.aiEnabled() || imageStrategyManager == null || Boolean.getBoolean("robot.seed.skipAiSceneMedia")
                || referenceMedia == null || referenceMedia.isEmpty()) {
            return List.of();
        }
        String prompt = buildSceneImagePrompt(profile, lane, contentIndex);
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("function_type", "robot_seed_scene_image");
            params.put("scene_code", "robot_seed_" + lane.name().toLowerCase(Locale.ROOT));
            params.put("size", "2K");
            params.put("response_format", "url");
            params.put("watermark", false);
            params.put("max_images", Math.max(1, desiredCount));
            params.put("sequential_image_generation", "auto");
            List<String> generatedUrls = imageStrategyManager.generateImages(prompt, referenceMedia, params);
            return persistGeneratedImages(generatedUrls, desiredCount);
        } catch (Exception ex) {
            log.warn("[robot-seed] scene image generation failed. uid={}, lane={}, reason={}", user.getUid(), lane, ex.getMessage());
            return List.of();
        }
    }

    private List<String> generateLocalSceneMediaByReference(AppUserEntity user,
                                                            List<String> referenceMedia,
                                                            RobotProfile profile,
                                                            ContentLane lane,
                                                            int contentIndex,
                                                            int desiredCount) {
        if (referenceMedia == null || referenceMedia.isEmpty()) {
            return List.of();
        }
        try {
            BufferedImage avatar = downloadReferenceImage(referenceMedia.get(0));
            if (avatar == null) {
                return List.of();
            }
            RuoyiSysClound cloudStorage = new RuoyiSysClound();
            List<String> uploaded = new ArrayList<>();
            for (int i = 0; i < Math.max(1, desiredCount); i++) {
                BufferedImage variant = buildLocalFigureVariant(avatar, profile, lane, contentIndex + i);
                String url = uploadBufferedImageToCloud(variant, cloudStorage);
                if (StringUtils.isNotBlank(url)) {
                    uploaded.add(url);
                }
            }
            return uploaded;
        } catch (Exception ex) {
            log.warn("[robot-seed] local figure generation failed. uid={}, lane={}, reason={}", user.getUid(), lane, ex.getMessage());
            return List.of();
        }
    }

    private String buildSceneImagePrompt(RobotProfile profile, ContentLane lane, int contentIndex) {
        String person = profile.gender() == 2 ? "亚洲女性" : "亚洲男性";
        String scene = switch (lane) {
            case IMAGE_SELF -> "下班后的真实生活切片，带一点" + pickInterestFocus(profile, contentIndex) + "场景";
            case IMAGE_SHOWCASE -> "生活化出片感，突出真实气质、身材状态和镜头感";
            case IMAGE_COUPLE -> "带约会氛围的单人生活照，突出恋爱感和松弛感";
            case IMAGE_ACTIVITY -> "线下社交活动现场感，主角在人群或活动空间里依然清晰";
            case ARTICLE_RELATIONSHIP -> "安静、真实、有思考感的生活场景封面";
            case VOTE_DATING -> "适合投票动态封面的清爽生活场景";
            case VIDEO_MONOLOGUE -> "口播视频封面质感，真人近景";
        };
        String flavorPack = buildPromptFlavorPack(profile, lane, contentIndex);
        String personaPack = buildFigurePersonaPack(profile, lane, contentIndex);
        String shotDirective = buildFigureShotDirective(profile, lane, contentIndex);
        return "参考图中的同一个人，保持五官和脸型一致。"
                + person + "，" + profile.age() + "岁，常住" + profile.city() + "，职业是" + profile.job() + "。"
                + "人物画像：" + personaPack + "。"
                + "场景要求：" + scene + "。"
                + "镜头指令：" + shotDirective + "。"
                + "随机风格包：" + flavorPack + "。"
                + "人物状态要符合" + String.join("、", profile.interests()) + "的生活方式，"
                + "整体风格为真实摄影、自然光、生活化、亚洲面孔、无文字、无水印、不要多人脸重复。"
                + "不要复用常见社交平台模板构图，不要统一姿势，不要像同一组棚拍。"
                + "每个用户都必须根据自己的城市、职业、兴趣、性格和关系状态生成不同照片。"
                + "同一个用户如果生成多张，也要切换场景入口、服装层次、动作重心和表情，不要像同一组连拍模板。";
    }

    private BufferedImage downloadReferenceImage(String imageUrl) {
        String normalizedUrl = StringUtils.trimToEmpty(imageUrl);
        if (StringUtils.isBlank(normalizedUrl)) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Referer", "https://image.baidu.com/")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = imageDownloadClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("download failed: http " + response.statusCode());
            }
            try (InputStream inputStream = response.body()) {
                return ImageIO.read(inputStream);
            }
        } catch (Exception ex) {
            log.warn("[robot-seed] download reference image failed: {}", ex.getMessage());
            return null;
        }
    }

    private BufferedImage buildLocalFigureVariant(BufferedImage avatar,
                                                  RobotProfile profile,
                                                  ContentLane lane,
                                                  int contentIndex) {
        int width = LOCAL_FIGURE_WIDTH;
        int height = LOCAL_FIGURE_HEIGHT;
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            String sceneStyle = pickLocalSceneStyle(profile, lane, contentIndex);
            Color[] palette = pickLocalPalette(profile, sceneStyle, contentIndex);
            drawBlurredAvatarBackground(g2d, avatar, width, height, palette, contentIndex);
            drawLocalSceneBackdrop(g2d, profile, lane, sceneStyle, palette, width, height, contentIndex);
            drawPortraitComposition(g2d, avatar, profile, lane, palette, width, height, contentIndex);
            drawForegroundAccents(g2d, profile, lane, palette, width, height, contentIndex);
        } finally {
            g2d.dispose();
        }
        return canvas;
    }

    private void drawBlurredAvatarBackground(Graphics2D g2d,
                                             BufferedImage avatar,
                                             int width,
                                             int height,
                                             Color[] palette,
                                             int contentIndex) {
        BufferedImage blurred = blurImage(avatar, Math.max(48, variant(contentIndex, 3) % 72 + 48));
        drawCoverImage(g2d, blurred, 0, 0, width, height, 0.52 + (variant(contentIndex, 5) % 12) * 0.015, 0.5, 0.52);
        Paint gradient = new LinearGradientPaint(
                new Point2D.Float(0, 0),
                new Point2D.Float(width, height),
                new float[]{0f, 0.5f, 1f},
                new Color[]{
                        withAlpha(palette[0], 210),
                        withAlpha(palette[1], 170),
                        withAlpha(palette[2], 220)
                }
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);
    }

    private void drawLocalSceneBackdrop(Graphics2D g2d,
                                        RobotProfile profile,
                                        ContentLane lane,
                                        String sceneStyle,
                                        Color[] palette,
                                        int width,
                                        int height,
                                        int contentIndex) {
        if ("outdoor".equals(sceneStyle)) {
            drawOutdoorBackdrop(g2d, palette, width, height, contentIndex);
        } else if ("cafe".equals(sceneStyle)) {
            drawCafeBackdrop(g2d, palette, width, height, contentIndex);
        } else if ("event".equals(sceneStyle)) {
            drawEventBackdrop(g2d, palette, width, height, contentIndex);
        } else if ("night".equals(sceneStyle)) {
            drawNightBackdrop(g2d, palette, width, height, contentIndex);
        } else {
            drawUrbanBackdrop(g2d, palette, width, height, contentIndex);
        }
        if (lane == ContentLane.IMAGE_COUPLE) {
            g2d.setColor(withAlpha(Color.WHITE, 36));
            g2d.fill(new Ellipse2D.Float(width * 0.18f, height * 0.78f, width * 0.64f, height * 0.1f));
        }
        if (lane == ContentLane.IMAGE_ACTIVITY) {
            g2d.setColor(withAlpha(Color.WHITE, 30));
            g2d.fill(new RoundRectangle2D.Float(width * 0.08f, height * 0.12f, width * 0.84f, height * 0.72f, 48, 48));
        }
    }

    private void drawUrbanBackdrop(Graphics2D g2d, Color[] palette, int width, int height, int contentIndex) {
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(withAlpha(Color.WHITE, 48));
        for (int i = 0; i < 5; i++) {
            float x = width * (0.1f + i * 0.17f);
            g2d.draw(new java.awt.geom.Line2D.Float(x, height * 0.1f, x, height * 0.92f));
        }
        g2d.setColor(withAlpha(palette[2], 88));
        for (int i = 0; i < 6; i++) {
            int barWidth = 70 + Math.floorMod(contentIndex + i * 11, 90);
            int barHeight = 160 + Math.floorMod(contentIndex * 17 + i * 37, 260);
            int x = 40 + i * 170;
            int y = height - barHeight - 40;
            g2d.fill(new RoundRectangle2D.Float(x, y, barWidth, barHeight, 24, 24));
        }
    }

    private void drawCafeBackdrop(Graphics2D g2d, Color[] palette, int width, int height, int contentIndex) {
        g2d.setColor(withAlpha(Color.WHITE, 42));
        for (int i = 0; i < 7; i++) {
            float size = 80 + Math.floorMod(contentIndex * 13 + i * 19, 120);
            float x = Math.floorMod(contentIndex * 37 + i * 131, width - 120);
            float y = Math.floorMod(contentIndex * 23 + i * 89, height - 160);
            g2d.fill(new Ellipse2D.Float(x, y, size, size));
        }
        g2d.setColor(withAlpha(palette[2], 96));
        g2d.fill(new RoundRectangle2D.Float(width * 0.12f, height * 0.72f, width * 0.76f, height * 0.2f, 60, 60));
    }

    private void drawOutdoorBackdrop(Graphics2D g2d, Color[] palette, int width, int height, int contentIndex) {
        Path2D.Float hill1 = new Path2D.Float();
        hill1.moveTo(0, height * 0.78);
        hill1.curveTo(width * 0.2, height * 0.62, width * 0.48, height * 0.84, width, height * 0.68);
        hill1.lineTo(width, height);
        hill1.lineTo(0, height);
        hill1.closePath();
        g2d.setColor(withAlpha(palette[1], 165));
        g2d.fill(hill1);

        Path2D.Float hill2 = new Path2D.Float();
        hill2.moveTo(0, height * 0.86);
        hill2.curveTo(width * 0.28, height * 0.74, width * 0.62, height * 0.92, width, height * 0.8);
        hill2.lineTo(width, height);
        hill2.lineTo(0, height);
        hill2.closePath();
        g2d.setColor(withAlpha(palette[2], 210));
        g2d.fill(hill2);

        g2d.setColor(withAlpha(Color.WHITE, 58));
        float sunSize = 110 + Math.floorMod(contentIndex * 9, 40);
        g2d.fill(new Ellipse2D.Float(width * 0.12f, height * 0.12f, sunSize, sunSize));
    }

    private void drawNightBackdrop(Graphics2D g2d, Color[] palette, int width, int height, int contentIndex) {
        g2d.setColor(withAlpha(Color.WHITE, 20));
        for (int i = 0; i < 22; i++) {
            float size = 8 + Math.floorMod(contentIndex * 7 + i * 5, 14);
            float x = Math.floorMod(contentIndex * 53 + i * 71, width - 30);
            float y = Math.floorMod(contentIndex * 17 + i * 97, Math.max(120, height - 180));
            g2d.fill(new Ellipse2D.Float(x, y, size, size));
        }
        g2d.setColor(withAlpha(palette[1], 120));
        g2d.fill(new RoundRectangle2D.Float(width * 0.1f, height * 0.8f, width * 0.8f, height * 0.09f, 50, 50));
    }

    private void drawEventBackdrop(Graphics2D g2d, Color[] palette, int width, int height, int contentIndex) {
        g2d.setColor(withAlpha(Color.WHITE, 20));
        for (int i = 0; i < 11; i++) {
            float x = width * (0.08f + i * 0.08f);
            g2d.fill(new RoundRectangle2D.Float(x, height * 0.1f, 16, height * 0.72f, 16, 16));
        }
        g2d.setColor(withAlpha(palette[2], 140));
        for (int i = 0; i < 8; i++) {
            float radius = 48 + Math.floorMod(contentIndex * 17 + i * 29, 42);
            float x = Math.floorMod(contentIndex * 61 + i * 127, width - 80);
            float y = height * (0.14f + (i % 3) * 0.16f);
            g2d.fill(new Ellipse2D.Float(x, y, radius, radius));
        }
    }

    private void drawPortraitComposition(Graphics2D g2d,
                                         BufferedImage avatar,
                                         RobotProfile profile,
                                         ContentLane lane,
                                         Color[] palette,
                                         int width,
                                         int height,
                                         int contentIndex) {
        int layout = Math.floorMod(variant(profile, contentIndex, 84), 3);
        float frameX = layout == 0 ? width * 0.18f : layout == 1 ? width * 0.1f : width * 0.28f;
        float frameY = layout == 2 ? height * 0.1f : height * 0.14f;
        float frameW = layout == 1 ? width * 0.8f : width * 0.54f;
        float frameH = layout == 1 ? height * 0.72f : height * 0.74f;

        RoundRectangle2D.Float shadow = new RoundRectangle2D.Float(frameX + 18, frameY + 26, frameW, frameH, 54, 54);
        g2d.setColor(withAlpha(Color.BLACK, 52));
        g2d.fill(shadow);

        RoundRectangle2D.Float frame = new RoundRectangle2D.Float(frameX, frameY, frameW, frameH, 54, 54);
        g2d.setColor(withAlpha(Color.WHITE, 228));
        g2d.fill(frame);

        Graphics2D portraitG = (Graphics2D) g2d.create();
        portraitG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        portraitG.setClip(frame);
        double scale = 1.12 + (Math.floorMod(variant(profile, contentIndex, 85), 18) * 0.01);
        double anchorX = 0.5 + (Math.floorMod(variant(profile, contentIndex, 86), 7) - 3) * 0.03;
        double anchorY = lane == ContentLane.IMAGE_SHOWCASE ? 0.44 : 0.38 + (Math.floorMod(variant(profile, contentIndex, 87), 7) - 3) * 0.02;
        drawCoverImage(portraitG, avatar, frameX, frameY, frameW, frameH, scale, anchorX, anchorY);
        portraitG.setComposite(AlphaComposite.SrcOver.derive(0.16f));
        portraitG.setPaint(new GradientPaint(0, frameY, withAlpha(palette[0], 120), 0, frameY + frameH, withAlpha(palette[2], 190)));
        portraitG.fill(frame);
        portraitG.dispose();

        g2d.setStroke(new BasicStroke(4f));
        g2d.setColor(withAlpha(Color.WHITE, 160));
        g2d.draw(frame);

        Ellipse2D.Float badge = new Ellipse2D.Float(frameX + frameW - 118, frameY + frameH - 118, 84, 84);
        g2d.setColor(withAlpha(palette[0], 195));
        g2d.fill(badge);
        g2d.setColor(withAlpha(Color.WHITE, 110));
        g2d.fill(new Ellipse2D.Float(frameX + frameW - 88, frameY + frameH - 88, 24, 24));
    }

    private void drawForegroundAccents(Graphics2D g2d,
                                       RobotProfile profile,
                                       ContentLane lane,
                                       Color[] palette,
                                       int width,
                                       int height,
                                       int contentIndex) {
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.85f));
        g2d.setColor(withAlpha(Color.WHITE, 34));
        for (int i = 0; i < 3; i++) {
            float size = 220 + Math.floorMod(variant(profile, contentIndex + i, 91), 90);
            float x = Math.floorMod(variant(profile, contentIndex + i, 92), width - 200);
            float y = Math.floorMod(variant(profile, contentIndex + i, 93), height - 280);
            g2d.fill(new Ellipse2D.Float(x, y, size, size));
        }
        if (lane == ContentLane.IMAGE_ACTIVITY || lane == ContentLane.IMAGE_COUPLE) {
            g2d.setColor(withAlpha(palette[1], 120));
            g2d.setStroke(new BasicStroke(10f));
            g2d.draw(new java.awt.geom.Arc2D.Float(width * 0.14f, height * 0.68f, width * 0.62f, height * 0.22f, 200, 105, java.awt.geom.Arc2D.OPEN));
        }
        g2d.setColor(withAlpha(Color.BLACK, 42));
        g2d.fill(new RoundRectangle2D.Float(width * 0.08f, height * 0.9f, width * 0.84f, height * 0.045f, 42, 42));
    }

    private Color[] pickLocalPalette(RobotProfile profile, String sceneStyle, int contentIndex) {
        int seed = variant(profile, contentIndex, 94);
        if ("outdoor".equals(sceneStyle)) {
            return profile.gender() == 2
                    ? new Color[]{new Color(120, 178, 170), new Color(214, 234, 205), new Color(66, 108, 104)}
                    : new Color[]{new Color(73, 134, 122), new Color(199, 221, 186), new Color(38, 72, 68)};
        }
        if ("cafe".equals(sceneStyle)) {
            return profile.gender() == 2
                    ? new Color[]{new Color(185, 138, 108), new Color(244, 223, 201), new Color(118, 82, 63)}
                    : new Color[]{new Color(136, 94, 68), new Color(223, 198, 168), new Color(87, 58, 43)};
        }
        if ("event".equals(sceneStyle)) {
            return new Color[]{new Color(91, 118, 170), new Color(196, 213, 237), new Color(43, 63, 99)};
        }
        if ("night".equals(sceneStyle)) {
            return new Color[]{new Color(109, 92, 164), new Color(222, 194, 163), new Color(40, 32, 64)};
        }
        if (Math.floorMod(seed, 2) == 0) {
            return new Color[]{new Color(84, 118, 146), new Color(214, 226, 235), new Color(42, 59, 76)};
        }
        return new Color[]{new Color(86, 109, 133), new Color(206, 214, 224), new Color(54, 69, 85)};
    }

    private String pickLocalSceneStyle(RobotProfile profile, ContentLane lane, int contentIndex) {
        if (lane == ContentLane.IMAGE_ACTIVITY) {
            return "event";
        }
        if (lane == ContentLane.IMAGE_COUPLE) {
            return "night";
        }
        String focus = pickInterestFocus(profile, contentIndex);
        if (StringUtils.containsAny(focus, "咖啡", "手冲咖啡", "烘焙", "做饭", "电影", "看展", "逛书店")) {
            return "cafe";
        }
        if (StringUtils.containsAny(focus, "徒步", "爬山", "露营", "骑行", "跑步", "游泳", "滑雪", "桨板", "皮划艇")) {
            return "outdoor";
        }
        if (StringUtils.containsAny(profile.job(), "工程师", "律师", "产品", "金融", "外贸", "设计")) {
            return "urban";
        }
        return lane == ContentLane.IMAGE_SHOWCASE ? "cafe" : "urban";
    }

    private BufferedImage blurImage(BufferedImage source, int divisor) {
        int reducedW = Math.max(18, source.getWidth() / Math.max(12, divisor / 6));
        int reducedH = Math.max(18, source.getHeight() / Math.max(12, divisor / 6));
        BufferedImage downscaled = new BufferedImage(reducedW, reducedH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = downscaled.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(source, 0, 0, reducedW, reducedH, null);
        } finally {
            g2d.dispose();
        }
        BufferedImage upscaled = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D up = upscaled.createGraphics();
        try {
            up.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            up.drawImage(downscaled, 0, 0, source.getWidth(), source.getHeight(), null);
        } finally {
            up.dispose();
        }
        return upscaled;
    }

    private void drawCoverImage(Graphics2D g2d,
                                BufferedImage image,
                                double x,
                                double y,
                                double width,
                                double height,
                                double scale,
                                double anchorX,
                                double anchorY) {
        if (image == null) {
            return;
        }
        double safeScale = Math.max(1.0, scale);
        double drawWidth = width * safeScale;
        double drawHeight = drawWidth * image.getHeight() / Math.max(1.0, image.getWidth());
        if (drawHeight < height * safeScale) {
            drawHeight = height * safeScale;
            drawWidth = drawHeight * image.getWidth() / Math.max(1.0, image.getHeight());
        }
        double offsetX = x + (width - drawWidth) * Math.max(0, Math.min(1, anchorX));
        double offsetY = y + (height - drawHeight) * Math.max(0, Math.min(1, anchorY));
        g2d.drawImage(image, (int) Math.round(offsetX), (int) Math.round(offsetY), (int) Math.round(drawWidth), (int) Math.round(drawHeight), null);
    }

    private String uploadBufferedImageToCloud(BufferedImage image, RuoyiSysClound cloudStorage) {
        if (image == null || cloudStorage == null) {
            return "";
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            try (InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                return cloudStorage.uploadSuffix(inputStream, "png");
            }
        } catch (Exception ex) {
            log.warn("[robot-seed] upload local figure failed: {}", ex.getMessage());
            return "";
        }
    }

    private Color withAlpha(Color color, int alpha) {
        Color base = color == null ? Color.WHITE : color;
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private int variant(int seed, int salt) {
        return Math.floorMod(seed * 31 + salt * 17, Integer.MAX_VALUE);
    }

    private String pickVideoTemplateCode(RobotProfile profile, int contentIndex) {
        List<String> templates = new ArrayList<>(List.of(
                "self_intro_fresh",
                "self_intro_youth",
                "self_intro_elegant",
                "dating_card_romantic",
                "dating_invite_romantic",
                "dating_dinner",
                "daily_mood"
        ));
        if (StringUtils.containsAny(String.join(" ", profile.interests()), "健身", "撸铁", "瑜伽", "普拉提", "跑步", "游泳")) {
            templates.add(0, "daily_mood");
        }
        if (StringUtils.containsAny(profile.relationshipGoal(), "长期关系", "认真交往", "稳定")) {
            templates.add(0, "dating_card_romantic");
        }
        return templates.get(Math.floorMod(profile.seed() + contentIndex, templates.size()));
    }

    private String buildVideoGenerationPrompt(RobotProfile profile, int contentIndex, SeedContentBundle aiBundle) {
        String flavorPack = buildPromptFlavorPack(profile, ContentLane.VIDEO_MONOLOGUE, contentIndex);
        return "请参考上传的同一个人头像和生活照，生成婚恋社交平台用的真人竖屏视频。"
                + "人物必须保持亚洲面孔、脸型一致、气质一致，不要变成欧美脸，不要卡通，不要多人抢镜。"
                + "主角是" + (profile.gender() == 2 ? "亚洲女性" : "亚洲男性")
                + "，" + profile.age() + "岁，常住" + profile.city()
                + "，职业是" + profile.job() + "。"
                + "视频主题：" + firstNonBlank(aiBundle.title(), buildVideoTitle(profile, contentIndex)) + "。"
                + "镜头方向：" + pickVideoDirection(profile, contentIndex) + "，" + buildLifeScene(profile, ContentLane.VIDEO_MONOLOGUE, contentIndex) + "。"
                + "随机风格包：" + flavorPack + "。"
                + "文案核心：" + firstNonBlank(aiBundle.content(), buildVideoCaption(profile, contentIndex)) + "。"
                + "画面要求：9:16竖屏、真人写实、自然光、轻电影感、生活场景真实、可做相亲自我介绍短视频。"
                + "每次都换一个生活场景入口和开场方式，不要做成同一套口播模板。";
    }

    private List<String> persistGeneratedImages(List<String> generatedUrls, int desiredCount) {
        if (generatedUrls == null || generatedUrls.isEmpty()) {
            return List.of();
        }
        List<String> persisted = new ArrayList<>();
        for (String url : generatedUrls) {
            String saved = persistGeneratedImage(url);
            if (StringUtils.isNotBlank(saved)) {
                persisted.add(saved);
            }
            if (persisted.size() >= desiredCount) {
                break;
            }
        }
        return persisted;
    }

    private String persistGeneratedImage(String imageUrl) {
        String normalizedUrl = StringUtils.trimToEmpty(imageUrl);
        if (StringUtils.isBlank(normalizedUrl)) {
            return "";
        }
        log.info("[robot-seed] keep generated image source url directly: {}", normalizedUrl);
        return normalizedUrl;
    }

    private PostEntity buildBasePost(AppUserEntity user, TopicEntity topic, int type) {
        PostEntity post = new PostEntity();
        post.setUid(user.getUid());
        post.setTopicId(topic != null ? topic.getId() : Constant.OFFICIAL_TOPIC_ID);
        post.setType(type);
        post.setStatus(Constant.POST_NORMAL);
        post.setCut(0);
        post.setPay(BigDecimal.ZERO);
        post.setPostTop(0);
        post.setReadCount(RandomUtil.randomInt(18, 380));
        post.setIsPrivate(topic != null ? topic.getIsPrivacy() : 0);
        post.setCreateTime(randomRecentDate(20));
        return post;
    }

    private ContentLane pickContentLane(RobotProfile profile,
                                        boolean allowRichMix,
                                        int contentIndex,
                                        Set<ContentLane> usedLanes,
                                        GenerationOptions generationOptions) {
        List<ContentLane> lanePool = new ArrayList<>(List.of(
                ContentLane.IMAGE_SELF,
                ContentLane.IMAGE_SHOWCASE,
                ContentLane.IMAGE_COUPLE,
                ContentLane.IMAGE_ACTIVITY,
                ContentLane.ARTICLE_RELATIONSHIP,
                ContentLane.VOTE_DATING
        ));
        if (allowRichMix && generationOptions != null && generationOptions.aiEnabled()) {
            lanePool.add(ContentLane.VIDEO_MONOLOGUE);
        }
        if (usedLanes != null && usedLanes.size() < lanePool.size()) {
            lanePool.removeIf(usedLanes::contains);
        }
        long sequence = CONTENT_SEQUENCE.incrementAndGet();
        int index = Math.floorMod((int) (profile.seed() + sequence + contentIndex * 3L), lanePool.size());
        ContentLane lane = lanePool.get(index);
        if (usedLanes != null) {
            usedLanes.add(lane);
        }
        return lane;
    }

    private String buildImageTitle(RobotProfile profile, ContentLane lane, int contentIndex) {
        String highlight = pickSceneHighlight(profile, contentIndex);
        String interestFocus = pickInterestFocus(profile, contentIndex);
        int style = variant(profile, contentIndex, lane.ordinal() + 11) % 6;
        return switch (lane) {
            case IMAGE_SELF -> trim(switch (style) {
                case 0 -> "下班后" + renderInterestAction(interestFocus) + "，顺手留一张";
                case 1 -> highlight;
                case 2 -> "这张挺像我最近的状态";
                case 3 -> profile.city() + "这几天的生活切片";
                case 4 -> "没有营业感的一条近况";
                default -> "把最近的自己发出来";
            }, 28);
            case IMAGE_SHOWCASE -> trim(switch (style) {
                case 0 -> "认真出片，也认真生活";
                case 1 -> "这组比资料卡更像我";
                case 2 -> "最近喜欢这种" + profile.aestheticStyle() + "的状态";
                case 3 -> "今天这组保留了真实感";
                case 4 -> "不是摆拍，是我平时会有的样子";
                default -> "顺手把状态最好的这一面发出来";
            }, 28);
            case IMAGE_COUPLE -> trim(switch (style) {
                case 0 -> "我想谈的恋爱，应该有这种日常";
                case 1 -> "如果两个人相处舒服，日子会很好看";
                case 2 -> "比心动更重要的，是把日子过顺";
                case 3 -> "我期待的关系，不用很吵也会有温度";
                case 4 -> "想要的不是热闹，是稳定靠近";
                default -> "我理想里的两个人状态";
            }, 28);
            case IMAGE_ACTIVITY -> trim(switch (style) {
                case 0 -> "今天这场线下局，状态都挺在线";
                case 1 -> "认真社交的人聚在一起，氛围确实不一样";
                case 2 -> "这种活动最打动我的，是分寸感";
                case 3 -> "现场比照片里更自然，也更有说服力";
                case 4 -> "不是硬社交，是能慢慢聊起来的那种局";
                default -> "今晚这场活动，氛围和人都挺加分";
            }, 28);
            default -> profile.city() + "生活记录";
        };
    }

    private String buildImageCaption(RobotProfile profile, ContentLane lane, int contentIndex) {
        String lifestyle = pickLifestyleLabel(profile);
        String workLine = "现在在" + profile.city() + "做" + profile.job() + "，" + buildWorkScene(profile) + "。";
        String selfLine = profile.temperament() + "，" + profile.socialStyle() + "。";
        String rhythmLine = "平时会用" + String.join("、", profile.interests()) + "把生活调顺一点，" + profile.lifeRhythm() + "。";
        String inviteLine = buildSoftInvite(profile, contentIndex);
        return switch (lane) {
            case IMAGE_SELF -> switch (variant(profile, contentIndex, 21) % 4) {
                case 0 -> joinSentences(
                        pickString(SELF_SHARE_OPENERS, profile.seed() + contentIndex),
                        "最近很喜欢" + buildLifeScene(profile, lane, contentIndex) + "这种具体的小瞬间。",
                        rhythmLine,
                        inviteLine
                );
                case 1 -> joinSentences(
                        workLine,
                        "下班以后" + renderInterestAction(pickInterestFocus(profile, contentIndex)) + "，人会一下子松下来。",
                        "这种" + lifestyle + "的状态，比刻意经营更像我本人。",
                        inviteLine
                );
                case 2 -> joinSentences(
                        selfLine,
                        "最近常记录" + buildLifeScene(profile, lane, contentIndex) + "这一面，因为这时候最接近真实生活里的我。",
                        "如果以后认识一个人，我也希望对方看到的是这种没什么滤镜的状态。",
                        inviteLine
                );
                default -> joinSentences(
                        "这条就当近况更新了，没什么包装，就是最近的我。",
                        workLine,
                        rhythmLine,
                        "现在更想认识能接得住真实日常的人。"
                );
            };
            case IMAGE_SHOWCASE -> switch (variant(profile, contentIndex, 22) % 4) {
                case 0 -> joinSentences(
                        pickString(SHOWCASE_OPENERS, profile.seed() + contentIndex),
                        "这组里保留了" + profile.aestheticStyle() + "的感觉，也保留了我平时真实的线条和状态。",
                        workLine,
                        "如果之后认识，希望不是只看资料，也能看到彼此真实的生活质感。"
                );
                case 1 -> joinSentences(
                        "偶尔会认真出片，但不想拍成谁都一样的样板照。",
                        "我更喜欢" + buildLifeScene(profile, lane, contentIndex) + "之后那种松弛、有呼吸感的自己。",
                        "比起精修，我会更在意画面里有没有我自己的气质。",
                        inviteLine
                );
                case 2 -> joinSentences(
                        workLine,
                        "生活里我更像" + lifestyle + "这一挂的人，" + profile.weekendStyle() + "。",
                        "把照片发出来不是为了刷存在感，是想让人先看到我本来的状态。",
                        inviteLine
                );
                default -> joinSentences(
                        selfLine,
                        "我挺喜欢把镜头当成生活记录，不会特意端着，反而更容易拍到像本人的瞬间。",
                        "认真找对象这件事，对我来说也该从真实开始。",
                        inviteLine
                );
            };
            case IMAGE_COUPLE -> switch (variant(profile, contentIndex, 23) % 4) {
                case 0 -> joinSentences(
                        pickString(COUPLE_VISION_LINES, profile.seed() + contentIndex),
                        "对我来说，" + pickRelationshipAngle(profile, contentIndex) + "会比表面热闹更重要。",
                        "更想遇到的是能一起把日子过顺、也能互相照顾情绪的人。"
                );
                case 1 -> joinSentences(
                        "现在越来越确定，关系里最打动我的不是形式感，而是" + profile.familyVision() + "。",
                        "我自己属于" + profile.emotionalStyle() + "的人，所以也会偏爱有回音的相处。",
                        "如果是认真靠近，我会比看起来更愿意投入。"
                );
                case 2 -> joinSentences(
                        "理想里的两个人，不一定每天都很热闹，但应该会彼此惦记。",
                        "忙完以后还能认真聊天，周末也能一起安排点具体生活，这样的关系我会很心动。",
                        "现在更适合" + profile.relationshipGoal() + "。"
                );
                default -> joinSentences(
                        profile.declaration(),
                        "年纪慢慢往前走以后，我会更在意关系能不能落到现实节奏里。",
                        "如果以后两个人能把吃饭、散步、旅行这些小事都过得有温度，我会很珍惜。"
                );
            };
            case IMAGE_ACTIVITY -> switch (variant(profile, contentIndex, 24) % 4) {
                case 0 -> joinSentences(
                        pickString(ACTIVITY_GLAMOUR_LINES, profile.seed() + contentIndex),
                        "我自己其实不喜欢太吵的社交，反而是这种能慢慢聊、能看见细节的局更容易留下好感。",
                        "现场会让我觉得，认真来认识人的人，状态都不会太差。"
                );
                case 1 -> joinSentences(
                        "今天这类线下活动最舒服的地方，是没有硬凹出来的热络感。",
                        "大家都在自然展示自己，聊工作、聊生活、聊关系观，反而比线上更快知道有没有同频。",
                        "如果之后认识，我也会更信面对面的感觉。"
                );
                case 2 -> joinSentences(
                        "认真社交这件事，果然还是得看现场。",
                        "比起只看照片和资料，我会更在意一个人说话的分寸、回应方式，还有他怎么对待别人。",
                        "这种活动也让我更确定自己适合" + profile.relationshipGoal() + "。"
                );
                default -> joinSentences(
                        "今天出门前本来只是想放松一下，结果现场不少人状态都挺舒服。",
                        "帅哥靓女是其次，真正加分的是大家都愿意认真交流，不会随便糊弄过去。",
                        "有时候线下一个眼神和一句话，比消息框里聊半天都更有用。"
                );
            };
            default -> joinSentences(pickString(SELF_SHARE_OPENERS, profile.seed() + contentIndex), workLine, inviteLine);
        };
    }

    private String buildArticleTitle(RobotProfile profile, int contentIndex) {
        int style = variant(profile, contentIndex, 31) % 6;
        return trim(switch (style) {
            case 0 -> "最近更确定自己适合怎样的关系";
            case 1 -> "工作几年以后，我反而更看重回应感";
            case 2 -> "比起热闹开始，我更想要稳定推进";
            case 3 -> "这段时间，我对恋爱有了更具体的想法";
            case 4 -> "现在的我，更偏爱能落到日常的喜欢";
            default -> "把最近的生活节奏和恋爱观说清楚";
        }, 28);
    }

    private String buildArticleContent(RobotProfile profile, int contentIndex) {
        String paragraph1 = switch (variant(profile, contentIndex, 32) % 3) {
            case 0 -> "这两年我会越来越确定，自己适合的关系不是一下子很上头，而是能稳定推进、能落到日常安排里的那种。"
                    + " 我本身" + profile.temperament() + "，所以比起嘴上热闹，我会更在意一个人是不是" + profile.emotionalStyle() + "。";
            case 1 -> "以前会觉得心动最重要，现在反而更看重相处能不能放松。"
                    + " 对我来说，真正有吸引力的不是表面条件，而是" + pickRelationshipAngle(profile, contentIndex) + "，以及两个人能不能把日常接起来。";
            default -> "认真找对象以后，我开始筛掉很多不必要的消耗。"
                    + " 我不太适合忽冷忽热的关系，更适合" + profile.relationshipGoal() + "，也希望过程里彼此都是真实的。";
        };
        String paragraph2 = "我现在在" + profile.city() + "做" + profile.job() + "，" + buildWorkScene(profile) + "。"
                + " 下班以后我通常会把时间留给" + String.join("、", profile.interests()) + "，让自己从工作状态里退出来。"
                + " " + describeWeekendStyle(profile) + "，这也是我最容易恢复元气的时候。";
        String paragraph3 = switch (variant(profile, contentIndex, 33) % 3) {
            case 0 -> "如果以后进入一段关系，我会很珍惜那种" + profile.familyVision() + "的感觉。"
                    + " 不需要天天黏在一起，但希望彼此都愿意留时间、留回应、留心思。";
            case 1 -> "我理想里的恋爱不是互相占满，而是各自有节奏，也愿意给彼此稳定的位置。"
                    + " 认真见面、认真沟通、认真安排生活，这些细节比任何漂亮话都更打动我。";
            default -> "现在想要的关系，其实没有那么复杂。"
                    + " 能一起吃饭、散步、出门，能在忙的时候互相接住，也能在普通日子里保持偏爱，这就已经很难得。";
        };
        String paragraph4 = switch (variant(profile, contentIndex, 34) % 3) {
            case 0 -> "如果你也不想把时间花在反复试探上，或许我们会聊得来。";
            case 1 -> "我会更愿意认识那种说话真诚、节奏稳定、愿意见面的人。";
            default -> "如果你也在认真生活、认真找对象，欢迎从一条真实的近况开始认识。";
        };
        return joinParagraphs(paragraph1, paragraph2, paragraph3, paragraph4);
    }

    private String buildVideoTitle(RobotProfile profile, int contentIndex) {
        int style = variant(profile, contentIndex, 41) % 6;
        return trim(switch (style) {
            case 0 -> "下班后录一条更像本人的自我介绍";
            case 1 -> "今天用口播的方式认识一下";
            case 2 -> "运动完顺手录一条近况";
            case 3 -> "不想只放资料卡，来条真人版介绍";
            case 4 -> "认真找对象，所以录一条生活化口播";
            default -> "把现在的我直接讲给你听";
        }, 28);
    }

    private String buildVideoCaption(RobotProfile profile, int contentIndex) {
        return switch (variant(profile, contentIndex, 42) % 4) {
            case 0 -> joinSentences(
                    pickString(VIDEO_MONOLOGUE_LINES, profile.seed() + contentIndex),
                    "我在" + profile.city() + "做" + profile.job() + "，" + buildWorkScene(profile) + "。",
                    "平时喜欢" + String.join("、", profile.interests()) + "，镜头里这条也更像我平时会聊天的状态。",
                    "如果你也更喜欢" + pickRelationshipAngle(profile, contentIndex) + "的关系，可以先从这条认识一下。"
            );
            case 1 -> joinSentences(
                    "不太想把自己写成一排条件，所以用口播更直接一点。",
                    profile.temperament() + "，" + profile.communicationStyle() + "。",
                    "最近比较常记录" + buildLifeScene(profile, ContentLane.VIDEO_MONOLOGUE, contentIndex) + "，因为这时候最接近真实生活里的我。",
                    "现在更适合" + profile.relationshipGoal() + "。"
            );
            case 2 -> joinSentences(
                    "这条没有台词模板，就当是一次真实版自我介绍。",
                    "白天做" + profile.job() + "，下班以后会" + renderInterestAction(pickInterestFocus(profile, contentIndex)) + "，把状态切回自己。",
                    "我其实不算特别会讲漂亮话，但如果认真认识，会比看起来更有耐心也更有行动力。",
                    buildSoftInvite(profile, contentIndex)
            );
            default -> joinSentences(
                    "如果之后会聊天，我希望你先看到的是一个真实的人，不是修饰过的资料卡。",
                    "生活里我更偏" + profile.aestheticStyle() + "，关系里则更在意" + profile.emotionalStyle() + "。",
                    "希望遇到的人，能和我一起把普通日子过顺，也愿意慢慢靠近。"
            );
        };
    }

    private VoteScenario buildVoteScenario(RobotProfile profile, int contentIndex) {
        String focus = pickInterestFocus(profile, contentIndex);
        String city = profile.city();
        return switch (variant(profile, contentIndex, 51) % 8) {
            case 0 -> new VoteScenario(
                    "第一次见面，哪种安排会让我加分",
                    joinSentences(
                            "最近在" + city + "做" + profile.job() + "，平时社交更偏向" + profile.socialStyle() + "。",
                            "如果真的开始认识一个人，我反而会很在意第一次见面的节奏是不是舒服，所以想看看大家会怎么选。"
                    ),
                    List.of("咖啡慢聊一小时", "散步顺路吃点东西", "一起看展再聊天", "近郊短途半日局")
            );
            case 1 -> new VoteScenario(
                    "工作忙的时候，哪种回应最让人安心",
                    joinSentences(
                            "我平时" + buildWorkScene(profile),
                            "越是忙的时候，越能看出一个人的表达方式和稳定度。"
                    ),
                    List.of("会主动报个平安", "说到做到不消失", "忙完第一时间回你", "给到具体见面安排")
            );
            case 2 -> new VoteScenario(
                    "你会被哪种生活状态吸引",
                    joinSentences(
                            "我自己平时会把" + renderInterestNoun(focus) + "和工作切得比较开，生活状态其实很能影响我对一个人的好感。",
                            "比起条件罗列，我更容易被具体生活方式打动。"
                    ),
                    buildLifestyleVoteOptions(profile)
            );
            case 3 -> new VoteScenario(
                    "认真认识阶段，哪种细节最加分",
                    joinSentences(
                            "我不太吃空头表达，反而会被一些很具体的小细节打动。",
                            "想看看大家在真正推进关系前，最看重的是哪一类回应。"
                    ),
                    List.of("聊天里会认真接话", "见面后会主动复盘", "约时间不含糊拖延", "相处时分寸感在线")
            );
            case 4 -> new VoteScenario(
                    "周末约会，你更想过哪种半天",
                    joinSentences(
                            "我自己的周末通常是" + describeWeekendStyle(profile) + "，所以约会方式也会直接影响相处感受。",
                            "如果真要选，我会更偏向能自然聊天、不赶场的安排。"
                    ),
                    buildWeekendVoteOptions(profile)
            );
            case 5 -> new VoteScenario(
                    "开始一段关系前，我会先观察什么",
                    joinSentences(
                            "现在更适合" + profile.relationshipGoal() + "以后，我会先看这个人能不能进到真实生活里。",
                            "有些点比心动来得更决定后续能不能走下去。"
                    ),
                    List.of("情绪稳不稳定", "是否愿意见面推进", "说话有没有分寸", "生活节奏能不能接上")
            );
            case 6 -> new VoteScenario(
                    "下班后的相处，哪种最有恋爱感",
                    joinSentences(
                            "我下班以后最喜欢做的事之一就是" + renderInterestAction(focus) + "，所以特别能感受到日常相处到底合不合拍。",
                            "真正让我心动的，往往都不是大场面。"
                    ),
                    List.of("一起散步顺便聊天", "各忙各的也会报备", "吃顿饭再慢慢聊", "开车兜一圈吹吹风")
            );
            default -> new VoteScenario(
                    "如果认真谈恋爱，你最想要哪种确定感",
                    joinSentences(
                            "我自己属于" + profile.emotionalStyle() + "的人，所以在关系里会更想要一些清晰的回应。",
                            "不是要求很多，只是不想再猜来猜去。"
                    ),
                    List.of("被坚定选择", "推进节奏稳定", "情绪被认真接住", "未来安排说得清楚")
            );
        };
    }

    private String pickString(String[] values, int seed) {
        if (values == null || values.length == 0) {
            return "";
        }
        return values[Math.floorMod(seed, values.length)];
    }

    private String pickSceneHighlight(RobotProfile profile, int seed) {
        String[] pool = profile.gender() == 2 ? FEMALE_SCENE_HIGHLIGHTS : MALE_SCENE_HIGHLIGHTS;
        return pickString(pool, profile.seed() + seed);
    }

    private String pickLifestyleLabel(RobotProfile profile) {
        if (profile == null || profile.interests() == null || profile.interests().isEmpty()) {
            return "真实生活";
        }
        String interest = profile.interests().get(0);
        return switch (interest) {
            case "健身", "撸铁", "普拉提", "瑜伽" -> "运动和身材管理";
            case "爬山", "徒步", "露营", "骑行" -> "户外和周末出走";
            case "美食探店", "做饭", "烘焙", "手冲咖啡", "咖啡" -> "美食和日常松弛感";
            case "超跑", "自驾" -> "开车和城市夜景";
            case "拍照", "摄影", "看展", "Citywalk" -> "拍照出片和城市生活";
            default -> interest + "和生活记录";
        };
    }

    private String buildWorkScene(RobotProfile profile) {
        String job = StringUtils.defaultString(profile.job());
        if (StringUtils.containsAny(job, "产品", "运营", "编辑", "策划", "品牌")) {
            return "白天开会对方案、晚上把节奏切回自己生活";
        }
        if (StringUtils.containsAny(job, "老师", "护士", "咨询师", "医生")) {
            return "工作里耐心照顾别人，下班以后更想回到安静松弛的自己";
        }
        if (StringUtils.containsAny(job, "工程师", "分析师", "项目经理", "设计师", "导演")) {
            return "白天专注输出和交付，晚上靠运动或出门放空清掉工作感";
        }
        if (StringUtils.containsAny(job, "律师", "财务", "外贸")) {
            return "工作节奏比较紧，但我会主动给生活留一点质量";
        }
        if (StringUtils.containsAny(job, "主理人", "摄影师", "乐手", "教练", "烘焙师")) {
            return "工作本身就很有个人风格，所以生活里也会更讲究状态和表达";
        }
        return "工作认真推进，生活也认真安排";
    }

    private String buildLifeScene(RobotProfile profile, ContentLane lane, int contentIndex) {
        String interest = profile.interests().isEmpty() ? "Citywalk" : profile.interests().get(Math.floorMod(profile.seed() + contentIndex, profile.interests().size()));
        if (StringUtils.containsAny(interest, "健身", "撸铁", "普拉提", "瑜伽", "跑步", "游泳")) {
            return "运动完以后身体和情绪都在线的状态";
        }
        if (StringUtils.containsAny(interest, "爬山", "徒步", "露营", "骑行", "滑雪")) {
            return "周末往户外走一走、顺手记录风景和自己的状态";
        }
        if (StringUtils.containsAny(interest, "美食探店", "做饭", "烘焙", "手冲咖啡", "咖啡")) {
            return "下班去吃点好吃的或者喝杯咖啡，把一天慢慢收住";
        }
        if (StringUtils.containsAny(interest, "超跑", "自驾")) {
            return "开车兜风看夜景，把脑子从工作里抽出来";
        }
        if (lane == ContentLane.IMAGE_ACTIVITY) {
            return "线下活动里自然社交、慢慢筛选同频的人";
        }
        return "日常出门拍照、散步、把生活过得有一点好看";
    }

    private String pickRelationshipAngle(RobotProfile profile, int contentIndex) {
        return pickString(RELATIONSHIP_ANGLES, profile.seed() + contentIndex);
    }

    private String pickToneSeed(RobotProfile profile, int contentIndex) {
        return pickString(CONTENT_TONE_SEEDS, profile.seed() + contentIndex);
    }

    private String pickVisualDirection(RobotProfile profile, int contentIndex) {
        return pickString(VISUAL_DIRECTIONS, profile.seed() * 3 + contentIndex);
    }

    private String pickVideoDirection(RobotProfile profile, int contentIndex) {
        return pickString(VIDEO_DIRECTIONS, profile.seed() * 5 + contentIndex);
    }

    private ContentLane pickFigureRefreshLane(RobotProfile profile, int contentIndex) {
        List<ContentLane> lanes = List.of(
                ContentLane.IMAGE_SELF,
                ContentLane.IMAGE_SHOWCASE,
                ContentLane.IMAGE_ACTIVITY,
                ContentLane.IMAGE_COUPLE
        );
        return lanes.get(Math.floorMod(variant(profile, contentIndex, 76), lanes.size()));
    }

    private String pickNarrativeFrame(RobotProfile profile, int contentIndex) {
        String[] pool = {
                "像随手发出的近况，不像精修营业",
                "像认真找对象前的一次真实自我暴露",
                "像朋友视角拍到的自然状态",
                "像下班后情绪刚松下来时的生活记录",
                "像周末约会前的轻松试探",
                "像真实资料卡背后的日常切片",
                "像关系刚开始推进时的真诚表达",
                "像一个人把生活过顺后的稳定状态"
        };
        return pickString(pool, variant(profile, contentIndex, 71));
    }

    private String pickSceneWindow(RobotProfile profile, ContentLane lane, int contentIndex) {
        String focus = pickInterestFocus(profile, contentIndex);
        String[] pool = {
                "清晨出门前十分钟的状态",
                "午后短暂放空的一段空镜",
                "下班后回到自己生活节奏里的片段",
                "周末准备出门时的轻松状态",
                "夜晚城市灯光刚亮起来的时候",
                "和兴趣爱好真正发生连接的瞬间",
                "有点好看但不过分摆拍的生活切面",
                "关系感还没说破前的克制氛围"
        };
        if (lane == ContentLane.IMAGE_ACTIVITY) {
            pool = new String[]{
                    "活动开场前后的人群氛围",
                    "线下局里自然互动的侧拍瞬间",
                    "社交场里仍然保持个人状态的镜头",
                    "现场有松弛感但不喧闹的片段"
            };
        } else if (lane == ContentLane.VOTE_DATING) {
            pool = new String[]{
                    "看完投票标题后会停下来思考的封面场景",
                    "适合做关系提问的轻生活切片",
                    "有真实情绪但不浓烈的日常片段",
                    "能让人联想到" + focus + "生活方式的场景入口"
            };
        }
        return pickString(pool, variant(profile, contentIndex, 72));
    }

    private String pickCameraMood(RobotProfile profile, int contentIndex) {
        String[] pool = {
                "近景真人感，保留皮肤和表情细节",
                "中景生活感，人物和场景都有呼吸感",
                "抓拍感更强，像真的有人在记录",
                "留一点空气和留白，不要过满",
                "有电影感但不过度滤镜",
                "像社交平台上会让人停留的真实镜头",
                "轻度出片，但核心还是本人状态",
                "让人物先成立，再谈氛围"
        };
        return pickString(pool, variant(profile, contentIndex, 73));
    }

    private String pickRelationshipStage(RobotProfile profile, int contentIndex) {
        String[] pool = {
                "刚开始认识、彼此还在判断阶段",
                "已经能正常聊天、准备推进见面阶段",
                "认真筛选同频对象的阶段",
                "从资料卡走向真实交流的阶段",
                "想把生活状态直接展示出来的阶段",
                "不想再无效社交、更看重有效连接的阶段"
        };
        return pickString(pool, variant(profile, contentIndex, 74));
    }

    private String pickContentObjective(ContentLane lane, int contentIndex) {
        String[] pool = switch (lane) {
            case IMAGE_SELF -> new String[]{"让人相信这是她/他真实会发的生活动态", "先建立真人感，再让人想继续聊", "展示本人状态而不是摆条件"};
            case IMAGE_SHOWCASE -> new String[]{"突出镜头感但不网红化", "让自我展示和生活感同时成立", "有吸引力，但不是模板式美照"};
            case IMAGE_COUPLE -> new String[]{"让人看到恋爱想象里的具体日常", "把关系感落到普通生活里", "不是糖水感，而是能代入的两人氛围"};
            case IMAGE_ACTIVITY -> new String[]{"有活动氛围，也能看清主角本人", "让线下社交感自然成立", "像帅哥靓女真实出现在同一个活动里"};
            case ARTICLE_RELATIONSHIP -> new String[]{"让长文像认真生活的人写的，不像鸡汤", "有观点，但必须落在日常和工作里", "让人看完能记住这个人的生活纹理"};
            case VOTE_DATING -> new String[]{"让投票像真人会发的关系提问", "重点是问题和选项都贴着本人关心点", "让人愿意投票，而不是只看热闹"};
            case VIDEO_MONOLOGUE -> new String[]{"像真人口播，不像脚本朗读", "像朋友圈自拍视频的升级版", "像认真找对象时的一次真诚出镜"};
        };
        return pickString(pool, contentIndex + lane.ordinal() * 17);
    }

    private String buildFigurePersonaPack(RobotProfile profile, ContentLane lane, int contentIndex) {
        return "工作切片=" + buildWorkScene(profile)
                + "；生活切片=" + buildLifeScene(profile, lane, contentIndex)
                + "；性格=" + profile.temperament()
                + "；社交方式=" + profile.socialStyle()
                + "；情感表达=" + profile.emotionalStyle()
                + "；生活节奏=" + profile.lifeRhythm()
                + "；周末状态=" + profile.weekendStyle()
                + "；关系目标=" + profile.relationshipGoal()
                + "；审美状态=" + profile.aestheticStyle()
                + "；自我表达=" + trim(firstNonBlank(profile.intro(), profile.declaration()), 48);
    }

    private String buildFigureShotDirective(RobotProfile profile, ContentLane lane, int contentIndex) {
        return "地点=" + pickFigureLocation(profile, lane, contentIndex)
                + "；穿搭=" + pickFigureWardrobe(profile, lane, contentIndex)
                + "；动作=" + pickFigurePose(profile, lane, contentIndex)
                + "；表情=" + pickFigureExpression(profile, contentIndex)
                + "；视觉气质=" + pickVisualDirection(profile, contentIndex)
                + "；画幅=" + pickFigureFraming(profile, lane, contentIndex);
    }

    private String pickFigureLocation(RobotProfile profile, ContentLane lane, int contentIndex) {
        String focus = pickInterestFocus(profile, contentIndex);
        String city = StringUtils.defaultIfBlank(profile.city(), "城市");
        String[] pool = switch (lane) {
            case IMAGE_SELF -> new String[]{
                    city + "的街角咖啡店外",
                    city + "下班路上的街区转角",
                    city + "河边或公园步道",
                    city + "住宅区附近有生活感的街边"
            };
            case IMAGE_SHOWCASE -> new String[]{
                    city + "光线干净的落地窗边",
                    city + "有层次感的城市街景前",
                    city + "安静但高级感的室内空间",
                    city + "带一点通勤质感的建筑外立面前"
            };
            case IMAGE_COUPLE -> new String[]{
                    city + "适合约会散步的街区",
                    city + "晚风感明显的江边或天桥边",
                    city + "适合两个人慢慢聊天的小店附近",
                    city + "夜景刚亮起来的街头"
            };
            case IMAGE_ACTIVITY -> new String[]{
                    city + "线下社交活动签到区附近",
                    city + "桌游局或咖啡破冰局的活动空间",
                    city + "有少量人群但主角清晰的活动现场",
                    city + "活动结束后还在继续聊天的公共区域"
            };
            case ARTICLE_RELATIONSHIP -> new String[]{
                    city + "书店或窗边座位",
                    city + "安静的桌面和生活空间",
                    city + "适合发长文封面的日常场景",
                    city + "有真实居家感的角落"
            };
            case VOTE_DATING -> new String[]{
                    city + "让人愿意停下来投票的封面场景",
                    city + "和" + focus + "生活方式相关的日常空间",
                    city + "清爽直接的单人生活场景",
                    city + "轻社交感但不过分热闹的环境"
            };
            case VIDEO_MONOLOGUE -> new String[]{
                    city + "适合竖屏口播的窗边",
                    city + "出门前的镜子侧边或门口",
                    city + "下班后自然光还在的室内一角",
                    city + "运动结束后的休息区附近"
            };
        };
        return pickString(pool, variant(profile, contentIndex, 77));
    }

    private String pickFigureWardrobe(RobotProfile profile, ContentLane lane, int contentIndex) {
        String aesthetic = StringUtils.defaultIfBlank(profile.aestheticStyle(), "清爽克制");
        String focus = pickInterestFocus(profile, contentIndex);
        String[] pool = switch (lane) {
            case IMAGE_ACTIVITY -> new String[]{
                    "适合线下社交的干净穿搭，带一点" + aesthetic,
                    "有分寸感的轻熟日常装，不夸张不抢镜",
                    "像真的会穿去活动现场的私服，不要礼服感"
            };
            case IMAGE_COUPLE -> new String[]{
                    "适合约会的日常私服，低饱和、有生活质感",
                    "轻松但有一点认真感的约会穿搭",
                    "能体现" + aesthetic + "气质的真实私服"
            };
            default -> new String[]{
                    "符合" + profile.job() + "和" + focus + "生活方式的真实私服",
                    "不要统一网红穿搭，要像本人平时真的会穿",
                    "带一点" + aesthetic + "的层次感，但不要夸张造型"
            };
        };
        return pickString(pool, variant(profile, contentIndex, 78));
    }

    private String pickFigurePose(RobotProfile profile, ContentLane lane, int contentIndex) {
        String[] pool = switch (lane) {
            case IMAGE_SELF -> new String[]{"边走边看镜头", "刚停下来整理头发或衣角", "拿着咖啡或包自然站定", "坐下后身体略向镜头放松"};
            case IMAGE_SHOWCASE -> new String[]{"站姿舒展但不过度摆拍", "微侧身让人物线条自然成立", "看向镜头但保留一点留白", "让手部动作参与构图"};
            case IMAGE_COUPLE -> new String[]{"像准备赴约前的自然停顿", "像散步聊天中被拍到的一瞬间", "动作克制但有一点关系感想象", "不要情侣合照，只保留单人约会氛围"};
            case IMAGE_ACTIVITY -> new String[]{"像在活动现场转身回应别人", "手里有活动道具或饮品但不抢戏", "在人群中仍保持自然主体姿态", "像交流间隙被抓拍到的真实动作"};
            case ARTICLE_RELATIONSHIP -> new String[]{"坐姿放松，像在整理思绪", "看向窗外或低头片刻", "动作轻一点，保留思考感", "像真实生活里安静待着的瞬间"};
            case VOTE_DATING -> new String[]{"动作简洁直接，适合作为封面", "姿态自然，不要过多肢体设计", "镜头一眼能看懂人物状态", "留出标题可覆盖的画面空间"};
            case VIDEO_MONOLOGUE -> new String[]{"像口播前半秒的自然定格", "对镜头讲话前的预备状态", "上半身动作真实，像正在表达", "不要主播手势，要像真人自拍"};
        };
        return pickString(pool, variant(profile, contentIndex, 79));
    }

    private String pickFigureExpression(RobotProfile profile, int contentIndex) {
        String[] pool = {
                "像" + profile.temperament() + "的人会有的自然表情",
                "眼神松弛但有交流感，不要空洞职业笑",
                "表情和" + profile.emotionalStyle() + "一致，要有本人状态",
                "像准备认真认识人时的真实神情，不要模式化假笑"
        };
        return pickString(pool, variant(profile, contentIndex, 80));
    }

    private String pickFigureFraming(RobotProfile profile, ContentLane lane, int contentIndex) {
        String[] pool = switch (lane) {
            case IMAGE_ACTIVITY -> new String[]{"中景为主，保留一点环境交代", "半身到中景，主角清晰、人群弱化", "抓拍式中景，不要大头证件照"};
            case IMAGE_COUPLE -> new String[]{"半身或中景，保留约会环境空气感", "中近景，人物和夜景各占一部分", "不要纯特写，要让生活场景成立"};
            default -> new String[]{"半身或中近景", "近景和环境各保留一点", "不要统一大头照构图", "让人物和空间都有呼吸感"};
        };
        return pickString(pool, variant(profile, contentIndex, 81));
    }

    private String buildPromptFlavorPack(RobotProfile profile, ContentLane lane, int contentIndex) {
        return "叙事方式=" + pickNarrativeFrame(profile, contentIndex)
                + "；生活窗口=" + pickSceneWindow(profile, lane, contentIndex)
                + "；镜头气质=" + pickCameraMood(profile, contentIndex)
                + "；关系阶段=" + pickRelationshipStage(profile, contentIndex)
                + "；内容目标=" + pickContentObjective(lane, contentIndex);
    }

    private int variant(RobotProfile profile, int contentIndex, int salt) {
        return Math.floorMod(profile.seed() * 31 + contentIndex * 17 + salt * 13, Integer.MAX_VALUE);
    }

    private String pickInterestFocus(RobotProfile profile, int contentIndex) {
        if (profile == null || profile.interests() == null || profile.interests().isEmpty()) {
            return "散步";
        }
        return profile.interests().get(Math.floorMod(variant(profile, contentIndex, 61), profile.interests().size()));
    }

    private String renderInterestAction(String interest) {
        String value = StringUtils.defaultIfBlank(interest, "散步");
        return switch (value) {
            case "健身", "撸铁", "普拉提", "瑜伽", "跑步", "游泳", "拳击", "羽毛球", "篮球", "飞盘" -> "去" + value;
            case "徒步", "爬山", "露营", "骑行", "滑雪", "桨板", "皮划艇", "路亚" -> "去户外走走";
            case "Citywalk" -> "出门 Citywalk";
            case "拍照", "摄影" -> "出门拍照";
            case "香薰" -> "点香薰放空一下";
            case "手冲咖啡", "咖啡" -> "喝杯咖啡";
            case "吉他" -> "弹会儿吉他";
            case "超跑", "自驾" -> "开车兜风";
            case "美食探店", "探店" -> "去吃点好吃的";
            case "做饭", "烘焙" -> "回家做点吃的";
            case "电影", "看展", "逛书店" -> "安排一点自己的小节目";
            default -> "去" + value;
        };
    }

    private String renderInterestNoun(String interest) {
        String value = StringUtils.defaultIfBlank(interest, "生活");
        return switch (value) {
            case "手冲咖啡" -> "喝咖啡这件小事";
            case "Citywalk" -> "城市里慢慢走走";
            case "超跑", "自驾" -> "开车出去兜风";
            case "徒步", "爬山", "露营", "骑行" -> "往户外走";
            default -> value;
        };
    }

    private String describeWeekendStyle(RobotProfile profile) {
        String value = StringUtils.defaultIfBlank(profile.weekendStyle(), "周末愿意出门走走");
        if (StringUtils.startsWith(value, "周末")) {
            return value;
        }
        return "周末通常会" + value;
    }

    private String buildSoftInvite(RobotProfile profile, int contentIndex) {
        return switch (variant(profile, contentIndex, 62) % 5) {
            case 0 -> "如果你也喜欢真实一点、能落到生活里的相处，可以慢慢认识。";
            case 1 -> "要是你也在认真找对象，而且不喜欢消耗型聊天，也许我们会聊得来。";
            case 2 -> "如果你也更看重生活质感和稳定回应，欢迎从近况开始认识。";
            case 3 -> "比起一上来聊条件，我更希望先认识一个有真实生活的人。";
            default -> "如果你也偏爱同频、清爽、可持续的关系，可以往前走一步。";
        };
    }

    private String joinSentences(String... parts) {
        List<String> normalized = new ArrayList<>();
        if (parts == null) {
            return "";
        }
        for (String part : parts) {
            String value = StringUtils.trimToEmpty(part);
            if (StringUtils.isBlank(value)) {
                continue;
            }
            if (!StringUtils.endsWithAny(value, "。", "！", "？")) {
                value = value + "。";
            }
            if (!normalized.contains(value)) {
                normalized.add(value);
            }
        }
        return String.join(" ", normalized);
    }

    private String joinParagraphs(String... parts) {
        List<String> normalized = new ArrayList<>();
        if (parts == null) {
            return "";
        }
        for (String part : parts) {
            String value = StringUtils.trimToEmpty(part);
            if (StringUtils.isBlank(value) || normalized.contains(value)) {
                continue;
            }
            normalized.add(value);
        }
        return String.join("\n\n", normalized);
    }

    private List<String> buildLifestyleVoteOptions(RobotProfile profile) {
        String lifestyle = pickLifestyleLabel(profile);
        if (StringUtils.contains(lifestyle, "运动")) {
            return List.of("自律有活力型", "会一起运动型", "松弛会照顾人型", "事业稳情绪稳型");
        }
        if (StringUtils.contains(lifestyle, "户外")) {
            return List.of("会往外走的人", "能聊天也能安静", "安排感强的人", "情绪稳定不扫兴");
        }
        if (StringUtils.contains(lifestyle, "美食")) {
            return List.of("会吃也会分享型", "做饭有烟火气型", "松弛会生活型", "细节感很强型");
        }
        if (StringUtils.contains(lifestyle, "开车")) {
            return List.of("行动力很强型", "会安排路线型", "成熟有边界型", "陪伴感很足型");
        }
        return List.of("情绪稳定型", "生活感很强型", "认真沟通型", "有行动力型");
    }

    private List<String> buildWeekendVoteOptions(RobotProfile profile) {
        String focus = pickInterestFocus(profile, 0);
        if (StringUtils.containsAny(focus, "健身", "撸铁", "普拉提", "瑜伽", "跑步", "游泳")) {
            return List.of("早上运动下午喝咖啡", "先吃饭再散步聊天", "去近郊走走拍照", "宅家做饭看电影");
        }
        if (StringUtils.containsAny(focus, "爬山", "徒步", "露营", "骑行", "滑雪", "桨板", "皮划艇")) {
            return List.of("早起户外半日", "找家小店慢慢聊", "城市里边走边拍", "开车去近郊发呆");
        }
        if (StringUtils.containsAny(focus, "超跑", "自驾")) {
            return List.of("开车兜风看夜景", "吃完饭再压马路", "周边短途小旅行", "白天看展晚上聊天");
        }
        return List.of("早午餐加散步", "看展或逛书店", "做饭看电影", "近郊半日出走");
    }

    private SeedContentBundle buildAiContentBundle(RobotProfile profile,
                                                   ContentLane lane,
                                                   int contentIndex,
                                                   GenerationOptions generationOptions) {
        SeedContentBundle localBundle = buildLocalContentBundle(profile, lane, contentIndex);
        if (Boolean.getBoolean("robot.seed.skipAiText")) {
            return localBundle;
        }
        if (!generationOptions.aiEnabled()) {
            return localBundle;
        }
        if (chatModelGatewayService == null) {
            return localBundle;
        }
        try {
            String raw = chatModelGatewayService.complete(
                    ROBOT_AI_FACTORY_SCENE,
                    buildAiSystemPrompt(),
                    buildAiUserPrompt(profile, lane, contentIndex),
                    lane == ContentLane.ARTICLE_RELATIONSHIP ? 900 : 520,
                    1.0D,
                    firstNonBlank(generationOptions.preferredProvider(), ROBOT_AI_FACTORY_PROVIDER),
                    generationOptions.preferredModel()
            );
            SeedContentBundle aiBundle = parseAiBundle(raw, lane);
            return mergeSeedContentBundle(aiBundle, localBundle);
        } catch (Exception ex) {
            log.warn("[robot-seed] ai factory fallback, lane={}, profileSeed={}, reason={}", lane, profile.seed(), ex.getMessage());
            return localBundle;
        }
    }

    private SeedContentBundle buildLocalContentBundle(RobotProfile profile,
                                                      ContentLane lane,
                                                      int contentIndex) {
        return switch (lane) {
            case IMAGE_SELF, IMAGE_SHOWCASE, IMAGE_COUPLE, IMAGE_ACTIVITY -> new SeedContentBundle(
                    buildImageTitle(profile, lane, contentIndex),
                    buildImageCaption(profile, lane, contentIndex),
                    "",
                    List.of()
            );
            case ARTICLE_RELATIONSHIP -> new SeedContentBundle(
                    buildArticleTitle(profile, contentIndex),
                    buildArticleContent(profile, contentIndex),
                    "",
                    List.of()
            );
            case VOTE_DATING -> {
                VoteScenario scenario = buildVoteScenario(profile, contentIndex);
                yield new SeedContentBundle(
                        "",
                        scenario.intro(),
                        scenario.title(),
                        scenario.options()
                );
            }
            case VIDEO_MONOLOGUE -> new SeedContentBundle(
                    buildVideoTitle(profile, contentIndex),
                    buildVideoCaption(profile, contentIndex),
                    "",
                    List.of()
            );
        };
    }

    private SeedContentBundle mergeSeedContentBundle(SeedContentBundle primary, SeedContentBundle fallback) {
        if (primary == null) {
            return fallback;
        }
        if (fallback == null) {
            return primary;
        }
        return new SeedContentBundle(
                firstNonBlank(primary.title(), fallback.title()),
                firstNonBlank(primary.content(), fallback.content()),
                firstNonBlank(primary.voteTitle(), fallback.voteTitle()),
                normalizeVoteOptions(primary.voteOptions(), fallback.voteOptions() == null ? List.of() : fallback.voteOptions())
        );
    }

    private String buildAiSystemPrompt() {
        return "你是婚恋社交产品的数据内容工厂。"
                + "请按人物资料生成像真人会发的中文内容。"
                + "不同人物不能写成同一套模板语气。"
                + "禁止解释，禁止 markdown，禁止代码块，禁止额外前后缀。"
                + "只输出一个 JSON 对象，字段固定为 title、content、voteTitle、voteOptions。";
    }

    private String buildAiUserPrompt(RobotProfile profile, ContentLane lane, int contentIndex) {
        String openingMove = pickOpeningMove(profile, lane, contentIndex);
        String narrativeLens = pickNarrativeLens(profile, lane, contentIndex);
        String detailAnchor = buildDetailAnchor(profile, lane, contentIndex);
        String languageRhythm = pickLanguageRhythm(profile, contentIndex);
        String bannedPhrases = pickBannedPhrases(profile, lane, contentIndex);
        return "人物资料："
                + "\n性别=" + (profile.gender() == 2 ? "女" : "男")
                + "；年龄=" + profile.age()
                + "；城市=" + profile.city()
                + "；学历=" + educationText(profile.education())
                + "；职业=" + profile.job()
                + "；兴趣=" + String.join("、", profile.interests())
                + "；性格=" + profile.temperament()
                + "；社交方式=" + profile.socialStyle()
                + "；情感表达=" + profile.emotionalStyle()
                + "；生活节奏=" + profile.lifeRhythm()
                + "；周末偏好=" + profile.weekendStyle()
                + "；沟通习惯=" + profile.communicationStyle()
                + "；关系目标=" + profile.relationshipGoal()
                + "；审美状态=" + profile.aestheticStyle()
                + "；家庭想象=" + profile.familyVision()
                + "\n工作切片=" + buildWorkScene(profile)
                + "\n生活切片=" + buildLifeScene(profile, lane, contentIndex)
                + "\n关系视角=" + pickRelationshipAngle(profile, contentIndex)
                + "\n语气=" + pickToneSeed(profile, contentIndex)
                + "\n视觉方向=" + pickVisualDirection(profile, contentIndex)
                + "\n随机风格包=" + buildPromptFlavorPack(profile, lane, contentIndex)
                + "\n开场方式=" + openingMove
                + "\n叙事切口=" + narrativeLens
                + "\n细节锚点=" + detailAnchor
                + "\n语言节奏=" + languageRhythm
                + "\n禁用套话=" + bannedPhrases
                + "\n内容类型=" + lanePrompt(lane)
                + "\n要求："
                + "\n1. 内容必须像真人发动态，不要模板腔，不要空话，同一批不同人物不能像一个人写的。"
                + "\n2. 首句必须从“开场方式”切入，不要用“最近”“这条”“分享一下”“想认识”“认真找对象”这类万能起手式。"
                + "\n3. 标题和正文都要自然带出“叙事切口”和“细节锚点”，让内容像此人刚过完这一天。"
                + "\n4. 严格避开“禁用套话”，不要写成资料卡、自我介绍稿、征友模板或鸡汤。"
                + "\n5. 语言节奏遵守“语言节奏”，保留口语感，但不要油腻和悬浮。"
                + "\n6. 不要 emoji、不要 hashtag、不要解释。"
                + "\n7. 投票类型必须返回 4 个不重复选项。"
                + "\n8. 长文可写 3-5 段，其他类型正文控制在自然短内容。"
                + "\n9. 直接输出 JSON，不要加 ```。"
                + "\n输出示例：{\"title\":\"标题\",\"content\":\"正文\",\"voteTitle\":\"投票标题\",\"voteOptions\":[\"选项1\",\"选项2\",\"选项3\",\"选项4\"]}";
    }

    private String pickOpeningMove(RobotProfile profile, ContentLane lane, int contentIndex) {
        String[] pool = switch (lane) {
            case IMAGE_SELF, IMAGE_SHOWCASE -> new String[]{
                    "从下班后的一个具体动作开头",
                    "从刚到某个生活场景时的第一反应开头",
                    "从朋友视角能看到的一幕开头",
                    "从今天某个让自己放松下来的瞬间开头"
            };
            case IMAGE_COUPLE -> new String[]{
                    "从自己最近对关系的一次小体会开头",
                    "从设想两个人某个普通日常开头",
                    "从一次差点说出口的期待开头",
                    "从对稳定相处的具体想象开头"
            };
            case IMAGE_ACTIVITY -> new String[]{
                    "从走进活动现场的第一感受开头",
                    "从现场某个细节观察开头",
                    "从和人慢慢聊起来的瞬间开头",
                    "从线下社交和线上差异的感受开头"
            };
            case ARTICLE_RELATIONSHIP -> new String[]{
                    "从最近生活节奏变化里的一个发现开头",
                    "从工作几年后的关系判断开头",
                    "从自己筛掉某类消耗后的感受开头",
                    "从某个普通日常里想明白的事开头"
            };
            case VOTE_DATING -> new String[]{
                    "从自己真实会纠结的一道选择题开头",
                    "从一次见面或聊天后的感受开头",
                    "从关系推进里最在意的细节开头",
                    "从周末安排或下班后相处的真实偏好开头"
            };
            case VIDEO_MONOLOGUE -> new String[]{
                    "从镜头刚打开时的第一句开头",
                    "从此刻所在场景的一句话开头",
                    "从自己不想写资料卡的原因开头",
                    "从刚做完某件小事后的状态开头"
            };
        };
        return pickString(pool, variant(profile, contentIndex, 81));
    }

    private String pickNarrativeLens(RobotProfile profile, ContentLane lane, int contentIndex) {
        String[] pool = switch (lane) {
            case IMAGE_SELF, IMAGE_SHOWCASE -> new String[]{
                    "把工作状态切回生活状态的那一刻",
                    "今天这个场景为什么像自己本人",
                    "镜头背后那个更真实的自己",
                    "生活松弛感是怎么来的"
            };
            case IMAGE_COUPLE -> new String[]{
                    "关系里什么细节比热闹更重要",
                    "两个人怎么把日子过顺",
                    "自己会被什么样的回应打动",
                    "为什么现在更偏爱稳定推进"
            };
            case IMAGE_ACTIVITY -> new String[]{
                    "线下社交里真正加分的点",
                    "见面之后才看得出来的分寸感",
                    "人群里如何辨认同频的人",
                    "为什么面对面更容易判断感觉"
            };
            case ARTICLE_RELATIONSHIP -> new String[]{
                    "现在的自己为什么不再吃表面热闹",
                    "工作和生活节奏怎样影响关系选择",
                    "真正会留下来的关系需要什么",
                    "把喜欢落到日常之后会发生什么"
            };
            case VOTE_DATING -> new String[]{
                    "第一次见面最能暴露匹配度的地方",
                    "忙的时候什么回应最让人安心",
                    "周末相处方式为什么决定后续感觉",
                    "什么细节最能判断对方是不是认真"
            };
            case VIDEO_MONOLOGUE -> new String[]{
                    "不靠条件罗列也能介绍清楚自己",
                    "让人先看到真实生活状态",
                    "说清楚自己当下的关系目标",
                    "把平时聊天的感觉直接讲出来"
            };
        };
        return pickString(pool, variant(profile, contentIndex, 82));
    }

    private String buildDetailAnchor(RobotProfile profile, ContentLane lane, int contentIndex) {
        String interestFocus = pickInterestFocus(profile, contentIndex);
        return switch (lane) {
            case IMAGE_ACTIVITY -> "活动现场、人和人聊开的节奏、" + profile.city() + "线下社交氛围";
            case IMAGE_COUPLE -> profile.weekendStyle() + "，以及自己期待的两人日常安排";
            case ARTICLE_RELATIONSHIP -> buildWorkScene(profile) + "，" + describeWeekendStyle(profile) + "，" + profile.familyVision();
            case VOTE_DATING -> renderInterestAction(interestFocus) + "后的状态、第一次见面或周末安排偏好";
            case VIDEO_MONOLOGUE -> "此刻录视频所在的小场景、" + buildWorkScene(profile) + "、" + renderInterestAction(interestFocus);
            default -> buildWorkScene(profile) + "，" + buildLifeScene(profile, lane, contentIndex) + "，" + renderInterestAction(interestFocus);
        };
    }

    private String pickLanguageRhythm(RobotProfile profile, int contentIndex) {
        String[] pool = {
                "短句偏多，像刚聊开时自然说出来的话",
                "一半生活描述，一半关系判断，不要全是抒情",
                "先讲场景再落到感受，语气克制一点",
                "像和同频的人慢慢聊天，不端着也不卖惨",
                "保留一点停顿感，像真人想到哪说到哪"
        };
        return pickString(pool, variant(profile, contentIndex, 83));
    }

    private String pickBannedPhrases(RobotProfile profile, ContentLane lane, int contentIndex) {
        String[] pool = switch (lane) {
            case IMAGE_SELF, IMAGE_SHOWCASE -> new String[]{
                    "最近的我、生活记录一下、分享一下、把最近的自己发出来",
                    "这条就当近况更新、顺手发一条、营业一下、刷个存在感",
                    "想认识真诚的人、认真找对象、资料卡之外的我、真实生活切片"
            };
            case IMAGE_COUPLE, ARTICLE_RELATIONSHIP -> new String[]{
                    "希望遇到对的人、双向奔赴、细水长流、余生",
                    "慢慢来比较快、灵魂契合、偏爱和例外、未来可期",
                    "认真找对象、想要稳定关系、把日子过成诗、互相治愈"
            };
            case IMAGE_ACTIVITY, VOTE_DATING -> new String[]{
                    "欢迎大家投票、你会怎么选、评论区见、都来说说",
                    "线下果然更有感觉、氛围感拉满、好感度直接拉满、同频的人不多",
                    "如果是你会怎么做、你们觉得呢、有没有同款想法、在线等答案"
            };
            case VIDEO_MONOLOGUE -> new String[]{
                    "哈喽大家好、简单介绍一下自己、录条视频认识一下、希望你们喜欢",
                    "今天来做个自我介绍、给大家打个招呼、屏幕前的你们好、感谢观看",
                    "不想只放资料卡、认真找对象、希望遇到同频的人、先从视频认识"
            };
        };
        return pickString(pool, variant(profile, contentIndex, 84));
    }

    private SeedContentBundle parseAiBundle(String raw, ContentLane lane) {
        if (StringUtils.isBlank(raw)) {
            return SeedContentBundle.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(raw));
            String title = safeAiText(root.path("title").asText(""), lane == ContentLane.ARTICLE_RELATIONSHIP ? 32 : 28);
            String content = safeAiText(root.path("content").asText(""), lane == ContentLane.ARTICLE_RELATIONSHIP ? 900 : 220);
            String voteTitle = safeAiText(root.path("voteTitle").asText(""), 28);
            List<String> options = new ArrayList<>();
            JsonNode voteOptionsNode = root.path("voteOptions");
            if (voteOptionsNode.isArray()) {
                for (JsonNode item : voteOptionsNode) {
                    String text = safeAiText(item.asText(""), 18);
                    if (StringUtils.isNotBlank(text)) {
                        options.add(text);
                    }
                }
            }
            return new SeedContentBundle(title, content, voteTitle, options);
        } catch (Exception ex) {
            log.warn("[robot-seed] ai bundle parse failed: {}", ex.getMessage());
            return SeedContentBundle.empty();
        }
    }

    private String stripCodeFence(String text) {
        String value = StringUtils.trimToEmpty(text);
        if (StringUtils.startsWith(value, "```") && StringUtils.endsWith(value, "```")) {
            String stripped = value.replaceFirst("^```[a-zA-Z]*\\s*", "");
            return stripped.replaceFirst("\\s*```$", "").trim();
        }
        return value;
    }

    private String safeAiText(String text, int maxLength) {
        String value = StringUtils.trimToEmpty(text)
                .replace("```", "")
                .replace("\r", "")
                .replace("\t", " ");
        value = value.replaceAll("\\n{3,}", "\n\n");
        if (StringUtils.length(value) > maxLength) {
            return StringUtils.substring(value, 0, maxLength);
        }
        return value;
    }

    private String lanePrompt(ContentLane lane) {
        return switch (lane) {
            case IMAGE_SELF -> "图文动态，偏本人状态和近期生活切片";
            case IMAGE_SHOWCASE -> "图文动态，偏自我展示、出片、真人感";
            case IMAGE_COUPLE -> "图文动态，偏恋爱观、理想相处和两人日常想象";
            case IMAGE_ACTIVITY -> "图文动态，偏线下活动、帅哥靓女、真实社交氛围";
            case ARTICLE_RELATIONSHIP -> "长文动态，3到5段，有观点但不要说教";
            case VOTE_DATING -> "投票动态，需要简短引言、投票标题、4个选项";
            case VIDEO_MONOLOGUE -> "视频动态，像真人口播自我介绍，文案要能配自拍视频";
        };
    }

    private String educationText(int education) {
        return switch (education) {
            case 1 -> "高中";
            case 2 -> "大专";
            case 3 -> "本科";
            case 4 -> "硕士";
            case 5 -> "博士";
            default -> "本科";
        };
    }

    private List<String> normalizeVoteOptions(List<String> aiOptions, List<String> fallbackOptions) {
        List<String> safe = new ArrayList<>();
        if (aiOptions != null) {
            for (String option : aiOptions) {
                String value = trim(StringUtils.trimToEmpty(option), 18);
                if (StringUtils.isNotBlank(value) && !safe.contains(value)) {
                    safe.add(value);
                }
            }
        }
        if (safe.size() >= 4) {
            return safe.subList(0, 4);
        }
        for (String option : fallbackOptions) {
            if (!safe.contains(option)) {
                safe.add(option);
            }
            if (safe.size() >= 4) {
                break;
            }
        }
        return safe;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private GenerationOptions parseGenerationOptions(Map<String, Object> options) {
        if (options == null || options.isEmpty()) {
            return GenerationOptions.aiDefault();
        }
        boolean createUserIfNeeded = parseBooleanOption(options.get("createUserIfNeeded"), true);
        String contentFactoryMode = StringUtils.defaultIfBlank(StringUtils.trimToEmpty(String.valueOf(options.getOrDefault("contentFactoryMode", "ai"))), "ai");
        String preferredProvider = StringUtils.trimToEmpty(String.valueOf(options.getOrDefault("preferredProvider", "")));
        String preferredModel = StringUtils.trimToEmpty(String.valueOf(options.getOrDefault("preferredModel", "")));
        return new GenerationOptions(createUserIfNeeded, normalizeFactoryMode(contentFactoryMode), preferredProvider, preferredModel);
    }

    private ExistingUserBatchOptions parseExistingUserBatchOptions(Map<String, Object> options) {
        Map<String, Object> safeOptions = options == null ? Map.of() : options;
        int userLimit = parseIntOption(safeOptions.get("userLimit"), 100, 1, 200);
        int minPostsPerUser = parseIntOption(safeOptions.get("minPostsPerUser"), 1, 1, 3);
        int maxPostsPerUser = parseIntOption(safeOptions.get("maxPostsPerUser"), 2, minPostsPerUser, 4);
        String userScope = normalizeUserScope(String.valueOf(safeOptions.getOrDefault("userScope", "seed")));
        boolean shuffle = parseBooleanOption(safeOptions.get("shuffle"), true);
        List<Integer> uidList = parseUidListOption(safeOptions.get("uidList"));
        GenerationOptions generationOptions = parseGenerationOptionsWithDefaults(
                safeOptions,
                false,
                "ai"
        );
        return new ExistingUserBatchOptions(userLimit, minPostsPerUser, maxPostsPerUser, userScope, shuffle, uidList, generationOptions);
    }

    private GenerationOptions parseGenerationOptionsWithDefaults(Map<String, Object> options,
                                                                 boolean defaultCreateUserIfNeeded,
                                                                 String defaultFactoryMode) {
        if (options == null || options.isEmpty()) {
            return new GenerationOptions(defaultCreateUserIfNeeded, normalizeFactoryMode(defaultFactoryMode), "", "");
        }
        boolean createUserIfNeeded = parseBooleanOption(options.get("createUserIfNeeded"), defaultCreateUserIfNeeded);
        String contentFactoryMode = StringUtils.defaultIfBlank(
                StringUtils.trimToEmpty(String.valueOf(options.getOrDefault("contentFactoryMode", defaultFactoryMode))),
                defaultFactoryMode
        );
        String preferredProvider = StringUtils.trimToEmpty(String.valueOf(options.getOrDefault("preferredProvider", "")));
        String preferredModel = StringUtils.trimToEmpty(String.valueOf(options.getOrDefault("preferredModel", "")));
        return new GenerationOptions(createUserIfNeeded, normalizeFactoryMode(contentFactoryMode), preferredProvider, preferredModel);
    }

    private int parseIntOption(Object value, int defaultValue, int minValue, int maxValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(StringUtils.trimToEmpty(String.valueOf(value)));
            return Math.max(minValue, Math.min(parsed, maxValue));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private boolean parseBooleanOption(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String normalized = StringUtils.lowerCase(StringUtils.trimToEmpty(String.valueOf(value)));
        if (StringUtils.isBlank(normalized)) {
            return defaultValue;
        }
        return switch (normalized) {
            case "1", "true", "yes", "y", "on" -> true;
            case "0", "false", "no", "n", "off" -> false;
            default -> defaultValue;
        };
    }

    private List<Integer> parseUidListOption(Object value) {
        if (value == null) {
            return List.of();
        }
        LinkedHashSet<Integer> uidSet = new LinkedHashSet<>();
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                Integer uid = safeParseUid(String.valueOf(item));
                if (uid != null) {
                    uidSet.add(uid);
                }
            }
        } else {
            String raw = StringUtils.trimToEmpty(String.valueOf(value));
            if (StringUtils.isBlank(raw)) {
                return List.of();
            }
            for (String item : StringUtils.split(raw, ",，|/\\s")) {
                Integer uid = safeParseUid(item);
                if (uid != null) {
                    uidSet.add(uid);
                }
            }
        }
        return uidSet.stream().limit(200).collect(Collectors.toList());
    }

    private Integer safeParseUid(String value) {
        try {
            int uid = Integer.parseInt(StringUtils.trimToEmpty(value));
            return uid > 0 ? uid : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeFactoryMode(String raw) {
        String normalized = StringUtils.lowerCase(StringUtils.trimToEmpty(raw));
        return switch (normalized) {
            case "ai", "hybrid", "local" -> normalized;
            default -> "local";
        };
    }

    private String normalizeUserScope(String raw) {
        String normalized = StringUtils.lowerCase(StringUtils.trimToEmpty(raw));
        return switch (normalized) {
            case "robot", "legacy", "all", "seed" -> normalized;
            default -> "seed";
        };
    }

    private String resolveSchool(int education) {
        return switch (education) {
            case 5 -> RandomUtil.randomEle(List.of("浙江大学", "复旦大学", "中山大学", "四川大学"));
            case 4 -> RandomUtil.randomEle(List.of("苏州大学", "宁波大学", "华侨大学", "深圳大学"));
            case 3 -> RandomUtil.randomEle(List.of("浙江工商大学", "南京信息工程大学", "成都理工大学", "集美大学"));
            case 2 -> RandomUtil.randomEle(List.of("杭州职业技术学院", "深圳信息职业技术学院", "无锡职业技术学院"));
            default -> "";
        };
    }

    private String nextUniqueMobile() {
        while (true) {
            long candidate = MOBILE_SEQUENCE.incrementAndGet() % 1_000_000_000L;
            String mobile = "17" + String.format("%09d", candidate);
            boolean exists = appUserService.lambdaQuery().eq(AppUserEntity::getMobile, mobile).count() > 0;
            if (!exists) {
                return mobile;
            }
        }
    }

    private String nextUniqueUsername(Integer gender, String city, String seed) {
        for (int i = 0; i < 8; i++) {
            String candidate = NaturalUsernameGenerator.generate(gender, city, seed + i);
            boolean exists = appUserService.lambdaQuery().eq(AppUserEntity::getUsername, candidate).count() > 0;
            if (!exists) {
                return candidate;
            }
        }
        return NaturalUsernameGenerator.generate(gender, city, seed) + RandomUtil.randomNumbers(2);
    }

    private Date randomRecentDate(int dayWindow) {
        DateTime now = DateUtil.date();
        return DateUtil.offset(now, DateField.DAY_OF_YEAR, -RandomUtil.randomInt(0, Math.max(dayWindow, 1)));
    }

    private String normalizeTheme(String value) {
        if (StringUtils.isBlank(value)) {
            return "daily";
        }
        return "seed" + Math.abs(value.hashCode());
    }

    private String trim(String value, int maxLength) {
        if (StringUtils.length(value) <= maxLength) {
            return value;
        }
        return StringUtils.substring(value, 0, maxLength);
    }

    private Set<String> findDuplicateFigureMedia(List<AppUserEntity> users) {
        Map<String, Integer> counts = new HashMap<>();
        if (users == null) {
            return Set.of();
        }
        for (AppUserEntity user : users) {
            if (user == null || StringUtils.isBlank(user.getFigur())) {
                continue;
            }
            for (String item : StringUtils.split(user.getFigur(), ",")) {
                String value = StringUtils.trimToEmpty(item);
                if (StringUtils.isBlank(value)) {
                    continue;
                }
                counts.merge(value, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private boolean hasDirtyOrDuplicateFigure(AppUserEntity user, Set<String> duplicateFigureMedia) {
        if (user == null || StringUtils.isBlank(user.getFigur())) {
            return true;
        }
        String avatar = StringUtils.trimToEmpty(user.getAvatar());
        for (String item : StringUtils.split(user.getFigur(), ",")) {
            String value = StringUtils.trimToEmpty(item);
            if (StringUtils.isBlank(value)) {
                return true;
            }
            if (duplicateFigureMedia != null && duplicateFigureMedia.contains(value)) {
                return true;
            }
            if (StringUtils.equals(value, avatar)) {
                return true;
            }
            if (StringUtils.containsAnyIgnoreCase(value,
                    "picsum.photos",
                    "randomuser.me",
                    "itouxiang.com",
                    "tuxiangyan.com")) {
                return true;
            }
        }
        return false;
    }

    private static List<String> resolveExistingUserInterests(AppUserEntity user) {
        if (user != null && StringUtils.isNotBlank(user.getInterest())) {
            List<String> parsed = Arrays.stream(StringUtils.split(user.getInterest(), "、,，/|"))
                    .map(StringUtils::trimToEmpty)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .limit(4)
                    .collect(Collectors.toList());
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }
        String job = user == null ? "" : StringUtils.trimToEmpty(user.getJob());
        if (StringUtils.containsAny(job, "老师", "咨询", "编辑", "策划")) {
            return List.of("看展", "逛书店", "咖啡");
        }
        if (StringUtils.containsAny(job, "设计", "摄影", "导演", "品牌")) {
            return List.of("拍照", "Citywalk", "探店");
        }
        if (StringUtils.containsAny(job, "健身", "运动", "教练")) {
            return List.of("健身", "跑步", "徒步");
        }
        if (StringUtils.containsAny(job, "医生", "律师", "金融", "工程师", "产品")) {
            return List.of("咖啡", "跑步", "电影");
        }
        return List.of("Citywalk", "电影", "咖啡");
    }

    private static String resolveExistingCity(AppUserEntity user) {
        return firstNonBlankStatic(
                user == null ? "" : user.getLocationCity(),
                user == null ? "" : user.getCity(),
                user == null ? "" : user.getAbodeCity(),
                user == null ? "" : user.getHomeCity(),
                "杭州"
        );
    }

    private static String resolveExistingProvince(AppUserEntity user) {
        return firstNonBlankStatic(
                user == null ? "" : user.getProvince(),
                cityToProvince(resolveExistingCity(user)),
                "浙江省"
        );
    }

    private static String resolveExistingIntro(AppUserEntity user, List<String> interests, String city, String job) {
        String source = firstNonBlankStatic(
                user == null ? "" : user.getSelfIntroduction(),
                user == null ? "" : user.getIntro()
        );
        if (StringUtils.isNotBlank(source)) {
            return source.trim();
        }
        String interestFocus = interests.isEmpty() ? "咖啡" : interests.get(0);
        return "常住" + city + "，做" + job + "。平时会用" + interestFocus + "把生活调回自己的节奏，想认识真诚一点的人。";
    }

    private static String resolveExistingDeclaration(AppUserEntity user, List<String> interests, String city) {
        String source = user == null ? "" : user.getLoveDeclaration();
        if (StringUtils.isNotBlank(source)) {
            return source.trim();
        }
        String interestFocus = interests.isEmpty() ? "散步" : interests.get(0);
        return "希望认识能一起在" + city + "认真生活的人，忙完也愿意一起" + interestFocus + "，慢慢把关系走实。";
    }

    private static String inferTemperament(AppUserEntity user, List<String> interests, String job) {
        String text = buildProfileSourceText(user, interests, job);
        if (StringUtils.containsAny(text, "慢热", "安静", "内向", "不吵", "独处")) {
            return "慢热但不冷淡，熟起来以后会很真诚";
        }
        if (StringUtils.containsAny(text, "健身", "跑步", "徒步", "瑜伽", "教练")) {
            return "清爽自律，状态稳定，也有点行动力";
        }
        if (StringUtils.containsAny(text, "摄影", "设计", "策划", "导演", "品牌")) {
            return "有审美，也有表达欲，聊天不空";
        }
        if (StringUtils.containsAny(text, "医生", "律师", "金融", "工程师", "产品")) {
            return "做事有条理，情绪相对稳定";
        }
        return "自然真实，不端着";
    }

    private static String inferSocialStyle(AppUserEntity user, List<String> interests, String job) {
        String text = buildProfileSourceText(user, interests, job);
        if (StringUtils.containsAny(text, "活动", "社交", "朋友多", "外向")) {
            return "面对面会更有状态，也更容易把气氛聊开";
        }
        if (StringUtils.containsAny(text, "慢热", "内向", "安静")) {
            return "更适合一对一慢慢聊，熟了以后会更放松";
        }
        if (StringUtils.containsAny(text, "健身", "徒步", "露营", "骑行", "飞盘")) {
            return "边走边聊、一起做点事情会更自然";
        }
        return "更适合一对一慢慢聊";
    }

    private static String inferEmotionalStyle(AppUserEntity user, List<String> interests, String job) {
        String text = buildProfileSourceText(user, interests, job);
        if (StringUtils.containsAny(text, "偏爱", "稳定", "长期", "认真")) {
            return "不太会嘴上轰炸，但行动会尽量稳定";
        }
        if (StringUtils.containsAny(text, "回应", "沟通", "说清楚")) {
            return "更看重回应感，也愿意把喜欢说清楚";
        }
        if (StringUtils.containsAny(text, "照顾", "陪伴", "体贴")) {
            return "会通过陪伴和照顾细节表达喜欢";
        }
        return "喜欢稳定回应";
    }

    private static String inferLifeRhythm(AppUserEntity user, List<String> interests, String job) {
        String text = buildProfileSourceText(user, interests, job);
        if (StringUtils.containsAny(text, "医生", "护士", "空乘", "律师", "项目经理")) {
            return "工作时会比较投入，空下来会主动把生活节奏拉回来";
        }
        if (StringUtils.containsAny(text, "老师", "咨询", "编辑", "运营", "品牌")) {
            return "白天处理工作，晚上更想把生活过得松弛一点";
        }
        if (StringUtils.containsAny(text, "健身", "跑步", "瑜伽", "普拉提")) {
            return "会把运动和休息安排进去，状态基本自己能调顺";
        }
        return "工作和生活会主动切换开";
    }

    private static String inferWeekendStyle(AppUserEntity user, List<String> interests, String job) {
        String focus = interests.isEmpty() ? "散步" : interests.get(0);
        if (StringUtils.containsAny(focus, "健身", "跑步", "瑜伽", "普拉提")) {
            return "周末通常会先运动，再找家舒服的小店慢慢待着";
        }
        if (StringUtils.containsAny(focus, "徒步", "爬山", "露营", "骑行")) {
            return "周末更想往户外走，半天活动半天放空";
        }
        if (StringUtils.containsAny(focus, "拍照", "摄影", "Citywalk", "看展")) {
            return "周末喜欢边走边看，顺手把生活拍下来";
        }
        if (StringUtils.containsAny(job, "厨", "咖啡", "烘焙")) {
            return "周末会给自己留点烟火气，吃饭和散步都不能少";
        }
        return "周末愿意认真出门";
    }

    private static String inferCommunicationStyle(AppUserEntity user, List<String> interests, String job) {
        String text = buildProfileSourceText(user, interests, job);
        if (StringUtils.containsAny(text, "直接", "坦诚", "不绕弯")) {
            return "更适合真实直接一点的表达";
        }
        if (StringUtils.containsAny(text, "查户口", "尬聊", "敷衍")) {
            return "不喜欢查户口式聊天，更喜欢从日常切进去";
        }
        if (StringUtils.containsAny(text, "回应", "认真", "稳定")) {
            return "回复不一定秒回，但会认真接住话题";
        }
        return "不喜欢查户口式聊天";
    }

    private static String inferRelationshipGoal(AppUserEntity user, List<String> interests, String job) {
        String text = buildProfileSourceText(user, interests, job);
        if (StringUtils.containsAny(text, "结婚", "长期", "以后", "未来")) {
            return "认真交往并自然走向长期关系";
        }
        if (StringUtils.containsAny(text, "见面", "现实", "生活")) {
            return "希望认识以后可以尽快见面，不只停留在线上";
        }
        if (StringUtils.containsAny(text, "稳定", "认真")) {
            return "先从稳定认识开始，再看能不能走进彼此生活";
        }
        return "先认真认识，再自然推进";
    }

    private static String inferAestheticStyle(AppUserEntity user, List<String> interests, String job) {
        String text = buildProfileSourceText(user, interests, job);
        if (StringUtils.containsAny(text, "摄影", "设计", "品牌", "时尚", "买手")) {
            return "有点镜头感但不过度摆拍";
        }
        if (StringUtils.containsAny(text, "健身", "跑步", "徒步", "瑜伽")) {
            return "自然有力量感";
        }
        if (StringUtils.containsAny(text, "律师", "金融", "医生", "工程师", "产品")) {
            return "干净利落";
        }
        return "真实比精修重要";
    }

    private static String inferFamilyVision(AppUserEntity user, List<String> interests, String job) {
        String text = buildProfileSourceText(user, interests, job);
        if (StringUtils.containsAny(text, "做饭", "美食", "烘焙", "烟火")) {
            return "两个人一起把普通日子过顺，家里也有烟火气";
        }
        if (StringUtils.containsAny(text, "旅行", "徒步", "露营", "自驾")) {
            return "忙完也愿意一起出门，看世界也看彼此";
        }
        if (StringUtils.containsAny(text, "稳定", "长期", "未来")) {
            return "不是互相消耗，而是一起变得更稳定";
        }
        return "把普通日子过顺";
    }

    private static String buildProfileSourceText(AppUserEntity user, List<String> interests, String job) {
        List<String> parts = new ArrayList<>();
        if (user != null) {
            parts.add(StringUtils.trimToEmpty(user.getSelfIntroduction()));
            parts.add(StringUtils.trimToEmpty(user.getLoveDeclaration()));
            parts.add(StringUtils.trimToEmpty(user.getIntro()));
            parts.add(StringUtils.trimToEmpty(user.getTagStr()));
            parts.add(StringUtils.trimToEmpty(user.getInfo()));
        }
        parts.add(StringUtils.trimToEmpty(job));
        if (interests != null && !interests.isEmpty()) {
            parts.add(String.join(" ", interests));
        }
        return String.join(" ", parts);
    }

    private static String firstNonBlankStatic(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static String cityToProvince(String city) {
        if (StringUtils.isBlank(city)) {
            return "";
        }
        for (int i = 0; i < CITIES.length; i++) {
            if (StringUtils.equals(CITIES[i], city)) {
                return PROVINCES[i];
            }
        }
        return "";
    }

    private record RobotProfile(
            int seed,
            int gender,
            int age,
            String city,
            String province,
            int education,
            String job,
            List<String> interests,
            String intro,
            String declaration,
            String temperament,
            String socialStyle,
            String emotionalStyle,
            String lifeRhythm,
            String weekendStyle,
            String communicationStyle,
            String relationshipGoal,
            String aestheticStyle,
            String familyVision,
            String backgroundTheme,
            TopicEntity preferredTopic
    ) {
        static RobotProfile fromExisting(AppUserEntity user) {
            return fromExisting(user, null);
        }

        static RobotProfile fromExisting(AppUserEntity user, TopicEntity preferredTopic) {
            List<String> interestList = resolveExistingUserInterests(user);
            String city = resolveExistingCity(user);
            String province = resolveExistingProvince(user);
            String job = StringUtils.defaultIfBlank(user.getJob(), "自由职业");
            String intro = resolveExistingIntro(user, interestList, city, job);
            String declaration = resolveExistingDeclaration(user, interestList, city);
            return new RobotProfile(
                    user.getUid() == null ? RandomUtil.randomInt(100000) : user.getUid(),
                    user.getGender() == null ? 0 : user.getGender(),
                    user.getAge() == null ? 28 : user.getAge(),
                    city,
                    province,
                    user.getEducation() == null ? 3 : user.getEducation(),
                    job,
                    interestList.isEmpty() ? List.of("Citywalk", "电影", "咖啡") : interestList,
                    intro,
                    declaration,
                    inferTemperament(user, interestList, job),
                    inferSocialStyle(user, interestList, job),
                    inferEmotionalStyle(user, interestList, job),
                    inferLifeRhythm(user, interestList, job),
                    inferWeekendStyle(user, interestList, job),
                    inferCommunicationStyle(user, interestList, job),
                    inferRelationshipGoal(user, interestList, job),
                    inferAestheticStyle(user, interestList, job),
                    inferFamilyVision(user, interestList, job),
                    StringUtils.defaultIfBlank(job, String.join("_", interestList)),
                    preferredTopic
            );
        }
    }

    private record SeedContentBundle(
            String title,
            String content,
            String voteTitle,
            List<String> voteOptions
    ) {
        static SeedContentBundle empty() {
            return new SeedContentBundle("", "", "", List.of());
        }
    }

    private record VoteScenario(
            String title,
            String intro,
            List<String> options
    ) {
    }

    private record SeedMediaBundle(
            String avatar,
            String figur
    ) {
    }

    private record ContentCreationResult(
            Integer postId,
            Integer videoId,
            ContentLane lane,
            String status
    ) {
        static ContentCreationResult post(Integer postId, ContentLane lane) {
            return new ContentCreationResult(postId, null, lane, "post_created");
        }

        static ContentCreationResult post(Integer postId, ContentLane lane, String status) {
            return new ContentCreationResult(postId, null, lane, status);
        }
    }

    private record GenerationOptions(
            boolean createUserIfNeeded,
            String contentFactoryMode,
            String preferredProvider,
            String preferredModel
    ) {
        static GenerationOptions localDefault() {
            return new GenerationOptions(true, "local", "", "");
        }

        static GenerationOptions localDefault(boolean createUserIfNeeded) {
            return new GenerationOptions(createUserIfNeeded, "local", "", "");
        }

        static GenerationOptions aiDefault() {
            return new GenerationOptions(true, "ai", ROBOT_AI_FACTORY_PROVIDER, "");
        }

        static GenerationOptions aiDefault(boolean createUserIfNeeded) {
            return new GenerationOptions(createUserIfNeeded, "ai", ROBOT_AI_FACTORY_PROVIDER, "");
        }

        boolean aiEnabled() {
            return "ai".equals(contentFactoryMode) || "hybrid".equals(contentFactoryMode);
        }

        boolean strictModelContent() {
            return "ai".equals(contentFactoryMode);
        }
    }

    private record ExistingUserBatchOptions(
            int userLimit,
            int minPostsPerUser,
            int maxPostsPerUser,
            String userScope,
            Boolean shuffle,
            List<Integer> uidList,
            GenerationOptions generationOptions
    ) {
    }

    private enum ContentLane {
        IMAGE_SELF,
        IMAGE_SHOWCASE,
        IMAGE_COUPLE,
        IMAGE_ACTIVITY,
        ARTICLE_RELATIONSHIP,
        VOTE_DATING,
        VIDEO_MONOLOGUE
    }
}
