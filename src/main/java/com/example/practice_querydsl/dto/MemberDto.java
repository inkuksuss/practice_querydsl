package com.example.practice_querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberDto {

    private String name;
    private int age;

    public MemberDto() {
    }

    @QueryProjection
    public MemberDto(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
