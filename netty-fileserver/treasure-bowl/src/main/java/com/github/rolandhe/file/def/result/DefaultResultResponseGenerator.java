package com.github.rolandhe.file.def.result;

import com.github.rolandhe.file.api.ResultResponseGenerator;
import com.github.rolandhe.file.api.entiities.OutputTransferResult;
import com.github.rolandhe.file.api.entiities.TransferResult;
import com.github.rolandhe.file.json.JsonHelper;

public class DefaultResultResponseGenerator implements ResultResponseGenerator {
    @Override
    public String genNotAuthing() {
        ResultMessage message = new ResultMessage();
        message.setCode(401);
        message.setMessage("请求没有被授权");
        return JsonHelper.toJson(message);
    }

    @Override
    public String invalidRequest() {
        ResultMessage message = new ResultMessage();
        message.setCode(405);
        message.setMessage("请求方法必须是POST");
        return JsonHelper.toJson(message);
    }

    @Override
    public String exceedConcurrent() {
        ResultMessage message = new ResultMessage();
        message.setCode(429);
        message.setMessage("超出并发限制");
        return JsonHelper.toJson(message);
    }

    @Override
    public String tooLarge() {
        ResultMessage message = new ResultMessage();
        message.setCode(413);
        message.setMessage("文件大小超出限制");
        return JsonHelper.toJson(message);
    }

    @Override
    public String internalError() {
        ResultMessage message = new ResultMessage();
        message.setCode(500);
        message.setMessage("内部发生错误");
        return JsonHelper.toJson(message);
    }

    @Override
    public String forResult(OutputTransferResult outputTransferResult) {
        ResultMessage message = new ResultMessage();
        message.setCode(200);
        message.setData(outputTransferResult);
        return JsonHelper.toJson(message);
    }

    @Override
    public String contextType() {
        return "application/json; charset=UTF-8\"";
    }
}
