package com.jhon.mihealthreader;

import android.os.ParcelFileDescriptor;

interface IUserService {
    List<String> run(String command);
    ParcelFileDescriptor openFile(String path);
}
