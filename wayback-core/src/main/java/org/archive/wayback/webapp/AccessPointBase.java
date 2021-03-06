/*
 *  This file is part of the Wayback archival access software
 *   (http://archive-access.sourceforge.net/projects/wayback/).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.archive.wayback.webapp;

import java.io.IOException;

import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.accesspoint.AccessPointAdapter;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.core.Resource;
import org.archive.wayback.core.WaybackRequest;
import org.archive.wayback.util.url.UrlOperations;
import org.archive.wayback.util.webapp.AbstractRequestHandler;

/**
 * AccessPointBase provides fields and methods common to AbstractRequestHandler
 * implementations for core wayback machine functionalities (playback, search,
 * live web archiving, etc.)
 * 
 * Note: this class should not have fields whose getter is overridden in {@link AccessPointAdapter}.
 * If you need to access those fields, be sure to declare an abstract getter method, and access
 * the value through it.
 */
public abstract class AccessPointBase extends AbstractRequestHandler {

	protected boolean isSelfRedirect(Resource resource,
			CaptureSearchResult closest, WaybackRequest wbRequest,
			String canonRequestURL) {
		int status = resource.getStatusCode();

		// Only applies to redirects
		if ((status < 300) || (status >= 400)) {
			return false;
		}

		String location = resource.getHeader("Location");

		if (location == null) {
			return false;
		}

		//		if (!closest.getCaptureTimestamp().equals(wbRequest.getReplayTimestamp())) {
		//			return false;
		//		}

		String redirScheme = UrlOperations.urlToScheme(location);

		try {
			if (redirScheme == null && isExactSchemeMatch()) {
				location = UrlOperations.resolveUrl(closest.getOriginalUrl(),
					location);
				redirScheme = UrlOperations.urlToScheme(location);
			} else if (location.startsWith("/")) {
				location = UrlOperations.resolveUrl(closest.getOriginalUrl(),
					location);
			}

			if (getSelfRedirectCanonicalizer() != null) {
				location = getSelfRedirectCanonicalizer().urlStringToKey(
					location);
			}
		} catch (IOException e) {
			return false;
		}

		if (location.equals(canonRequestURL)) {
			// if not exact scheme, don't do scheme compare, must be equal
			if (!isExactSchemeMatch()) {
				return true;
			}

			String origScheme = UrlOperations.urlToScheme(wbRequest
				.getRequestUrl());

			if ((origScheme != null) && (redirScheme != null) &&
					(origScheme.compareTo(redirScheme) == 0)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @return the exactSchemeMatch
	 */
	public abstract boolean isExactSchemeMatch();

	/**
	 * URL canonicalizer for testing self-redirect.
	 * @return UrlCanonicalizer
	 */
	public abstract UrlCanonicalizer getSelfRedirectCanonicalizer();

	protected String urlToKey(String url) {
		// getSelfRedirectCanonicalizer() can be overridden in a sub-class.
		// Always access selfRedirectCanonicalizer through getter method.
		UrlCanonicalizer canonicalizer = getSelfRedirectCanonicalizer();
		if (canonicalizer != null) {
			try {
				return canonicalizer.urlStringToKey(url);
			} catch (IOException ex) {
			}
		}
		return url;
	}
}
