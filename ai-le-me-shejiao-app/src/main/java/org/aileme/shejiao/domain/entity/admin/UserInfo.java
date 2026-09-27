package org.aileme.shejiao.domain.entity.admin;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserInfo implements Serializable {
    //照片模糊化
    private String mohuAvatar;
    //学历爬虫json串
    private String xueli;

    /*身份证正面*/
    private String idCardFront;
    /*身份证反面*/
    private String idCardBehind;
    /*实名信息*/
    private String txRealNameStr;
}
