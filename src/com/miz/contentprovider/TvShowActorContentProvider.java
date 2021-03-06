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

package com.miz.contentprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.miz.db.DbAdapterTvShow;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

public class TvShowActorContentProvider extends SearchRecentSuggestionsProvider {

	static final String TAG = TvShowActorContentProvider.class.getSimpleName();
	public static final String AUTHORITY = TvShowActorContentProvider.class.getName();
	public static final int MODE = DATABASE_MODE_QUERIES | DATABASE_MODE_2LINES;
	private static final String[] COLUMNS = {
		BaseColumns._ID, // must include this column
		SearchManager.SUGGEST_COLUMN_TEXT_1, // First line (title)
		SearchManager.SUGGEST_COLUMN_TEXT_2, // Second line (smaller text)
		SearchManager.SUGGEST_COLUMN_INTENT_DATA, // Icon
		SearchManager.SUGGEST_COLUMN_ICON_1, // Icon
		SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA, // TV show ID
		SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
		SearchManager.SUGGEST_COLUMN_SHORTCUT_ID };
	private String mActor;

	public TvShowActorContentProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		mActor = getContext().getString(R.string.actor);

		String query = selectionArgs[0];
		if (query == null || query.length() == 0) {
			return null;
		}

		MatrixCursor cursor = new MatrixCursor(COLUMNS);

		try {
			List<String> list = getSearchResults(query);
			for (int i = 0; i < list.size(); i++) {
				cursor.addRow(createRow(Integer.valueOf(i), list.get(i), mActor, ContentResolver.SCHEME_ANDROID_RESOURCE + "://com.miz.mizuu/drawable/photo", list.get(i)));
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to lookup " + query, e);
		}
		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	private Object[] createRow(Integer id, String text1, String text2, String icon, String rowId) {
		return new Object[] {
				id, // _id
				text1, // text1
				text2, // text2
				icon,
				icon,
				rowId,
				"android.intent.action.SEARCH", // action
				SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT };
	}

	private List<String> getSearchResults(String query) {		
		query = query.replace("actor:", "").trim();

		HashMap<String, String> actorMap = new HashMap<String, String>();

		List<String> actorList = new ArrayList<String>();
		if (!MizLib.isEmpty(query)) {

			DbAdapterTvShow db = MizuuApplication.getTvDbAdapter();

			query = query.toLowerCase(Locale.ENGLISH);

			Cursor c = db.getAllShows();
			String actors = ""; // Reuse String variable
			
			while (c.moveToNext()) {
				actors = c.getString(c.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ACTORS)).toLowerCase(Locale.ENGLISH);

				if (actors.indexOf(query) != -1) {
					for (String actor : c.getString(c.getColumnIndex(DbAdapterTvShow.KEY_SHOW_ACTORS)).split("\\|"))
						if (actor.toLowerCase(Locale.ENGLISH).startsWith(query))
							actorMap.put(actor, actor);
				}
			}
			
			actorList = new ArrayList<String>(actorMap.values());
			Collections.sort(actorList);
		}
		
		return actorList;
	}
}