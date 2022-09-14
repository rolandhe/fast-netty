package com.github.rolandhe.file.persist.mysql;

import com.github.rolandhe.file.api.persist.MetaPersist;
import com.github.rolandhe.file.api.persist.UploadFileMeta;
import org.apache.commons.codec.digest.MurmurHash3;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class MysqlMetaPersist implements MetaPersist {
    private static final String INSERT_SQL = "insert into upload_file_meta(upload_user_id,biz_line,original_file_name,target_url,file_length,url_hash,store_name,status,created_at,finished_at) values(?,?,?,?,?,?,?,?,?,?)";
    private static final String SELECT_SQL = "select upload_user_id,biz_line,original_file_name,target_url,file_length,store_name from upload_file_meta where id=?";
    public static final long LOW_HASH_MASK = 0x7FFL;

    @Resource
    private DataSource dataSource;

    @Override
    public Long prePersist(UploadFileMeta meta) {
         return insertMeta(meta);
    }

    @Override
    public Long persist(UploadFileMeta meta, Long preId,String targetFile) {
        long hash = hashLong(targetFile);
        if(!updateStatus(preId,targetFile,hash)) {
            return null;
        }
        return codeId(preId,hash);
    }

    @Override
    public UploadFileMeta getMetaByPersistId(long persistId) {
        long id = persistId >>> 11;
        UploadFileMeta meta =  getUploadFileMeta(id);
        if(null == meta){
            return null;
        }
        long hash = hashLong(meta.getTargetUrl());
        long h = hash & LOW_HASH_MASK;
        if(h == (persistId & LOW_HASH_MASK)){
            return meta;
        }
        return null;
    }

    private UploadFileMeta getUploadFileMeta(long id) {
        try (Connection connection = dataSource.getConnection()){
            connection.setAutoCommit(true);
            try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL)) {
                preparedStatement.setLong(1, id);
                try(ResultSet resultSet = preparedStatement.executeQuery()){
                    if(!resultSet.next()){
                        return null;
                    }
                    return fromResultSet(resultSet);
                }
            }

        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }

    private UploadFileMeta fromResultSet(ResultSet resultSet) throws SQLException {
        UploadFileMeta meta = new UploadFileMeta();
        meta.setUploadUserId(resultSet.getLong(1));
        meta.setBizLine(resultSet.getString(2));
        meta.setOriginalFileName(resultSet.getString(3));
        meta.setTargetUrl(resultSet.getString(4));
        meta.setFileLength(resultSet.getLong(5));
        meta.setStoreName(resultSet.getString(6));
        return meta;
    }


    private boolean updateStatus(Long preId,String targetFile,long hash){
        if(preId == null){
            return false;
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement("update upload_file_meta set status=1,target_url=?,url_hash=?, finished_at = now() where id=?")) {
                preparedStatement.setString(1, targetFile);
                preparedStatement.setLong(2, hash);
                preparedStatement.setLong(3, preId);
                boolean ok = preparedStatement.executeUpdate() == 1;
                connection.setAutoCommit(true);
                return ok;
            }

        } catch (SQLException throwables) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            throw new RuntimeException(throwables);
        } finally {
            safeClose(connection);
        }
    }

    private Long insertMeta(UploadFileMeta meta) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL)) {
                preparedStatement.setLong(1, meta.getUploadUserId());
                preparedStatement.setString(2, meta.getBizLine());
                preparedStatement.setString(3, meta.getOriginalFileName());
                preparedStatement.setString(4, meta.getTargetUrl());
                preparedStatement.setLong(5, meta.getFileLength());
                long hash = hashLong(meta.getTargetUrl());
                preparedStatement.setLong(6, hash);
                preparedStatement.setString(7, meta.getStoreName());
                preparedStatement.setInt(8, 0);
                LocalDateTime localDateTime =  LocalDateTime.now();
                preparedStatement.setObject(9, localDateTime);
                preparedStatement.setObject(10, localDateTime);
                boolean ok = preparedStatement.executeUpdate() == 1;
                Long id = null;
                if(ok) {
                   id = getCurrentId(connection);
                }
                connection.setAutoCommit(true);
                return id;
            }

        } catch (SQLException throwables) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            throw new RuntimeException(throwables);
        } finally {
            safeClose(connection);
        }
    }


    private Long codeId(Long id, long hash){
        if(id == null){
            return null;
        }
        long v = id << 11;
        long h = hash & 0x7FFL;
        return v | h;
    }

    private Long getCurrentId(Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT LAST_INSERT_ID();")) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return null;
            }
        }
    }

    private void safeClose(AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private long hashLong(String targetUrl) {
        return MurmurHash3.hash128(targetUrl.getBytes(StandardCharsets.UTF_8))[0];
    }
}
