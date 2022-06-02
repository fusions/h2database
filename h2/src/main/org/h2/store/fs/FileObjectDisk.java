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
import java.util.Date;

import org.h2.util.FileUtils;

/**
 * This class is extends a java.io.RandomAccessFile.
 */
public class FileObjectDisk extends RandomAccessFile implements FileObject {

    private final String name;
    private long posBefore;
    private byte[] bytesBefore;
    private long lengthBefore;
    private boolean backuped;

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
        if(restore(b)){
          throw e;
        } else {
          throw new IOExceptionAndRollbackFailed(e);
        }
      }
    }
  
    private void backup(int length) throws IOException  {
      backuped = false;

      if( length <= 0) {
        return;
      }

      posBefore = getFilePointer();
      lengthBefore = length();
      
      long valid = lengthBefore - posBefore;
  
      if( valid <= 0) {
        return;
      }
      
      bytesBefore = valid >= length ? new byte[length] : new byte[(int)valid]; 
  
      read(bytesBefore);
      seek(posBefore);

      backuped = true;
    }
  
    private boolean restore(byte[] b) {
      if( ! backuped ) {
        return true;
      }

      try {
        long posCur = getFilePointer();
        int writtenLen = (int)(posCur - posBefore);
    
        if(writtenLen > bytesBefore.length) {
          writtenLen = bytesBefore.length;
        }
    
        // restore file length.
        if( posCur > lengthBefore) {
          setFileLength(lengthBefore);
          printWarning(" - File lenth restored.");
        }
        // restore content
        if( writtenLen > 0){
          seek(posBefore);
          super.write(bytesBefore, 0, writtenLen);
          printWarning(" - File content restored.");
          seek(posBefore);
        }

        // for test: check before/after
        // --------------
        // if( posCur > lengthBefore || writtenLen > 0) {
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

        return true;
      } catch (Exception e) {
        printWarning("File restoration failed with error:");
        e.printStackTrace();
        return false;
      }
    }
  
    private void printWarning(String text) {
      System.out.println("[WARNING] " + new Date().toString() + " " + text);
    }
}
