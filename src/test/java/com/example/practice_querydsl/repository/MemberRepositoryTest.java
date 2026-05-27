package com.example.practice_querydsl.repository;

import com.example.practice_querydsl.dto.MemberSearchCondition;
import com.example.practice_querydsl.dto.MemberTeamDto;
import com.example.practice_querydsl.entity.Member;
import com.example.practice_querydsl.entity.QMember;
import com.example.practice_querydsl.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);


        Member find = memberRepository.findById(member.getId()).get();
        assertThat(find).isEqualTo(member);

        List<Member> all = memberRepository.findAll();
        assertThat(all).containsExactly(member);

        Optional<Member> member1 = memberRepository.findByName("member1");
        assertThat(member1.get().getName()).isEqualTo("member1");
    }

    @Test
    void searchTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member memberA = new Member("memberA", 10, teamA);
        Member memberB = new Member("memberB", 20, teamA);
        Member memberC = new Member("memberC", 30, teamB);
        Member memberD = new Member("memberD", 40, teamB);
        em.persist(memberA);
        em.persist(memberB);
        em.persist(memberC);
        em.persist(memberD);

        em.flush();
        em.clear();

        MemberSearchCondition cond = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by("age").ascending());

//        Page<MemberTeamDto> memberTeamDtos1 = memberRepository.searchPageSimple(cond, pageRequest);
        Page<MemberTeamDto> memberTeamDtos1 = memberRepository.searchPageComplex(cond, pageRequest);
        assertThat(memberTeamDtos1).hasSize(3);
        assertThat(memberTeamDtos1.getTotalElements()).isEqualTo(4);
    }

    @Test
    void querydslPredicatedExecutorTest() {
        QMember member = QMember.member;
        Iterable<Member> member1 = memberRepository.findAll(member.age.between(10, 40).and(member.name.eq("member1")));
        for (Member m : member1) {
            System.out.println("m = " + m);
        }
    }
}