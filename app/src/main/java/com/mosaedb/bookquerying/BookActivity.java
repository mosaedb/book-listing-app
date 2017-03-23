package com.mosaedb.bookquerying;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class BookActivity extends AppCompatActivity {

    private static final String LOG_TAG = BookActivity.class.getSimpleName();

    private String mSearchText;
    private String fullBookRequestUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);

        Button search = (Button) findViewById(R.id.button_search);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (haveInternetConnection()) {
                    createUrlQuery();
                    BookAsyncTask task = new BookAsyncTask();
                    task.execute(fullBookRequestUrl);
                } else {
                    Toast.makeText(BookActivity.this, R.string.message_no_internet, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public boolean haveInternetConnection() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) BookActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    private String createUrlQuery() {
        EditText editTextView = (EditText) findViewById(R.id.editText_field);
        mSearchText = editTextView.getText().toString();
        String prefixQueryUrl = "https://www.googleapis.com/books/v1/volumes?q=";
        String maxResults = "&maxResults=40";
        fullBookRequestUrl = prefixQueryUrl
                        + mSearchText
                        + maxResults;
        return fullBookRequestUrl;
    }

    private void updateUi(List<Book> books) {
        final BookAdapter bookAdapter = new BookAdapter(this, books);
        ListView listView = (ListView) findViewById(R.id.listView_books);
        TextView textView = (TextView) findViewById(R.id.textView_empty);

        if (books == null) {
            textView.setText(R.string.no_results);
            listView.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            listView.setAdapter(bookAdapter);
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Find the current book that was clicked on
                Book currentBook = bookAdapter.getItem(position);

                // Convert the String URL into a URI object (to pass into the Intent constructor)
                Uri bookUri = null;
                if (currentBook != null) {
                    bookUri = Uri.parse(currentBook.getBookPreviewLink());
                }

                // Create a new intent to view the book URI
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, bookUri);
                if (websiteIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(websiteIntent);
                }
            }
        });
    }

    private class BookAsyncTask extends AsyncTask<String, Void, List<Book>> {

        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSearchText = Uri.encode(mSearchText);
            mProgressDialog = new ProgressDialog(BookActivity.this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        @Override
        protected ArrayList<Book> doInBackground(String... urls) {
            if (urls.length < 1 || urls[0] == null) {
                return null;
            }

            return fetchBookData(urls[0]);
        }

        @Override
        protected void onPostExecute(List<Book> books) {
            updateUi(books);
            mProgressDialog.dismiss();
        }

        public ArrayList<Book> fetchBookData(String requestUrl) {
            // Create URL object
            URL url = createUrl(requestUrl);

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                // TODO Handle the IOException
            }

            // Extract relevant fields from the JSON response and create and return
            // an {@link Book} object as the result for the {@link BookAsyncTask}
            return extractItemFromJson(jsonResponse);
        }

        /**
         * Returns new URL object from the given string URL.
         */
        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }

        /**
         * Make an HTTP request to the given URL and return a String as the response.
         */
        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";

            // If the URL is null, then return early.
            if (url == null) {
                return jsonResponse;
            }

            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();

                // If the request was successful (response code 200),
                // then read the input stream and parse the response.
                if (urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                } else {
                    Log.e(LOG_TAG, "Error response code " + urlConnection.getResponseCode());
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving the book JSON results.", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        /**
         * Convert the {@link InputStream} into a String which contains the
         * whole JSON response from the server.
         */
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        private ArrayList<Book> extractItemFromJson(String bookJSON) {

            if (TextUtils.isEmpty(bookJSON)) {
                return null;
            }

            ArrayList<Book> books = new ArrayList<>();

            try {
                JSONObject baseJsonResponse = new JSONObject(bookJSON);
                int totalItems = baseJsonResponse.getInt("totalItems");
                if (totalItems == 0) {
                    return null;
                }

                JSONArray itemsArray = baseJsonResponse.optJSONArray("items");
                if (itemsArray != null) {
                    String bookTitle;
                    String bookAuthor = "Unknown";
                    String bookImageLink = null;
                    String bookPreviewLink;

                    for (int i = 0; i < itemsArray.length(); i++) {
                        if (itemsArray.length() > 0) {
                            JSONObject currentItem = itemsArray.getJSONObject(i);
                            JSONObject volumeInfo = currentItem.getJSONObject("volumeInfo");
                            bookTitle = volumeInfo.getString("title");

                            JSONArray authorsArray = volumeInfo.optJSONArray("authors");
                            if (authorsArray != null) {
                                bookAuthor = authorsArray.getString(0);
                            }

                            JSONObject imageLinks = volumeInfo.optJSONObject("imageLinks");
                            if (imageLinks != null) {
                                bookImageLink = imageLinks.getString("thumbnail");
                            }

                            bookPreviewLink = volumeInfo.getString("previewLink");

                            Book book = new Book(bookTitle, bookAuthor, bookImageLink, bookPreviewLink);
                            books.add(book);
                        }
                    }
                }

                return books;

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the book JSON results", e);
            }
            return null;
        }
    }
}
