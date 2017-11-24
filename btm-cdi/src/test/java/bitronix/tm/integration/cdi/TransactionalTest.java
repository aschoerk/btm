package bitronix.tm.integration.cdi;

import com.oneandone.ejbcdiunit.EjbUnitRunner;
import org.jglue.cdiunit.AdditionalClasses;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author aschoerk
 */
@RunWith(EjbUnitRunner.class)
@AdditionalClasses({PlatformTransactionManager.class, Resources.class, TInterceptor.class})
public class TransactionalTest {

    static Logger log = LoggerFactory.getLogger("testlogger");

    @BeforeClass
    public static void loginit() {
        log.info("log");
    }


    @Inject
    TransactionManager tm;

    @Inject
    H2PersistenceFactory entityManagerFactory;

    @Before
    public void initContext() throws Exception {
        tm.begin();

        try (Connection connection = entityManagerFactory.getDatasource().getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("create table a (a varchar(200))");
                stmt.execute("create table b (a varchar(200))");
                stmt.execute("create table hibernate_sequences (sequence_name varchar(255) not null, sequence_next_hi_value bigint, primary key (sequence_name))");
                // stmt.execute("insert into hibernate_sequences (sequence_name, sequence_next_hi_value) values ('test_entity_1', 1)");
                stmt.execute("create table test_entity_1 (id bigint not null, intAttribute integer not null, stringAttribute varchar(255), primary key (id))");
            }
        }
        tm.commit();
    }

    @Test
    public void test() throws Exception {
        tm.begin();

        try (Connection connection = entityManagerFactory.getDatasource().getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("create table t (a varchar(200), b integer)");
                stmt.execute("insert into t (a, b) values ('a', 1 )");
                stmt.execute("insert into t (a, b) values ('b', 2 )");
                stmt.execute("select * from t");
                try (ResultSet result = stmt.getResultSet()) {
                    assert (result.next());
                    assert (result.next());
                    assert (!result.next());
                }
            }
        }
        tm.commit();
    }

    @Inject
    TransactionalJPABean transactionalJPABean;

    @Test
    public void testTraMethod() throws Exception {

        transactionalJPABean.insertTestEntityInNewTra();
        Assert.assertEquals(1L,transactionalJPABean.countTestEntity());
        tm.begin();
        transactionalJPABean.insertTestEntityInRequired();
        tm.rollback();
        Assert.assertEquals(1L,transactionalJPABean.countTestEntity());
        tm.begin();
        transactionalJPABean.insertTestEntityInRequired();
        transactionalJPABean.insertTestEntityInNewTra();
        tm.rollback();
        Assert.assertEquals(2L,transactionalJPABean.countTestEntity());
        tm.begin();
        transactionalJPABean.insertTestEntityInRequired();
        transactionalJPABean.insertTestEntityInNewTra();
        transactionalJPABean.insertTestEntityInNewTraAndRollback();
        tm.commit();
        Assert.assertEquals(4L,transactionalJPABean.countTestEntity());
        tm.begin();
        transactionalJPABean.insertTestEntityInRequired();
        transactionalJPABean.insertTestEntityInNewTraAndRollback();
        tm.commit();
        Assert.assertEquals(5L,transactionalJPABean.countTestEntity());
        tm.begin();
        transactionalJPABean.insertTestEntityInNewTraAndRollback();
        transactionalJPABean.insertTestEntityInRequired();
        tm.commit();
        Assert.assertEquals(6L,transactionalJPABean.countTestEntity());
        tm.begin();
        transactionalJPABean.insertTestEntityInNewTraAndRollback();
        transactionalJPABean.insertTestEntityInRequired();
        transactionalJPABean.insertTestEntityInNewTra();
        tm.rollback();
        Assert.assertEquals(7L,transactionalJPABean.countTestEntity());

        tm.begin();
        transactionalJPABean.insertTestEntityInRequired();
        transactionalJPABean.insertTestEntityInNewTra();
        transactionalJPABean.insertTestEntityInNewTraAndSetRollbackOnly();
        tm.commit();
        Assert.assertEquals(9L,transactionalJPABean.countTestEntity());
        tm.begin();
        transactionalJPABean.insertTestEntityInRequired();
        transactionalJPABean.insertTestEntityInNewTraAndSetRollbackOnly();
        tm.commit();
        Assert.assertEquals(10L,transactionalJPABean.countTestEntity());
        tm.begin();
        transactionalJPABean.insertTestEntityInNewTraAndSetRollbackOnly();
        transactionalJPABean.insertTestEntityInRequired();
        tm.commit();
        Assert.assertEquals(11L,transactionalJPABean.countTestEntity());
        tm.begin();
        transactionalJPABean.insertTestEntityInNewTraAndSetRollbackOnly();
        transactionalJPABean.insertTestEntityInRequired();
        transactionalJPABean.insertTestEntityInNewTra();
        tm.rollback();
        Assert.assertEquals(12L,transactionalJPABean.countTestEntity());

    }

}
