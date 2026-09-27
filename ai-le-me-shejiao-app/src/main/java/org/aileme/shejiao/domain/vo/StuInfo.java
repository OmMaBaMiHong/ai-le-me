package org.aileme.shejiao.domain.vo;

import lombok.Data;

//@Data  // Temporarily commented out due to Maven compilation issue
public class StuInfo {
    private String name;
    /**
     * 性别： 0 为 男，1 为 女
     */
    private String gender;
    /**
     * 证件号码
     */
    private String idCard;
    /**
     * 民族
     */
    private String nation;
    /**
     * 出生日期
     */
    private String birthDay;
    /**
     * 院校
     */
    private String university;
    /**
     * 院系
     */
    private String department;
    /**
     * 专业
     */
    private String domain;
    /**
     * 层次，如本科
     */
    private String level;
    /**
     * 班级
     */
    private String sClass;
    /**
     * 学号
     */
    private String stuNum;
    /**
     * 形式
     */
    private String form;
    /**
     * 入学时间
     */
    private String entranceDate;
    /**
     * 学制
     */
    private String lenOfSchooling;
    /**
     * 类型
     */
    private String type;
    /**
     * 学籍状态
     */
    private String status;
    /**
     * 毕业时间
     */
    private String graduationDate;
    /**
     * 学历证书编号
     */
    private String certificateNum;

    // Added missing setter methods for compilation
    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public void setBirthDay(String birthDay) {
        this.birthDay = birthDay;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setSClass(String sClass) {
        this.sClass = sClass;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setStuNum(String stuNum) {
        this.stuNum = stuNum;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public void setEntranceDate(String entranceDate) {
        this.entranceDate = entranceDate;
    }

    public void setLenOfSchooling(String lenOfSchooling) {
        this.lenOfSchooling = lenOfSchooling;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setGraduationDate(String graduationDate) {
        this.graduationDate = graduationDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Add missing setter for gender
    public void setGender(String gender) {
        this.gender = gender;
    }
}