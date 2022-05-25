/*
 * Copyright (c) 2022 BlackBerry Limited. All Rights Reserved.
 * Some modifications to the original Cordova SQLite Storage plugin
 * from https://github.com/xpbrew/cordova-sqlite-storage/
 *
 * Copyright (c) 2012-present Christopher J. Brody (aka Chris Brody)
 * Copyright (c) 2005-2010, Nitobi Software Inc.
 * Copyright (c) 2010, IBM Corporation
 */

package com.blackberry.bbd.cordova.plugins;

import android.content.res.AssetManager;
import android.database.Cursor;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.blackberry.bbd.cordova.plugins.file.FileUtils;
import com.good.gd.cordova.plugins.helpers.delegates.GDFileSystemDelegate;
import com.good.gd.cordova.plugins.GDBasePlugin;
import com.good.gd.database.sqlite.SQLiteConstraintException;
import com.good.gd.database.sqlite.SQLiteDatabase;
import com.good.gd.database.sqlite.SQLiteException;
import com.good.gd.database.sqlite.SQLiteStatement;
import com.good.gd.file.GDFileSystem;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.lang.IllegalArgumentException;
import java.lang.Number;

import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BBDSQLitePlugin extends GDBasePlugin {

    private static final Pattern FIRST_WORD = Pattern.compile("^\\s*(\\S+)",
          Pattern.CASE_INSENSITIVE);

    private static final String LOG_TAG = BBDSQLitePlugin.class.getSimpleName();

    private GDFileSystemDelegate gdFileSystem = new GDFileSystemDelegate();

    /**
     * Concurrent database runner map.
     *
     * NOTE: no public static accessor to db (runner) map since it is not
     * expected to work properly with db threading.
     *
     * FUTURE TBD put DBRunner into a public class that can provide external accessor.
     *
     * ADDITIONAL NOTE: Storing as Map<String, DBRunner> to avoid portabiity issue
     * between Java 6/7/8 as discussed in:
     * https://gist.github.com/AlainODea/1375759b8720a3f9f094
     *
     * THANKS to @NeoLSN (Jason Yang/楊朝傑) for giving the pointer in:
     * https://github.com/litehelpers/Cordova-sqlite-storage/issues/727
     */
    private ConcurrentHashMap<String, DBRunner> dbrmap = new ConcurrentHashMap<String, DBRunner>();

    /**
     * NOTE: Using default constructor, no explicit constructor.
     */

    /**
     * Executes the request and returns PluginResult.
     *
     * @param actionAsString The action to execute.
     * @param args   JSONArry of arguments for the plugin.
     * @param cbc    Callback context from Cordova API
     * @return       Whether the action was valid.
     */
    @Override
    public boolean execute(String actionAsString, JSONArray args, CallbackContext cbc) {

        Action action;
        try {
            action = Action.valueOf(actionAsString);
        } catch (IllegalArgumentException e) {
            // shouldn't ever happen
            Log.e(LOG_TAG, "unexpected error", e);
            cbc.error("Unexpected error executing processing SQLite query");
            return false;
        }

        try {
            return executeAndPossiblyThrow(action, args, cbc);
        } catch (JSONException e) {
            // TODO: signal JSON problem to JS
            Log.e(LOG_TAG, "unexpected error", e);
            cbc.error("Unexpected error executing processing SQLite query");
            return false;
        }
    }

    private boolean executeAndPossiblyThrow(Action action, JSONArray args, CallbackContext cbc)
            throws JSONException {

        boolean status = true;
        JSONObject o;
        String echo_value;
        String dbname;

        switch (action) {
            case echoStringValue:
                o = args.getJSONObject(0);
                echo_value = o.getString("value");
                cbc.success(echo_value);
                break;

            case open:
                o = args.getJSONObject(0);
                dbname = o.getString("name");
                // open database and start reading its queue
                this.startDatabase(dbname, o, cbc);
                break;

            case close:
                o = args.getJSONObject(0);
                dbname = o.getString("path");
                // put request in the q to close the db
                this.closeDatabase(dbname, cbc);
                break;

            case attach:
                o = args.getJSONObject(0);
                dbname = o.getString("path");
                String dbnameToAttach = o.getString("dbname");
                String dbalias = o.getString("dbalias");
                this.attachDatabase(dbname, dbnameToAttach, dbalias, cbc);
                break;

            case delete:
                o = args.getJSONObject(0);
                dbname = o.getString("path");
                deleteDatabase(dbname, cbc);
                break;

            case executeSqlBatch:
            case sqlite3enc_import:
                handleSQLite3EncImport(args, cbc);
            case backgroundExecuteSqlBatch:
                String [] queries;
                String [] queryIDs = null;
                JSONArray [] queryParams = null;
                o = args.getJSONObject(0);
                JSONObject dbargs = o.getJSONObject("dbargs");
                dbname = dbargs.getString("dbname");
                JSONArray txargs = o.getJSONArray("executes");
                boolean noNeedQueryId = false;

                if (txargs.isNull(0)) {
                    Log.v(LOG_TAG, "missing executes list");
                    cbc.error("INTERNAL PLUGIN ERROR: missing executes list");
                    queries = new String[0];
                } else {
                    int len = txargs.length();
                    queryIDs = new String[len];
                    queries = new String[len];
                    queryParams = new JSONArray[len];

                    for (int i = 0; i < len; i++) {
                        JSONObject a = txargs.getJSONObject(i);
                        if (a.isNull("qid")) {
                            noNeedQueryId = true;
                        }
                        else {
                            queryIDs[i] = a.getString("qid");
                        }
                        queries[i] = a.getString("sql");
                        queryParams[i] = a.getJSONArray("params");
                    }
                }

                // put db query in the queue to be executed in the db thread:
                DBQuery q = new DBQuery(queries, noNeedQueryId ? null : queryIDs, queryParams, cbc);
                DBRunner r = dbrmap.get(dbname);
                if (r != null) {
                    try {
                        r.q.put(q);
                    } catch(Exception e) {
                        Log.e(LOG_TAG, "couldn't add to queue", e);
                        cbc.error("INTERNAL PLUGIN ERROR: couldn't add to queue");
                    }
                } else {
                    Log.v(LOG_TAG, "database not open");
                    cbc.error("INTERNAL PLUGIN ERROR: database not open");
                }
                break;
        }

        return status;
    }

    private void handleSQLite3EncImport(final JSONArray arguments,
                                        final CallbackContext callbackContext) {
        OutputStream out = null;
        InputStream in = null;
        File tempDbFile = null;
        Context context = this.cordova.getContext().getApplicationContext();
        AssetManager assetsManager = this.cordova.getContext().getAssets();
        File dbFile = null;

        try {
            final String srcFileName = arguments.getString(0);
            final String destFileName = arguments.getString(1);

            dbFile = context.getDatabasePath(destFileName);
            String dbPath = srcFileName.substring(0, srcFileName.lastIndexOf("/") + 1);

            tempDbFile = gdFileSystem.createFile(dbPath + "tempdata.db");
            tempDbFile.getParentFile().mkdirs();
            tempDbFile.createNewFile();

            out = gdFileSystem.openFileOutput(tempDbFile.getAbsolutePath(), GDFileSystem.MODE_PRIVATE);
            // TODO: implement creating InputStream of file from secure container
            // in = gdFileSystem.openFileInput("Inbox"+srcFileName);
            in = assetsManager.open("www" + srcFileName);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                out.flush();
            }
            out.close();
            out = null;

            final boolean isImported = SQLiteDatabase.importDatabase(tempDbFile.getAbsolutePath(), dbFile.getAbsolutePath());

            if (isImported) {
                callbackContext.success(FileUtils.getEntry(new com.good.gd.file.File(dbFile.getAbsolutePath())));
            } else {
                callbackContext.error("Can't import database");
            }

        } catch (JSONException | IOException e) {
            callbackContext.error("Malformed params");
        }
    }

    /**
     * Clean up and close all open databases.
     */
    @Override
    public void onDestroy() {
        while (!dbrmap.isEmpty()) {
            String dbname = dbrmap.keySet().iterator().next();

            this.closeDatabaseNow(dbname);

            DBRunner r = dbrmap.get(dbname);
            try {
                // stop the db runner thread:
                r.q.put(new DBQuery());
            } catch(Exception e) {
                Log.e(LOG_TAG, "INTERNAL PLUGIN CLEANUP ERROR: could not stop db thread due to exception", e);
            }
            dbrmap.remove(dbname);
        }
    }

    // --------------------------------------------------------------------------
    // LOCAL METHODS
    // --------------------------------------------------------------------------
 
    /**
    *
    * @param dbname - The name of the database file
    * @param options - options passed in from JS
    * @param cbc - JS callback context
    */
    private void startDatabase(String dbname, JSONObject options, CallbackContext cbc) {
        // TODO: is it an issue that we can orphan an existing thread?  What should we do here?
        // If we re-use the existing DBRunner it might be in the process of closing...
        DBRunner r = dbrmap.get(dbname);

        // Brody TODO: It may be better to terminate the existing db thread here & start a new one, instead.
        if (r != null) {
            // don't orphan the existing thread; just re-open the existing database.
            // In the worst case it might be in the process of closing, but even that's less serious
            // than orphaning the old DBRunner.
            cbc.success("database started");
        } else {
            r = new DBRunner(dbname, options, cbc);
            dbrmap.put(dbname, r);
            this.cordova.getThreadPool().execute(r);
        }
    }

    /**
     * Open a database.
     *
     * @param dbname - The name of the database file
     * @param assetFilePath - path to the pre-populated database file
     * @param openFlags - the db open options
     * @param cbc - JS callback
     * @return instance of SQLite database
    //   * @throws Exception
    */
    private SQLiteDatabase openDatabase(String dbname, String assetFilePath, int openFlags, CallbackContext cbc) throws Exception {
        Context context = this.cordova.getContext().getApplicationContext();
        InputStream in = null;
        File dbfile = null;
        try {
            SQLiteDatabase database = this.getDatabase(dbname);
            if (database != null && database.isOpen()) {
                // this only happens when DBRunner is cycling the db for the locking work around.
                // otherwise, this should not happen - should be blocked at the execute("open") level
                throw new Exception("Database already open");
            }

            boolean assetImportError = false;
            boolean assetImportRequested = assetFilePath != null && assetFilePath.length() > 0;
            if (assetImportRequested) {
                if (assetFilePath.compareTo("1") == 0) {
                    assetFilePath = "www/" + dbname;
                    try {
                        in = context.getAssets().open(assetFilePath);
                        Log.v(LOG_TAG, "Pre-populated DB asset FOUND  in app bundle www subdirectory: " + assetFilePath);
                    } catch (Exception e){
                        assetImportError = true;
                        Log.e(LOG_TAG, "pre-populated DB asset NOT FOUND in app bundle www subdirectory: " + assetFilePath);
                    }
                } else if (assetFilePath.charAt(0) == '~') {
                    assetFilePath = assetFilePath.startsWith("~/") ? assetFilePath.substring(2) : assetFilePath.substring(1);
                    try {
                        in = context.getAssets().open(assetFilePath);
                        Log.v(LOG_TAG, "Pre-populated DB asset FOUND in app bundle subdirectory: " + assetFilePath);
                    } catch (Exception e){
                        assetImportError = true;
                        Log.e(LOG_TAG, "pre-populated DB asset NOT FOUND in app bundle www subdirectory: " + assetFilePath);
                    }
                } else {
                    File filesDir = context.getFilesDir();
                    assetFilePath = assetFilePath.startsWith("/") ? assetFilePath.substring(1) : assetFilePath;
                    try {
                        File assetFile = new File(filesDir, assetFilePath);
                        in = new FileInputStream(assetFile);
                        Log.v(LOG_TAG, "Pre-populated DB asset FOUND in Files subdirectory: " + assetFile.getCanonicalPath());
                        if (openFlags == SQLiteDatabase.OPEN_READONLY) {
                            dbfile = assetFile;
                            Log.v(LOG_TAG, "Detected read-only mode request for external asset.");
                        }
                    } catch (Exception e){
                        assetImportError = true;
                        Log.e(LOG_TAG, "Error opening pre-populated DB asset in app bundle www subdirectory: " + assetFilePath);
                    }
                }
            }

            if (dbfile == null) {
                openFlags = SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY;
                dbfile = context.getDatabasePath(dbname);

                // importing pre-populated db from assets is not supported for now
                // TODO: udpate the code below support importing pre-populated db withing SQlite Storage plugin
                // NOTE: importing pre-populated db is now supported in Storage plugin
                // if (!dbfile.exists() && assetImportRequested) {
                //     if (assetImportError || in == null) {
                //         Log.e(LOG_TAG, "Unable to import pre-populated db asset");
                //         throw new Exception("Unable to import pre-populated db asset");
                //     } else {
                //         Log.v(LOG_TAG, "Copying pre-populated db asset to destination");
                //         try {
                //             this.createFromAssets(dbname, dbfile, in);
                //         } catch (Exception ex){
                //             Log.e(LOG_TAG, "Error importing pre-populated DB asset", ex);
                //             throw new Exception("Error importing pre-populated DB asset");
                //         }
                //     }
                // }

                if (!dbfile.exists()) {
                    dbfile = gdFileSystem.createFile(dbfile.getAbsolutePath());
                    dbfile.getParentFile().mkdirs();
                }
            }

            Log.v(LOG_TAG, "DB file is ready, proceeding to OPEN SQLite DB: " + dbfile.getAbsolutePath());

            SQLiteDatabase mydb = SQLiteDatabase.openDatabase(dbfile.getAbsolutePath(), null, openFlags);

            if (cbc != null)
                cbc.success("Database opened");

            return mydb;
        } finally {
            closeQuietly(in);
        }
    }

    /**
     * If a prepopulated DB file exists in the assets folder it is copied to the dbPath.
     * Only runs the first time the app runs.
     *
     * @param dbname The name of the database file - could be used as filename for imported asset
     * @param dbfile The File of the destination db
     * @param assetFileInputStream input file stream for pre-populated db asset
    */
    private void createFromAssets(String dbname, File dbfile, InputStream assetFileInputStream) throws Exception {
        OutputStream out = null;

        try {
            Log.v(LOG_TAG, "Copying pre-populated DB content");
            String dbPath = dbfile.getAbsolutePath();
            dbPath = dbPath.substring(0, dbPath.lastIndexOf("/") + 1);

            File dbPathFile = new File(dbPath);
            if (!dbPathFile.exists())
                dbPathFile.mkdirs();

            File newDbFile = new File(dbPath + dbname);
            out = new FileOutputStream(newDbFile);

            // XXX TODO: this is very primitive, other alternatives at:
            // http://www.journaldev.com/861/4-ways-to-copy-file-in-java
            byte[] buf = new byte[1024];
            int len;
            while ((len = assetFileInputStream.read(buf)) > 0)
                out.write(buf, 0, len);

            Log.v(LOG_TAG, "Copied pre-populated DB asset to: " + newDbFile.getAbsolutePath());
        } finally {
            closeQuietly(out);
        }
    }

    /**
     * Close a database (in another thread).
     *
     * @param dbname   The name of the database file
     * @param cbc - JS callback
     */
    private void closeDatabase(String dbname, CallbackContext cbc) {
        DBRunner r = dbrmap.get(dbname);
        if (r != null) {
            try {
                r.q.put(new DBQuery(false, cbc));
            } catch(Exception e) {
                if (cbc != null) {
                    cbc.error("couldn't close database" + e);
                }
                Log.e(LOG_TAG, "couldn't close database", e);
            }
        } else {
            if (cbc != null) {
                cbc.success();
            }
        }
    }

    /**
     * Close a database (in the current thread).
     *
     * @param dbname   The name of the database file
     */
    private void closeDatabaseNow(String dbname) {
        SQLiteDatabase mydb = this.getDatabase(dbname);

        if (mydb != null) {
            // make to end all transaction pending before close the database
            // to prevent an unexpected database lock
            while (mydb.inTransaction()) {
                mydb.endTransaction();
            }
            mydb.close();
        }
    }

    /**
     * Attach a database
     *
     * @param dbname - The name of the database file
     * @param dbnameToAttach - The name of the database file to attach
     * @param dbalias - The alias of the attached database
     * @param cbc - JS callback
    */
    private void attachDatabase(String dbname, String dbnameToAttach, String dbalias, CallbackContext cbc) {
        Context context = this.cordova.getContext().getApplicationContext();
        DBRunner runner = dbrmap.get(dbname);
        if (runner != null) {
            File databasePath = context.getDatabasePath(dbnameToAttach);
            String filePathToAttached = databasePath.getAbsolutePath();
            String statement = "ATTACH DATABASE '" + filePathToAttached + "' AS " + dbalias;
            // TODO: get rid of qid as it's just hardcoded to 1111 in js layer
            DBQuery query = new DBQuery(new String [] {statement}, new String[] {"1111"}, null, cbc);
            try {
                runner.q.put(query);
            } catch (InterruptedException e) {
                cbc.error("Can't put query in the queue. Interrupted.");
            }
        } else {
            cbc.error("Database " + dbname + "i s not created yet");
        }
    }

  /**
   *
   * @param dbname - The name of the database file
   * @param cbc - callback
   */
    private void deleteDatabase(String dbname, CallbackContext cbc) {
        DBRunner r = dbrmap.get(dbname);
        if (r != null) {
            try {
                r.q.put(new DBQuery(true, cbc));
            } catch(Exception e) {
                if (cbc != null) {
                    cbc.error("couldn't close database" + e);
                }
                Log.e(LOG_TAG, "couldn't close database", e);
            }
        } else {
            boolean deleteResult = this.deleteDatabaseNow(dbname);
            if (deleteResult) {
                cbc.success("database deleted");
            } else {
                cbc.error("couldn't delete database");
            }
        }
    }

    /**
     * Delete a database.
     *
     * @param dbname   The name of the database file
     *
     * @return true if successful or false if an exception was encountered
     */
    private boolean deleteDatabaseNow(String dbname) {
        Context context = this.cordova.getContext().getApplicationContext();
        File dbfile = context.getDatabasePath(dbname);
        File gdFile = gdFileSystem.createFile(dbfile.getAbsolutePath());
        return SQLiteDatabase.deleteDatabase((com.good.gd.file.File) gdFile);
    }

    /**
     * Get a database from the db map.
     *
     * @param dbname The name of the database.
     */
    private SQLiteDatabase getDatabase(String dbname) {
        DBRunner r = dbrmap.get(dbname);
        return (r == null) ? null :  r.mydb;
    }

    /**
     * Executes a batch request and sends the results via cbc.
     *
     * @param dbname     The name of the database.
     * @param queries   Array of query strings
     * @param queryParams Array of JSON query parameters
     * @param queryIDs   Array of query ids
     * @param cbc        Callback context from Cordova API
    */
    private void executeSqlBatch(String dbname, String[] queries, JSONArray[] queryParams,
                               String[] queryIDs, CallbackContext cbc) {

        SQLiteDatabase mydb = getDatabase(dbname);

        if (mydb == null) {
            // not allowed - can only happen if someone has closed (and possibly deleted) a database and then re-used the database
            cbc.error("database has been closed");
            return;
        }

        String query;
        String query_id = null;
        int len = queries.length;
        JSONArray batchResults = new JSONArray();

        for (int i = 0; i < len; i++) {
            if (queryIDs != null) {
                query_id = queryIDs[i];
            }
            
            JSONObject queryResult = null;
            String errorMessage = "unknown";
            int code = 0; // SQLException.UNKNOWN_ERR

            try {
                boolean needRawQuery = true;
                query = queries[i];
                QueryType queryType = getQueryType(query);

                if (queryType == QueryType.update || queryType == QueryType.delete) {
                    SQLiteStatement myStatement = null;
                    int rowsAffected = -1; // (assuming invalid)

                    try {
                        myStatement = mydb.compileStatement(query);
                        if (queryParams != null) {
                            bindArgsToStatement(myStatement, queryParams[i]);
                        }

                        rowsAffected = myStatement.executeUpdateDelete();
                        // Indicate valid results:
                        needRawQuery = false;
                    } catch (SQLiteConstraintException e) {
                        // Indicate problem & stop this query:
                        errorMessage = "constraint failure: " + e.getMessage();
                        code = 6; // SQLException.CONSTRAINT_ERR
                        Log.e(LOG_TAG, "SQLiteStatement.executeUpdateDelete() failed", e);
                        needRawQuery = false;
                    } catch (SQLiteException e) {
                        // Indicate problem & stop this query:
                        errorMessage = e.getMessage();
                        Log.e(LOG_TAG, "SQLiteStatement.executeUpdateDelete() failed", e);
                        needRawQuery = false;
                    } finally {
                        closeQuietly(myStatement);
                    }

                    if (rowsAffected != -1) {
                        queryResult = new JSONObject();
                        queryResult.put("rowsAffected", rowsAffected);
                    }
                }

                // INSERT:
                else if (queryType == QueryType.insert && queryParams != null) {
                    Log.d(LOG_TAG, "executeSqlBatch INSERT");
                    needRawQuery = false;

                    SQLiteStatement myStatement = mydb.compileStatement(query);

                    bindArgsToStatement(myStatement, queryParams[i]);

                    long insertId; // (invalid) = -1

                    try {
                        insertId = myStatement.executeInsert();

                        // statement has finished with no constraint violation:
                        queryResult = new JSONObject();
                        if (insertId != -1) {
                            queryResult.put("insertId", insertId);
                            queryResult.put("rowsAffected", 1);
                        } else {
                            queryResult.put("rowsAffected", 0);
                        }
                    } catch (SQLiteConstraintException e) {
                        // report constraint violation error result with the error message
                        errorMessage = "constraint failure: " + e.getMessage();
                        code = 6; // SQLException.CONSTRAINT_ERR
                        Log.e(LOG_TAG, "SQLiteDatabase.executeInsert() failed", e);
                    } catch (SQLiteException e) {
                        // report error result with the error message
                        errorMessage = e.getMessage();
                        Log.e(LOG_TAG, "SQLiteDatabase.executeInsert() failed", e);
                    } finally {
                        closeQuietly(myStatement);
                    }
                }

                else if (queryType == QueryType.begin) {
                    needRawQuery = false;
                    try {
                        mydb.beginTransaction();

                        queryResult = new JSONObject();
                        queryResult.put("rowsAffected", 0);
                    } catch (SQLiteException e) {
                        errorMessage = e.getMessage();
                        Log.e(LOG_TAG, "SQLiteDatabase.beginTransaction() failed", e);
                    }
                }

                else if (queryType == QueryType.commit) {
                    needRawQuery = false;
                    try {
                        mydb.setTransactionSuccessful();
                        mydb.endTransaction();

                        queryResult = new JSONObject();
                        queryResult.put("rowsAffected", 0);
                    } catch (SQLiteException e) {
                        errorMessage = e.getMessage();
                        Log.e(LOG_TAG, "SQLiteDatabase.setTransactionSuccessful/endTransaction() failed", e);
                    }
                }

                else if (queryType == QueryType.rollback) {
                    needRawQuery = false;
                    try {
                        mydb.endTransaction();

                        queryResult = new JSONObject();
                        queryResult.put("rowsAffected", 0);
                    } catch (SQLiteException e) {
                        errorMessage = e.getMessage();
                        Log.e(LOG_TAG, "SQLiteDatabase.endTransaction() failed", e);
                    }
                }

                // raw query for other statements:
                if (needRawQuery) {
                    try {
                        queryResult = this.executeSqlStatementQuery(mydb, query, queryParams != null ? queryParams[i] : null, cbc);
                    } catch (SQLiteConstraintException e) {
                        // report constraint violation error result with the error message
                        errorMessage = "constraint failure: " + e.getMessage();
                        code = 6; // SQLException.CONSTRAINT_ERR
                        Log.e(LOG_TAG, "SQLiteDatabase.executeSqlStatementQuery() failed", e);
                    } catch (SQLiteException e) {
                        // report error result with the error message
                        errorMessage = e.getMessage();
                        Log.e(LOG_TAG, "SQLiteDatabase.executeSqlStatementQuery() failed", e);
                    }
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
                Log.e(LOG_TAG, "SQLitePlugin.executeSql[Batch](): failed", e);
            }

            try {
                if (queryResult != null) {
                    JSONObject r = new JSONObject();
                    if (query_id != null) {
                        r.put("qid", query_id);
                    }

                    r.put("type", "success");
                    r.put("result", queryResult);

                    batchResults.put(r);
                } else {
                    JSONObject r = new JSONObject();
                    if (query_id != null) {
                        r.put("qid", query_id);
                    }
                    r.put("type", "error");

                    JSONObject er = new JSONObject();
                    er.put("message", errorMessage);
                    er.put("code", code);
                    r.put("result", er);

                    batchResults.put(r);
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "SQLitePlugin.executeSql[Batch](): batchResults failed", e);
            }
        }

        cbc.success(batchResults);
    }

    private QueryType getQueryType(String query) {
        Matcher matcher = FIRST_WORD.matcher(query);
        if (matcher.find()) {
            try {
                return QueryType.valueOf(matcher.group(1).toLowerCase(Locale.US));
            } catch (IllegalArgumentException ignore) {
                // unknown verb
            }
        }
        return QueryType.other;
    }

    private void bindArgsToStatement(SQLiteStatement myStatement, JSONArray sqlArgs) throws JSONException {
        for (int i = 0; i < sqlArgs.length(); i++) {
            if (sqlArgs.get(i) instanceof Float || sqlArgs.get(i) instanceof Double) {
                myStatement.bindDouble(i + 1, sqlArgs.getDouble(i));
            } else if (sqlArgs.get(i) instanceof Number) {
                myStatement.bindLong(i + 1, sqlArgs.getLong(i));
            } else if (sqlArgs.isNull(i)) {
                myStatement.bindNull(i + 1);
            } else {
                myStatement.bindString(i + 1, sqlArgs.getString(i));
            }
        }
    }

    /**
     * Execute Sql Statement Query
     *
     * @param mydb - database
     * @param query - SQL query to execute
     * @param queryParams - parameters to the query
     * @param cbc - callback object
     *
     //   * @throws Exception
     * @return results in string form
     */
    private JSONObject executeSqlStatementQuery(SQLiteDatabase mydb,
                                                String query, JSONArray queryParams,
                                                CallbackContext cbc) throws Exception {
        JSONObject rowsResult = new JSONObject();

        Cursor cur = null;
        try {
            try {
                String[] params = new String[0];
                if (queryParams != null) {
                    int size = queryParams.length();
                    params = new String[size];
                    for (int j = 0; j < size; j++) {
                        if (queryParams.isNull(j)) {
                            params[j] = "";
                        } else {
                            params[j] = queryParams.getString(j);
                        }
                    }
                }

                cur = mydb.rawQuery(query, params);
            } catch (Exception e) {
                Log.e(LOG_TAG, "SQLitePlugin.executeSql[Batch]() failed", e);
                throw e;
            }

            // If query result has rows
            if (cur != null && cur.moveToFirst()) {
                JSONArray rowsArrayResult = new JSONArray();
                String key;
                int colCount = cur.getColumnCount();

                // Build up result object for each row
                do {
                    JSONObject row = new JSONObject();
                    for (int i = 0; i < colCount; ++i) {
                        key = cur.getColumnName(i);
                        bindRow(row, key, cur, i);
                    }

                    rowsArrayResult.put(row);
                } while (cur.moveToNext());

                rowsResult.put("rows", rowsArrayResult);
            }
        } finally {
            closeQuietly(cur);
        }

        return rowsResult;
    }

    private void bindRow(JSONObject row, String key, Cursor cur, int i) {
        int curType = cur.getType(i);

        try {
            switch (curType) {
                case Cursor.FIELD_TYPE_NULL:
                    row.put(key, JSONObject.NULL);
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    row.put(key, cur.getLong(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    row.put(key, cur.getDouble(i));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    row.put(key, new String(Base64.encode(cur.getBlob(i), Base64.DEFAULT)));
                    break;
                case Cursor.FIELD_TYPE_STRING:
                default: /* (not expected) */
                    row.put(key, cur.getString(i));
                    break;
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error bindRow:",e);
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private class DBRunner implements Runnable {
        private final String dbname;
        private final int openFlags;
        private String assetFilename;
        private boolean androidLockWorkaround;
        private final BlockingQueue<DBQuery> q;
        private final CallbackContext openCbc;

        private SQLiteDatabase mydb;

        DBRunner(final String dbname, JSONObject options, CallbackContext cbc) {
            this.dbname = dbname;
            int openFlags = SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY;
            this.assetFilename = null;

            if (options.has("assetFilename")) {
                try {
                    this.assetFilename = options.getString("assetFilename");
                    if (this.assetFilename != null && this.assetFilename.length() > 0) {
                        boolean readOnly = false;
                        if (options.has("readOnly")) {
                            readOnly = options.getBoolean("readOnly");
                        }
                        openFlags = readOnly ? SQLiteDatabase.OPEN_READONLY : openFlags;
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error retrieving assetFilename or mode from options:",e);
                }
            }
            this.openFlags = openFlags;

            //
            // try {
            //    this.androidLockWorkaround = options.getBoolean("androidLockWorkaround");
            // } catch (JSONException e) {
            //    this.androidLockWorkaround = false;
            // }
            // if (this.androidLockWorkaround)
            //    Log.i(LOG_TAG, "Android db closing/locking workaround applied");
            //

            // the android workaround is always enabled because of the using sqlite database
            // behavior is equivalent of 'androidDatabaseProvider: system'.
            this.androidLockWorkaround = true;

            this.q = new LinkedBlockingQueue<DBQuery>();
            this.openCbc = cbc;
        }

        public void run() {
            try {
                this.mydb = openDatabase(this.dbname, this.assetFilename, this.openFlags, this.openCbc);
            } catch (SQLiteException e) {
                Log.e(LOG_TAG, "SQLite error opening database, stopping db thread", e);
                if (this.openCbc != null) {
                    this.openCbc.error("Can't open database." + e);
                }
                dbrmap.remove(dbname);
                return;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Unexpected error opening database, stopping db thread", e);
                 if (this.openCbc != null) {
                    this.openCbc.error("Can't open database." + e);
                }
                dbrmap.remove(dbname);
                return;
            }

            DBQuery dbq = null;

            try {
                dbq = q.take();

                while (!dbq.stop) {
                    executeSqlBatch(dbname, dbq.queries, dbq.queryParams, dbq.queryIDs, dbq.cbc);

                    // XXX workaround for Android locking/closing issue:
                    if (androidLockWorkaround && dbq.queries.length == 1 && dbq.queries[0].equals("COMMIT")) {
                        // Log.v(SQLitePlugin.class.getSimpleName(), "close and reopen db");
                        closeDatabaseNow(dbname);
                        this.mydb = openDatabase(dbname, "", this.openFlags, null);
                        // Log.v(SQLitePlugin.class.getSimpleName(), "close and reopen db finished");
                    }

                    dbq = q.take();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "unexpected error", e);
            }

            if (dbq != null && dbq.close) {
                try {
                    closeDatabaseNow(dbname);

                    dbrmap.remove(dbname); // (should) remove ourself

                    if (!dbq.delete) {
                        dbq.cbc.success("database removed");
                    } else {
                        try {
                            boolean deleteResult = deleteDatabaseNow(dbname);
                            if (deleteResult) {
                                dbq.cbc.success("database removed");
                            } else {
                                dbq.cbc.error("couldn't delete database");
                            }
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "couldn't delete database", e);
                            dbq.cbc.error("couldn't delete database: " + e);
                        }
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "couldn't close database", e);
                    if (dbq.cbc != null) {
                        dbq.cbc.error("couldn't close database: " + e);
                    }
                }
            }
        }
    }

    private final class DBQuery {
        // XXX TODO replace with DBRunner action enum:
        final boolean stop;
        final boolean close;
        final boolean delete;
        final String[] queries;
        final String[] queryIDs;
        final JSONArray[] queryParams;
        final CallbackContext cbc;

        DBQuery(String[] myqueries, String[] qids, JSONArray[] params, CallbackContext c) {
            this.stop = false;
            this.close = false;
            this.delete = false;
            this.queries = myqueries;
            this.queryIDs = qids;
            this.queryParams = params;
            this.cbc = c;
        }

        DBQuery(boolean delete, CallbackContext cbc) {
            this.stop = true;
            this.close = true;
            this.delete = delete;
            this.queries = null;
            this.queryIDs = null;
            this.queryParams = null;
            this.cbc = cbc;
        }

        // signal the DBRunner thread to stop:
        DBQuery() {
            this.stop = true;
            this.close = false;
            this.delete = false;
            this.queries = null;
            this.queryIDs = null;
            this.queryParams = null;
            this.cbc = null;
        }
    }

    private static enum Action {
        open,
        close,
        attach,
        delete,
        executeSqlBatch,
        sqlite3enc_import,
        backgroundExecuteSqlBatch,
        echoStringValue
    }

    private enum QueryType {
        update,
        insert,
        delete,
        select,
        begin,
        commit,
        rollback,
        other
    }
}

/* vim: set expandtab : */
