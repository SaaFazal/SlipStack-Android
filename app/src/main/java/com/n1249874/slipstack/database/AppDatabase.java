package com.n1249874.slipstack.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = { ReceiptEntity.class, LineItemEntity.class,
        SplitHistoryEntity.class }, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract ReceiptDao receiptDao();

    public abstract LineItemDao lineItemDao();

    public abstract SplitHistoryDao splitHistoryDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `line_items` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`receiptId` INTEGER NOT NULL, " +
                            "`productName` TEXT, " +
                            "`price` REAL NOT NULL, " +
                            "FOREIGN KEY(`receiptId`) REFERENCES `receipts`(`id`) ON DELETE CASCADE)");
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_line_items_receiptId` ON `line_items` (`receiptId`)");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE receipts ADD COLUMN imagePath TEXT");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `split_history` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`receiptMerchant` TEXT, " +
                    "`date` TEXT, " +
                    "`totalAmount` REAL NOT NULL, " +
                    "`peopleCount` INTEGER NOT NULL, " +
                    "`amountPerPerson` REAL NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `split_history` ADD COLUMN `peopleNames` TEXT");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "slipstack_db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            .addCallback(new Callback() {
                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    // Enable Foreign Keys for CASCADE DELETE
                                    db.execSQL("PRAGMA foreign_keys = ON;");
                                    // Remove orphaned line items from previous deletions
                                    db.execSQL(
                                            "DELETE FROM line_items WHERE receiptId NOT IN (SELECT id FROM receipts)");
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
