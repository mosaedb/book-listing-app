package com.mosaedb.bookquerying;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by Mosaed on 19/09/16.
 */
class BookAdapter extends ArrayAdapter<Book> {

    BookAdapter(Context context, List<Book> books) {
        super(context, 0, books);
    }

    private static class ViewHolder {
        private TextView bookTitleView;
        private TextView bookAuthorView;
        private ImageView bookCoverView;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        ViewHolder holder;

        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.book_list_item, parent, false);
            holder = new ViewHolder();
            holder.bookTitleView = (TextView) listItemView.findViewById(R.id.book_title);
            holder.bookAuthorView = (TextView) listItemView.findViewById(R.id.book_author);
            holder.bookCoverView = (ImageView) listItemView.findViewById(R.id.book_image);
            listItemView.setTag(holder);
        } else {
            holder = (ViewHolder) listItemView.getTag();
        }

        Book currentBook = getItem(position);

        holder.bookTitleView.setText(currentBook.getBookTitle());
        holder.bookAuthorView.setText(currentBook.getBookAuthor());
        Picasso.with(getContext())
                .load(currentBook.getBookImageLink())
                .resize(280, 400)
                .into(holder.bookCoverView);

        return listItemView;
    }

}