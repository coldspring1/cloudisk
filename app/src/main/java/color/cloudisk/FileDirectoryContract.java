package color.cloudisk;

import android.provider.BaseColumns;

/**
 * Created by Dongke on 3/3/2016.
 */
public final class FileDirectoryContract {

     public FileDirectoryContract(){}

     public static abstract class fileTable implements BaseColumns {
            public static final String TABLE_NAME = "fileSystem";
            public static final String FILE_NAME= "fileName";
            public static final String PARENT_DIRECTORY_OF_FILE="parentFolder";
            public static final String FILE_MODIFY_TIME="modifyTime";
            public static final String FILE_TYPE ="fileType";
            public static final String FILE_ID = "fileId";


        }
    }

