package com.example.practice_querydsl.repository;

import com.example.practice_querydsl.dto.MemberSearchCondition;
import com.example.practice_querydsl.dto.MemberTeamDto;
import com.example.practice_querydsl.dto.QMemberTeamDto;
import com.example.practice_querydsl.entity.Member;
import com.example.practice_querydsl.entity.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class MemberJPARepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJPARepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(em.find(Member.class, id));
    }

    public List<Member> findAll() {
        return em.createQuery("SELECT m FROM Member m", Member.class).getResultList();
    }

    public Optional<Member> findByName(String name) {
        return em.createQuery("SELECT m FROM Member m WHERE m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultStream()
                .findAny();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(

                ))
//                .from(member)
//                .leftJoin(team)
//                .where(
//                        member.name.eq(condition.getMemberName()),
//                        team.name.eq(condition.getTeamName()),
//                        member.age.goe(condition.getAgeGoe()),
//                        member.age.loe(condition.getAgeLoe())
//                )
//                .fetch();
        return new ArrayList<>();
    }
}
