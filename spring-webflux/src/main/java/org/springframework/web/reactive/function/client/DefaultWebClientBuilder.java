/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.util.Arrays;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link WebClient.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultWebClientBuilder implements WebClient.Builder {

	private UriBuilderFactory uriBuilderFactory;

	private ClientHttpConnector connector;

	private ExchangeStrategies exchangeStrategies = ExchangeStrategies.withDefaults();

	private ExchangeFunction exchangeFunction;

	private HttpHeaders defaultHeaders;

	private MultiValueMap<String, String> defaultCookies;


	public DefaultWebClientBuilder() {
		this(new DefaultUriBuilderFactory());
	}

	public DefaultWebClientBuilder(String baseUrl) {
		this(new DefaultUriBuilderFactory(baseUrl));
	}

	public DefaultWebClientBuilder(UriBuilderFactory uriBuilderFactory) {
		Assert.notNull(uriBuilderFactory, "UriBuilderFactory is required.");
		this.uriBuilderFactory = uriBuilderFactory;
	}


	@Override
	public WebClient.Builder clientConnector(ClientHttpConnector connector) {
		this.connector = connector;
		return this;
	}

	@Override
	public WebClient.Builder exchangeStrategies(ExchangeStrategies strategies) {
		Assert.notNull(strategies, "ExchangeStrategies is required.");
		this.exchangeStrategies = strategies;
		return this;
	}

	@Override
	public WebClient.Builder exchangeFunction(ExchangeFunction exchangeFunction) {
		this.exchangeFunction = exchangeFunction;
		return this;
	}

	@Override
	public WebClient.Builder defaultHeader(String headerName, String... headerValues) {
		if (this.defaultHeaders == null) {
			this.defaultHeaders = new HttpHeaders();
		}
		for (String headerValue : headerValues) {
			this.defaultHeaders.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public WebClient.Builder defaultCookie(String cookieName, String... cookieValues) {
		if (this.defaultCookies == null) {
			this.defaultCookies = new LinkedMultiValueMap<>(4);
		}
		this.defaultCookies.addAll(cookieName, Arrays.asList(cookieValues));
		return this;
	}

	@Override
	public WebClient build() {
		return new DefaultWebClient(initExchangeFunction(),
				this.uriBuilderFactory, this.defaultHeaders, this.defaultCookies);
	}

	private ExchangeFunction initExchangeFunction() {
		if (this.exchangeFunction != null) {
			return this.exchangeFunction;
		}
		else if (this.connector != null) {
			return ExchangeFunctions.create(this.connector, this.exchangeStrategies);
		}

		else {
			return ExchangeFunctions.create(new ReactorClientHttpConnector(), this.exchangeStrategies);
		}
	}

}
