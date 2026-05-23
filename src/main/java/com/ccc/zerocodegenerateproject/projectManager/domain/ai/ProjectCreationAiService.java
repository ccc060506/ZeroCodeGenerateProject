package com.ccc.zerocodegenerateproject.projectManager.domain.ai;

import com.ccc.zerocodegenerateproject.projectManager.domain.dto.AiProjectSuggestion;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 项目创建专用 AI 服务接口
 * LangChain4j 会自动生成实现类，支持结构化输出
 */
@AiService
public interface ProjectCreationAiService {
    @SystemMessage("""
            你是专业的零代码生成平台项目创建助手。
            
                用户会输入一句话需求，你必须严格按照以下规则返回结果：
                - 只返回一个有效的 JSON 对象，不要添加任何额外文字、解释、markdown 或 ```json 标记。
                - 返回的 JSON 必须完全符合 AiProjectSuggestion 类的结构。
                - projectType 只能是：CRM、OA、管理后台、H5应用、内部工具、电商、其他
                - techStack 只能是：react_ant、vue_element、uniapp、html5、其他
                - description 只能是整个项目的描述和各个组件的如下类型属性的组合
                     String type;                    // Button, Input, Table, Form, Card
                     String name;                    // 组件业务名称--表单字段名
                     String label;                   // 显示标签（中文名）
                     Map<String, Object> props;      // 组件核心属性
                     Map<String, String> style;      // 样式配置
                     Map<String, Object> events;     // 事件绑定
                     List<ComponentDSL> children;    // 子组件（支持递归嵌套）
                     
            输出格式要求（严格遵守）：
                1. 先输出 DSL 的 JSON：{"dsl": {...}}
                2. 然后输出固定分隔符：===CODE_SEPARATOR===
                3. 然后输出原始的 Vue/React 代码（不需要包在 JSON 里）
            
                示例：
                {"dsl": {"pages": [...]}}
                ===CODE_SEPARATOR===
                <template>
                  ...
                </template>
            
            请生成一个简洁、专业、有吸引力的项目名称。
        """)
    AiProjectSuggestion createProjectSuggestion(
            @UserMessage String userPrompt);
}
