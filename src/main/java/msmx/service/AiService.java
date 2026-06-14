package msmx.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import msmx.entity.DatingProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateCardContent(String prompt) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 500);

        String systemPrompt = "你是一个专业的卡片内容创作助手。请根据用户的需求，生成适合放在二维码卡片上的内容。内容要简洁、专业、有吸引力。";

        java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        requestBody.put("messages", messages);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = objectMapper.writeValueAsString(requestBody).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        return jsonResponse.get("choices").get(0).get("message").get("content").asText();
    }

    public String generateTitleSuggestion(String keywords) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.8);
        requestBody.put("max_tokens", 200);

        String systemPrompt = "你是一个创意标题生成器。请根据用户提供的关键词，生成3个适合二维码名片使用的简洁、专业、有吸引力的标题。只返回标题，用换行分隔。";

        java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", keywords);
        messages.add(userMessage);

        requestBody.put("messages", messages);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = objectMapper.writeValueAsString(requestBody).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        return jsonResponse.get("choices").get(0).get("message").get("content").asText();
    }

    public Map<String, Object> analyzeDatingMatch(String user1Info, String user2Info) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1500);

        String systemPrompt = "你是一个专业的心理学和人际关系分析专家。请根据两个人的信息，分析他们最适合的关系类型，并给出详细分析。\n\n" +
                "测试题说明（共10题，答案为A或B）：\n" +
                "1. 社交方式：A=外向喜欢热闹聚会，B=内向喜欢安静独处\n" +
                "2. 决策方式：A=凭直觉感受，B=理性分析\n" +
                "3. 压力应对：A=找人倾诉，B=独自消化\n" +
                "4. 价值观：A=重视真诚坦率，B=欣赏聪明睿智\n" +
                "5. 生活节奏：A=喜欢变化和冒险，B=追求稳定规律\n" +
                "6. 团队角色：A=领导者和发起者，B=支持者和执行者\n" +
                "7. 冲突处理：A=坚持己见，B=寻求妥协\n" +
                "8. 未来态度：A=乐观期待，B=谨慎规划\n" +
                "9. 情绪驱动：A=情感驱动，B=逻辑导向\n" +
                "10.自我认知：A=希望被评价为温暖可靠，B=希望被评价为独立坚强\n\n" +
                "关系类型包括：灵魂伴侣、恋人、闺蜜、姐妹、兄弟、死党、知己、合伙人、朋友、师生、父子、姐弟、兄妹、师徒、君臣\n\n" +
                "注意：在分析时，不要直接使用A/B这样的符号，而是将其转换为委婉的性格描述。\n" +
                "例如：不要说\"测试答案A\"，要说\"性格外向，喜欢社交活动\"；不要说\"测试答案B\"，要说\"性格内向，喜欢安静独处\"。\n\n" +
                "请根据用户的基本信息、性格标签、兴趣爱好和测试答案，综合分析双方性格特点，并给出匹配度最高的关系类型。\n\n" +
                "请严格按照以下JSON格式返回，不要添加其他文字：\n" +
                "{\n" +
                "  \"relation\": \"关系类型（从上面选择一个）\",\n" +
                "  \"personality1\": \"用户1的性格分析（将测试答案转换为委婉描述，结合性格标签，100字左右）\",\n" +
                "  \"personality2\": \"用户2的性格分析（将测试答案转换为委婉描述，结合性格标签，100字左右）\",\n" +
                "  \"analysis\": \"匹配原因分析（结合双方的性格特点和兴趣爱好，不要出现A/B符号，200字左右）\"\n" +
                "}";

        java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", "请分析以下两个人的匹配关系：\n\n用户1信息：\n" + user1Info + "\n\n用户2信息：\n" + user2Info);
        messages.add(userMessage);

        requestBody.put("messages", messages);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = objectMapper.writeValueAsString(requestBody).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();
        
        int jsonStart = content.indexOf("{");
        int jsonEnd = content.lastIndexOf("}");
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            content = content.substring(jsonStart, jsonEnd + 1);
        }
        
        JsonNode result = objectMapper.readTree(content);
        
        Map<String, Object> map = new HashMap<>();
        map.put("relation", result.has("relation") ? result.get("relation").asText() : "朋友");
        map.put("personality1", result.has("personality1") ? result.get("personality1").asText() : "性格待分析");
        map.put("personality2", result.has("personality2") ? result.get("personality2").asText() : "性格待分析");
        map.put("analysis", result.has("analysis") ? result.get("analysis").asText() : "匹配分析待生成");
        
        return map;
    }

    public String analyzeMatch(DatingProfile userProfile, DatingProfile targetProfile, 
                               int matchScore, java.util.List<String> commonFeatures, 
                               java.util.List<String> complementaryFeatures) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 800);

        String systemPrompt = "你是一个专业的人际关系分析专家。请根据两个人的性格特点和匹配数据，给出友好、中性的分析建议。\n\n" +
                "请使用以下原则：\n" +
                "1. 不要使用'恋人'、'闺蜜'、'父子'等可能带来压力的标签\n" +
                "2. 使用中性描述如'高默契度'、'爱好重合度高'、'性格互补'、'情感支持型'\n" +
                "3. 分析要委婉、友好，让用户自己定义关系\n" +
                "4. 结合共同特征和互补特质进行分析\n" +
                "5. 语言简洁自然，像朋友聊天一样\n\n" +
                "请直接返回分析内容，不要添加额外格式。";

        java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        StringBuilder userContent = new StringBuilder();
        userContent.append("请分析以下两位用户的匹配情况：\n\n");
        userContent.append("匹配度：").append(matchScore).append("/100\n\n");
        
        userContent.append(userProfile.getNickname()).append("的信息：\n");
        userContent.append("- 年龄：").append(userProfile.getAge()).append("岁\n");
        userContent.append("- 个性签名：").append(userProfile.getSignature() != null ? userProfile.getSignature() : "无").append("\n\n");
        
        userContent.append(targetProfile.getNickname()).append("的信息：\n");
        userContent.append("- 年龄：").append(targetProfile.getAge()).append("岁\n");
        userContent.append("- 个性签名：").append(targetProfile.getSignature() != null ? targetProfile.getSignature() : "无").append("\n\n");
        
        if (!commonFeatures.isEmpty()) {
            userContent.append("共同特征：\n");
            for (String feature : commonFeatures) {
                userContent.append("- ").append(feature).append("\n");
            }
            userContent.append("\n");
        }
        
        if (!complementaryFeatures.isEmpty()) {
            userContent.append("互补特质：\n");
            for (String feature : complementaryFeatures) {
                userContent.append("- ").append(feature).append("\n");
            }
        }

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userContent.toString());
        messages.add(userMessage);

        requestBody.put("messages", messages);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = objectMapper.writeValueAsString(requestBody).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        return jsonResponse.get("choices").get(0).get("message").get("content").asText();
    }

    public Map<String, Object> analyzeMatchWithRelation(DatingProfile userProfile, DatingProfile targetProfile, 
                                                       int matchScore, List<String> commonFeatures, 
                                                       List<String> complementaryFeatures) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1500);

        String systemPrompt = "你是一个专业的心理学和人际关系分析专家。请根据两个人的大五人格测试数据，分析他们最适合的关系类型，并给出详细分析。\n\n" +
                "大五人格维度说明：\n" +
                "1. 开放性(Openness)：衡量一个人对新体验、艺术、文化和抽象概念的兴趣程度。高分者富有创造力和好奇心，低分者喜欢熟悉的环境。\n" +
                "2. 尽责性(Conscientiousness)：衡量一个人的组织能力、自律性和可靠性。高分者有条理且可靠，低分者比较随性。\n" +
                "3. 外向性(Extraversion)：衡量一个人在社交场合中的能量水平和互动倾向。高分者外向开朗善于社交，低分者更享受独处。\n" +
                "4. 宜人性(Agreeableness)：衡量一个人对他人的友善、合作和同理心程度。高分者乐于助人，低分者比较独立。\n" +
                "5. 神经质(Neuroticism)：衡量一个人情绪稳定性和抗压能力。高分者情感丰富但敏感，低分者心态平和抗压能力强。\n\n" +
                "关系类型包括：灵魂伴侣、恋人、闺蜜、姐妹、兄弟、死党、知己、合伙人、朋友、师生、父子、姐弟、兄妹、师徒、君臣\n\n" +
                "【重要原则】请遵循心理学互补吸引理论：\n" +
                "- 情侣/恋人：性格互补的人更适合做情侣（如外向配内向、感性配理性），互补能带来新鲜感和成长\n" +
                "- 灵魂伴侣/知己：性格高度相似的人更适合做知己，彼此理解深刻但可能缺乏激情\n" +
                "- 闺蜜/兄弟：性格相似的同性朋友，能相互理解和支持\n" +
                "- 合伙人：性格互补的同性朋友，能优势互补共同发展\n\n" +
                "请根据用户的基本信息、大五人格分数、性格标签和兴趣爱好，综合分析双方性格特点，并给出匹配度最高的关系类型。\n\n" +
                "请严格按照以下JSON格式返回，不要添加其他文字：\n" +
                "{\n" +
                "  \"relation\": \"关系类型（从上面选择一个）\",\n" +
                "  \"analysis\": \"匹配原因分析（结合双方的性格特点和兴趣爱好，300字左右）\"\n" +
                "}";

        java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        Map<String, Object> userBigFive = analyzeBigFiveFromProfile(userProfile);
        Map<String, Object> targetBigFive = analyzeBigFiveFromProfile(targetProfile);

        StringBuilder userContent = new StringBuilder();
        userContent.append("请分析以下两个人的匹配关系：\n\n");
        userContent.append(userProfile.getNickname()).append("的信息：\n");
        userContent.append("- 年龄：").append(userProfile.getAge()).append("岁\n");
        userContent.append("- 性别：").append(userProfile.getGender()).append("\n");
        userContent.append("- 个性签名：").append(userProfile.getSignature() != null ? userProfile.getSignature() : "无").append("\n");
        userContent.append("- 性格标签：").append(userProfile.getPersonality() != null ? userProfile.getPersonality() : "无").append("\n");
        userContent.append("- 兴趣爱好：").append(userProfile.getHobbies() != null ? userProfile.getHobbies() : "无").append("\n");
        userContent.append("- 大五人格：\n");
        userContent.append("  * 开放性：").append(userBigFive.get("openness")).append("\n");
        userContent.append("  * 尽责性：").append(userBigFive.get("conscientiousness")).append("\n");
        userContent.append("  * 外向性：").append(userBigFive.get("extraversion")).append("\n");
        userContent.append("  * 宜人性：").append(userBigFive.get("agreeableness")).append("\n");
        userContent.append("  * 神经质：").append(userBigFive.get("neuroticism")).append("\n");
        userContent.append("  * 性格描述：").append(userBigFive.get("summary")).append("\n\n");
        
        userContent.append(targetProfile.getNickname()).append("的信息：\n");
        userContent.append("- 年龄：").append(targetProfile.getAge()).append("岁\n");
        userContent.append("- 性别：").append(targetProfile.getGender()).append("\n");
        userContent.append("- 个性签名：").append(targetProfile.getSignature() != null ? targetProfile.getSignature() : "无").append("\n");
        userContent.append("- 性格标签：").append(targetProfile.getPersonality() != null ? targetProfile.getPersonality() : "无").append("\n");
        userContent.append("- 兴趣爱好：").append(targetProfile.getHobbies() != null ? targetProfile.getHobbies() : "无").append("\n");
        userContent.append("- 大五人格：\n");
        userContent.append("  * 开放性：").append(targetBigFive.get("openness")).append("\n");
        userContent.append("  * 尽责性：").append(targetBigFive.get("conscientiousness")).append("\n");
        userContent.append("  * 外向性：").append(targetBigFive.get("extraversion")).append("\n");
        userContent.append("  * 宜人性：").append(targetBigFive.get("agreeableness")).append("\n");
        userContent.append("  * 神经质：").append(targetBigFive.get("neuroticism")).append("\n");
        userContent.append("  * 性格描述：").append(targetBigFive.get("summary")).append("\n\n");

        userContent.append("匹配度：").append(matchScore).append("/100\n\n");
        
        if (!commonFeatures.isEmpty()) {
            userContent.append("共同特征：\n");
            for (String feature : commonFeatures) {
                userContent.append("- ").append(feature).append("\n");
            }
            userContent.append("\n");
        }
        
        if (!complementaryFeatures.isEmpty()) {
            userContent.append("互补特质：\n");
            for (String feature : complementaryFeatures) {
                userContent.append("- ").append(feature).append("\n");
            }
        }

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userContent.toString());
        messages.add(userMessage);

        requestBody.put("messages", messages);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = objectMapper.writeValueAsString(requestBody).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        String content = jsonResponse.get("choices").get(0).get("message").get("content").asText();
        
        int jsonStart = content.indexOf("{");
        int jsonEnd = content.lastIndexOf("}");
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            content = content.substring(jsonStart, jsonEnd + 1);
        }
        
        JsonNode result = objectMapper.readTree(content);
        
        Map<String, Object> map = new HashMap<>();
        map.put("relation", result.has("relation") ? result.get("relation").asText() : "朋友");
        map.put("analysis", result.has("analysis") ? result.get("analysis").asText() : "匹配分析待生成");
        
        return map;
    }

    private Map<String, Object> analyzeBigFiveFromProfile(DatingProfile profile) {
        Map<String, Object> result = new HashMap<>();
        String testAnswers = profile.getTestAnswers();
        
        if (testAnswers == null || testAnswers.isEmpty()) {
            result.put("openness", 0);
            result.put("conscientiousness", 0);
            result.put("extraversion", 0);
            result.put("agreeableness", 0);
            result.put("neuroticism", 0);
            result.put("summary", "性格特点待分析");
            return result;
        }
        
        try {
            List<Integer> scores = objectMapper.readValue(testAnswers, new TypeReference<List<Integer>>() {});
            
            if (scores.isEmpty()) {
                result.put("openness", 0);
                result.put("conscientiousness", 0);
                result.put("extraversion", 0);
                result.put("agreeableness", 0);
                result.put("neuroticism", 0);
                result.put("summary", "性格特点待分析");
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
            
        } catch (Exception e) {
            result.put("openness", 0);
            result.put("conscientiousness", 0);
            result.put("extraversion", 0);
            result.put("agreeableness", 0);
            result.put("neuroticism", 0);
            result.put("summary", "性格特点待分析");
        }
        
        return result;
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
        
        if (e >= 4) sb.append("外向开朗善于社交，");
        else if (e >= 3) sb.append("善于平衡社交与独处，");
        else sb.append("更享受独处时光，");
        
        if (a >= 4) sb.append("非常友善乐于助人，");
        else if (a >= 3) sb.append("容易相处，");
        else sb.append("比较独立，");
        
        if (n >= 4) sb.append("情感丰富但可能比较敏感");
        else if (n >= 3) sb.append("情绪较为稳定");
        else sb.append("心态平和抗压能力强");
        
        return sb.toString();
    }
}
