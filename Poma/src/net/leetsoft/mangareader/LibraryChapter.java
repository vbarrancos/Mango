package net.leetsoft.mangareader;

import java.io.Serializable;

public class LibraryChapter implements Serializable
{
	public Manga            manga             = new Manga();
	public Chapter          chapter           = new Chapter();
	public int              chapterIndex;
	public int              chapterCount;

	public int              siteId;
	public String           path;
	public long             rowId;

	// Filesystemchapter data
	public FilesystemChapter filesystemChapter = null;
}
