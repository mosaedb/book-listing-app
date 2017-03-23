package com.mosaedb.bookquerying;

/**
 * Created by Mosaed on 19/09/16.
 */
public class Book {
    private String mBookTitle;
    private String mBookAuthor;
    private String mBookImageLink;
    private String mBookPreviewLink;

    public Book(String bookTitle, String bookAuthor, String bookImageLink, String bookPreviewLink) {
        this.mBookTitle = bookTitle;
        this.mBookAuthor = bookAuthor;
        this.mBookImageLink = bookImageLink;
        mBookPreviewLink = bookPreviewLink;
    }

    public String getBookTitle() {
        return mBookTitle;
    }

    public String getBookAuthor() {
        return mBookAuthor;
    }

    public String getBookImageLink() {
        return mBookImageLink;
    }

    public String getBookPreviewLink() {
        return mBookPreviewLink;
    }
}
