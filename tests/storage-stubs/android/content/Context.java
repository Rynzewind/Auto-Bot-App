package android.content;
public final class Context {private final java.io.File files;public Context(java.io.File f){files=f;f.mkdirs();}public java.io.File getFilesDir(){return files;}}
