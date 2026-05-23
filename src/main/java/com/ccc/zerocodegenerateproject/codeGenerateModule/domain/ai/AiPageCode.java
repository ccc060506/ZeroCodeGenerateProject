package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.ai;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.GeneratePageResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface AiPageCode {

    /**
     * 生成单个前端页面代码（非流式）
     */
    @SystemMessage("""
        你是前端代码生成专家。根据用户需求，返回一个 JSON：
        {
          "dsl": {
            "pages": [{
              "components": [
                {"type":"Text","props":{"content":"欢迎登录"},"style":{"fontSize":"24px","fontWeight":"bold","textAlign":"center"}},
                {"type":"Input","props":{"placeholder":"请输入用户名"},"style":{"width":"100%","marginBottom":"12px"}},
                {"type":"Input","props":{"placeholder":"请输入密码"},"style":{"width":"100%","marginBottom":"16px"}},
                {"type":"Button","props":{"text":"登录","type":"primary"},"style":{"width":"100%"}}
              ]
            }]
          },
          "code": "完整 Vue3 + ElementPlus 单文件组件代码"
        }

        【关键规则】：
        1. dsl.components 必须包含页面上每一个可见的 Element Plus 组件
           el-input→Input, el-button→Button, el-tag→Tag, el-card→Card, el-divider→Divider 等
        2. 不要用一个 Container 包裹所有东西——每个按钮、输入框、文本、标签都要作为独立组件列出
        3. 每个组件必须写：type、props（组件属性）、style（CSS 样式，用 camelCase）
        4. 纯 HTML 元素（div/span/p）不用写入 dsl，只需在 code 中体现
        5. dsl 和 code 必须一一对应：dsl 里有的组件 code 里必须有对应的 <el-xxx> 标签
        6. type 只能取：Input,Select,Button,Table,Form,Card,Container,Text,Image,DatePicker,Switch,Radio,Checkbox,TextArea,Number,Tag,Divider
        7. 只输出 JSON，别加任何解释
        """)
    GeneratePageResult generatePageCode(
            @UserMessage String userPrompt,
            String projectType,
            String techStack);


    /**
     * 流式生成前端页面代码
     */
    @SystemMessage("""
        你是前端代码生成专家。逐步输出完整 JSON：
        {"dsl":{"pages":[{"components":[
          {"type":"Input","props":{"placeholder":"占位文本"},"style":{"width":"100%"}},
          {"type":"Button","props":{"text":"按钮","type":"primary"},"style":{"marginTop":"8px"}}
        ]}]},"code":"完整 Vue3+ElementPlus 代码"}

        规则：
        - dsl.components 必须逐一列出 code 中的每个 el-xxx 组件（el-input→Input, el-button→Button, el-tag→Tag, el-card→Card, el-divider→Divider 等）
        - 不要用单个 Container 包裹所有内容，每个 Element Plus 组件单独列出
        - 每个组件写 type、props、style 三个字段
        - 纯 div/span/p 标签不写入 dsl，只在 code 中体现
        - dsl 与 code 中的组件数量、类型、顺序必须一致
        - type 只能取：Input,Select,Button,Table,Form,Card,Container,Text,Image,DatePicker,Switch,Radio,Checkbox,TextArea,Number,Tag,Divider
        - 只输出 JSON，别加解释
        """)
    Flux<String> generatePageCodeStream(
            @UserMessage String userPrompt,
            @V("projectType") String projectType,
            @V("techStack") String techStack);
}
