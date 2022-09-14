package com.github.rolandhe.file.api;

import com.github.rolandhe.file.api.entiities.OutputTransferResult;

public interface ResultResponseGenerator {
    String genNotAuthing();
    String invalidRequest();
    String exceedConcurrent();

    String tooLarge();

    String internalError();

    String forResult(OutputTransferResult outputTransferResult);

    String contextType();
}
