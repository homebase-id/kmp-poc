package id.homebase.homebasekmppoc.prototype.lib.database

import id.homebase.homebasekmppoc.lib.database.DriveLocalTagIndex
import id.homebase.homebasekmppoc.lib.database.DriveMainIndex
import id.homebase.homebasekmppoc.lib.database.DriveTagIndex
import id.homebase.homebasekmppoc.lib.database.OdinDatabase
import id.homebase.homebasekmppoc.prototype.lib.drives.query.QueryBatchCursor
import kotlin.uuid.Uuid

/**
 * Helper functions for DriveMainIndex operations
 */
object MainIndexMetaHelpers {
   /**
     * Helper upsert function that takes a DriveMainIndex record and calls
     * database.upsertDriveMainIndex() with all the members.
     */
    fun upsertDriveMainIndex(
       database: OdinDatabase,
       driveMainIndex: DriveMainIndex
    ) {
        database.driveMainIndexQueries.upsertDriveMainIndex(
            identityId = driveMainIndex.identityId,
            driveId = driveMainIndex.driveId,
            fileId = driveMainIndex.fileId,
            globalTransitId = driveMainIndex.globalTransitId,
            uniqueId = driveMainIndex.uniqueId,
            groupId = driveMainIndex.groupId,
            senderId = driveMainIndex.senderId,
            fileType = driveMainIndex.fileType,
            dataType = driveMainIndex.dataType,
            archivalStatus = driveMainIndex.archivalStatus,
            historyStatus = driveMainIndex.historyStatus,
            userDate = driveMainIndex.userDate,
            created = driveMainIndex.created,
            modified = driveMainIndex.modified,
            systemFileType = driveMainIndex.systemFileType,
            jsonHeader = driveMainIndex.jsonHeader,
        )
    }
}

/**
 * Processes file metadata with associated tags from different index tables.
 * Takes a DriveMainIndex record and lists of DriveTagIndex and DriveLocalTagIndex records.
 */
class FileMetadataProcessor(
    private val database: OdinDatabase
) {

    fun deleteEntryDriveMainIndex(
        database: OdinDatabase,
        identityId : Uuid,
        driveId : Uuid,
        fileId : Uuid
    ) {
        database.transaction {
            database.driveMainIndexQueries.deleteBy(identityId, driveId, fileId);
            database.driveTagIndexQueries.deleteByFile(identityId,driveId,fileId);
            database.driveLocalTagIndexQueries.deleteByFile(identityId,driveId,fileId);
        }
    }


/**
     * Stores driveMainIndex and tags and optionally a cursor in one commit
     * @param jsonHeader JSON header string containing file metadata and tags
     * @param identityId Identity ID for the record
     * @param driveId Drive ID for the record
     * @param tagIndexRecords Additional tag index records for this file (merged with tags from JSON)
     * @param localTagIndexRecords Additional local tag index records for this file (merged with local tags from JSON)
     * @param cursor Optional current cursor to be saved
     */
    fun BaseUpsertEntryZapZap(
        identityId: Uuid,
        driveId: Uuid,
        jsonHeader: String,
        cursor : QueryBatchCursor?
    ) {
        // Parse JSON header to extract DriveMainIndex fields and tag records
        val parsedResult = parseJsonHeaderToDriveMainIndex(identityId, driveId, jsonHeader)

        database.transaction {
            MainIndexMetaHelpers.upsertDriveMainIndex(database, parsedResult.driveMainIndex);

            database.driveTagIndexQueries.deleteByFile(identityId = identityId, driveId = driveId, fileId = parsedResult.driveMainIndex.fileId);
            database.driveLocalTagIndexQueries.deleteByFile(identityId = identityId, driveId = driveId, fileId = parsedResult.driveMainIndex.fileId);

            parsedResult.tagIndexRecords.forEach { tagRecord ->
                database.driveTagIndexQueries.insertTag(
                    identityId = tagRecord.identityId,
                    driveId = tagRecord.driveId,
                    fileId = tagRecord.fileId,
                    tagId = tagRecord.tagId
                )
            }

            parsedResult.localTagIndexRecords.forEach { tagRecord ->
                database.driveLocalTagIndexQueries.insertLocalTag(
                    identityId = tagRecord.identityId,
                    driveId = tagRecord.driveId,
                    fileId = tagRecord.fileId,
                    tagId = tagRecord.tagId
                )
            }

            if (cursor != null)
            {
                val cursorSync = CursorSync(database)
                cursorSync.saveCursor(cursor)
            }
        }
    }
}
