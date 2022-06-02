package org.h2.store.fs;

import java.io.IOException;

public class IOExceptionAndRollbackFailed extends IOException{
    private IOException realError;
    
    public IOExceptionAndRollbackFailed(IOException e){
        super("Wrong use of IOExceptionAndRollbackFailed: this class is only for type check. please use getException() to get the real IOException.");
        this.realError = e;
    }

    public IOException getException(){
        return realError;
    }
}