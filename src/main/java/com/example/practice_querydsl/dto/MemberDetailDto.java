package com.example.practice_querydsl.dto;

import lombok.ToString;

@ToString
public class MemberDetailDto {

    private String name;
    private Integer age;
    private String teamName;

    public MemberDetailDto(String name, Integer age, String teamName) {
        this.name = name;
        this.age = age;
        this.teamName = teamName;
    }
}
