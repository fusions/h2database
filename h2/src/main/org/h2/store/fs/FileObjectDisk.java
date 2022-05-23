/*
 * Copyright 2004-2009 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.store.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.h2.util.FileUtils;

import java.util.Date;
// for debug
import java.io.File;

/**
 * This class is extends a java.io.RandomAccessFile.
 */
public class FileObjectDisk extends RandomAccessFile implements FileObject {

    private final String name;
    private long posBefore;
    private byte[] bytesBefore;
    private long lengthBefore;

    FileObjectDisk(String fileName, String mode) throws FileNotFoundException {
        super(fileName, mode);
        this.name = fileName;
    }

    public void sync() throws IOException {
        getFD().sync();
    }

    public void setFileLength(long newLength) throws IOException {
        FileUtils.setLength(this, newLength);
    }

    public String getName() {
        return name;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      try{
        backup(len);
        super.write(b, off, len);
      } catch (IOException e) {
        printWarning("Error occured when writing file(" + name + ").");
        // e.printStackTrace(System.out);
        restore(b);
        seek(posBefore); //for redo writing in FileStore.java:339
        throw e;
      }
    }
  
    private void backup(int length) throws IOException  {
      posBefore = getFilePointer();
      lengthBefore = length();
      
      long valid = lengthBefore - posBefore;
  
      if( valid <= 0) {
        bytesBefore = new byte[0]; 
        return;
      }
      
      bytesBefore = valid >= length ? new byte[length] : new byte[(int)valid]; 
  
      read(bytesBefore);
      seek(posBefore);
    }
  
    private void restore(byte[] b) throws IOException  {
      long posCur = getFilePointer();
      long lenCur = length();
      int writtenLen = (int)(posCur - posBefore);
  
      if(writtenLen > bytesBefore.length) {
        writtenLen = bytesBefore.length;
      }
  
      try {
        // restore file length.
        if( posCur > lengthBefore) {
          setFileLength(lengthBefore);
          printWarning(" - File lenth restored.");
        }
        // restore content
        if( bytesBefore.length > 0 && writtenLen > 0){
          seek(posBefore);
          super.write(bytesBefore, 0, writtenLen);
          printWarning(" - File content restored.");
        }
  
        // debug: check before/after
        // --------------
        // if( posCur > lengthBefore || (bytesBefore.length > 0 && writtenLen > 0)) {
        //   byte bytesRestored[] = new byte[bytesBefore.length];
        //   seek(posBefore);
        //   read(bytesRestored);
        //   String strBefore = new String(bytesBefore);
        //   String strRestored = new String(bytesRestored);
        //   if( ! strBefore.equals(strRestored)){
        //     System.err.println(new Date().toString() + " - Restored content not equals backuped content!! pos = " + posBefore + ", len = " + bytesBefore.length);
        //   }
        // }
        // --------------
      } catch (IOException e) {
        printWarning("File restoration failed with error:");
        e.printStackTrace();
        // throw e;
      }
    }
  
    private void printWarning(String text) {
      System.out.println("[WARNING] " + new Date().toString() + " " + text);
    }
}
