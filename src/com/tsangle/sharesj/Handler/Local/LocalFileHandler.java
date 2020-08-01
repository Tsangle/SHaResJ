package com.tsangle.sharesj.Handler.Local;

import com.tsangle.sharesj.Handler.FileHandler;
import com.tsangle.sharesj.HttpServer.RequestSocket;

public class LocalFileHandler extends FileHandler {
    @Override
    protected void ReturnFileSystemEntries(RequestSocket requestSocket) throws Exception {
        super.ReturnFileSystemEntries(requestSocket);
    }
}
