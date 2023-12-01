package com.aispeech.lite.vprint;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.aispeech.common.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 声纹数据库管理类
 */
public class VprintDatabaseManager {
    private static final String TAG = "VprintDatabaseManager";
    private static final int VERSION = 1; //数据库版本
    private static final String DB_NAME_DEFAULT = "vprint.db";
    private static final String TABLE_NAME = "VPRINT";

    // 与 VprintSqlEntity 一一对应
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TIMESTAMP = "_timestamp";
    private static final String COLUMN_DATA = "_data";

    private SQLiteDatabase db;
    private SqlHelper sqlHelper;

    /**
     * 在app的包名下创建 {@linkplain #DB_NAME_DEFAULT} 数据库
     *
     * @param context 上下文
     */
    private VprintDatabaseManager(Context context) {
        this(context, DB_NAME_DEFAULT);
    }

    /**
     * 在app的包名下创建 dbName 数据库
     *
     * @param context 上下文
     * @param dbName  数据库的名称
     */
    private VprintDatabaseManager(Context context, String dbName) {
        this.sqlHelper = new SqlHelper(context.getApplicationContext(), dbName);
        this.db = sqlHelper.getWritableDatabase();
    }


    /**
     * 读取指定位置的数据库文件，如果该路径下没有文件，也可创建数据库
     *
     * @param databaseFilepath 数据库文件的绝对路径
     */
    public VprintDatabaseManager(String databaseFilepath) {
        sqlHelper = null;
        // openOrCreateDatabase 创建的数据库是没有表，version 是 0
        db = SQLiteDatabase.openOrCreateDatabase(databaseFilepath, null);
        Log.d(TAG, "db version " + db.getVersion());
        if (db.getVersion() == 0) {
            createTableIfNotExists();
            db.setVersion(1);
            Log.d(TAG, "db has set new version " + db.getVersion());
        }
    }

    public void createTableIfNotExists() {
        createTableIfNotExists(db);
    }

    public void dropTableIfExists() {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    public synchronized List<VprintSqlEntity> query(String id) {
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_ID + " = ? ", new String[]{id}, null, null, null);
        return cursorToList(cursor, true);
    }

    public synchronized List<VprintSqlEntity> queryAll() {
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
        return cursorToList(cursor, true);
    }

    private static List<VprintSqlEntity> cursorToList(Cursor cursor, boolean includeData) {
        List<VprintSqlEntity> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            String _id = cursor.getString(cursor.getColumnIndex(COLUMN_ID));
            long _timestamp = cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP));

            byte[] _data = null;
            if (includeData)
                _data = cursor.getBlob(cursor.getColumnIndex(COLUMN_DATA));

            list.add(new VprintSqlEntity(_id, _data, _timestamp));
        }
        cursor.close();
        Log.d(TAG, "cursorToList " + list.size());
        return list;
    }

    public synchronized boolean insertOrUpdate(VprintSqlEntity entity) {
        if (TextUtils.isEmpty(entity.getId()))
            return false;

        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, entity.getId());
        values.put(COLUMN_TIMESTAMP, entity.getTimestamp());

        values.put(COLUMN_DATA, entity.getData());
        long result = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d(TAG, "insertOrUpdate result " + result);
        return result > 0;
    }

    public synchronized boolean update(VprintSqlEntity entity) {
        if (entity == null || TextUtils.isEmpty(entity.getId()))
            return false;
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, entity.getId());
        values.put(COLUMN_TIMESTAMP, entity.getTimestamp());
        values.put(COLUMN_DATA, entity.getData());
        long result = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{entity.getId()});
        Log.d(TAG, "update result " + result);
        return result > 0;
    }

    public synchronized boolean update(String id, byte[] data) {
        if (TextUtils.isEmpty(id) || data == null || data.length == 0)
            return false;
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, id);
        values.put(COLUMN_DATA, data);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        long result = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{id});
        Log.d(TAG, "update result " + result);
        return result > 0;
    }

    public synchronized boolean delete(String id) {
        if (TextUtils.isEmpty(id))
            return false;
        long result = db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{id});
        Log.d(TAG, "delete result " + result);
        return result > 0;
    }

    public synchronized boolean deleteAll() {
        long result = db.delete(TABLE_NAME, null, null);
        Log.d(TAG, "deleteAll result " + result);
        return result > 0;
    }

    public synchronized void close() {
        if (this.db != null) {
            this.db.close();
            this.db = null;
        }
        if (sqlHelper != null) {
            sqlHelper.close();
            sqlHelper = null;
        }
        Log.d(TAG, "db close");
    }

    private static void createTableIfNotExists(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    COLUMN_ID + " VCHAR PRIMARY KEY, " +
                    COLUMN_TIMESTAMP + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_DATA + " BLOB"
                    + " )");
        } catch (Exception e) {
            Log.d(TAG, "createTableIfNotExists " + e);
        }
    }

    private static class SqlHelper extends SQLiteOpenHelper {

        SqlHelper(Context context, String name) {
            super(context, name, null, VERSION);
            Log.d(TAG, "db name is " + name);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createTableIfNotExists(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

}
