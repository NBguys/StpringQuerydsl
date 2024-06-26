package spring.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import spring.querydsl.entity.Member;
import spring.querydsl.entity.QMember;
import spring.querydsl.entity.QTeam;
import spring.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static spring.querydsl.entity.QMember.*;
import static spring.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@PersistenceContext
	EntityManager em;

	JPAQueryFactory queryFactory;

	@Test
	void contextLoads() {

		Member member = new Member("memberA");
		em.persist(member);

		JPAQueryFactory query = new JPAQueryFactory(em);
		QMember qMember = QMember.member;


		Member findMember = query.selectFrom(qMember)
				.where(qMember.username.eq("memberA"))
				.fetchOne();

		assertThat(member).isEqualTo(findMember);

	}

	@BeforeEach
	void init() {
		queryFactory = new JPAQueryFactory(em);
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);
		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
	}


	@Test
	void ex1Jpql() {

		String sql = "select m from Member m where m.username = :username";
		Member result = em.createQuery(sql, Member.class)
				.setParameter("username", "member1")
				.getSingleResult();

		assertThat(result.getUsername()).isEqualTo("member1");

	}

	@Test
	void ex1Querydsl() {

		Member result = queryFactory.select(member)
				.from(member)
				.where(member.username.eq("member1"))
				.fetchOne();

		assertThat(result.getUsername()).isEqualTo("member1");

	}

	@Test
	public void search() {
		Member findMember = queryFactory
				.selectFrom(member)
				.where(member.username.eq("member1")
						.and(member.age.eq(10)))
				.fetchOne();
		assertThat(findMember.getUsername()).isEqualTo("member1");

//		member.username.eq("member1") // username = 'member1'
//		member.username.ne("member1") //username != 'member1'
//		member.username.eq("member1").not() // username != 'member1'
//		member.username.isNotNull() //이름이 is not null
//		member.age.in(10, 20) // age in (10,20)
//		member.age.notIn(10, 20) // age not in (10, 20)
//		member.age.between(10,30) //between 10, 30
//		member.age.goe(30) // age >= 30
//		member.age.gt(30) // age > 30
//		member.age.loe(30) // age <= 30
//		member.age.lt(30) // age < 30
//		member.username.like("member%") //like 검색
//		member.username.contains("member") // like ‘%member%’ 검색
//		member.username.startsWith("member") //like ‘member%’ 검색

	}

	@Test
	public void searchAndParam() {
		List<Member> result1 = queryFactory
				.selectFrom(member)
				.where(member.username.eq("member1"),
						member.age.eq(10))
				.fetch();
		assertThat(result1.size()).isEqualTo(1);

		//List
//		List<Member> fetch = queryFactory
//				.selectFrom(member)
//				.fetch();
//		//단 건
//		Member findMember1 = queryFactory
//				.selectFrom(member)
//				.fetchOne();
//		//처음 한 건 조회
//		Member findMember2 = queryFactory
//				.selectFrom(member)
//				.fetchFirst();
//		//페이징에서 사용
//		QueryResults<Member> results = queryFactory
//				.selectFrom(member)
//				.fetchResults();
//
//		//count 쿼리로 변경
//		long count = queryFactory
//				.selectFrom(member)
//				.fetchCount();
	}


	/**
	 * 회원 정렬 순서
	 * 1. 회원 나이 내림차순(desc)
	 * 2. 회원 이름 올림차순(asc)
	 * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
	 */
	@Test
	public void sort() {
		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));
		List<Member> result = queryFactory
				.selectFrom(member)
				.where(member.age.eq(100))
				.orderBy(member.age.desc(), member.username.asc().nullsLast())
				.fetch();
		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member memberNull = result.get(2);
		assertThat(member5.getUsername()).isEqualTo("member5");
		assertThat(member6.getUsername()).isEqualTo("member6");
		assertThat(memberNull.getUsername()).isNull();
	}

	@Test
	public void paging1() {
		List<Member> result = queryFactory
				.selectFrom(member)
				.orderBy(member.username.desc())
				.offset(1) //0부터 시작(zero index)
				.limit(2) //최대 2건 조회
				.fetch();
		assertThat(result.size()).isEqualTo(2);
	}

	@Test
	public void paging2() {
		QueryResults<Member> queryResults = queryFactory
				.selectFrom(member)
				.orderBy(member.username.desc())
				.offset(1)
				.limit(2)
				.fetchResults();
		assertThat(queryResults.getTotal()).isEqualTo(4);
		assertThat(queryResults.getLimit()).isEqualTo(2);
		assertThat(queryResults.getOffset()).isEqualTo(1);
		assertThat(queryResults.getResults().size()).isEqualTo(2);
	}

    /**
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */
    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }


    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {
        QMember member = QMember.member;
        QTeam team = QTeam.team;
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }


}
