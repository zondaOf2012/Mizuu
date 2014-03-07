package com.miz.filesources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import jcifs.smb.SmbFile;

import com.miz.abstractclasses.AbstractFileSourceBrowser;
import com.miz.functions.BrowserFileObject;
import com.miz.functions.MizLib;

public class BrowserSmb extends AbstractFileSourceBrowser<SmbFile> {

	public BrowserSmb(SmbFile folder) {
		super(folder);
	}

	@Override
	public boolean browse(int index) {
		return browse(getCurrentFiles()[index]);
	}

	@Override
	public boolean browse(SmbFile folder) {
		if (folder.getName().equals("smb://"))
			return false;
		
		List<BrowserFileObject> list = new ArrayList<BrowserFileObject>();
		List<SmbFile> orderedArray = new ArrayList<SmbFile>();
		List<SmbFile> temp = new ArrayList<SmbFile>();

		try {
			SmbFile[] listFiles = folder.listFiles();
			if (listFiles == null)
				return false;

			for (SmbFile f : listFiles)
				orderedArray.add(f);

			Collections.sort(orderedArray, new Comparator<SmbFile>() {
				@Override
				public int compare(SmbFile f1, SmbFile f2) {
					try {
						if (f1.isDirectory() && !f2.isDirectory()) {  
							// Directory before non-directory  
							return -1;  
						} else if (!f1.isDirectory() && f2.isDirectory()) {  
							// Non-directory after directory  
							return 1;  
						}
					} catch (Exception ignored) {}

					return f1.getName().toLowerCase(Locale.getDefault()).compareTo(f2.getName().toLowerCase(Locale.getDefault()));  
				}
			});

			for (SmbFile f : orderedArray)
				if (MizLib.isValidFilename(f.getName())) {
					list.add(new BrowserFileObject(f.getName(), f.isDirectory(), f.isDirectory() ? 0 : f.length()));
					temp.add(f);
				}

			listFiles = temp.toArray(new SmbFile[temp.size()]);
			temp.clear();
			orderedArray.clear();

			setParentFolder(new SmbFile(folder.getParent()));
			setCurrentFolder(folder);
			setCurrentFiles(listFiles);

			setBrowserFiles(list);
		} catch (Exception e) {
			return false;
		}

		return true;
	}
}
