package msmx.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import msmx.entity.DatingProfile;
import msmx.repository.DatingProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class DatingService {

    private final DatingProfileRepository datingProfileRepository;
    private final ObjectMapper objectMapper;
    private final AiService aiService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    public DatingService(DatingProfileRepository datingProfileRepository, 
                         ObjectMapper objectMapper,
                         AiService aiService) {
        this.datingProfileRepository = datingProfileRepository;
        this.objectMapper = objectMapper;
        this.aiService = aiService;
    }

    public static final Map<String, String[]> BIG_FIVE_QUESTIONS = new LinkedHashMap<>();
    static {
        BIG_FIVE_QUESTIONS.put("开放性", new String[]{
            "我喜欢尝试新的事物和体验",
            "我对艺术和文化活动很感兴趣",
            "我经常有创造性的想法",
            "我喜欢思考抽象的概念"
        });
        BIG_FIVE_QUESTIONS.put("尽责性", new String[]{
            "我做事总是有条不紊",
            "我非常注重细节",
            "我总是按时完成任务",
            "我善于制定计划并坚持执行"
        });
        BIG_FIVE_QUESTIONS.put("外向性", new String[]{
            "我喜欢参加社交活动",
            "我很容易交到新朋友",
            "我在人群中感到精力充沛",
            "我喜欢成为关注的焦点"
        });
        BIG_FIVE_QUESTIONS.put("宜人性", new String[]{
            "我总是乐于助人",
            "我善于理解他人的感受",
            "我容易原谅别人",
            "我喜欢与他人合作"
        });
        BIG_FIVE_QUESTIONS.put("神经质", new String[]{
            "我经常感到焦虑或紧张",
            "我情绪波动较大",
            "我容易感到沮丧",
            "我对批评很敏感"
        });
    }

    public static final String[] RESPONSE_OPTIONS = {"非常不符合", "不太符合", "一般", "比较符合", "非常符合"};

    public DatingProfile saveProfile(Long userId, String nickname, String gender, String birthday,
                                     String signature, String personalityJson, String hobbiesJson,
                                     String testAnswersJson, Boolean isPublic, MultipartFile photo) throws IOException, WriterException {
        System.out.println("开始保存名片 - userId: " + userId);
        System.out.println("上传目录: " + uploadDir);
        
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            boolean created = uploadDirFile.mkdirs();
            System.out.println("上传目录不存在，尝试创建: " + (created ? "成功" : "失败"));
        }
        
        DatingProfile profile = datingProfileRepository.findByUserId(userId).orElse(new DatingProfile());
        System.out.println("找到/创建 profile - id: " + (profile.getId() != null ? profile.getId() : "new"));
        
        profile.setUserId(userId);
        profile.setNickname(nickname);
        profile.setGender(gender);
        profile.setBirthday(birthday);
        profile.setAge(calculateAgeFromBirthday(birthday));
        profile.setSignature(signature);
        profile.setPersonality(personalityJson);
        profile.setHobbies(hobbiesJson);
        profile.setTestAnswers(testAnswersJson);
        profile.setIsPublic(isPublic != null ? isPublic : false);

        if (photo != null && !photo.isEmpty()) {
            System.out.println("准备保存图片");
            String savedName = saveFile(photo);
            profile.setPhoto("/uploads/" + savedName);
            System.out.println("图片保存成功: " + profile.getPhoto());
        } else {
            System.out.println("没有上传图片");
        }

        if (profile.getMatchCode() == null || profile.getMatchCode().isEmpty()) {
            profile.setMatchCode(generateMatchCode());
            System.out.println("生成匹配码: " + profile.getMatchCode());
        }

        String qrContent = "http://localhost:8080/dating-detail.html?code=" + profile.getMatchCode();
        String qrTitle = nickname;
        String logoPath = profile.getPhoto();
        System.out.println("准备生成二维码 - content: " + qrContent + ", logo: " + logoPath);
        
        String qrSavedName = QRCodeGenerator.generateQRCodeImageWithTitleAndLogo(qrContent, qrTitle, uploadDir, logoPath);
        profile.setQrCodeUrl("/uploads/" + qrSavedName);
        System.out.println("二维码生成成功: " + profile.getQrCodeUrl());

        System.out.println("准备保存到数据库");
        DatingProfile savedProfile = datingProfileRepository.save(profile);
        System.out.println("保存成功 - profileId: " + savedProfile.getId());
        
        return savedProfile;
    }
    
    private Integer calculateAgeFromBirthday(String birthday) {
        if (birthday == null || birthday.isEmpty()) {
            return null;
        }
        try {
            String[] parts = birthday.split("-");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                
                java.time.LocalDate birthDate = java.time.LocalDate.of(year, month, day);
                java.time.LocalDate now = java.time.LocalDate.now();
                java.time.Period period = java.time.Period.between(birthDate, now);
                return period.getYears();
            }
        } catch (Exception e) {
            System.out.println("计算年龄失败: " + e.getMessage());
        }
        return null;
    }
    
    private String generateMatchCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }
    
    private String saveFile(MultipartFile file) throws IOException {
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("创建上传目录: " + dir.getAbsolutePath());
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            originalName = "file";
        }
        String suffix = "";
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex > 0) {
            suffix = originalName.substring(dotIndex);
        }
        String newName = UUID.randomUUID().toString() + suffix;
        Path path = Paths.get(uploadDir, newName);
        Files.write(path, file.getBytes());
        System.out.println("文件保存: " + path.toAbsolutePath());
        return newName;
    }

    public DatingProfile getProfileByUserId(Long userId) {
        return datingProfileRepository.findByUserId(userId).orElse(null);
    }

    public DatingProfile getProfileByCode(String code) {
        return datingProfileRepository.findByMatchCode(code).orElse(null);
    }

    private String buildUserInfo(DatingProfile profile) throws JsonProcessingException {
        StringBuilder sb = new StringBuilder();
        sb.append("昵称：").append(profile.getNickname()).append("\n");
        sb.append("性别：").append(profile.getGender() != null ? 
                (profile.getGender().equals("male") ? "男" : "女") : "未知").append("\n");
        sb.append("年龄：").append(profile.getAge()).append("岁\n");
        sb.append("个性签名：").append(profile.getSignature() != null ? profile.getSignature() : "无").append("\n");
        
        List<String> personality = objectMapper.readValue(profile.getPersonality(), List.class);
        sb.append("性格标签：").append(String.join("、", personality)).append("\n");
        
        List<String> hobbies = objectMapper.readValue(profile.getHobbies(), List.class);
        sb.append("兴趣爱好：").append(String.join("、", hobbies)).append("\n");
        
        // 使用新的大五人格测试分析
        List<String> answers = parseTestAnswers(profile.getTestAnswers());
        if (answers.size() == 20) {
            Map<String, Double> bigFive = calculateBigFiveScores(answers);
            sb.append("\n【大五人格分析】\n");
            sb.append("开放性：").append(String.format("%.1f", bigFive.get("开放性"))).append("/5分 - ");
            sb.append(getDimensionDesc("开放性", bigFive.get("开放性"))).append("\n");
            sb.append("尽责性：").append(String.format("%.1f", bigFive.get("尽责性"))).append("/5分 - ");
            sb.append(getDimensionDesc("尽责性", bigFive.get("尽责性"))).append("\n");
            sb.append("外向性：").append(String.format("%.1f", bigFive.get("外向性"))).append("/5分 - ");
            sb.append(getDimensionDesc("外向性", bigFive.get("外向性"))).append("\n");
            sb.append("宜人性：").append(String.format("%.1f", bigFive.get("宜人性"))).append("/5分 - ");
            sb.append(getDimensionDesc("宜人性", bigFive.get("宜人性"))).append("\n");
            sb.append("神经质：").append(String.format("%.1f", bigFive.get("神经质"))).append("/5分 - ");
            sb.append(getDimensionDesc("神经质", bigFive.get("神经质"))).append("\n");
        }
        
        return sb.toString();
    }
    
    // 计算大五人格各维度分数（1-5分）
    private Map<String, Double> calculateBigFiveScores(List<String> answers) {
        Map<String, Double> scores = new LinkedHashMap<>();
        
        // 开放性：题目0-3
        scores.put("开放性", calculateDimensionScore(answers, 0, 4));
        // 尽责性：题目4-7
        scores.put("尽责性", calculateDimensionScore(answers, 4, 8));
        // 外向性：题目8-11
        scores.put("外向性", calculateDimensionScore(answers, 8, 12));
        // 宜人性：题目12-15
        scores.put("宜人性", calculateDimensionScore(answers, 12, 16));
        // 神经质：题目16-19
        scores.put("神经质", calculateDimensionScore(answers, 16, 20));
        
        return scores;
    }
    
    // 计算单个维度分数
    private double calculateDimensionScore(List<String> answers, int start, int end) {
        double sum = 0;
        int count = 0;
        for (int i = start; i < end && i < answers.size(); i++) {
            sum += answerToScore(answers.get(i));
            count++;
        }
        return count > 0 ? sum / count : 0;
    }
    
    // 将答案转换为分数
    private double answerToScore(String answer) {
        if (answer == null) return 0;
        switch (answer) {
            case "非常不符合": return 1;
            case "不太符合": return 2;
            case "一般": return 3;
            case "比较符合": return 4;
            case "非常符合": return 5;
            default: return 3;
        }
    }
    
    // 获取维度描述
    private String getDimensionDesc(String dimension, Double score) {
        if (score >= 4.5) {
            return dimension.equals("神经质") ? "极度敏感" : "非常突出";
        } else if (score >= 3.5) {
            return dimension.equals("神经质") ? "比较敏感" : "比较突出";
        } else if (score >= 2.5) {
            return dimension.equals("神经质") ? "情绪稳定" : "中等水平";
        } else if (score >= 1.5) {
            return dimension.equals("神经质") ? "非常稳定" : "相对内敛";
        } else {
            return dimension.equals("神经质") ? "极其稳定" : "非常内敛";
        }
    }

    private List<String> parseTestAnswers(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String analyzePersonality(List<String> answers) {
        if (answers.size() < 20) return "性格特点待分析（请完成全部20题测试）";
        
        Map<String, Double> bigFive = calculateBigFiveScores(answers);
        StringBuilder traits = new StringBuilder();
        
        // 开放性分析
        double openness = bigFive.get("开放性");
        if (openness >= 4) traits.append("富有想象力和创造力、");
        else if (openness >= 3) traits.append("对新事物保持开放态度、");
        else traits.append("务实稳重、");
        
        // 尽责性分析
        double conscientiousness = bigFive.get("尽责性");
        if (conscientiousness >= 4) traits.append("做事有条理有计划、");
        else if (conscientiousness >= 3) traits.append("有责任心、");
        else traits.append("随性自在、");
        
        // 外向性分析
        double extraversion = bigFive.get("外向性");
        if (extraversion >= 4) traits.append("开朗外向善于社交、");
        else if (extraversion >= 3) traits.append("社交型、");
        else traits.append("内敛沉静、");
        
        // 宜人性分析
        double agreeableness = bigFive.get("宜人性");
        if (agreeableness >= 4) traits.append("善解人意乐于助人、");
        else if (agreeableness >= 3) traits.append("友善温和、");
        else traits.append("独立自主、");
        
        // 神经质分析（反向，越低越稳定）
        double neuroticism = bigFive.get("神经质");
        if (neuroticism >= 4) traits.append("情感丰富敏感");
        else if (neuroticism >= 3) traits.append("情感波动一般");
        else traits.append("情绪稳定心态平和");
        
        return traits.toString();
    }

    private List<String> extractPersonalityTraits(List<String> answers) {
        List<String> traits = new ArrayList<>();
        
        if (answers.size() >= 20) {
            Map<String, Double> bigFive = calculateBigFiveScores(answers);
            
            // 开放性
            double openness = bigFive.get("开放性");
            if (openness >= 4) traits.add("富有创造力");
            else if (openness >= 3) traits.add("思维开放");
            else traits.add("务实保守");
            
            // 尽责性
            double conscientiousness = bigFive.get("尽责性");
            if (conscientiousness >= 4) traits.add("有条理");
            else if (conscientiousness >= 3) traits.add("有责任心");
            else traits.add("随性自由");
            
            // 外向性
            double extraversion = bigFive.get("外向性");
            if (extraversion >= 4) traits.add("开朗外向");
            else if (extraversion >= 3) traits.add("善于交际");
            else traits.add("内敛沉静");
            
            // 宜人性
            double agreeableness = bigFive.get("宜人性");
            if (agreeableness >= 4) traits.add("友善热情");
            else if (agreeableness >= 3) traits.add("温和友善");
            else traits.add("独立自主");
            
            // 神经质（反向）
            double neuroticism = bigFive.get("神经质");
            if (neuroticism <= 2) traits.add("情绪稳定");
            else if (neuroticism <= 3) traits.add("心态平和");
            else traits.add("情感丰富");
        }
        
        return traits;
    }

    private String determineRelationByAnswers(List<String> uAnswers, List<String> tAnswers,
                                             List<String> uPersonality, List<String> tPersonality,
                                             int uAge, int tAge, String uGender, String tGender) {
        if (uAnswers.size() < 20 || tAnswers.size() < 20) {
            return "朋友";
        }
        
        // 计算双方的大五人格分数
        Map<String, Double> uBigFive = calculateBigFiveScores(uAnswers);
        Map<String, Double> tBigFive = calculateBigFiveScores(tAnswers);
        
        // 计算各维度的相似度和互补度
        double opennessDiff = Math.abs(uBigFive.get("开放性") - tBigFive.get("开放性"));
        double conscientiousnessDiff = Math.abs(uBigFive.get("尽责性") - tBigFive.get("尽责性"));
        double extraversionDiff = Math.abs(uBigFive.get("外向性") - tBigFive.get("外向性"));
        double agreeablenessDiff = Math.abs(uBigFive.get("宜人性") - tBigFive.get("宜人性"));
        double neuroticismDiff = Math.abs(uBigFive.get("神经质") - tBigFive.get("神经质"));
        
        double totalDiff = opennessDiff + conscientiousnessDiff + extraversionDiff + agreeablenessDiff + neuroticismDiff;
        double avgDiff = totalDiff / 5.0;
        
        // 判断外向性互补（一个高外向一个低外向是互补）
        boolean uExtrovert = uBigFive.get("外向性") >= 3.5;
        boolean tExtrovert = tBigFive.get("外向性") >= 3.5;
        boolean extrovertComplement = (uExtrovert && !tExtrovert) || (!uExtrovert && tExtrovert);
        
        // 判断尽责性相似度
        boolean similarConscientious = conscientiousnessDiff <= 0.8;
        
        // 判断宜人性相似度
        boolean similarAgreeableness = agreeablenessDiff <= 0.8;
        
        int ageDiff = Math.abs(uAge - tAge);
        boolean sameGender = uGender.equals(tGender);
        
        Set<String> commonPersonality = new HashSet<>(uPersonality);
        commonPersonality.retainAll(tPersonality);
        
        // 灵魂伴侣：性格高度相似（平均差异<=0.5）且外向性相似
        if (avgDiff <= 0.5 && !extrovertComplement && similarAgreeableness) {
            return "灵魂伴侣";
        }
        
        // 恋人：性格互补（外向性互补） + 宜人性相似 + 不同性别 + 年龄相近
        if (extrovertComplement && similarAgreeableness && !sameGender && ageDiff <= 10) {
            return "恋人";
        }
        
        // 闺蜜/兄弟：性格相似（外向性相似）+ 同性别 + 年龄相近
        if (!extrovertComplement && sameGender && ageDiff <= 8 && commonPersonality.size() >= 3) {
            if (uGender.equals("female")) return "闺蜜";
            return "兄弟";
        }
        
        // 知己：性格相似但外向性不同 + 不同性别 + 年龄相近
        if (!extrovertComplement && !sameGender && ageDiff <= 8) {
            return "知己";
        }
        
        // 死党/合伙人：性格互补 + 同性别
        if (extrovertComplement && sameGender && avgDiff >= 0.6) {
            if (ageDiff <= 5) return "死党";
            return "合伙人";
        }
        
        // 姐弟/兄妹：年龄差距5-12岁 + 不同性别
        if (ageDiff >= 5 && ageDiff < 12 && !sameGender) {
            if (uAge > tAge) {
                if (uGender.equals("male")) return "兄妹";
                return "姐弟";
            } else {
                if (uGender.equals("male")) return "姐弟";
                return "兄妹";
            }
        }
        
        // 师徒：年龄差距10-25岁 + 年长者更外向更开放
        if (ageDiff >= 10 && ageDiff <= 25) {
            if (uAge > tAge && uBigFive.get("外向性") > tBigFive.get("外向性") + 0.5) {
                return "师徒";
            }
            if (tAge > uAge && tBigFive.get("外向性") > uBigFive.get("外向性") + 0.5) {
                return "师徒";
            }
        }
        
        // 师生：年龄差距10-25岁
        if (ageDiff >= 10 && ageDiff <= 25) {
            return "师生";
        }
        
        // 君臣：宜人性差异大 + 一方非常外向一方非常内敛
        if (agreeablenessDiff >= 1.5 && extrovertComplement) {
            return "君臣";
        }
        
        // 良师益友：年龄差距5-10岁
        if (ageDiff >= 5 && ageDiff < 10) {
            return "良师益友";
        }
        
        // 普通朋友
        return "普通朋友";
    }

    public Map<String, Object> analyzeBigFivePersonality(String testAnswersJson) {
        Map<String, Object> result = new HashMap<>();
        List<Integer> scores = parseBigFiveAnswers(testAnswersJson);
        
        if (scores.isEmpty()) {
            result.put("openness", 0);
            result.put("conscientiousness", 0);
            result.put("extraversion", 0);
            result.put("agreeableness", 0);
            result.put("neuroticism", 0);
            result.put("summary", "请先完成性格测试");
            return result;
        }
        
        int openness = calculateDimensionScore(scores, 0);
        int conscientiousness = calculateDimensionScore(scores, 4);
        int extraversion = calculateDimensionScore(scores, 8);
        int agreeableness = calculateDimensionScore(scores, 12);
        int neuroticism = calculateDimensionScore(scores, 16);
        
        result.put("openness", openness);
        result.put("conscientiousness", conscientiousness);
        result.put("extraversion", extraversion);
        result.put("agreeableness", agreeableness);
        result.put("neuroticism", neuroticism);
        result.put("summary", generatePersonalitySummary(openness, conscientiousness, extraversion, agreeableness, neuroticism));
        
        return result;
    }

    private List<Integer> parseBigFiveAnswers(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private int calculateDimensionScore(List<Integer> scores, int startIndex) {
        int sum = 0;
        int count = 0;
        for (int i = startIndex; i < startIndex + 4 && i < scores.size(); i++) {
            sum += scores.get(i);
            count++;
        }
        return count > 0 ? Math.round((float) sum / count) : 0;
    }

    private String generatePersonalitySummary(int o, int c, int e, int a, int n) {
        StringBuilder sb = new StringBuilder();
        
        if (o >= 4) sb.append("富有创造力和好奇心，");
        else if (o >= 3) sb.append("乐于接受新事物，");
        else sb.append("喜欢熟悉的环境，");
        
        if (c >= 4) sb.append("非常有条理和可靠，");
        else if (c >= 3) sb.append("做事认真负责，");
        else sb.append("比较随性自在，");
        
        if (e >= 4) sb.append("外向开朗，善于社交，");
        else if (e >= 3) sb.append("善于平衡社交与独处，");
        else sb.append("更享受独处时光，");
        
        if (a >= 4) sb.append("非常友善和乐于助人，");
        else if (a >= 3) sb.append("容易相处，");
        else sb.append("比较独立，");
        
        if (n >= 4) sb.append("情感丰富但可能比较敏感");
        else if (n >= 3) sb.append("情绪较为稳定");
        else sb.append("心态平和，抗压能力强");
        
        return sb.toString();
    }

    public Map<String, Object> calculateMatchScore(Long userId, String targetCode) throws Exception {
        DatingProfile userProfile = datingProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("请先创建交友名片"));
        DatingProfile targetProfile = datingProfileRepository.findByMatchCode(targetCode)
                .orElseThrow(() -> new RuntimeException("未找到匹配对象"));
        
        Map<String, Object> result = new HashMap<>();
        
        Map<String, Object> userBigFive = analyzeBigFivePersonality(userProfile.getTestAnswers());
        Map<String, Object> targetBigFive = analyzeBigFivePersonality(targetProfile.getTestAnswers());
        
        double personalityCompatibility = calculatePersonalityCompatibility(userBigFive, targetBigFive);
        double interestSimilarity = calculateInterestSimilarity(userProfile.getHobbies(), targetProfile.getHobbies());
        double ageFactor = calculateAgeFactor(userProfile.getAge(), targetProfile.getAge());
        double birthdayFactor = calculateBirthdayFactor(userProfile.getBirthday(), targetProfile.getBirthday());
        
        double overallScore = personalityCompatibility * 0.35 + interestSimilarity * 0.35 + ageFactor * 0.15 + birthdayFactor * 0.15;
        
        List<String> commonFeatures = findCommonFeatures(userProfile, targetProfile, userBigFive, targetBigFive);
        List<String> complementaryFeatures = findComplementaryFeatures(userBigFive, targetBigFive);
        addBirthdayFeatures(userProfile, targetProfile, commonFeatures, complementaryFeatures);
        
        Map<String, Object> aiAnalysis = aiService.analyzeMatchWithRelation(userProfile, targetProfile, 
                (int) Math.round(overallScore), commonFeatures, complementaryFeatures);
        
        String relation = (String) aiAnalysis.get("relation");
        String analysis = (String) aiAnalysis.get("analysis");
        
        int finalScore = (int) Math.min(Math.round(overallScore) + 50, 100);
        result.put("overallScore", finalScore);
        result.put("relation", relation);
        result.put("personalityCompatibility", Math.round(personalityCompatibility));
        result.put("interestSimilarity", Math.round(interestSimilarity));
        result.put("ageFactor", Math.round(ageFactor));
        result.put("birthdayFactor", Math.round(birthdayFactor));
        result.put("commonFeatures", commonFeatures);
        result.put("complementaryFeatures", complementaryFeatures);
        result.put("analysis", analysis);
        result.put("targetProfile", buildProfileInfo(targetProfile));
        
        return result;
    }
    
    private double calculateBirthdayFactor(String birthday1, String birthday2) {
        if (birthday1 == null || birthday2 == null || birthday1.isEmpty() || birthday2.isEmpty()) {
            return 70;
        }
        
        try {
            String[] parts1 = birthday1.split("-");
            String[] parts2 = birthday2.split("-");
            
            if (parts1.length == 3 && parts2.length == 3) {
                int month1 = Integer.parseInt(parts1[1]);
                int day1 = Integer.parseInt(parts1[2]);
                int month2 = Integer.parseInt(parts2[1]);
                int day2 = Integer.parseInt(parts2[2]);
                
                int dayOfYear1 = getDayOfYear(month1, day1);
                int dayOfYear2 = getDayOfYear(month2, day2);
                
                int diff = Math.abs(dayOfYear1 - dayOfYear2);
                if (diff > 182) {
                    diff = 365 - diff;
                }
                
                if (diff == 0) return 100;
                if (diff <= 7) return 90;
                if (diff <= 14) return 80;
                if (diff <= 30) return 70;
                if (diff <= 60) return 60;
                if (diff <= 90) return 50;
                return 40;
            }
        } catch (Exception e) {
            System.out.println("计算生日匹配失败: " + e.getMessage());
        }
        
        return 70;
    }
    
    private int getDayOfYear(int month, int day) {
        int[] daysInMonth = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int dayOfYear = 0;
        for (int i = 1; i < month; i++) {
            dayOfYear += daysInMonth[i];
        }
        dayOfYear += day;
        return dayOfYear;
    }
    
    private void addBirthdayFeatures(DatingProfile p1, DatingProfile p2, List<String> commonFeatures, List<String> complementaryFeatures) {
        if (p1.getBirthday() == null || p2.getBirthday() == null) {
            return;
        }
        
        try {
            String[] parts1 = p1.getBirthday().split("-");
            String[] parts2 = p2.getBirthday().split("-");
            
            if (parts1.length == 3 && parts2.length == 3) {
                int month1 = Integer.parseInt(parts1[1]);
                int day1 = Integer.parseInt(parts1[2]);
                int month2 = Integer.parseInt(parts2[1]);
                int day2 = Integer.parseInt(parts2[2]);
                
                int dayOfYear1 = getDayOfYear(month1, day1);
                int dayOfYear2 = getDayOfYear(month2, day2);
                
                int diff = Math.abs(dayOfYear1 - dayOfYear2);
                if (diff > 182) {
                    diff = 365 - diff;
                }
                
                String zodiac1 = getZodiacSign(month1, day1);
                String zodiac2 = getZodiacSign(month2, day2);
                
                if (month1 == month2 && day1 == day2) {
                    commonFeatures.add("同一天生日，缘分天注定！");
                } else if (diff <= 7) {
                    commonFeatures.add("生日相近，同月同日不同年");
                } else if (month1 == month2) {
                    commonFeatures.add("同一个月生日");
                }
                
                if (zodiac1.equals(zodiac2)) {
                    commonFeatures.add("相同星座：" + zodiac1);
                } else {
                    complementaryFeatures.add("星座互补：" + zodiac1 + "与" + zodiac2);
                }
            }
        } catch (Exception e) {
            System.out.println("分析生日特征失败: " + e.getMessage());
        }
    }
    
    private String getZodiacSign(int month, int day) {
        if ((month == 3 && day >= 21) || (month == 4 && day <= 19)) return "白羊座";
        if ((month == 4 && day >= 20) || (month == 5 && day <= 20)) return "金牛座";
        if ((month == 5 && day >= 21) || (month == 6 && day <= 21)) return "双子座";
        if ((month == 6 && day >= 22) || (month == 7 && day <= 22)) return "巨蟹座";
        if ((month == 7 && day >= 23) || (month == 8 && day <= 22)) return "狮子座";
        if ((month == 8 && day >= 23) || (month == 9 && day <= 22)) return "处女座";
        if ((month == 9 && day >= 23) || (month == 10 && day <= 23)) return "天秤座";
        if ((month == 10 && day >= 24) || (month == 11 && day <= 22)) return "天蝎座";
        if ((month == 11 && day >= 23) || (month == 12 && day <= 21)) return "射手座";
        if ((month == 12 && day >= 22) || (month == 1 && day <= 19)) return "摩羯座";
        if ((month == 1 && day >= 20) || (month == 2 && day <= 18)) return "水瓶座";
        return "双鱼座";
    }

    private double calculatePersonalityCompatibility(Map<String, Object> p1, Map<String, Object> p2) {
        int o1 = (Integer) p1.get("openness");
        int c1 = (Integer) p1.get("conscientiousness");
        int e1 = (Integer) p1.get("extraversion");
        int a1 = (Integer) p1.get("agreeableness");
        int n1 = (Integer) p1.get("neuroticism");
        
        int o2 = (Integer) p2.get("openness");
        int c2 = (Integer) p2.get("conscientiousness");
        int e2 = (Integer) p2.get("extraversion");
        int a2 = (Integer) p2.get("agreeableness");
        int n2 = (Integer) p2.get("neuroticism");
        
        double similarity = 0;
        
        similarity += calculateDimensionCompatibility(e1, e2, true);
        similarity += calculateDimensionCompatibility(a1, a2, true);
        similarity += calculateDimensionCompatibility(c1, c2, true);
        similarity += calculateDimensionCompatibility(o1, o2, false);
        similarity += calculateDimensionCompatibility(n1, n2, true);
        
        return (similarity / 5) * 100;
    }

    private double calculateDimensionCompatibility(int s1, int s2, boolean preferSimilar) {
        int diff = Math.abs(s1 - s2);
        
        if (preferSimilar) {
            if (diff == 0) return 1.0;
            if (diff == 1) return 0.85;
            if (diff == 2) return 0.6;
            if (diff == 3) return 0.3;
            return 0.1;
        } else {
            if (diff >= 2) return 0.9;
            if (diff == 1) return 0.7;
            return 0.5;
        }
    }

    private double calculateInterestSimilarity(String hobbies1, String hobbies2) {
        try {
            List<String> h1 = objectMapper.readValue(hobbies1, List.class);
            List<String> h2 = objectMapper.readValue(hobbies2, List.class);
            
            if (h1.isEmpty() || h2.isEmpty()) return 30;
            
            Set<String> set1 = new HashSet<>(h1);
            Set<String> set2 = new HashSet<>(h2);
            
            Set<String> intersection = new HashSet<>(set1);
            intersection.retainAll(set2);
            
            Set<String> union = new HashSet<>(set1);
            union.addAll(set2);
            
            if (union.isEmpty()) return 0;
            
            return (double) intersection.size() / union.size() * 100;
        } catch (Exception e) {
            return 30;
        }
    }

    private double calculateAgeFactor(Integer age1, Integer age2) {
        if (age1 == null || age2 == null) return 70;
        
        int diff = Math.abs(age1 - age2);
        
        if (diff <= 3) return 100;
        if (diff <= 5) return 90;
        if (diff <= 8) return 80;
        if (diff <= 12) return 70;
        if (diff <= 18) return 50;
        return 30;
    }

    private List<String> findCommonFeatures(DatingProfile p1, DatingProfile p2, 
                                            Map<String, Object> b1, Map<String, Object> b2) {
        List<String> features = new ArrayList<>();
        
        try {
            List<String> h1 = objectMapper.readValue(p1.getHobbies(), List.class);
            List<String> h2 = objectMapper.readValue(p2.getHobbies(), List.class);
            Set<String> commonHobbies = new HashSet<>(h1);
            commonHobbies.retainAll(h2);
            
            if (!commonHobbies.isEmpty()) {
                features.add("共同爱好：" + String.join("、", commonHobbies));
            }
        } catch (Exception e) {}
        
        int e1 = (Integer) b1.get("extraversion");
        int e2 = (Integer) b2.get("extraversion");
        if (e1 >= 4 && e2 >= 4) features.add("都是外向型性格，社交互动会很愉快");
        if (e1 <= 2 && e2 <= 2) features.add("都是内向型性格，能相互理解彼此的空间需求");
        
        int a1 = (Integer) b1.get("agreeableness");
        int a2 = (Integer) b2.get("agreeableness");
        if (a1 >= 4 && a2 >= 4) features.add("双方都很友善，容易建立良好关系");
        
        return features;
    }

    private List<String> findComplementaryFeatures(Map<String, Object> b1, Map<String, Object> b2) {
        List<String> features = new ArrayList<>();
        
        int e1 = (Integer) b1.get("extraversion");
        int e2 = (Integer) b2.get("extraversion");
        if (Math.abs(e1 - e2) >= 2) {
            features.add("性格互补：一方外向一方内向，相处会很平衡");
        }
        
        int c1 = (Integer) b1.get("conscientiousness");
        int c2 = (Integer) b2.get("conscientiousness");
        if (Math.abs(c1 - c2) >= 2) {
            features.add("做事风格互补：一方细致一方随性，能相互配合");
        }
        
        int n1 = (Integer) b1.get("neuroticism");
        int n2 = (Integer) b2.get("neuroticism");
        if (n1 >= 4 && n2 <= 2) {
            features.add("情感互补：一方情感丰富一方心态平和，能相互支持");
        }
        
        return features;
    }

    private Map<String, Object> buildProfileInfo(DatingProfile profile) {
        Map<String, Object> info = new HashMap<>();
        info.put("nickname", profile.getNickname());
        info.put("age", profile.getAge());
        info.put("gender", profile.getGender());
        info.put("signature", profile.getSignature());
        info.put("matchCode", profile.getMatchCode());
        return info;
    }

    public Map<String, Object> getTestQuestions() {
        Map<String, Object> result = new HashMap<>();
        result.put("questions", BIG_FIVE_QUESTIONS);
        result.put("options", RESPONSE_OPTIONS);
        result.put("dimensions", Arrays.asList("开放性", "尽责性", "外向性", "宜人性", "神经质"));
        return result;
    }

    public Map<String, Object> matchProfiles(Long userId, String targetCode) throws Exception {
        return calculateMatchScore(userId, targetCode);
    }

    private String generateAnalysisByPersonality(DatingProfile p1, DatingProfile p2, String relation,
                                                String p1Personality, String p2Personality,
                                                List<String> p1Traits, List<String> p2Traits,
                                                List<String> p1Hobbies, List<String> p2Hobbies) {
        Set<String> commonHobbies = new HashSet<>(p1Hobbies);
        commonHobbies.retainAll(p2Hobbies);
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("根据你们的测试答案分析：\n\n");
        
        sb.append(p1.getNickname()).append("的性格特点：\n");
        sb.append("  ").append(p1Personality).append("\n\n");
        
        sb.append(p2.getNickname()).append("的性格特点：\n");
        sb.append("  ").append(p2Personality).append("\n\n");
        
        if (!commonHobbies.isEmpty()) {
            sb.append("共同兴趣爱好：\n");
            sb.append("  ").append(String.join("、", commonHobbies)).append("\n\n");
        }
        
        sb.append("匹配原因分析：\n");
        
        switch (relation) {
            case "灵魂伴侣":
                sb.append("  你们在价值观、情感表达方式上高度契合，能够深刻理解彼此的内心世界。\n");
                sb.append("  无论是社交方式还是决策风格都极为相似，是难得的灵魂知己。");
                break;
            case "恋人":
                sb.append("  你们的性格互补且吸引力十足，在情感需求和生活追求上非常匹配。\n");
                sb.append("  一方的感性与另一方的理性形成完美平衡，是天生一对！");
                break;
            case "闺蜜":
                sb.append("  你们都注重情感交流，喜欢相似的社交方式，能够成为彼此最信任的倾诉对象。\n");
                sb.append("  在一起总是有聊不完的话题，分享生活中的点点滴滴。");
                break;
            case "姐妹":
                sb.append("  你们有着天然的亲切感，性格相近又各有特点。\n");
                sb.append("  会像亲姐妹一样互相支持、互相陪伴，共同成长。");
                break;
            case "兄弟":
                sb.append("  你们都是讲义气、重情义的人，有着共同的兴趣爱好和价值观。\n");
                sb.append("  能够成为彼此坚实的后盾，一起面对生活的挑战。");
                break;
            case "死党":
                sb.append("  你们都是热情开朗的人，在一起总是充满欢乐和活力。\n");
                sb.append("  是那种可以一起疯一起笑的最佳伙伴，友谊经得起时间考验！");
                break;
            case "知己":
                sb.append("  你们都是善于思考的人，能够在思想层面深度交流。\n");
                sb.append("  彼此欣赏对方的智慧和见解，是难得的知己好友。");
                break;
            case "合伙人":
                sb.append("  你们中有领导者也有执行者，能力互补，思维敏捷。\n");
                sb.append("  一起合作创业或完成项目会非常顺利，是最佳拍档！");
                break;
            case "朋友":
                sb.append("  你们有很多共同话题，相处轻松愉快。\n");
                sb.append("  虽然不是最亲密的关系，但也是非常值得珍惜的友谊。");
                break;
            case "师生":
                sb.append("  你们之间有明显的年龄差距和经验差异。\n");
                sb.append("  一方可以成为另一方的导师和引路人，传承知识和智慧。");
                break;
            case "父子":
                sb.append("  你们之间有着天然的亲情般的吸引力。\n");
                sb.append("  一方成熟稳重，另一方年轻有活力，形成良好的代际互动。");
                break;
            case "姐弟":
                sb.append("  你们之间年龄差距适中，性格上可以互相照顾和依赖。\n");
                sb.append("  形成温馨的姐弟/兄妹般的情谊，互相扶持共同成长。");
                break;
            default:
                sb.append("  每一段关系都是独特的，用心去经营，你们会发现彼此的闪光点！");
        }
        
        return sb.toString();
    }

    private int calculateMatchScore(DatingProfile p1, DatingProfile p2) throws JsonProcessingException {
        List<String> p1Personality = objectMapper.readValue(p1.getPersonality(), List.class);
        List<String> p2Personality = objectMapper.readValue(p2.getPersonality(), List.class);
        List<String> p1Hobbies = objectMapper.readValue(p1.getHobbies(), List.class);
        List<String> p2Hobbies = objectMapper.readValue(p2.getHobbies(), List.class);

        int score = 0;
        int maxScore = 100;

        Set<String> commonPersonality = new HashSet<>(p1Personality);
        commonPersonality.retainAll(p2Personality);
        int personalityScore = commonPersonality.size() * 10;
        score += Math.min(personalityScore, 40);

        Set<String> commonHobbies = new HashSet<>(p1Hobbies);
        commonHobbies.retainAll(p2Hobbies);
        int hobbyScore = commonHobbies.size() * 8;
        score += Math.min(hobbyScore, 30);

        int ageDiff = Math.abs(p1.getAge() - p2.getAge());
        if (ageDiff <= 3) score += 20;
        else if (ageDiff <= 5) score += 15;
        else if (ageDiff <= 10) score += 10;

        int personalityCompatibility = calculatePersonalityCompatibility(p1Personality, p2Personality);
        score += Math.min(personalityCompatibility, 10);

        return Math.min(score, 100);
    }

    private int calculatePersonalityCompatibility(List<String> p1, List<String> p2) {
        int compatibility = 0;
        if (p1.contains("开朗") && p2.contains("幽默")) compatibility += 5;
        if (p1.contains("稳重") && p2.contains("冷静")) compatibility += 5;
        if (p1.contains("热情") && p2.contains("随和")) compatibility += 5;
        if (p1.contains("体贴") && p2.contains("体贴")) compatibility += 5;
        return Math.min(compatibility, 10);
    }

    private String generateAnalysis(DatingProfile p1, DatingProfile p2, int score, String relation) throws JsonProcessingException {
        List<String> p1Personality = objectMapper.readValue(p1.getPersonality(), List.class);
        List<String> p2Personality = objectMapper.readValue(p2.getPersonality(), List.class);
        List<String> p1Hobbies = objectMapper.readValue(p1.getHobbies(), List.class);
        List<String> p2Hobbies = objectMapper.readValue(p2.getHobbies(), List.class);

        Set<String> commonHobbies = new HashSet<>(p1Hobbies);
        commonHobbies.retainAll(p2Hobbies);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("你们的匹配度为%d%%，是很适合%s关系的两个人！\n\n", score, relation));

        if (!commonHobbies.isEmpty()) {
            sb.append("共同爱好：");
            sb.append(String.join("、", commonHobbies));
            sb.append("\n\n");
        }

        sb.append("性格特点：\n");
        sb.append("- ").append(p1.getNickname()).append("：").append(String.join("、", p1Personality)).append("\n");
        sb.append("- ").append(p2.getNickname()).append("：").append(String.join("、", p2Personality)).append("\n\n");

        sb.append("心理学分析：\n");
        if (score >= 70) {
            sb.append("根据大数据心理学分析，你们的性格互补性很强，相处会非常融洽。");
        } else if (score >= 50) {
            sb.append("你们有一定的相似之处，需要一些时间互相了解，会成为不错的朋友。");
        } else {
            sb.append("虽然目前匹配度一般，但每个人都是独特的，不妨多交流看看！");
        }

        return sb.toString();
    }
}