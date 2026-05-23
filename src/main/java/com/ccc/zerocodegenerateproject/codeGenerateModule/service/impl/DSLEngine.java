package com.ccc.zerocodegenerateproject.codeGenerateModule.service.impl;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.CodeDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.ComponentDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.PageDSL;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class DSLEngine {

    private final ObjectMapper mapper = new ObjectMapper();

    // ====================== 1. 序列化 & 反序列化 ======================

    /**
     * JSON字符串 → CodeDSL 对象
     */
    public CodeDSL parse(String dslJson) {
        try {
            return mapper.readValue(dslJson, CodeDSL.class);
        } catch (Exception e) {
            throw new RuntimeException("DSL解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * JSON字符串 → PageDSL 对象 (新增)
     * 用于处理 PageVersion 表中的 dslJson 字段
     */
    public PageDSL parsePage(String pageJson) {
        try {
            return  mapper.readValue(pageJson, PageDSL.class);
        } catch (Exception e) {
            throw new RuntimeException("PageDSL解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * CodeDSL 对象 → JSON字符串
     */
    public String stringify(CodeDSL dsl) {
        try {
            return mapper.writeValueAsString(dsl);
        } catch (Exception e) {
            throw new RuntimeException("DSL序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * PageDSL 对象 → JSON字符串
     * （专门用于保存版本时的 dslJson）
     */
    public String stringify(PageDSL pageDSL) {
        try {
            return mapper.writeValueAsString(pageDSL);
        } catch (Exception e) {
            throw new RuntimeException("PageDSL序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 深拷贝（通过序列化+反序列化实现）
     */
    public CodeDSL deepCopy(CodeDSL dsl) {
        return parse(stringify(dsl));
    }

    // ====================== 2. 校验 ======================

    /**
     * 完整校验 CodeDSL
     */
    private void validate(CodeDSL dsl) {
        if (dsl == null) {
            throw new RuntimeException("DSL不能为空");
        }
        if (dsl.getProjectId() == null) {
            throw new RuntimeException("项目ID不能为空");
        }
        if (dsl.getPages() == null || dsl.getPages().isEmpty()) {
            throw new RuntimeException("DSL至少需要一个页面");
        }

        // 校验每个页面
        for (PageDSL page : dsl.getPages()) {
            validatePage(page);
        }
    }

    /**
     * 校验单个 PageDSL
     */
    private void validatePage(PageDSL page) {
        if (page == null) {
            throw new RuntimeException("页面不能为空");
        }
        if (page.getId() == null || page.getId().toString().isEmpty()) {
            throw new RuntimeException("页面ID不能为空");
        }
        if (page.getTitle() == null || page.getTitle().isEmpty()) {
            throw new RuntimeException("页面标题不能为空，页面ID: " + page.getId());
        }

        // 校验页面下的组件
        validateComponents(page.getComponents());
    }

    /**
     * 递归校验组件列表
     */
    private void validateComponents(List<ComponentDSL> components) {
        if (components == null || components.isEmpty()) {
            return;
        }

        for (ComponentDSL comp : components) {
            if (comp.getId() == null || comp.getId().toString().isEmpty()) {
                throw new RuntimeException("组件ID不能为空");
            }
            if (comp.getType() == null || comp.getType().isEmpty()) {
                throw new RuntimeException("组件类型不能为空，组件ID: " + comp.getId());
            }

            // 递归校验子组件
            validateComponents(comp.getChildren());
        }
    }

    /**
     * 根据组件ID直接查找组件（自动遍历所有页面和所有层级）
     */
    public ComponentDSL findComponentById(CodeDSL codeDSL, Long componentId) {
        if (codeDSL == null || codeDSL.getPages() == null || componentId == null) {
            return null;
        }

        for (PageDSL page : codeDSL.getPages()) {
            ComponentDSL found = findComponentInPage(page, componentId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 在单个页面内递归查找组件（支持 children 嵌套）
     */
    private ComponentDSL findComponentInPage(PageDSL page, Long componentId) {
        if (page == null || page.getComponents() == null) {
            return null;
        }
        return findComponentInList(page.getComponents(), componentId);
    }

    // ====================== 3. 查询方法 ======================

    /**
     * 根据页面ID查找页面
     */
    public PageDSL findPage(CodeDSL dsl, Long pageId, Integer versionNum) {
        if (dsl == null || dsl.getPages() == null || pageId == null ) {
            return null;
        }
        if(versionNum==null){
            return dsl.getPages().stream()
                    .filter(page -> pageId.equals(page.getId()))
                    .max(Comparator.comparingInt(PageDSL::getCurrentVersion))
                    .orElse(null);
        }
        else {
            return dsl.getPages().stream()
                    .filter(page -> pageId.equals(page.getId()))
                    .filter(page-> page.getCurrentVersion().equals(versionNum))
                    .findFirst()
                    .orElse(null);
        }

    }

    /**
     * 根据组件ID在整个DSL中查找组件（支持跨页面、递归子组件）
     */
    public ComponentDSL findComponent(CodeDSL dsl, Long componentId) {
        if (dsl == null || dsl.getPages() == null || componentId == null) {
            return null;
        }

        for (PageDSL page : dsl.getPages()) {
            ComponentDSL found = findComponentInList(page.getComponents(), componentId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 根据组件ID查找它所在的页面
     */
    public PageDSL findPageByComponentId(CodeDSL codeDSL, Long componentId) {
        if (codeDSL == null || codeDSL.getPages() == null || componentId == null) {
            return null;
        }

        for (PageDSL page : codeDSL.getPages()) {
            if (containsComponent(page.getComponents(), componentId)) {
                return page;
            }
        }
        return null;
    }

    /**
     * 判断某个页面下（包括所有子组件层级）是否包含指定组件ID
     */
    private boolean containsComponent(List<ComponentDSL> components, Long componentId) {
        if (components == null || components.isEmpty()) {
            return false;
        }

        for (ComponentDSL comp : components) {
            if (componentId.equals(comp.getId())) {
                return true;
            }
            // 递归检查子组件
            if (containsComponent(comp.getChildren(), componentId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 递归查找子组件
     */
    private ComponentDSL findComponentInList(List<ComponentDSL> components, Long componentId) {
        if (components == null) return null;

        for (ComponentDSL comp : components) {
            if (componentId.equals(comp.getId())) {
                return comp;
            }
            // 递归查找子组件
            ComponentDSL found = findComponentInList(comp.getChildren(), componentId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    // ====================== 4. 工具方法 ======================

    /**
     * 判断某个页面是否存在
     */
    public boolean pageExists(CodeDSL dsl, Long pageId, Integer versionNum) {
        return findPage(dsl, pageId, versionNum) != null;
    }

    /**
     * 判断某个组件是否存在
     */
    public boolean componentExists(CodeDSL dsl, Long componentId) {
        return findComponent(dsl, componentId) != null;
    }
}
