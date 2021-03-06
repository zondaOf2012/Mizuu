/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miz.mizuu.fragments;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.db.DbAdapter;
import com.miz.functions.AsyncTask;
import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.functions.MovieVersion;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.views.ObservableScrollView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.test.smbstreamer.variant1.Streamer;

public class MovieDetailsFragment extends Fragment {

	private Movie thisMovie;
	private DbAdapter db;
	private int movieId;
	private TextView textTitle, textPlot, textSrc, textGenre, textRuntime, textReleaseDate, textRating, textTagline, textCertification;
	private boolean ignorePrefixes, useWildcard, prefsDisableEthernetWiFiCheck, prefsRemoveMoviesFromWatchlist, ignoreNfo;
	private ImageView background, cover;
	private long videoPlaybackStarted, videoPlaybackEnded;
	private Picasso mPicasso;
	private Typeface mLight;
	private Drawable mActionBarBackgroundDrawable;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public MovieDetailsFragment() {}

	public static MovieDetailsFragment newInstance(int movieId) { 
		MovieDetailsFragment pageFragment = new MovieDetailsFragment();
		Bundle bundle = new Bundle();
		bundle.putInt("movieId", movieId);
		pageFragment.setArguments(bundle);
		return pageFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(true);

		ignorePrefixes = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("prefsIgnorePrefixesInTitles", false);
		ignoreNfo = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("prefsIgnoreNfoFiles", true);
		useWildcard = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("prefsIgnoreFileType", false);
		prefsDisableEthernetWiFiCheck = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("prefsDisableEthernetWiFiCheck", false);
		prefsRemoveMoviesFromWatchlist = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("prefsRemoveMoviesFromWatchlist", true);

		// Get the database ID of the movie in question
		movieId = getArguments().getInt("movieId");

		mLight = MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf");

		// Set up database and open it
		db = MizuuApplication.getMovieAdapter();

		Cursor cursor = null;
		try {
			cursor = db.fetchMovie(movieId);
			thisMovie = new Movie(getActivity(),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_ROWID)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TITLE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_PLOT)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TAGLINE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TMDBID)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_IMDBID)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RATING)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RELEASEDATE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CERTIFICATION)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RUNTIME)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TRAILER)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_GENRES)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FAVOURITE)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CAST)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COLLECTION)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_EXTRA_2)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TO_WATCH)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_HAS_WATCHED)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COVERPATH)),
					cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_EXTRA_1)),
					ignorePrefixes,
					ignoreNfo
					);
		} catch (Exception e) {
			getActivity().finish();
			return;
		} finally {
			cursor.close();
		}

		if (db.hasMultipleVersions(thisMovie.getTmdbId()) && !thisMovie.isUnidentified()) {
			thisMovie.setMultipleVersions(db.getRowIdsForMovie(thisMovie.getTmdbId()));
		}

		mPicasso = MizuuApplication.getPicasso(getActivity());

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movie-cover-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movie-backdrop-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("movie-play-button"));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.play_video:
			playMovie();
		}
		return false;
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("movie-play-button")) {
				playMovie();
			} else {
				loadImages();
			}
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.movie_details, container, false);
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (MizLib.isPortrait(getActivity())) {
			if (!MizLib.isTablet(getActivity()))
				MizLib.addActionBarPaddingBottom(getActivity(), view.findViewById(R.id.scrollView1));

			mActionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0x00000000, 0xaa000000});
			getActivity().getActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);

			ObservableScrollView sv = (ObservableScrollView) view.findViewById(R.id.scrollView1);
			sv.setOnScrollChangedListener(new OnScrollChangedListener() {
				@Override
				public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
					final int headerHeight = view.findViewById(R.id.imageBackground).getHeight() - getActivity().getActionBar().getHeight();
					final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
					final int newAlpha = (int) (ratio * 255);
					mActionBarBackgroundDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{Color.parseColor("#" + ((Integer.toHexString(newAlpha).length() == 1) ? ("0" + Integer.toHexString(newAlpha)) : Integer.toHexString(newAlpha)) + "080808"), (newAlpha >= 170) ? Color.parseColor("#" + Integer.toHexString(newAlpha) + "080808") : 0xaa080808});
					getActivity().getActionBar().setBackgroundDrawable(mActionBarBackgroundDrawable);
				}
			});
		}

		background = (ImageView) view.findViewById(R.id.imageBackground);
		textTitle = (TextView) view.findViewById(R.id.movieTitle);
		textPlot = (TextView) view.findViewById(R.id.textView2);
		textSrc = (TextView) view.findViewById(R.id.textView3);
		textGenre = (TextView) view.findViewById(R.id.textView7);
		textRuntime = (TextView) view.findViewById(R.id.textView9);
		textReleaseDate = (TextView) view.findViewById(R.id.textReleaseDate);
		textRating = (TextView) view.findViewById(R.id.textView12);
		textTagline = (TextView) view.findViewById(R.id.textView6);
		textCertification = (TextView) view.findViewById(R.id.textView11);
		cover = (ImageView) view.findViewById(R.id.traktIcon);

		// Set the movie title
		textTitle.setVisibility(View.VISIBLE);
		textTitle.setText(thisMovie.getTitle());
		textTitle.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf"));
		textTitle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		textPlot.setTypeface(mLight);
		textSrc.setTypeface(mLight);
		textGenre.setTypeface(mLight);
		textRuntime.setTypeface(mLight);
		textReleaseDate.setTypeface(mLight);
		textRating.setTypeface(mLight);
		textCertification.setTypeface(mLight);

		// Set the movie plot
		textPlot.setText(thisMovie.getPlot());

		// Set the movie file source
		if (thisMovie.hasMultipleVersions() && !thisMovie.isUnidentified()) {
			String sources = "";
			MovieVersion[] versions = thisMovie.getMultipleVersions();
			for (int i = 0; i < versions.length; i++)
				sources = sources + MizLib.transformSmbPath(versions[i].getFilepath()) + "\n\n";
			textSrc.setText(sources.trim());
		} else {
			textSrc.setText(thisMovie.getFilepath());
		}

		// Set movie tag line
		if (thisMovie.getTagline().isEmpty())
			textTagline.setVisibility(TextView.GONE);
		else
			textTagline.setText(thisMovie.getTagline());

		// Set the movie genre
		if (!MizLib.isEmpty(thisMovie.getGenres())) {
			textGenre.setText(thisMovie.getGenres());
		} else {
			textGenre.setText(R.string.stringNA);
		}

		// Set the movie runtime
		textRuntime.setText(MizLib.getPrettyTime(getActivity(), Integer.parseInt(thisMovie.getRuntime())));

		// Set the movie release date
		textReleaseDate.setText(MizLib.getPrettyDate(getActivity(), thisMovie.getReleasedate()));

		// Set the movie rating
		if (!thisMovie.getRating().equals("0.0/10")) {
			if (thisMovie.getRating().contains("/")) {
				try {
					int rating = (int) (Double.parseDouble(thisMovie.getRating().substring(0, thisMovie.getRating().indexOf("/"))) * 10);
					textRating.setText(Html.fromHtml(rating + "<small> %</small>"));
				} catch (NumberFormatException e) {
					textRating.setText(Html.fromHtml(thisMovie.getRating().replace("/", "<small> / ") + "</small>"));
				}
			} else {
				textRating.setText(thisMovie.getRating());
			}
		} else {
			textRating.setText(R.string.stringNA);
		}

		// Set the movie certification
		if (!MizLib.isEmpty(thisMovie.getCertification())) {
			textCertification.setText(thisMovie.getCertification());
		} else {
			textCertification.setText(R.string.stringNA);
		}

		loadImages();
	}

	public void onResume() {
		super.onResume();

		videoPlaybackEnded = System.currentTimeMillis();

		if (videoPlaybackStarted > 0 && videoPlaybackEnded - videoPlaybackStarted > (1000 * 60 * 5)) {
			if (!thisMovie.hasWatched())
				watched(false); // Mark it as watched
		}
	}

	private void checkIn() {
		new Thread() {
			@Override
			public void run() {
				MizLib.checkInMovieTrakt(thisMovie, getActivity());
			}
		}.start();
	}

	private void playMovie() {
		if (thisMovie.hasOfflineCopy()) {
			playMovie(thisMovie.getOfflineCopyUri(), false);
		} else if (thisMovie.hasMultipleVersions() && !thisMovie.isUnidentified()) {
			final MovieVersion[] versions = thisMovie.getMultipleVersions();
			CharSequence[] items = new CharSequence[versions.length];
			for (int i = 0; i < versions.length; i++)
				items[i] = MizLib.transformSmbPath(versions[i].getFilepath());

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.fileToPlay));
			builder.setItems(items, new AlertDialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					playMovie(versions[which].getFilepath(), versions[which].getFilepath().contains("smb:/"));
				};
			});
			builder.show();
		} else {
			playMovie(thisMovie.getFilepath(), thisMovie.isNetworkFile());
		}
	}

	private void playMovie(String filepath, boolean isNetworkFile) {
		videoPlaybackStarted = System.currentTimeMillis();
		if (filepath.toLowerCase(Locale.getDefault()).matches(".*(cd1|part1).*")) {
			new GetSplitFiles(filepath, isNetworkFile).execute();
		} else {
			if (isNetworkFile) {
				playNetworkFile(filepath);
			} else {
				try { // Attempt to launch intent based on the MIME type
					getActivity().startActivity(MizLib.getVideoIntent(filepath, useWildcard, thisMovie));
					checkIn();
				} catch (Exception e) {
					try { // Attempt to launch intent based on wildcard MIME type
						getActivity().startActivity(MizLib.getVideoIntent(filepath, "video/*", thisMovie));
						checkIn();
					} catch (Exception e2) {
						Toast.makeText(getActivity(), getString(R.string.noVideoPlayerFound), Toast.LENGTH_LONG).show();
					}
				}
			}
		}
	}

	private class GetSplitFiles extends AsyncTask<String, Void, List<SplitFile>> {

		private ProgressDialog progress;
		private String orig_filepath;
		private boolean isNetworkFile;

		public GetSplitFiles(String filepath, boolean isNetworkFile) {
			this.orig_filepath = filepath;
			this.isNetworkFile = isNetworkFile;
		}

		@Override
		protected void onPreExecute() {
			if (isAdded()) {
				progress = new ProgressDialog(getActivity());
				progress.setIndeterminate(true);
				progress.setTitle(getString(R.string.loading_movie_parts));
				progress.setMessage(getString(R.string.few_moments));
				progress.show();
			}
		}

		@Override
		protected List<SplitFile> doInBackground(String... params) {
			List<SplitFile> parts = new ArrayList<SplitFile>();
			List<String> temp;

			try {				
				if (isNetworkFile)
					temp = MizLib.getSplitParts(orig_filepath, MizLib.getAuthFromFilepath(MizLib.TYPE_MOVIE, orig_filepath));
				else
					temp = MizLib.getSplitParts(orig_filepath, null);

				for (int i = 0; i < temp.size(); i++)
					parts.add(new SplitFile(temp.get(i)));

			} catch (Exception e) {}

			return parts;
		}

		@Override
		protected void onPostExecute(final List<SplitFile> result) {
			if (isAdded()) {
				progress.dismiss();
				if (result.size() > 1) {
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setTitle(getString(R.string.playPart));
					builder.setAdapter(new SplitAdapter(getActivity(), result), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							String filepath = result.get(which).getFilepath();

							if (isNetworkFile)
								playNetworkFile(filepath);
							else
								play(filepath);
						}});
					builder.show();
				} else if (result.size() == 1) {
					String filepath = result.get(0).getFilepath();

					if (isNetworkFile)
						playNetworkFile(filepath);
					else
						play(filepath);
				} else {
					Toast.makeText(getActivity(), getString(R.string.errorSomethingWentWrong), Toast.LENGTH_LONG).show();
				}
			}
		}

	}

	private class SplitAdapter implements android.widget.ListAdapter {

		private List<SplitFile> mFiles;
		private Context mContext;
		private LayoutInflater inflater;

		public SplitAdapter(Context context, List<SplitFile> files) {
			super();
			mContext = context;
			inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mFiles = files;
		}

		@Override
		public int getCount() {
			return mFiles.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			convertView = inflater.inflate(R.layout.split_file_item, parent, false);

			TextView title = (TextView) convertView.findViewById(R.id.title);
			TextView description = (TextView) convertView.findViewById(R.id.description);

			title.setText(getString(R.string.part) + " " + mFiles.get(position).getPartNumber());
			description.setText(mFiles.get(position).getUserFilepath());

			return convertView;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isEmpty() {
			return mFiles.isEmpty();
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

	}

	private class SplitFile {

		String filepath;

		public SplitFile(String filepath) {
			this.filepath = filepath;
		}

		public String getFilepath() {
			return filepath;
		}

		public String getUserFilepath() {
			return MizLib.transformSmbPath(filepath);
		}

		public int getPartNumber() {
			return MizLib.getPartNumberFromFilepath(getUserFilepath());
		}

	}

	private void playNetworkFile(final String networkFilepath) {
		if (!MizLib.isWifiConnected(getActivity(), prefsDisableEthernetWiFiCheck)) {
			Toast.makeText(getActivity(), getString(R.string.noConnection), Toast.LENGTH_LONG).show();
			return;
		}

		int bufferSize;
		String buff = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("prefsBufferSize", getString(R.string._16kb));
		if (buff.equals(getString(R.string._16kb)))
			bufferSize = 8192 * 2; // This appears to be the limit for most video players
		else bufferSize = 8192;

		final Streamer s = Streamer.getInstance();
		if (s != null)
			s.setBufferSize(bufferSize);
		else {
			Toast.makeText(getActivity(), getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();
			return;
		}

		final NtlmPasswordAuthentication auth = MizLib.getAuthFromFilepath(MizLib.TYPE_MOVIE, networkFilepath);

		new Thread(){
			public void run(){
				try{
					final SmbFile file = new SmbFile(
							MizLib.createSmbLoginString(
									URLEncoder.encode(auth.getDomain(), "utf-8"),
									URLEncoder.encode(auth.getUsername(), "utf-8"),
									URLEncoder.encode(auth.getPassword(), "utf-8"),
									networkFilepath,
									false
									));

					if (networkFilepath.endsWith("VIDEO_TS.IFO"))
						s.setStreamSrc(file, MizLib.getDVDFiles(networkFilepath, auth));
					else
						s.setStreamSrc(file, MizLib.getSubtitleFiles(networkFilepath, auth));

					if (isAdded())
						getActivity().runOnUiThread(new Runnable(){
							public void run(){
								try{
									Uri uri = Uri.parse(Streamer.URL + Uri.fromFile(new File(Uri.parse(networkFilepath).getPath())).getEncodedPath());	
									startActivity(MizLib.getVideoIntent(uri, useWildcard, thisMovie));
									checkIn();
								} catch (Exception e) {
									try { // Attempt to launch intent based on wildcard MIME type
										Uri uri = Uri.parse(Streamer.URL + Uri.fromFile(new File(Uri.parse(networkFilepath).getPath())).getEncodedPath());	
										startActivity(MizLib.getVideoIntent(uri, "video/*", thisMovie));
										checkIn();
									} catch (Exception e2) {
										Toast.makeText(getActivity(), getString(R.string.noVideoPlayerFound), Toast.LENGTH_LONG).show();
									}
								}
							}
						});
				}
				catch (MalformedURLException e) {}
				catch (UnsupportedEncodingException e1) {}
			}
		}.start();
	}

	private void play(String filepath) {
		try { // Attempt to launch intent based on the MIME type
			getActivity().startActivity(MizLib.getVideoIntent(filepath, useWildcard, thisMovie));
			checkIn();
		} catch (Exception e) {
			try { // Attempt to launch intent based on wildcard MIME type
				getActivity().startActivity(MizLib.getVideoIntent(filepath, "video/*", thisMovie));
				checkIn();
			} catch (Exception e2) {
				Toast.makeText(getActivity(), getString(R.string.noVideoPlayerFound), Toast.LENGTH_LONG).show();
			}
		}
	}

	private void loadImages() {
		if (!MizLib.isPortrait(getActivity())) {
			if (!ignoreNfo && thisMovie.isNetworkFile()) {
				int height = (int) (MizLib.getDisplaySize(getActivity(), MizLib.HEIGHT) * 0.6);
				int width = (int) (height * 0.67);
				mPicasso.load(thisMovie.getFilepath() + "<MiZ>" + thisMovie.getThumbnail()).placeholder(R.drawable.loading_image).error(R.drawable.loading_image).resize(width, height).into(cover);
				mPicasso.load(thisMovie.getFilepath() + "MIZ_BG<MiZ>" + thisMovie.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
			} else {
				mPicasso.load(thisMovie.getThumbnail()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(cover);
				mPicasso.load(thisMovie.getBackdrop()).skipMemoryCache().error(R.drawable.bg).placeholder(R.drawable.bg).into(background);
			}
		} else {
			if (!ignoreNfo && thisMovie.isNetworkFile()) {
				int height = (int) (MizLib.getDisplaySize(getActivity(), MizLib.HEIGHT) * 0.6);
				int width = (int) (height * 0.67);
				mPicasso.load(thisMovie.getFilepath() + "<MiZ>" + thisMovie.getThumbnail()).placeholder(R.drawable.loading_image).error(R.drawable.loading_image).resize(width, height).into(cover);
				mPicasso.load(thisMovie.getFilepath() + "MIZ_BG<MiZ>" + thisMovie.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).into(background, new Callback() {
					@Override
					public void onError() {
						mPicasso.load(thisMovie.getFilepath() + "<MiZ>" + thisMovie.getThumbnail()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
					}

					@Override
					public void onSuccess() {}					
				});
			} else {
				mPicasso.load(thisMovie.getThumbnail()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(cover);
				mPicasso.load(thisMovie.getBackdrop()).skipMemoryCache().placeholder(R.drawable.bg).into(background, new Callback() {
					@Override
					public void onError() {
						mPicasso.load(thisMovie.getThumbnail()).skipMemoryCache().placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
					}

					@Override
					public void onSuccess() {}					
				});
			}
		}
	}

	public void watched(boolean showToast) {
		thisMovie.setHasWatched(true); // Reverse the hasWatched boolean

		if (db.updateMovieSingleItem(Long.valueOf(thisMovie.getRowId()), DbAdapter.KEY_HAS_WATCHED, thisMovie.getHasWatched())) {
			getActivity().invalidateOptionsMenu();

			if (showToast)
				if (thisMovie.hasWatched()) {
					Toast.makeText(getActivity(), getString(R.string.markedAsWatched), Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getActivity(), getString(R.string.markedAsUnwatched), Toast.LENGTH_SHORT).show();
				}

			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("mizuu-library-change"));

		} else {
			if (showToast)
				Toast.makeText(getActivity(), getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();
		}

		if (prefsRemoveMoviesFromWatchlist)
			removeFromWatchlist();

		new Thread() {
			@Override
			public void run() {
				ArrayList<Movie> watchedMovies = new ArrayList<Movie>();
				watchedMovies.add(thisMovie);
				MizLib.markMovieAsWatched(watchedMovies, getActivity());
			}
		}.start();
	}

	public void removeFromWatchlist() {
		thisMovie.setToWatch(false); // Remove it

		if (db.updateMovieSingleItem(Long.valueOf(thisMovie.getRowId()), DbAdapter.KEY_TO_WATCH, thisMovie.getToWatch())) {
			getActivity().invalidateOptionsMenu();
			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent("mizuu-library-change"));
		}

		new Thread() {
			@Override
			public void run() {
				ArrayList<Movie> watchlist = new ArrayList<Movie>();
				watchlist.add(thisMovie);
				MizLib.movieWatchlist(watchlist, getActivity());
			}
		}.start();
	}
}