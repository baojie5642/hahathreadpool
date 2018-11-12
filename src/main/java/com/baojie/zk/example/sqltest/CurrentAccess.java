package com.baojie.zk.example.sqltest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class CurrentAccess implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CurrentAccess.class);
    private final AtomicLong state;
    private final HikariDS ds;
    private final long id;

    public CurrentAccess(HikariDS ds, long id, AtomicLong state) {
        this.state = state;
        this.ds = ds;
        this.id = id;
    }

    @Override
    public void run() {
        for (; ; ) {
            LockSupport.parkNanos(1000);
            Connection conn = ds.getConnect();
            if (null == conn) {
                LockSupport.parkNanos(1000);
                continue;
            } else {
                final String sql = "update t_flow_order set state=?,order_id=?,last_updated=? where id=?";
                List<Object[]> list = new ArrayList<>(10);
                for (int i = 0; i < 10; i++) {
                    Object[] o = content(id + 1);
                    list.add(o);
                }
                update(conn, sql, list);
            }
        }
    }

    private Object[] content(long id) {
        long s = state.getAndIncrement();
        StringBuilder sbu = new StringBuilder();
        sbu.append("oid_");
        sbu.append(s);
        String oid = sbu.toString();
        Timestamp ts = new Timestamp(new Date(System.currentTimeMillis() + s).getTime());
        Object[] val = {s, oid, ts, id};
        return val;
    }

    private void update(Connection conn, String sql, List<Object[]> list) {
        PreparedStatement ps = statement(conn, sql);
        String tn = Thread.currentThread().getName();
        if (null == ps) {
            close(conn, null);
            log.error("ps = null, thread name=" + tn);
            return;
        }
        try {
            conn.setAutoCommit(false);
            if (addBatch(ps, list)) {
                int re[] = ps.executeBatch();
                if (null != re) {
                    log.info("thread=" + tn + ", has update lines=" + re.length);
                }
                conn.commit();
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error(e.toString(), e);
        } finally {
            close(conn, ps);
        }
    }

    private PreparedStatement statement(Connection conn, String sql) {
        try {
            return conn.prepareStatement(sql);
        } catch (SQLException e) {
            log.error(e.toString(), e);
        }
        return null;
    }

    private boolean addBatch(PreparedStatement ps, List<Object[]> list) {
        for (Object[] o : list) {
            for (int j = 0; j < o.length; j++) {
                try {
                    ps.setObject(j + 1, o[j]);
                } catch (SQLException e) {
                    log.error(e.toString(), e);
                    return false;
                }
            }
            try {
                ps.addBatch();
            } catch (SQLException e) {
                log.error(e.toString(), e);
                return false;
            }
        }
        return true;
    }

    private void close(Connection conn, PreparedStatement ps) {
        try {
            if (null != ps) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.error(e.toString(), e);
                }
            }
        } finally {
            if (null != conn) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e.toString(), e);
                }
            }
        }
    }

}
