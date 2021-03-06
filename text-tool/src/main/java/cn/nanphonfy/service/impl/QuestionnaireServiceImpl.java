package cn.nanphonfy.service.impl;

import cn.nanphonfy.domain.*;
import cn.nanphonfy.service.QuestionnaireService;
import cn.nanphonfy.util.Constant;
import cn.nanphonfy.util.FastJsonUtil;
import cn.nanphonfy.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * Created by zhengshaorong on 2018/11/5.
 */
public class QuestionnaireServiceImpl implements QuestionnaireService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionnaireServiceImpl.class);
    private static final String PREFIX = "001";
    private static final String OTHER = "其他";
    private static final String OTHER_SEQ = "999";
    private static final String QUESTION_TYPE_INTERACTIVE = "1";
    private static final String QUESTION_TYPE_OTHER = "2";
    private static final String CHOICE_TYPE_SINGLE = "1";
    private static final String CHOICE_CATEGORY_INTERACTIVE = "1";
    /**
     * 健康问卷分类
     *
     * @param questions
     */
    @Override
    public List<HealthQuestionnaireClassification> parseHealthQuestionnaireClassification(List<InteractiveQuestionnaire> questions) {
        List<String> firstList = questions.stream().map(s -> s.getClassification()).distinct()
                .collect(Collectors.toList());
        List<String> secondList = questions.stream().map(s -> s.getHealthEnquirie()).distinct().collect(Collectors.toList());
        List<String> thirdList = questions.stream().map(s -> s.getLevel3Name()).distinct().collect(Collectors.toList());

        List<HealthQuestionnaireClassification> firstNewList = new ArrayList();
        List<HealthQuestionnaireClassification> seconndNewList = new ArrayList();
        List<HealthQuestionnaireClassification> ThridNewList = new ArrayList();

        int firstrow = 1;
        int choiceId = 1017;
        //遍历第一层父节点
        for (String classification : firstList) {
            //空值或null值
            if (StringUtil.isEmpty(classification)) {
                continue;
            }
            for (InteractiveQuestionnaire obj : questions) {
                int i = 1;
                //匹配到分类
                if (!StringUtil.isEmpty(obj.getClassification()) && obj.getClassification().equals(classification)) {
                    HealthQuestionnaireClassification classification1 = new HealthQuestionnaireClassification();
                    classification1.setId(StringUtil.getSequenceId(PREFIX, firstrow, -1, -1));
                    classification1.setClassification(classification);
                    classification1.setChoiceId(""+choiceId);
                    classification1.setParentId(Constant.PARENT_ID);
                    firstNewList.add(classification1);
                    choiceId++;
                    break;
                }
            }
            firstrow++;
        }

        //系统类型计数器，用于生成主键策略
        Map<String, Integer> classificationCountMap = new HashMap<>();
        //用户判断是否生成其他
        Map<String, Integer> classificationMap = new HashMap<>();
        //遍历询问告知->181129
        for (String healthEnquirie : secondList) {
            //false为还未添加其他的询问事项
            //遍历第一层 系统分类
            for (InteractiveQuestionnaire obj : questions) {
                //匹配到【健康询问告知】，取对应的parentId并对ICD10分类
                if (!StringUtil.isEmpty(obj.getHealthEnquirie()) && obj.getHealthEnquirie().equals(healthEnquirie)) {
                    //遍历系统类型
                    for (HealthQuestionnaireClassification firstObj : firstNewList) {
                        //系统类型匹配，则进入健康询问告知匹配
                        //获取相同的系统类型，抓出父ID
                        if (firstObj.getClassification().equals(obj.getClassification())) {
                            //询问告知：病种层有多层，则累加
                            String key = obj.getClassification();
                            if (classificationCountMap.containsKey(key)) {
                                classificationCountMap.put(key, classificationCountMap.get(key) + 1);
                                classificationMap.put(key, classificationMap.get(key) + 1);
                            } else {
                                classificationCountMap.put(key, 1);
                                classificationMap.put(key,1);
                            }

                            ///////////////////////////////////////////////////////////////////
                            if(classificationMap.get(key) == 1){
                                HealthQuestionnaireClassification classification2 = new HealthQuestionnaireClassification();
                                classification2.setId(StringUtil.getSequenceId(firstObj.getId(), 0, -1, -1));
                                classification2.setClassification(OTHER);
                                classification2.setChoiceId(firstObj.getChoiceId());
                                classification2.setParentId(firstObj.getId());
                                classification2.setSequence(OTHER_SEQ);
                                classification2.setInquires(obj.getHealthEnquirie());
                                seconndNewList.add(classification2);
                            }
                            ///////////////////////////////////////////////////////////////////
                            //封装
                            HealthQuestionnaireClassification classification1 = new HealthQuestionnaireClassification();
                            classification1.setId(StringUtil.getSequenceId(firstObj.getId(), classificationCountMap.get(key), -1, -1));
                            classification1.setClassification(healthEnquirie);
                            classification1.setParentId(firstObj.getId());
                            classification1.setChoiceId(firstObj.getChoiceId());
                            /*classification1.setCommonAppellation(obj.getCommonAppellation());*/
                            //顺序
                            classification1.setSequence(String.valueOf(classificationCountMap.get(key)));

                            /*String icd10 = obj.getICD10Code();
                            //转换成我们想要的ICD10
                            classification1.setICD10(icd10.substring(icd10.indexOf("[")).replace("[", "").replace("]", ""));
                            System.out.println("update uw_questionaire_cat set inquiries = '" + obj.getHealthEnquirie() + "' where id=" + classification1.getId() + ";");*/
                            seconndNewList.add(classification1);

                            choiceId++;
                            break;
                        }
                    }
                    break;
                }
            }
        }

        //系统类型计数器，用于生成主键策略
        classificationCountMap = new HashMap<>();
        /*A | AA  -> A,B
        A | AB   ->AA,AB,BA,BB->AA A +1,AB A +2....
        A | AB
        B | BA
        B | BB
        */
        //遍历病种
        for (String level3Name : thirdList) {
            //遍历第一层 系统分类
            for (InteractiveQuestionnaire obj : questions) {
                //匹配到病种，取对应的parentId并对ICD10分类
                if (!StringUtil.isEmpty(obj.getLevel3Name()) && obj.getLevel3Name().equals(level3Name)) {
                    //遍历系统类型
                    for (HealthQuestionnaireClassification secondObj : seconndNewList) {
                        //获取相同的系统类型，抓出父ID
                        if (secondObj.getClassification().equals(obj.getHealthEnquirie())) {
                            //三级病种层有多层，则累加
                            String key = obj.getHealthEnquirie();
                            if (classificationCountMap.containsKey(key)) {
                                classificationCountMap.put(key, classificationCountMap.get(key) + 1);
                            } else {
                                classificationCountMap.put(key, 1);
                            }

                            //封装
                            HealthQuestionnaireClassification classification1 = new HealthQuestionnaireClassification();
                            classification1.setId(StringUtil
                                    .getSequenceId(secondObj.getId(), classificationCountMap.get(key), -1, -1));
                            classification1.setClassification(level3Name);
                            classification1.setParentId(secondObj.getId());
                            classification1.setChoiceId(secondObj.getChoiceId());
                            classification1.setCommonAppellation(obj.getCommonAppellation());
                            //顺序
                            classification1.setSequence(String.valueOf(classificationCountMap.get(key)));

                            String icd10 = obj.getICD10Code();
                            //转换成我们想要的ICD10
                            classification1.setICD10(icd10.substring(icd10.indexOf("[")).replace("[", "").replace("]", ""));
                            System.out.println("update uw_questionaire_cat set inquiries = '" + obj.getHealthEnquirie() + "' where id=" + classification1.getId() + ";");
                            ThridNewList.add(classification1);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        List<HealthQuestionnaireClassification> healthQuestionnaireClassifications = new ArrayList<>();
        healthQuestionnaireClassifications.addAll(firstNewList);
        healthQuestionnaireClassifications.addAll(seconndNewList);
        healthQuestionnaireClassifications.addAll(ThridNewList);

        //logger.info(FastJsonUtil.toJson(healthQuestionnaireClassifications));
        return healthQuestionnaireClassifications;
    }

    @Override
    public void parseQuestionAndChoiceTable(List<InteractiveQuestionnaire> questions,List<HealthQuestionnaireClassification> healthQuestionnaireClassifications) {
        List<String> firstQuestionList = questions.stream().map(s -> s.getFirstLevelQuestion()).distinct().collect(Collectors.toList());
        List<String> secondQuestionList = questions.stream().map(s -> s.getSecondLevelQuestion()).distinct().collect(Collectors.toList());
        List<String> thirdQuestionList = questions.stream().map(s -> s.getThirdLevelQuestion()).distinct().collect(Collectors.toList());

        /////////////////////////
        //三类问题的计数器
        Map<String, Question> firstQuestionMap = new HashMap<>();
        Map<String, Question> secondQuestionMap = new HashMap<>();
        Map<String, Question> thirdQuestionMap = new HashMap<>();

        //三类问题的计数器,questionId+答案
        Map<String, Choice> firstChoiceMap = new HashMap<>();
        Map<String, Choice> secondChoiceMap = new HashMap<>();
        Map<String, Choice> thirdChoiceMap = new HashMap<>();
        /////////////////////////

        //健康问卷分类表
        Map<String,HealthQuestionnaireClassification> classificationMap = new HashMap<>();
        //问题表
        for(HealthQuestionnaireClassification classification:healthQuestionnaireClassifications){
            classificationMap.put(classification.getClassification(),classification);
        }

        //封装分类id，用该id生成一套id
        for (InteractiveQuestionnaire questionnaire : questions) {
            try {
                questionnaire.setLevel3Name(classificationMap.get(questionnaire.getLevel3Name()).getId());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        List<Question> newQuestionList = new ArrayList<>();
        List<Choice> newChoiceList = new ArrayList<>();
        int questionId = 1;
        int choiceId = 1;
        for (InteractiveQuestionnaire questionnaire : questions) {
            if (StringUtil.isEmpty(questionnaire.getFirstLevelQuestion())) {
                continue;
            }

            String questionIdStr = "";
            String levelQuestionStr = questionnaire.getFirstLevelQuestion();
            //还未保存的问题
            Question question1 = new Question();
            if (firstQuestionMap.get(levelQuestionStr) == null) {
                questionIdStr = String.valueOf(questionId);

                question1.setId(questionIdStr);
                question1.setContent(levelQuestionStr);
                question1.setParentId(null);
                question1.setClassificationId(questionnaire.getLevel3Name());

                newQuestionList.add(question1);
                questionId++;
                //第一层，需要放入map
                firstQuestionMap.put(levelQuestionStr,question1);
            } else {
                questionIdStr = firstQuestionMap.get(levelQuestionStr).getId();
            }

            String choiceIdStr = "";
            //questionId+答案，确定唯一性s
            String key = questionIdStr + "_" + questionnaire.getFirstLevelAnswer();
            Choice choice1 = new Choice();
            if (firstChoiceMap.get(key) == null) {
                choice1.setId(String.valueOf(choiceId));
                choice1.setQuestionId(questionIdStr);
                choice1.setContent(questionnaire.getFirstLevelAnswer());
                choice1.setIsValid("Y");
                newChoiceList.add(choice1);
                choiceId++;
                //第一层，需要放入map
                firstChoiceMap.put(key,choice1);
            }else {
                choice1 = firstChoiceMap.get(key);
            }
            ////////////////////////////////////////////
            if (StringUtil.isEmpty(questionnaire.getSecondLevelQuestion())) {
                continue;
            }
            questionIdStr = "";
            levelQuestionStr = questionnaire.getSecondLevelQuestion();
            //还未保存的问题
            Question question2 = new Question();
            if(secondQuestionMap.get(levelQuestionStr) == null){
                questionIdStr = String.valueOf(questionId);
                question2.setId(questionIdStr);
                question2.setContent(levelQuestionStr);
                question2.setParentId(question1.getId());
                question2.setClassificationId(questionnaire.getLevel3Name());

                newQuestionList.add(question2);
                questionId++;
                //第二层，也需要放入map
                secondQuestionMap.put(levelQuestionStr,question2);
            }else {
                questionIdStr = secondQuestionMap.get(levelQuestionStr).getId();
            }
            //questionId+答案，确定唯一性s
            key = questionIdStr + "_" + questionnaire.getSecondLevelAnswer();
            Choice choice2 = new Choice();
            if (secondChoiceMap.get(key) == null) {
                choice2.setId(String.valueOf(choiceId));
                choice2.setQuestionId(questionIdStr);
                choice2.setContent(questionnaire.getSecondLevelAnswer());
                //父id
                choice2.setParentId(choice1.getId());
                choice2.setIsValid("Y");
                newChoiceList.add(choice2);
                choiceId++;
                //第二层，需要放入map
                secondChoiceMap.put(key,choice2);
            }else {
                choice2 = secondChoiceMap.get(key);
            }
            ////////////////////////////////////////////
            if (StringUtil.isEmpty(questionnaire.getThirdLevelQuestion())) {
                continue;
            }
            questionIdStr = "";
            levelQuestionStr = questionnaire.getThirdLevelQuestion();
            //还未保存的问题
            Question question3 = new Question();
            if(thirdQuestionMap.get(levelQuestionStr) == null ){
                questionIdStr = String.valueOf(questionId);
                question3.setId(questionIdStr);
                question3.setContent(levelQuestionStr);
                question3.setParentId(question2.getId());
                question3.setClassificationId(questionnaire.getLevel3Name());

                newQuestionList.add(question3);
                questionId++;
                //第三层，也需要放入map
                thirdQuestionMap.put(levelQuestionStr,question3);
                //第一层，需要放入map
                firstChoiceMap.put(key,choice1);
            }else {
                questionIdStr = thirdQuestionMap.get(levelQuestionStr).getId();
            }

            //questionId+答案，确定唯一性s
            key = questionIdStr + "_" + questionnaire.getSecondLevelAnswer();
            Choice choice3 = new Choice();
            if (thirdChoiceMap.get(key) == null) {
                choice3.setId(String.valueOf(choiceId));
                choice3.setQuestionId(questionIdStr);
                choice3.setContent(questionnaire.getThirdLevelAnswer());
                //父id
                choice3.setParentId(choice2.getId());
                choice3.setIsValid("Y");
                newChoiceList.add(choice3);
                choiceId++;
                //第三层，需要放入map
                thirdChoiceMap.put(key,choice3);
            }
        }
//        System.out.println(FastJsonUtil.toJson(questions));
        System.out.println(FastJsonUtil.toJson(newQuestionList));
        System.out.println(FastJsonUtil.toJson(newChoiceList));
    }

    @Override
    public void parseQuestionAndChoiceTableNew(List<InteractiveQuestionnaire> questions, List<HealthQuestionnaireClassification> healthQuestionnaireClassifications, List<Question> newQuestionList, List<Choice> newChoiceList) {
        //三类问题的计数器
        Map<String, Question> firstQuestionMap = new HashMap<>();
        Map<String, Question> secondQuestionMap = new HashMap<>();
        Map<String, Question> thirdQuestionMap = new HashMap<>();

        //三类问题的计数器,questionId+答案
        Map<String, Choice> firstChoiceMap = new HashMap<>();
        Map<String, Choice> secondChoiceMap = new HashMap<>();
        Map<String, Choice> thirdChoiceMap = new HashMap<>();
        /////////////////////////

        //健康问卷分类表
        Map<String,HealthQuestionnaireClassification> classificationMap = new HashMap<>();
        //问题表
        for(HealthQuestionnaireClassification classification:healthQuestionnaireClassifications){
            classificationMap.put(classification.getClassification(),classification);
        }

        //封装分类id，用该id生成一套id
        for (InteractiveQuestionnaire questionnaire : questions) {
            try {
                questionnaire.setLevel3Name(classificationMap.get(questionnaire.getLevel3Name()).getId());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        int questionId = 1;
        int choiceId = 1;
        for (InteractiveQuestionnaire questionnaire : questions) {
            if (StringUtil.isEmpty(questionnaire.getFirstLevelQuestion())) {
                continue;
            }

            //防止相同病种的问题一样 181130
            String levelQuestionStr = questionnaire.getFirstLevelQuestion() + "_" + questionnaire.getLevel3Name();
            String parentId=null;
            String sequence = "1";
            //还未保存的问题
            Question question1 = new Question();
            boolean flag = judgeLevelQuestionMap(question1, newQuestionList, firstQuestionMap, questionnaire,levelQuestionStr, String.valueOf(questionId), parentId,sequence, QUESTION_TYPE_INTERACTIVE);
            if (flag == true) {
                questionId++;
            }

            String choiceAnswer = questionnaire.getFirstLevelAnswer();
            //questionId+答案，确定唯一性s
            String key = question1.getId() + "_" + questionnaire.getFirstLevelAnswer();
            Choice choice1 = new Choice();
            flag = judgeLevelChoiceMap(choice1, newChoiceList, firstChoiceMap, questionnaire,key, String.valueOf(choiceId),choiceAnswer,question1.getId(), null);
            if (flag == true) {
                choiceId++;
            }
            ////////////////////////////////////////////
            if (StringUtil.isEmpty(questionnaire.getSecondLevelQuestion())) {
                continue;
            }
            //防止相同病种的问题一样 181130
            levelQuestionStr = questionnaire.getSecondLevelQuestion() + "_" + questionnaire.getLevel3Name();
            parentId=question1.getId();
            sequence = "2";
            //还未保存的问题
            Question question2 = new Question();
            flag = judgeLevelQuestionMap(question2, newQuestionList, secondQuestionMap, questionnaire,levelQuestionStr, String.valueOf(questionId), parentId, sequence,QUESTION_TYPE_INTERACTIVE);
            if (flag == true) {
                questionId++;
            }

            choiceAnswer = questionnaire.getSecondLevelAnswer();
            //questionId+答案，确定唯一性s
            key = question2.getId() + "_" + questionnaire.getSecondLevelAnswer();
            Choice choice2 = new Choice();
            flag = judgeLevelChoiceMap(choice2, newChoiceList, secondChoiceMap, questionnaire,key, String.valueOf(choiceId),choiceAnswer,question2.getId(), choice1.getId());
            if (flag == true) {
                choiceId++;
            }
            ////////////////////////////////////////////
            if (StringUtil.isEmpty(questionnaire.getThirdLevelQuestion())) {
                continue;
            }
            //防止相同病种的问题一样 181130
            levelQuestionStr = questionnaire.getThirdLevelQuestion()+ "_" + questionnaire.getLevel3Name();
            parentId=question2.getId();
            sequence = "3";
            //还未保存的问题
            Question question3 = new Question();
            flag = judgeLevelQuestionMap(question3, newQuestionList, thirdQuestionMap, questionnaire,levelQuestionStr, String.valueOf(questionId), parentId, sequence, QUESTION_TYPE_INTERACTIVE);
            if (flag == true) {
                questionId++;
            }

            choiceAnswer = questionnaire.getThirdLevelAnswer();
            //questionId+答案，确定唯一性s
            key = question3.getId() + "_" + questionnaire.getThirdLevelAnswer();
            Choice choice3 = new Choice();
            flag = judgeLevelChoiceMap(choice3, newChoiceList, thirdChoiceMap, questionnaire,key, String.valueOf(choiceId),choiceAnswer,question3.getId(), choice2.getId());
            if (flag == true) {
                choiceId++;
            }
        }
    }

    @Override
    public void parseMedicalInsuranceRuleTable(List<InteractiveQuestionnaire> questionnaires, List<Question> newQuestionList, List<Choice> newChoiceList, List<MedicalInsuranceRule> newMedicalInsuranceRuleList, List<HealthQuestionnaireClassification> healthQuestionnaireClassifications) {
        //健康问卷分类表Map
        Map<String,String> classificationMap = new HashMap<>();
        //问题表Map
        Map<String,String> questionMap = new HashMap<>();
        //选项表Map
        Map<String,String> choiceMap = new HashMap<>();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // 健康问卷分类表
        for (HealthQuestionnaireClassification classification : healthQuestionnaireClassifications) {
            if(classification.getId().length() > 7){
                classificationMap.put(classification.getClassification(), classification.getId());
            }
        }
        // 问题表
        for (Question question : newQuestionList) {
            questionMap.put(question.getContent(), question.getId());
        }
        // 选项表
        for (Choice choice : newChoiceList) {
            String key = choice.getQuestionId() + "_" + choice.getContent();
            choiceMap.put(key, choice.getId());
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //替换Excel表格为id格式
        for (InteractiveQuestionnaire questionnaire : questionnaires) {
            try {
                //questionnaire.setLevel3Name(classificationMap.get(questionnaire.getLevel3Name()));

                questionnaire.setFirstLevelQuestion(questionMap.get(questionnaire.getFirstLevelQuestion()));
                String firstKey = questionnaire.getFirstLevelQuestion() + "_" + questionnaire.getFirstLevelAnswer();
                questionnaire.setFirstLevelAnswer(choiceMap.get(firstKey));

                questionnaire.setSecondLevelQuestion(questionMap.get(questionnaire.getSecondLevelQuestion()));
                String secondKey = questionnaire.getSecondLevelQuestion() + "_" + questionnaire.getSecondLevelAnswer();
                questionnaire.setSecondLevelAnswer(choiceMap.get(secondKey));

                questionnaire.setThirdLevelQuestion(questionMap.get(questionnaire.getThirdLevelQuestion()));
                String thirdKey = questionnaire.getThirdLevelQuestion() + "_" + questionnaire.getThirdLevelAnswer();
                questionnaire.setThirdLevelAnswer(choiceMap.get(thirdKey));

            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //生成【医疗核保结果表】
        int id = 1;
        for (InteractiveQuestionnaire questionnaire : questionnaires) {
            try {
                if(StringUtil.isEmpty(questionnaire.getLevel3Name())){
                    continue;
                }
                MedicalInsuranceRule rule = new MedicalInsuranceRule();
                rule.setId(id);
                rule.setClassificationId(questionnaire.getLevel3Name());

                rule.setQuestionId1(questionnaire.getFirstLevelQuestion());
                rule.setQuestionId2(questionnaire.getSecondLevelQuestion());
                rule.setQuestionId3(questionnaire.getThirdLevelQuestion());
                rule.setChoiceId1(questionnaire.getFirstLevelAnswer());
                rule.setChoiceId2(questionnaire.getSecondLevelAnswer());
                rule.setChoiceId3(StringUtil.isEmpty(questionnaire.getThirdLevelAnswer())?null:questionnaire.getThirdLevelAnswer());
                rule.setVersion(Constant.VERSION);

                int count=0;
                if(!StringUtil.isEmpty(rule.getQuestionId1())){
                    count++;
                }
                if(!StringUtil.isEmpty(rule.getQuestionId2())){
                    count++;
                }
                if(!StringUtil.isEmpty(rule.getQuestionId3())){
                    count++;
                }
                rule.setLevelCount(count);

                rule.setRiskDepiction(questionnaire.getRiskDepiction());
                rule.setMedicalUnderwriting(questionnaire.getMedicalUnderwriting());
                rule.setSeriousHealthAdvice(null);
                rule.setLifeInsuranceAdvice(null);
                rule.setAdditionalComment(null);

                //设置code
                String resultCode = "-1";
                if(rule.getMedicalUnderwriting().contains("通过")){
                    resultCode = "0";
                }else if(rule.getMedicalUnderwriting().contains("人工核保")){
                    resultCode = "1";
                }else if(rule.getMedicalUnderwriting().contains("除外")){
                    resultCode = "2";
                }
                rule.setResultCode(resultCode);

                newMedicalInsuranceRuleList.add(rule);
                id++;
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        //logger.info(FastJsonUtil.toJson(newMedicalInsuranceRuleList));
    }

    @Override
    public void addQuestionOther(List<Question> newQuestionList, List<HealthQuestionnaireClassification> healthQuestionnaireClassifications) {
        //取id最大值
        String maxQuestionId = newQuestionList.stream().max(comparing(s->StringUtil.getInt(s.getId(),-1))).orElse(null).getId();;
        List<HealthQuestionnaireClassification> otherClassifications = healthQuestionnaireClassifications.stream().filter(s->"其他".equals(s.getClassification())).collect(Collectors.toList());

        int id = StringUtil.getInt(maxQuestionId,-1)+1;
        for(HealthQuestionnaireClassification classification:otherClassifications){
            Question q = new Question();
            q.setId(String.valueOf(id));
            q.setContent("其他");
            q.setSequence("0");
            q.setParentId("0");
            q.setClassificationId(classification.getId());
            //问题类型(普通|特殊)
            q.setType(QUESTION_TYPE_OTHER);
            //版本号
            q.setVersion(Constant.VERSION);
            newQuestionList.add(q);
            id++;
        }
    }

    /**
     * 处理多级选项
     * @param choice
     * @param newChoiceList
     * @param levelChoiceMap
     * @param questionnaire
     * @param key
     * @param choiceId
     * @param questionId
     * @param parentId
     * @return
     */
    private boolean judgeLevelChoiceMap(Choice choice, List<Choice> newChoiceList, Map<String, Choice> levelChoiceMap,InteractiveQuestionnaire questionnaire, String key, String choiceId,String choiceAnswer, String questionId, String parentId) {
        boolean flag = false;
        if (levelChoiceMap.get(key) == null) {
            //选项类型	选项内容	选项顺序	选项类别	是否必填	是否显示下层选项	父节点ID	提示术语	是否有效	版本号
            choice.setId(choiceId);
            choice.setQuestionId(questionId);
            //bug出现的地方
            choice.setContent(choiceAnswer);
            //单选
            choice.setType(CHOICE_TYPE_SINGLE);
            //交互式选项
            choice.setCategory(QUESTION_TYPE_INTERACTIVE);
            if ("是".equals(choice.getContent())) {
                choice.setSequence("1");
            } else if ("否".equals(choice.getContent())) {
                choice.setSequence("2");
            }
            choice.setParentId(parentId);
            choice.setIsValid("Y");
            newChoiceList.add(choice);
            //第n层，需要放入map
            levelChoiceMap.put(key,choice);
            flag = true;
        }else {
            Choice temp = levelChoiceMap.get(key);
            //选项ID 问题ID 选项类型 选项内容 选项顺序 选项类别 是否必填 是否显示下层选项 父节点ID 提示术语 是否有效 版本号
            choice.setId(temp.getId());
            choice.setQuestionId(temp.getQuestionId());
            choice.setContent(temp.getContent());
            choice.setParentId(temp.getParentId());
            choice.setIsValid(temp.getIsValid());
            flag = false;
        }
        return flag;
    }

    /**
     * 处理多级问题
     * @param question
     * @param newQuestionList
     * @param levelQuestionMap
     * @param questionnaire
     * @param levelQuestionStr
     * @param questionId
     * @param parentId
     * @param sequence
     * @param questionType
     * @return
     */
    private boolean judgeLevelQuestionMap(Question question, List<Question> newQuestionList,
                                          Map<String, Question> levelQuestionMap, InteractiveQuestionnaire questionnaire, String levelQuestionStr,
                                          String questionId, String parentId, String sequence, String questionType) {
        boolean flag = false;
        if (levelQuestionMap.get(levelQuestionStr) == null) {
            question.setId(questionId);
            question.setClassificationId(questionnaire.getLevel3Name());
            question.setContent(levelQuestionStr.split("_")[0]);
            question.setParentId(StringUtil.isEmpty(parentId)?"0":parentId);
            // 问题类型(普通|特殊)
            question.setType(questionType);
            // 顺序
            question.setSequence(sequence);
            // 版本号
            question.setVersion(Constant.VERSION);

            newQuestionList.add(question);
            //第n层，需要放入map
            levelQuestionMap.put(levelQuestionStr, question);
            flag = true;
        } else {
            Question temp = levelQuestionMap.get(levelQuestionStr);
            question.setId(temp.getId());
            question.setClassificationId(temp.getClassificationId());
            question.setContent(temp.getContent());
            question.setParentId(temp.getParentId());
            question.setType(temp.getType());
            question.setSequence(temp.getSequence());
            question.setVersion(temp.getVersion());
            flag = false;
        }
        return flag;
    }
}
