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

package com.miz.functions;

import com.miz.abstractclasses.BaseMovie;

import android.content.Context;

public class SmallMovie extends BaseMovie {

	public SmallMovie(Context context, String rowId, String filepath, String title, String tmdbId, boolean ignorePrefixes, boolean ignoreNfo) {
		super(context, tmdbId, tmdbId, tmdbId, tmdbId, ignoreNfo, ignoreNfo);
		// Set up movie fields based on constructor
		CONTEXT = context;
		ROW_ID = rowId;
		FILEPATH = filepath;
		TITLE = title;
		TMDB_ID = tmdbId;
		this.ignorePrefixes = ignorePrefixes;
		this.ignoreNfo = ignoreNfo;
	}
}