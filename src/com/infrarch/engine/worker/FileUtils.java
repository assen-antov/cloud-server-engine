package com.infrarch.engine.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

/**
 * File utility methods.
 * 
 * @author Assen Antov
 * @version 1.0, 05/2016
 */
public class FileUtils {

	private static final Logger logger = Logger.getLogger(FileUtils.class);
	
	/**
	 * Copies a file. If a file of the same name exists, a new name for 
	 * the destination file will be generated. 
	 * 
	 * @param f source file 
	 * @param destDir destination directory
	 * @return a {@code File} instance for the new file
	 */
	public static File copyFile(File f, File destDir) {
		return copyFile(f, destDir, false, false);
	}
	
	/**
	 * Copies a file. If a file of the same name exists and {@code overwrite} 
	 * is set to <code>false</code>, a new name for the destination file will be 
	 * generated. The destination file will be overwritten otherwise. 
	 * 
	 * @param f source file 
	 * @param destDir destination directory 
	 * @param overwrite remove the file with the same name from the destination or not
	 * @return a {@code File} instance for the new file
	 */
	public static File copyFile(File f, File destDir, boolean overwrite) {
		return copyFile(f, destDir, overwrite, false);
	}
	
	/**
	 * Moves a file. If a file of the same name exists, a new name for 
	 * the destination file will be generated. 
	 * 
	 * @param f source file 
	 * @param destDir destination directory
	 * @return a {@code File} instance for the new file
	 */
	public static File moveFile(File f, File destDir) {
		return copyFile(f, destDir, false, true);
	}
	
	/**
	 * Moves a file. If a file of the same name exists and {@code overwrite} 
	 * is set to <code>false</code>, a new name for the destination file will be 
	 * generated. The destination file will be overwritten otherwise. 
	 * 
	 * @param f source file 
	 * @param destDir destination directory 
	 * @param overwrite remove the file with the same name from the destination or not
	 * @return a {@code File} instance for the new file
	 */
	public static File moveFile(File f, File destDir, boolean overwrite) {
		return copyFile(f, destDir, overwrite, true);
	}
	
	/**
	 * Copies a file. If a file of the same name exists and {@code overwrite} 
	 * is set to <code>false</code>, a new name for the destination file will be 
	 * generated. The destination file will be overwritten otherwise. If 
	 * {@code deleteSrc} is set to <code>true</code>, the source file will be 
	 * deleted (that is, the operation will be move).
	 * 
	 * @param f source file 
	 * @param destDir destination directory 
	 * @param overwrite remove the file with the same name from the destination or not
	 * @param deleteSrc delete the source file and revision or not
	 * @return a {@code File} instance for the new file
	 */
	public static File copyFile(File f, File destDir, boolean overwrite, boolean deleteSrc) {
		destDir.mkdirs();
		
		// check if file exists
		if (!f.exists()) return null;
		
		// check if should overwrite 
		File bakFile = null;
		if (overwrite) {
			
			// for now rename the destination file, if exists
			File destFile = new File(destDir, f.getName());
			if (destFile.exists()) {
				bakFile = getUniqueFileName(f.getName()+".bak", destDir);
				destFile.renameTo(bakFile);
			}
		}
		
		File result = nativeCopyFile(f, destDir);
		
		// see if should delete the bak file
		if (overwrite && result != null) {
			bakFile.delete();
		}
		
		return result;
	}
	
	private static File nativeCopyFile(File f1, File destDir) {
		logger.debug("Native copy of file " + f1.getAbsolutePath() + " to " + destDir.getAbsolutePath());
		File destFile = getUniqueFileName(f1.getName(), destDir);
		try {
			org.apache.commons.io.FileUtils.copyFile(f1, destFile);
		} catch (IOException ioe) {
			logger.debug("Could not copy " + f1.getAbsolutePath() + " to " + destDir.getAbsolutePath() + "; " + ioe);
			return null;
		} catch (NullPointerException npe) {
			logger.debug("Could not copy files due to null parameters");
			return null;
		}
		return destFile;
		
		/*
		File f2 = new File(destDir, f1.getName());
		if (f2.exists()) return false;
		
		try {
			f2.createNewFile();
			InputStream fr = new BufferedInputStream(new FileInputStream(f1), 2*1024*1024);
			OutputStream fw = new BufferedOutputStream(new FileOutputStream(f2), 2*1024*1024);
			int rb = -1;
			while ((rb = fr.read()) != -1) fw.write(rb);
			fr.close();
			fw.close();
		} catch (IOException ioe) { return false; }
		
		return true;
		*/
	}
	
	/**
	 * Moves the file to the recycle bin (if such is supported) or
	 * simply deletes it.
	 * 
	 * @param f file to delete
	 * @return success or failure
	 */
	public static boolean recycleFile(File f) {
		return f.delete();
	}
	
	/**
	 * Moves the contents of the specified directory to the recycle bin 
	 * (if such is supported) and deletes the original.
	 * 
	 * @param fDir directory
	 * @return success or failure
	 */
	public static boolean recycleDir(File fDir) {
		return deleteDir(fDir);
	}
	
	/**
	 * Deletes the specified directory from the file system. Will throw an 
	 * {@link IllegalArgumentException} if the parameter is not a directory.
	 * 
	 * @param dir directory to delete
	 * @return success or failure
	 */
	public static boolean deleteDir(File dir) {
		if (!dir.isDirectory()) throw new IllegalArgumentException("Not a directory: " + dir.getAbsolutePath());
		
		System.gc();
		boolean b = FileUtils.deleteInDir(dir) && dir.delete();
		if (b) logger.debug("Deleted directory: " + dir.getAbsolutePath());
		else logger.warn("Failed deleting directory: " + dir.getAbsolutePath());
		return b;
	}

	public static boolean deleteInDir(File dir) {
		File[] files = dir.listFiles();
		
		boolean b = true;
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					if (!deleteDir(files[i])) b = false;
				}
				if (!files[i].delete()) b = false;
			}
			if (b) logger.debug("Deleted all files in directory: " + dir.getAbsolutePath());
			else logger.warn("Failed deleting all files in directory: " + dir.getAbsolutePath());
		}
		
		return b;
	}
	
	/**
	 * Checks if a directory is empty. Returns <code>true</code> if directory's
	 * table contains no entries and there are no sub-directories. System files 
	 * will be ignored. Will throw an {@link IllegalArgumentException} 
	 * if the parameter is not a directory.
	 * 
	 * @param dir name of the directory to check
	 * @return empty or not
	 */
	public static boolean isEmpty(String dir) {
		return isEmpty(new File(dir));
	}
	
	/**
	 * Checks if a directory is empty. Returns <code>true</code> if directory's
	 * table contains no entries and there are no sub-directories. System files 
	 * will be ignored. Will throw an {@link IllegalArgumentException} 
	 * if the parameter is not a directory.
	 * 
	 * @param fDir directory to check
	 * @return empty or not
	 */
	public static boolean isEmpty(File fDir) {
		if (!fDir.isDirectory()) throw new IllegalArgumentException("Not a directory: " + fDir.getAbsolutePath());
		boolean hasFiles = false;
		File[] list = fDir.listFiles();
		for (File f : list) {
			if (!FileManager.getInstance().isSystemFile(f)) {
				hasFiles = true;
				break;
			}
		}
		return !hasFiles;
	}
	
	/**
	 * Returns a {@code File} instance for the given file name and containing
	 * directory. If a file of that name exists, sets a different name.
	 * 
	 * @param fn file name
	 * @param fDir target directory
	 * @return a unique {@code File} instance or <code>null</code> on failure
	 */
	public static File getUniqueFileName(String fn, File fDir) {
		File newFile = new File(fDir, fn);
		int i = 0;
		while (newFile != null && newFile.exists()) {
			int idx = fn.lastIndexOf('.');
			newFile = new File(fDir, fn.substring(0, idx) + "(" + ++i + ")" + fn.substring(idx, fn.length())); 
		}
		return newFile;
	}
	
	public static boolean copyDir(File srcDir, File destDir) {
		boolean b = nativeCopyDir(srcDir, destDir);
		return b;
	}
	
	public static boolean moveDir(File srcDir, File destDir) {
		boolean b = nativeCopyDir(srcDir, destDir);
		return b && deleteDir(srcDir);
	}
	
	private static boolean nativeCopyDir(File srcDir, File destDir) {
		logger.debug("Native copy of directory " + srcDir.getAbsolutePath() + " to " + destDir.getAbsolutePath());
		if (!srcDir.isDirectory()) return false;
		File[] files = srcDir.listFiles();
		if (files == null) return true;
		
		File destSubDir = new File(destDir, srcDir.getName());
		if (!destSubDir.exists()) { if (!destSubDir.mkdirs()) return false; }
		
		long lastModified = srcDir.lastModified();
		
		boolean b = true;
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				if (!nativeCopyDir(files[i], new File(destSubDir, files[i].getName()))) b = false;
			}
			else { 
				if (nativeCopyFile(files[i], destSubDir) == null) b = false;
			}
		}
		
		destSubDir.setLastModified(lastModified);
		
		return b;
	}
	
	public static String normalizeFileName(String name) {
		// don't allow spaces in the beginning and end of the name
		name = name.trim();
		// don't allow the file to end in a dot
		if (name.endsWith("."))
			name += "---";
		// replace invalid characters with underscores
		name = name.replaceAll("[\\\\/<>:\"\\|\\?\\*]", "_");
		return name;
	}

	public static boolean isValidFileName(String name) {
		return !name.matches("(.*[\\\\/<>:\"\\|\\?\\*].*)|( .*)|(.*[ \\.])");
	}

	public static String formatDirectory(String dir) {
		if (dir == null || dir.isEmpty())
			return dir;
		dir = dir.replace('\\', '/');
		dir = dir.replace("/+$", "");
		dir = "/" + dir;
		dir = dir.replaceAll("/+", "/");
		return dir;
	}

	public static long getSize(File f) {
		if (!f.exists()) return 0;
		if (f.isFile()) return f.length();
		
		long size = 0;
		
		File[] files = f.listFiles();
	
		for (int i = 0; i < files.length; i++) {
			size += getSize(new File(f, files[i].getName()));
		}
		
		return size;
	}

	/**
	 * Note: the stream must be opened before calling this method and closed afterwards.
	 * 
	 * @param dir the directory to zip
	 * @param out a zip stream to use
	 * @param recurse go into the tree or not
	 */
	public static int zipDir(File dir, ZipOutputStream out, boolean recurse) {
		return FileUtils.zipDir(dir, out, recurse, dir);
	}

	public static int zipDir(File dir, ZipOutputStream out, boolean recurse, File base) {
		int nFiles = 0;
		
		File[] files = dir.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory() && recurse) nFiles += zipDir(files[i], out, true);
				else {
					String fileName = FileUtils.getRelativeDir(base, dir) + File.separator + files[i].getName();
					while (fileName.startsWith(File.separator))
						fileName = fileName.substring(1);
					if (fileName.length() == 0)
						continue;
					FileUtils.zipFile(files[i], out, fileName);
					nFiles++;
				}
			}
		}
	    
		return nFiles;
	}

	public static long zipFile(File f, ZipOutputStream out) {
		return FileUtils.zipFile(f, out, f.getAbsolutePath());
	}

	public static long zipFile(File f, ZipOutputStream out, String fileName) {
		// a buffer for reading the files
		byte[] buf = new byte[1024];
	
		try {
	
			FileInputStream in = new FileInputStream(f);
	
			// add ZIP entry to output stream.
			ZipEntry entry = new ZipEntry(fileName);
			entry.setTime(f.lastModified());
			out.putNextEntry(entry);
	
			// transfer bytes from the file to the ZIP file
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
	
			// complete the entry
			out.closeEntry();
			in.close();
		} catch (IOException e) {
			return -1;
		}
		
		return f.length();
	}

	public static String getRelativeDir(File parentDir, File dir) {
		String d1 = parentDir.getAbsolutePath();
		String d2 = dir.getAbsolutePath();
		if (d2.startsWith(d1)) {
			d2 = d2.substring(d1.length());
			if (d2.isEmpty())
				d2 = "/";
			return formatDirectory(d2);
		}
		return null;
	}
}
