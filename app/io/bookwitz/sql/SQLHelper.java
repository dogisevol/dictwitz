package io.bookwitz.sql;

import io.bookwitz.models.Lemma;

import java.sql.*;
import java.util.Collection;

public class SQLHelper {

    private static SQLHelper sqlHelper;
    Connection connection;

    private SQLHelper() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:resources/dictionary.sqlite");
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public static SQLHelper getDefault() {
        if (sqlHelper == null) {
            sqlHelper = new SQLHelper();
        }
        return sqlHelper;
    }

    public void addTable(String tableName, Collection<Lemma> collection) throws Exception {
        Statement stmt = connection.createStatement();
        String tempTable = "TEMP_" + tableName;
        String sql = "CREATE TEMP TABLE " + tempTable + " (ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                + " word TEXT NOT NULL UNIQUE, " + " pos POS NOT NULL, " + " freq INT NOT NULL)";
        stmt.executeUpdate(sql);
        int i = 0;
        for (Lemma lemma : collection) {
            stmt.executeUpdate("INSERT INTO " + tempTable + " values (" + i++ + ", \"" + lemma.getWord().word() + "\", \""
                    + lemma.getWord().tag() + "\", " + lemma.getCount() + ")");
        }

        sql = "DROP TABLE IF EXISTS " + tableName;
        stmt.executeUpdate(sql);

        sql = "CREATE TABLE " + tableName + " AS SELECT I.WORD AS WORD, I.POS AS POS, I.FREQ AS FREQ, 0 AS NEW, 0 AS STATUS FROM "
                + tempTable + " I" + " JOIN fullWordsList MW ON LOWER(I.WORD)=LOWER(MW.WORD) WHERE STATUS = 0 COLLATE NOCASE"
                + " UNION" + " SELECT I.WORD AS WORD,  I.POS AS POS, I.FREQ AS FREQ, 1 AS NEW, 0 AS STATUS FROM " + tempTable
                + " I WHERE LOWER(WORD) NOT IN (SELECT LOWER(WORD) FROM fullWordsList) COLLATE NOCASE ORDER BY FREQ DESC";
        stmt.executeUpdate(sql);

        sql = "INSERT INTO fullWordsList SELECT word, freq, 0 FROM " + tableName + " where word not in (select word from fullWordsList)";

        stmt.executeUpdate(sql);

        stmt.close();
        connection.close();
        System.out.println(tableName + " is imported");
    }
}
