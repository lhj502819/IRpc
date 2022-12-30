package cn.onenine.irpc.framework.jmh.common;

import java.io.Serializable;

public class User implements Serializable {

    private static final long serialVersionUID = 5342726393385586669L;

    private Integer id;

    private String username;

    private String idCardNo;

    private String tel;

    private Integer age;

    private Integer sex;

    private Long bankNo;

    private String address;

    private String remark;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIdCardNo() {
        return idCardNo;
    }

    public void setIdCardNo(String idCardNo) {
        this.idCardNo = idCardNo;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public Long getBankNo() {
        return bankNo;
    }

    public void setBankNo(Long bankNo) {
        this.bankNo = bankNo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", idCardNo='" + idCardNo + '\'' +
                ", tel='" + tel + '\'' +
                ", age=" + age +
                ", sex=" + sex +
                ", bankNo=" + bankNo +
                ", address='" + address + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
}
