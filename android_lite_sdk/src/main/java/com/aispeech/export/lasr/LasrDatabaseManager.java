package com.aispeech.export.lasr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.aispeech.common.Log;

import java.util.ArrayList;
import java.util.List;

public class LasrDatabaseManager {
    private static final String TAG = "LasrDatabaseManager";
    private static final int VERSION = 1; //数据库版本
    private static final String DB_NAME_DEFAULT = "lasr.db";
    private static final String TABLE_NAME = "LASR";

    // 与 LasrSqlEntity 一一对应
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TIMESTAMP = "_timestamp";
    private static final String COLUMN_TYPE = "_type";
    private static final String COLUMN_URI = "_uri";
    private static final String COLUMN_FILE_LENGTH = "_file_length";
    private static final String COLUMN_AUDIO_ID = "_audio_id";
    private static final String COLUMN_UUID = "_uuid";
    private static final String COLUMN_BLOCK_SIZE = "_block_size";
    private static final String COLUMN_SLICE_NUM = "_slice_num";
    private static final String COLUMN_SLICE_INDEX = "_slice_index";
    private static final String COLUMN_TASK_ID = "_task_id";
    private static final String COLUMN_PROGRESS = "_progress";
    private static final String COLUMN_ASR = "_asr";


    private SQLiteDatabase db;
    private LasrSqlHelper lasrSqlHelper;

    /**
     * 在app的包名下创建 {@linkplain #DB_NAME_DEFAULT} 数据库
     *
     * @param context 上下文
     */
    public LasrDatabaseManager(Context context) {
        this(context, DB_NAME_DEFAULT);
    }

    /**
     * 在app的包名下创建 dbName 数据库
     *
     * @param context 上下文
     * @param dbName  数据库的名称
     */
    public LasrDatabaseManager(Context context, String dbName) {
        this.lasrSqlHelper = new LasrSqlHelper(context.getApplicationContext(), dbName);
        this.db = lasrSqlHelper.getWritableDatabase();
    }


    /**
     * 读取指定位置的数据库文件，如果该路径下没有文件，也可创建数据库
     *
     * @param databaseFilepath 数据库文件的绝对路径
     */
    public LasrDatabaseManager(String databaseFilepath) {
        lasrSqlHelper = null;
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

    public synchronized List<LasrSqlEntity> query(boolean includeData) {
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_TIMESTAMP + " desc ");
        return cursorToList(cursor, includeData);
    }

    public synchronized List<LasrSqlEntity> query(String taskId, boolean includeData) {
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_TASK_ID + " = ? ", new String[]{taskId}, null, null, null);
        return cursorToList(cursor, includeData);
    }

    /**
     * 查找最近上传但没有完全上传完的任务
     *
     * @return LasrSqlEntity
     */
    public synchronized LasrSqlEntity queryLatestUpload() {
        Cursor cursor = db.query(TABLE_NAME, null,
                " (_slice_num>0) AND (_slice_index <> _slice_num-1) ", null,
                null, null, COLUMN_TIMESTAMP + " desc ", " 1 ");
        List<LasrSqlEntity> list = cursorToList(cursor, false);
        return list.size() > 0 ? list.get(0) : null;
    }

    private static List<LasrSqlEntity> cursorToList(Cursor cursor, boolean includeData) {
        List<LasrSqlEntity> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            int _id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
            String _timestamp = cursor.getString(cursor.getColumnIndex(COLUMN_TIMESTAMP));
            int _type = cursor.getInt(cursor.getColumnIndex(COLUMN_TYPE));
            String _uri = cursor.getString(cursor.getColumnIndex(COLUMN_URI));
            int _file_length = cursor.getInt(cursor.getColumnIndex(COLUMN_FILE_LENGTH));
            String _audio_id = cursor.getString(cursor.getColumnIndex(COLUMN_AUDIO_ID));
            String _uuid = cursor.getString(cursor.getColumnIndex(COLUMN_UUID));
            int _block_size = cursor.getInt(cursor.getColumnIndex(COLUMN_BLOCK_SIZE));
            int _slice_num = cursor.getInt(cursor.getColumnIndex(COLUMN_SLICE_NUM));
            int _slice_index = cursor.getInt(cursor.getColumnIndex(COLUMN_SLICE_INDEX));
            String _task_id = cursor.getString(cursor.getColumnIndex(COLUMN_TASK_ID));
            int _progress = cursor.getInt(cursor.getColumnIndex(COLUMN_PROGRESS));

            String _asr = null;
            if (includeData)
                _asr = cursor.getString(cursor.getColumnIndex(COLUMN_ASR));

            list.add(new LasrSqlEntity(_id, _timestamp, _type, _uri, _file_length
                    , _audio_id, _uuid, _block_size, _slice_num, _slice_index, _task_id, _progress, _asr));
        }
        cursor.close();
        Log.d(TAG, "cursorToList " + list.size());
        return list;
    }

    public synchronized boolean insert(LasrSqlEntity lasrSqlEntity) {
        ContentValues values = new ContentValues();
        // values.put(COLUMN_ID, lasrSqlEntity.getId());
        // values.put(COLUMN_TIMESTAMP, lasrSqlEntity.getTimestamp());
        values.put(COLUMN_TYPE, lasrSqlEntity.getType());
        values.put(COLUMN_URI, lasrSqlEntity.getUri());
        values.put(COLUMN_FILE_LENGTH, lasrSqlEntity.getFileLength());
        values.put(COLUMN_AUDIO_ID, lasrSqlEntity.getAudioId());
        values.put(COLUMN_UUID, lasrSqlEntity.getUuid());
        if (lasrSqlEntity.getBlockSize() > 0)
            values.put(COLUMN_BLOCK_SIZE, lasrSqlEntity.getBlockSize());
        values.put(COLUMN_SLICE_NUM, lasrSqlEntity.getSliceNum());
        values.put(COLUMN_SLICE_INDEX, lasrSqlEntity.getSliceIndex());
        values.put(COLUMN_TASK_ID, lasrSqlEntity.getTaskId());
        values.put(COLUMN_PROGRESS, lasrSqlEntity.getProgress());
        values.put(COLUMN_ASR, lasrSqlEntity.getAsr());
        long result = db.insert(TABLE_NAME, null, values);
        Log.d(TAG, "insert result " + result);
        lasrSqlEntity.setId((int) result);
        return result > 0;
    }

    public synchronized boolean insertOrUpdate(LasrSqlEntity lasrSqlEntity) {
        ContentValues values = new ContentValues();
        if (lasrSqlEntity.getId() > 0)
            values.put(COLUMN_ID, lasrSqlEntity.getId());
        // values.put(COLUMN_TIMESTAMP, lasrSqlEntity.getTimestamp());
        values.put(COLUMN_TYPE, lasrSqlEntity.getType());
        values.put(COLUMN_URI, lasrSqlEntity.getUri());
        values.put(COLUMN_FILE_LENGTH, lasrSqlEntity.getFileLength());
        values.put(COLUMN_AUDIO_ID, lasrSqlEntity.getAudioId());
        values.put(COLUMN_UUID, lasrSqlEntity.getUuid());
        if (lasrSqlEntity.getBlockSize() > 0)
            values.put(COLUMN_BLOCK_SIZE, lasrSqlEntity.getBlockSize());
        values.put(COLUMN_SLICE_NUM, lasrSqlEntity.getSliceNum());
        values.put(COLUMN_SLICE_INDEX, lasrSqlEntity.getSliceIndex());
        values.put(COLUMN_TASK_ID, lasrSqlEntity.getTaskId());
        values.put(COLUMN_PROGRESS, lasrSqlEntity.getProgress());
        values.put(COLUMN_ASR, lasrSqlEntity.getAsr());
        long result = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d(TAG, "insertOrUpdate result " + result);
        return result > 0;
    }

    public synchronized boolean update(LasrSqlEntity lasrSqlEntity) {
        if (lasrSqlEntity == null || lasrSqlEntity.getId() <= 0)
            return false;
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, lasrSqlEntity.getId());
        // values.put(COLUMN_TIMESTAMP, lasrSqlEntity.getTimestamp());
        values.put(COLUMN_TYPE, lasrSqlEntity.getType());
        values.put(COLUMN_URI, lasrSqlEntity.getUri());
        values.put(COLUMN_FILE_LENGTH, lasrSqlEntity.getFileLength());
        values.put(COLUMN_AUDIO_ID, lasrSqlEntity.getAudioId());
        values.put(COLUMN_UUID, lasrSqlEntity.getUuid());
        if (lasrSqlEntity.getBlockSize() > 0)
            values.put(COLUMN_BLOCK_SIZE, lasrSqlEntity.getBlockSize());
        values.put(COLUMN_SLICE_NUM, lasrSqlEntity.getSliceNum());
        values.put(COLUMN_SLICE_INDEX, lasrSqlEntity.getSliceIndex());
        values.put(COLUMN_TASK_ID, lasrSqlEntity.getTaskId());
        values.put(COLUMN_PROGRESS, lasrSqlEntity.getProgress());
        values.put(COLUMN_ASR, lasrSqlEntity.getAsr());
        long result = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(lasrSqlEntity.getId())});
        Log.d(TAG, "update result " + result);
        return result > 0;
    }

    public synchronized boolean update(int id, String asr) {
        if (id <= 0 || TextUtils.isEmpty(asr))
            return false;
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, id);
        values.put(COLUMN_ASR, asr);
        long result = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        Log.d(TAG, "update result " + result);
        return result > 0;
    }

    public synchronized boolean delete(int id) {
        if (id <= 0)
            return false;
        long result = db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
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
        if (lasrSqlHelper != null) {
            lasrSqlHelper.close();
            lasrSqlHelper = null;
        }
    }

    private static void createTableIfNotExists(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TIMESTAMP + " TIMESTAMP NOT NULL DEFAULT (datetime(CURRENT_TIMESTAMP,'localtime')), " +
                    COLUMN_TYPE + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_URI + " VARCHAR(512), " +
                    COLUMN_FILE_LENGTH + " INTEGER DEFAULT 0, " +
                    COLUMN_AUDIO_ID + " VARCHAR(64), " +
                    COLUMN_UUID + " CHAR(36), " +
                    COLUMN_BLOCK_SIZE + " INTEGER DEFAULT 0, " +
                    COLUMN_SLICE_NUM + " INTEGER DEFAULT 0, " +
                    COLUMN_SLICE_INDEX + " INTEGER DEFAULT -1, " +
                    COLUMN_TASK_ID + " VARCHAR(64) unique, " +
                    COLUMN_PROGRESS + " INTEGER DEFAULT 0, " +
                    COLUMN_ASR + " TEXT"
                    + " )");
        } catch (Exception e) {
            Log.d(TAG, "createTableIfNotExists " + e);
        }
    }

    private static class LasrSqlHelper extends SQLiteOpenHelper {

        LasrSqlHelper(Context context, String name) {
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
