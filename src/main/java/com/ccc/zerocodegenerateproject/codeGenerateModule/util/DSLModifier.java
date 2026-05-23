package com.ccc.zerocodegenerateproject.codeGenerateModule.util;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.CodeDSL;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.ComponentDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.Operation;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.PageDSL;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class DSLModifier {

    private final ObjectMapper mapper = new ObjectMapper();

    // 存储每个会话的操作历史（用于撤销/重做）
    private final Map<String, List<Operation>> sessionHistories = new ConcurrentHashMap<>();

    /**
     * 应用一组修改操作
     */
    public CodeDSL apply(CodeDSL dsl, List<Operation> operations) {
        // 深拷贝，避免修改原始对象
        CodeDSL workingCopy = deepCopy(dsl);

        for (Operation op : operations) {
            applySingleOperation(workingCopy, op);
        }

        return workingCopy;
    }

    /**
     * 应用单个操作
     */
    private void applySingleOperation(CodeDSL dsl, Operation op) {
        String type = op.getType();

        switch (type) {
            case "update_props":
                updateProps(dsl, op);
                break;
            case "update_style":
                updateStyle(dsl, op);
                break;
            case "add_component":
                addComponent(dsl, op);
                break;
            case "delete_component":
                deleteComponent(dsl, op);
                break;
            case "move_component":
                moveComponent(dsl, op);
                break;
            case "copy_component":
                copyComponent(dsl, op);
                break;
            default:
                throw new IllegalArgumentException("不支持的操作类型: " + type);
        }
    }

    // ==================== 具体操作实现 ====================

    /**
     * 1. 修改组件属性（props）
     */
    private void updateProps(CodeDSL dsl, Operation op) {
        ComponentDSL comp = findComponent(dsl, Long.valueOf(op.getComponentId()));
        if (comp == null) {
            throw new RuntimeException("组件不存在: " + op.getComponentId());
        }

        if (comp.getProps() == null) {
            comp.setProps(new HashMap<>());
        }

        if (op.getNewProps() != null) {
            comp.getProps().putAll(op.getNewProps());
        }
    }

    /**
     * 2. 修改组件样式（style）
     */
    private void updateStyle(CodeDSL dsl, Operation op) {
        ComponentDSL comp = findComponent(dsl, Long.valueOf(op.getComponentId()));
        if (comp == null) {
            throw new RuntimeException("组件不存在: " + op.getComponentId());
        }

        if (comp.getStyle() == null) {
            comp.setStyle(new HashMap<>());
        }

        if (op.getNewStyle() != null) {
            comp.getStyle().putAll(op.getNewStyle());
        }
    }

    /**
     * 3. 添加组件
     */
    private void addComponent(CodeDSL dsl, Operation op) {
        ComponentDSL newComp = new ComponentDSL();
        newComp.setId(Long.valueOf(op.getNewComponentId()));
        newComp.setType(op.getComponentType());
        newComp.setProps(op.getProps() != null ? op.getProps() : new HashMap<>());
        newComp.setStyle(op.getStyle() != null ? op.getStyle() : new HashMap<>());
        newComp.setChildren(new ArrayList<>());
        newComp.setVisible(true);

        if (op.getParentId() != null) {
            // 添加到父组件的 children 中
            ComponentDSL parent = findComponent(dsl, Long.valueOf(op.getParentId()));
            if (parent == null) {
                throw new RuntimeException("父组件不存在: " + op.getParentId());
            }
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }

            int position = op.getPosition() != null ? op.getPosition() : parent.getChildren().size();
            parent.getChildren().add(position, newComp);

        } else {
            // 添加到页面根级别
            PageDSL page = findPage(dsl, Long.valueOf(op.getPageId()));
            if (page == null) {
                throw new RuntimeException("页面不存在: " + op.getPageId());
            }
            if (page.getComponents() == null) {
                page.setComponents(new ArrayList<>());
            }

            int position = op.getPosition() != null ? op.getPosition() : page.getComponents().size();
            page.getComponents().add(position, newComp);
        }
    }

    /**
     * 4. 删除组件（支持递归）
     */
    private void deleteComponent(CodeDSL dsl, Operation op) {
        boolean deleted = deleteComponentRecursive(dsl.getPages(), Long.valueOf(op.getComponentId()));
        if (!deleted) {
            throw new RuntimeException("删除失败，组件不存在: " + op.getComponentId());
        }
    }

    private boolean deleteComponentRecursive(List<PageDSL> pages, Long componentId) {
        for (PageDSL page : pages) {
            if (page.getComponents() != null) {
                if (deleteFromList(page.getComponents(), componentId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean deleteFromList(List<ComponentDSL> components, Long componentId) {
        if (components == null) return false;

        Iterator<ComponentDSL> it = components.iterator();
        while (it.hasNext()) {
            ComponentDSL comp = it.next();
            if (comp.getId().equals(componentId)) {
                it.remove();
                return true;
            }
            if (comp.getChildren() != null && deleteFromList(comp.getChildren(), componentId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 5. 移动组件（拖拽）
     */
    private void moveComponent(CodeDSL dsl, Operation op) {
        // 1. 先提取组件（从原位置移除）
        ComponentDSL comp = extractComponent(dsl, Long.valueOf(op.getComponentId()));
        if (comp == null) {
            throw new RuntimeException("移动失败，组件不存在: " + op.getComponentId());
        }

        // 2. 添加到新位置
        if (op.getTargetParentId() != null) {
            ComponentDSL targetParent = findComponent(dsl, Long.valueOf(op.getTargetParentId()));
            if (targetParent == null) {
                throw new RuntimeException("目标父组件不存在: " + op.getTargetParentId());
            }
            if (targetParent.getChildren() == null) {
                targetParent.setChildren(new ArrayList<>());
            }

            int position = op.getTargetPosition() != null ? op.getTargetPosition() : targetParent.getChildren().size();
            targetParent.getChildren().add(position, comp);

        } else {
            PageDSL targetPage = findPage(dsl, Long.valueOf(op.getPageId()));
            if (targetPage == null) {
                throw new RuntimeException("目标页面不存在: " + op.getPageId());
            }
            if (targetPage.getComponents() == null) {
                targetPage.setComponents(new ArrayList<>());
            }

            int position = op.getTargetPosition() != null ? op.getTargetPosition() : targetPage.getComponents().size();
            targetPage.getComponents().add(position, comp);
        }
    }

    /**
     * 6. 复制组件
     */
    private void copyComponent(CodeDSL dsl, Operation op) {
        ComponentDSL sourceComp = findComponent(dsl, Long.valueOf(op.getComponentId()));
        if (sourceComp == null) {
            throw new RuntimeException("复制失败，源组件不存在: " + op.getComponentId());
        }

        ComponentDSL newComp = deepCopyComponent(sourceComp);
        newComp.setId(Long.valueOf(op.getNewComponentId()));

        if (op.getTargetParentId() != null) {
            ComponentDSL targetParent = findComponent(dsl, Long.valueOf(op.getTargetParentId()));
            if (targetParent == null) {
                throw new RuntimeException("目标父组件不存在");
            }
            if (targetParent.getChildren() == null) {
                targetParent.setChildren(new ArrayList<>());
            }

            int position = op.getTargetPosition() != null ? op.getTargetPosition() : targetParent.getChildren().size();
            targetParent.getChildren().add(position, newComp);
        } else {
            PageDSL page = findPage(dsl, Long.valueOf(op.getPageId()));
            if (page == null) throw new RuntimeException("目标页面不存在");
            if (page.getComponents() == null) page.setComponents(new ArrayList<>());

            int position = op.getTargetPosition() != null ? op.getTargetPosition() : page.getComponents().size();
            page.getComponents().add(position, newComp);
        }
    }

    // ==================== 查找与辅助方法 ====================

    public ComponentDSL findComponent(CodeDSL dsl, Long componentId) {
        if (dsl == null || dsl.getPages() == null || componentId == null) return null;

        for (PageDSL page : dsl.getPages()) {
            ComponentDSL found = findComponentInList(page.getComponents(), componentId);
            if (found != null) return found;
        }
        return null;
    }

    private ComponentDSL findComponentInList(List<ComponentDSL> components, Long componentId) {
        if (components == null) return null;

        for (ComponentDSL comp : components) {
            if (componentId.equals(comp.getId())) {
                return comp;
            }
            ComponentDSL found = findComponentInList(comp.getChildren(), componentId);
            if (found != null) return found;
        }
        return null;
    }

    public PageDSL findPage(CodeDSL dsl, Long pageId) {
        if (dsl == null || dsl.getPages() == null || pageId == null) return null;

        return dsl.getPages().stream()
                .filter(p -> pageId.equals(p.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 提取组件（从原位置移除并返回）
     */
    private ComponentDSL extractComponent(CodeDSL dsl, Long componentId) {
        if (dsl == null || dsl.getPages() == null) return null;
        return extractFromPages(dsl.getPages(), componentId);
    }

    private ComponentDSL extractFromPages(List<PageDSL> pages, Long componentId) {
        for (PageDSL page : pages) {
            if (page.getComponents() != null) {
                ComponentDSL extracted = extractFromList(page.getComponents(), componentId);
                if (extracted != null) return extracted;
            }
        }
        return null;
    }

    private ComponentDSL extractFromList(List<ComponentDSL> components, Long componentId) {
        if (components == null) return null;

        Iterator<ComponentDSL> it = components.iterator();
        while (it.hasNext()) {
            ComponentDSL comp = it.next();
            if (comp.getId().equals(componentId)) {
                it.remove();
                return comp;
            }
            ComponentDSL extracted = extractFromList(comp.getChildren(), componentId);
            if (extracted != null) return extracted;
        }
        return null;
    }

    // ==================== 深拷贝 ====================

    private CodeDSL deepCopy(CodeDSL dsl) {
        try {
            String json = mapper.writeValueAsString(dsl);
            return mapper.readValue(json, CodeDSL.class);
        } catch (Exception e) {
            throw new RuntimeException("DSL深拷贝失败", e);
        }
    }

    private ComponentDSL deepCopyComponent(ComponentDSL comp) {
        try {
            String json = mapper.writeValueAsString(comp);
            return mapper.readValue(json, ComponentDSL.class);
        } catch (Exception e) {
            throw new RuntimeException("组件深拷贝失败", e);
        }
    }

    // ==================== 操作历史（撤销/重做） ====================

    public void recordHistory(Long sessionId, List<Operation> operations) {
        sessionHistories.computeIfAbsent(String.valueOf(sessionId), k -> new ArrayList<>()).addAll(operations);
    }

    public List<Operation> getHistory(Long sessionId) {
        return sessionHistories.getOrDefault(String.valueOf(sessionId), new ArrayList<>());
    }

    public void clearHistory(Long sessionId) {
        sessionHistories.remove(String.valueOf(sessionId));
    }
}
