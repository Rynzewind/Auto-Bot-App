package com.kev.fortniteautobot;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
public final class BackupArchiveTest {
 static byte[] zip(String name,byte[]data)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(bytes)){z.putNextEntry(new ZipEntry(name));z.write(data);z.closeEntry();}return bytes.toByteArray();}
 static void rejected(String name,int limit)throws Exception{File stage=Files.createTempDirectory("autobot-reject-").toFile();try{BackupArchive.extract(new ByteArrayInputStream(zip(name,new byte[64])),stage,300,limit);throw new AssertionError("Accepted invalid archive: "+name);}catch(IOException expected){}}
 public static void main(String[]args)throws Exception{
  byte[] data="recorded setup stays intact".getBytes("UTF-8");File stage=Files.createTempDirectory("autobot-valid-").toFile();BackupArchive.extract(new ByteArrayInputStream(zip("rules/party/rule.json",data)),stage,300,1000);if(!java.util.Arrays.equals(data,Files.readAllBytes(new File(stage,"rules/party/rule.json").toPath())))throw new AssertionError("Archive data changed");
  rejected("../outside.json",1000);rejected("/absolute.json",1000);rejected("rules/ready/../../escape.json",1000);rejected("rules/unknown/rule.json",1000);rejected("rules/party/execute.exe",1000);rejected("rules/ready/rule.json",32);
  System.out.println("BackupArchiveTest PASS: byte-preserving extraction, path restrictions, unknown entries, and size limit.");
 }
}
