package com.example.practice_querydsl.repository;

import com.example.practice_querydsl.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJPARepositoryTest {

    @Autowired
    EntityManager em;
    @Autowired
    MemberJPARepository repository;

    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        repository.save(member);

        Member find = repository.findById(member.getId()).get();
        assertThat(find).isEqualTo(member);

        List<Member> all = repository.findAll();
        assertThat(all).containsExactly(member);
    }


}