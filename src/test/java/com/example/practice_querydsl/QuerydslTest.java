package com.example.practice_querydsl;

import com.example.practice_querydsl.entity.Member;
import com.example.practice_querydsl.entity.QMember;
import com.example.practice_querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static com.example.practice_querydsl.entity.QMember.member;
import static com.example.practice_querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.*;

@Transactional
@SpringBootTest
public class QuerydslTest {

    @Autowired
    private EntityManager em;

    @PersistenceUnit
    EntityManagerFactory emf;

    JPAQueryFactory query;

    @BeforeEach
    public void beforeEach() {
        query = new JPAQueryFactory(em);
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
    }

    @Test
    public void startJPQL() {
        Member find = em.createQuery("SELECT m FROM Member m WHERE m.name = :name", Member.class)
                .setParameter("name", "memberA")
                .getSingleResult();

        assertThat(find.getName()).isEqualTo("memberA");
    }

    @Test
    public void startQuerydsl() {
        Member find = query
                .selectFrom(member)
                .where(member.name.eq("memberA"))
                .fetchOne();

        assertThat(find.getName()).isEqualTo("memberA");
    }

    @Test
    public void search() {
        Member findMember = query
                .selectFrom(member)
                .where(member.name.eq("memberA").and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getName()).isEqualTo("memberA");
    }

    @Test
    public void resultFetch() {
        List<Member> fetch = query
                .selectFrom(member)
                .fetch();

//        Member fetchOne = query
//                .selectFrom(member)
//                .fetchOne();

        Member fetchFirst = query
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> fetchResults = query
                .selectFrom(member)
                .fetchResults();

        long total = fetchResults.getTotal();

        long cnt = query
                .selectFrom(member)
                .fetchCount();
    }

    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> list = query.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.name.asc().nullsLast())
                .fetch();

        assertThat(list.get(0).getName()).isEqualTo("member5");
        assertThat(list.get(1).getName()).isEqualTo("member6");
        assertThat(list.get(2).getName()).isNull();
    }

    @Test
    void paging1() {
        List<Member> fetch = query
                .selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(fetch.size()).isEqualTo(2);
    }

    @Test
    void paging2() {
        QueryResults<Member> results = query
                .selectFrom(member)
                .orderBy(member.name.asc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(results.getTotal()).isEqualTo(4);
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getResults().size()).isEqualTo(2);
        assertThat(results.getResults().get(0).getName()).isEqualTo("memberB");
    }

    @Test
    void aggregation() {
        List<Tuple> fetch = query
                .select(member.count(),
                        member.age.sumLong(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = fetch.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4L);
        assertThat(tuple.get(member.age.sumLong())).isEqualTo(100L);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25.0);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    @DisplayName("팀의 이름과 각 팀의 평균")
    void group() {
        // given
        // when
        List<Tuple> fetch = query
                .select(team.name, member.age.avg())
                .from(member)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(1);
        // then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    @DisplayName("teamA")
    void join() {
        // given
        // when
        List<Member> result = query
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        // then
        assertThat(result)
                .extracting("name")
                .containsExactly("memberA", "memberB");
    }

    @Test
    @DisplayName("member name join")
    void theta_join() {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        // when
        List<Member> fetch = query
                .select(member)
                .from(member, team)
                .where(member.name.eq(team.name))
                .fetch();

        // then
        assertThat(fetch)
                .extracting("name")
                .containsExactly("teamA", "teamB");
    }

    @Test
    void join_on_filter() {
        // given
        // when
        List<Tuple> fetch = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        // then
        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void join_on_no_relation() {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // when
        List<Member> fetch = query
                .select(member)
                .from(member)
                .leftJoin(team)
                .on(team.name.eq(member.name))
                .fetch();

        // then
        for (Member fetch1 : fetch) {
            System.out.println("member = " + fetch1 + " team = " + fetch1.getTeam());
        }
    }



    @Test
    void fetchJoinNo() {
        em.flush();
        em.clear();

        Member find = query
                .select(member)
                .from(member)
                .join(member.team, team)
                .fetchJoin()
                .where(member.name.eq("memberA"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(find.getTeam());
        assertThat(loaded).isTrue();
    }

    @Test
    void subQuery() {
        QMember subMember = new QMember("sub");

        List<Member> fetch = query
                .select(member)
                .from(member)
                .where(
                        member.age.eq(
                                JPAExpressions
                                        .select(subMember.age.max())
                                        .from(subMember))
                )
                .fetch();

        assertThat(fetch.get(0).getAge()).isEqualTo(40);
    }

    @Test
    void subQueryGoe() {
        QMember subMember = new QMember("sub");

        List<Member> fetch = query
                .select(member)
                .from(member)
                .where(
                        member.age.goe(
                                JPAExpressions
                                        .select(subMember.age.avg())
                                        .from(subMember))
                )
                .fetch();

        assertThat(fetch)
                .extracting("age")
                .containsExactly(30, 40);
    }

    @Test
    void subQueryIn() {
        QMember subMember = new QMember("sub");

        List<Member> fetch = query
                .select(member)
                .from(member)
                .where(
                        member.age.in(
                                JPAExpressions
                                        .select(subMember.age)
                                        .from(subMember)
                                        .where(subMember.age.gt(10))
                ))
                .fetch();

        assertThat(fetch)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    void selectSubQuery() {
        QMember subMember = new QMember("sub");

        List<Tuple> fetch = query
                .select(member.name,
                        JPAExpressions.select(subMember.age.avg())
                                .from(subMember))
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void basicCase() {
        List<String> fetch = query
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void complexCase() {
        List<String> fetch = query
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void constant() {
        List<Tuple> fetch = query
                .select(member.name, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void concat() {
        List<String> fetch = query
                .select(member.name.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.name.eq("memberA"))
                .fetch();

        System.out.println("fetch = " + fetch);
    }
}
