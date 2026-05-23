package com.ccc.zerocodegenerateproject.common.result;

import com.ccc.zerocodegenerateproject.common.constant.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result<T> {

    // 状态码（200成功/500失败等）
    private int code;
    // 返回提示信息
    private String msg;
    // 返回数据体（泛型，支持任意类型）
    private T data;
    private Result() {}

    // 1. 成功返回（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }
    public static <T> Result<T> success(T data, String msg) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    // 2. 成功返回（无数据，仅提示）
    public static <T> Result<T> success() {
        return success(null);
    }

    // 3. 失败返回（自定义提示）
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.ERROR);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

    // 4. 失败返回（自定义状态码+提示）
    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

    // 5. 通用失败（默认提示）
    public static <T> Result<T> error() {
        return error("操作失败");
    }

}
