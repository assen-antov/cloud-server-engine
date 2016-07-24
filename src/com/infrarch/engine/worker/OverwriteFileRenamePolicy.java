package com.infrarch.engine.worker;

import java.io.File;
import java.io.IOException;

import com.oreilly.servlet.multipart.FileRenamePolicy;

public class OverwriteFileRenamePolicy extends Object implements
		FileRenamePolicy {

	public File rename(File f) {
		f.delete();
		try {
			f.createNewFile();
		} catch (IOException ioe) {
			return null;
		}
		return f;
	}
}
