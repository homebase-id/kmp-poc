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
            fileState = driveMainIndex.fileState,
            requiredSecurityGroup = driveMainIndex.requiredSecurityGroup,
            fileSystemType = driveMainIndex.fileSystemType,
            userDate = driveMainIndex.userDate,
            fileType = driveMainIndex.fileType,
            dataType = driveMainIndex.dataType,
            archivalStatus = driveMainIndex.archivalStatus,
            historyStatus = driveMainIndex.historyStatus,
            senderId = driveMainIndex.senderId,
            groupId = driveMainIndex.groupId,
            uniqueId = driveMainIndex.uniqueId,
            byteCount = driveMainIndex.byteCount,
            hdrEncryptedKeyHeader = driveMainIndex.hdrEncryptedKeyHeader,
            hdrVersionTag = driveMainIndex.hdrVersionTag,
            hdrAppData = driveMainIndex.hdrAppData,
            hdrLocalVersionTag = driveMainIndex.hdrLocalVersionTag,
            hdrLocalAppData = driveMainIndex.hdrLocalAppData,
            hdrReactionSummary = driveMainIndex.hdrReactionSummary,
            hdrServerData = driveMainIndex.hdrServerData,
            hdrTransferHistory = driveMainIndex.hdrTransferHistory,
            hdrFileMetaData = driveMainIndex.hdrFileMetaData,
            created = driveMainIndex.created,
            modified = driveMainIndex.modified
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
     * @param driveMainIndex The main file record
     * @param tagIndexRecords List of tag index records for this file
     * @param localTagIndexRecords List of local tag index records for this file
     * @param cursor Optional current cursor to be saved
     */
    fun BaseUpsertEntryZapZap(
        driveMainIndex: DriveMainIndex,
        tagIndexRecords: List<DriveTagIndex>,
        localTagIndexRecords: List<DriveLocalTagIndex>,
        cursor : QueryBatchCursor?
    ) {
        database.transaction {
            MainIndexMetaHelpers.upsertDriveMainIndex(database, driveMainIndex);

            database.driveTagIndexQueries.deleteByFile(identityId = driveMainIndex.identityId, driveId = driveMainIndex.driveId, fileId = driveMainIndex.fileId);
            database.driveLocalTagIndexQueries.deleteByFile(identityId = driveMainIndex.identityId, driveId = driveMainIndex.driveId, fileId = driveMainIndex.fileId);

            tagIndexRecords.forEach { tagRecord ->
                database.driveTagIndexQueries.insertTag(
                    identityId = tagRecord.identityId,
                    driveId = tagRecord.driveId,
                    fileId = tagRecord.fileId,
                    tagId = tagRecord.tagId
                )
            }

            localTagIndexRecords.forEach { localTagRecord ->
                database.driveLocalTagIndexQueries.insertLocalTag(
                    identityId = localTagRecord.identityId,
                    driveId = localTagRecord.driveId,
                    fileId = localTagRecord.fileId,
                    tagId = localTagRecord.tagId
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
