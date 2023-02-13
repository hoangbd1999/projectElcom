package com.elcom.metacen.comment.validation;

import com.elcom.metacen.comment.model.dto.CommentRequestDTO;
import com.elcom.metacen.utils.StringUtil;

public class CommentValidation extends AbstractValidation {

    public String ValidationComment(CommentRequestDTO commentRequestDTO) {
        if (StringUtil.isNullOrEmpty(commentRequestDTO.getContent())) {
            getMessageDes().add("Chưa nhập nội dung ghi chú!");
        }

        return !isValid() ? this.buildValidationMessage() : null;
    }
}
