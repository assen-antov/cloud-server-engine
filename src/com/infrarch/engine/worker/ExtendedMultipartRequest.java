package com.infrarch.engine.worker;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;

import javax.servlet.http.HttpServletRequest;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;
import com.oreilly.servlet.multipart.FileRenamePolicy;

public class ExtendedMultipartRequest {
	private static class ExtendedRenamePolicy implements FileRenamePolicy {
		private LinkedList<File> files  = new LinkedList<File>();
		private LinkedList<File> originalFiles = new LinkedList<File>();
		private FileRenamePolicy policy;
		
		public ExtendedRenamePolicy(FileRenamePolicy policy) {
			this.policy = policy;
		}
		
		public File rename(File originalFile) {
			File file = policy.rename(originalFile);
			files.add(file);
			originalFiles.add(originalFile);
			return file;
		}

		public File[] getFiles() {
			return files.toArray(new File[files.size()]);
		}
		
		public File[] getOriginalFiles() {
			return originalFiles.toArray(new File[originalFiles.size()]);
		}
	}
	
	private ExtendedRenamePolicy policy;
	private MultipartRequest multipartRequest;
	
	public ExtendedMultipartRequest(HttpServletRequest request,
			String saveDirectory, int maxPostSize, String encoding, boolean overwrite)
			throws IOException {
		this.policy = new ExtendedRenamePolicy(overwrite ? new OverwriteFileRenamePolicy() : new DefaultFileRenamePolicy());
		this.multipartRequest = new MultipartRequest(request, saveDirectory, maxPostSize, encoding, this.policy);
	}
	
	public File[] getFiles() {
		return policy.getFiles();
	}
	
	public File[] getOriginalFiles() {
		return policy.getOriginalFiles();
	}

	@SuppressWarnings("unchecked")
	public Enumeration<String> getParameterNames() {
		return multipartRequest.getParameterNames();
	}
	
	public String[] getParameterValues(String name) {
		return multipartRequest.getParameterValues(name);
	}
	
	public String getParameter(String name) {
		return multipartRequest.getParameter(name);
	}
	
	public File getFile(String name) {
		return multipartRequest.getFile(name);
	}
}
