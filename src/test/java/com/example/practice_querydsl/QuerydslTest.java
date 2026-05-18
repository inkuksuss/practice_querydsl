package com.example.practice_querydsl;

import com.example.practice_querydsl.dto.MemberDetailDto;
import com.example.practice_querydsl.dto.MemberDto;
import com.example.practice_querydsl.dto.QMemberDto;
import com.example.practice_querydsl.dto.UserDto;
import com.example.practice_querydsl.entity.Member;
import com.example.practice_querydsl.entity.QMember;
import com.example.practice_querydsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
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
import org.springframework.test.annotation.Commit;

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
    void fetchJoin() {
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

    @Test
    void simpleProjection() {
        List<String> fetch = query
                .select(member.name)
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void tupleProjection() {
        List<Tuple> fetch = query
                .select(member.name, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple.get(member.name) = " + tuple.get(member.name));
            System.out.println("tuple.get(member.age) = " + tuple.get(member.age));
        }
    }

    @Test
    void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("SELECT new com.example.practice_querydsl.dto.MemberDto(member.name, member.age) FROM Member member")
                .getResultList();
        for (MemberDto dto : resultList) {
            System.out.println("dto = " + dto);
        }
    }

    @Test
    void findDtoBySetter() {
        List<MemberDto> fetch = query
                .select(Projections.bean(MemberDto.class, member.name, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoByField() {
        List<MemberDto> fetch = query
                .select(Projections.fields(MemberDto.class, member.name, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoByConstructor() {
        List<MemberDto> fetch = query
                .select(Projections.constructor(MemberDto.class, member.name, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findUserDtoByField() {
        QMember subM = new QMember("subM");
        List<UserDto> fetch = query
                .select(Projections.fields(
                        UserDto.class, member.name.as("username"),
                        ExpressionUtils.as(
                                JPAExpressions.select(subM.age.max())
                                        .from(subM), "age"
                        )))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findUserDtoByConstructor() {
        List<UserDto> fetch = query
                .select(Projections.constructor(
                        UserDto.class,
                        member.name.as("username"),
                        member.age
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> fetch = query
                .select(new QMemberDto(member.name, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findMyDto() {
        List<MemberDetailDto> fetch = query
                .select(Projections.constructor(MemberDetailDto.class, member.name, member.age, team.name))
                .from(member)
                .join(team)
                .on(team.id.eq(member.team.id))
                .fetch();

        for (MemberDetailDto memberDetailDto : fetch) {
            System.out.println("memberDetailDto = " + memberDetailDto);
        }
    }

    @Test
    void findMyDto2() {
        List<MemberDetailDto> fetch = query
                .select(Projections.constructor(MemberDetailDto.class, member.name, member.age, team.name))
                .from(team)
                .join(member)
                .on(team.id.eq(member.team.id))
                .fetch();

        for (MemberDetailDto memberDetailDto : fetch) {
            System.out.println("memberDetailDto = " + memberDetailDto);
        }
    }

    @Test
    void dynamicQuery_boolean() {
        String nameParam = "memberA";
        Integer ageParam = 10;

        List<Member> members = searchMember1(nameParam, ageParam);

        for (Member member1 : members) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    void dynamicQuery_whereParam() {
        String nameParam = "memberA";
        Integer ageParam = 10;

        List<Member> members = searchMember2(nameParam, ageParam);

        for (Member member1 : members) {
            System.out.println("member1 = " + member1);
        }
    }

    private List<Member> searchMember2(String nameParam, Integer ageParam) {
        return query
                .selectFrom(member)
                .where(nameEq(nameParam), ageEq(ageParam))
                .fetch();
    }

    private BooleanExpression nameEq(String name) {
         return name != null ? member.name.eq(name) : null;
    }

    private BooleanExpression ageEq(Integer age) {
         return age != null ? member.age.eq(age) : null;
    }

    private List<Member> searchMember1(String nameParam, Integer ageParam) {
        BooleanBuilder builder = new BooleanBuilder();
        if (nameParam != null) {
            builder.and(member.name.eq(nameParam));
        }
        if (ageParam != null) {
            builder.and(member.age.eq(ageParam));
        }

        return query
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    @Commit
    void bulkUpdate() {
        long updatedName = query
                .update(member)
                .set(member.name, "_updated")
                .where(member.age.lt(21))
                .execute();
        em.flush();
        em.clear();
    }

    @Test
    void bulkAdd() {
        long execute = query
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    void bulkDelete() {
        long execute = query
                .delete(member)
                .where(member.age.lt(21))
                .execute();
    }

    @Test
    void sqlFunc() {
        List<String> fetch = query
                .select(
                        Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.name, "member", "M")
                )
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void sqlFunc2() {
        List<String> fetch = query
                .select(member.name)
                .from(member)
                .where(member.name.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.name)
                        member.name.lower()
                ))
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

}
