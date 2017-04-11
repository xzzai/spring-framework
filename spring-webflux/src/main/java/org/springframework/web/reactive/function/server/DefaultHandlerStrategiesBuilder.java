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

package org.springframework.web.reactive.function.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.Assert;
import org.springframework.web.reactive.result.view.ViewResolver;

/**
 * Default implementation of {@link HandlerStrategies.Builder}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultHandlerStrategiesBuilder implements HandlerStrategies.Builder {

	static final Function<ServerRequest, Optional<Locale>> DEFAULT_LOCALE_RESOLVER =
			request -> request.headers().acceptLanguage().stream()
					.map(Locale.LanguageRange::getRange)
					.map(Locale::forLanguageTag).findFirst();


	private final ServerCodecConfigurer codecConfigurer = new ServerCodecConfigurer();
	private final List<ViewResolver> viewResolvers = new ArrayList<>();

	private Function<ServerRequest, Optional<Locale>> localeResolver;



	public DefaultHandlerStrategiesBuilder() {
		this.codecConfigurer.registerDefaults(false);
	}

	public void defaultConfiguration() {
		this.codecConfigurer.registerDefaults(true);
		localeResolver(DEFAULT_LOCALE_RESOLVER);
	}

	@Override
	public HandlerStrategies.Builder serverSentEventEncoder(Encoder<?> encoder) {
		Assert.notNull(encoder, "'encoder' must not be null");
		this.codecConfigurer.defaultCodecs().serverSentEventEncoder(encoder);
		return this;
	}

	@Override
	public HandlerStrategies.Builder jackson2Decoder(Jackson2JsonDecoder decoder) {
		Assert.notNull(decoder, "'decoder' must not be null");
		this.codecConfigurer.defaultCodecs().jackson2Decoder(decoder);
		return this;
	}

	@Override
	public HandlerStrategies.Builder jackson2Encoder(Jackson2JsonEncoder encoder) {
		Assert.notNull(encoder, "'encoder' must not be null");
		this.codecConfigurer.defaultCodecs().jackson2Encoder(encoder);
		return this;
	}

	@Override
	public HandlerStrategies.Builder customDecoder(Decoder<?> decoder) {
		Assert.notNull(decoder, "'decoder' must not be null");
		this.codecConfigurer.customCodecs().decoder(decoder);
		return this;
	}

	@Override
	public HandlerStrategies.Builder customEncoder(Encoder<?> encoder) {
		Assert.notNull(encoder, "'encoder' must not be null");
		this.codecConfigurer.customCodecs().encoder(encoder);
		return this;
	}

	@Override
	public HandlerStrategies.Builder customMessageReader(HttpMessageReader<?> reader) {
		Assert.notNull(reader, "'reader' must not be null");
		this.codecConfigurer.customCodecs().reader(reader);
		return this;
	}

	@Override
	public HandlerStrategies.Builder customMessageWriter(HttpMessageWriter<?> writer) {
		Assert.notNull(writer, "'writer' must not be null");
		this.codecConfigurer.customCodecs().writer(writer);
		return this;
	}

	@Override
	public HandlerStrategies.Builder viewResolver(ViewResolver viewResolver) {
		Assert.notNull(viewResolver, "'viewResolver' must not be null");
		this.viewResolvers.add(viewResolver);
		return this;
	}

	@Override
	public HandlerStrategies.Builder localeResolver(Function<ServerRequest, Optional<Locale>> localeResolver) {
		Assert.notNull(localeResolver, "'localeResolver' must not be null");
		this.localeResolver = localeResolver;
		return this;
	}

	@Override
	public HandlerStrategies build() {
		return new DefaultHandlerStrategies(this.codecConfigurer.getReaders(),
				this.codecConfigurer.getWriters(), this.viewResolvers, this.localeResolver);
	}


	private static class DefaultHandlerStrategies implements HandlerStrategies {

		private final List<HttpMessageReader<?>> messageReaders;

		private final List<HttpMessageWriter<?>> messageWriters;

		private final List<ViewResolver> viewResolvers;

		private final Function<ServerRequest, Optional<Locale>> localeResolver;

		public DefaultHandlerStrategies(
				List<HttpMessageReader<?>> messageReaders,
				List<HttpMessageWriter<?>> messageWriters,
				List<ViewResolver> viewResolvers,
				Function<ServerRequest, Optional<Locale>> localeResolver) {

			this.messageReaders = unmodifiableCopy(messageReaders);
			this.messageWriters = unmodifiableCopy(messageWriters);
			this.viewResolvers = unmodifiableCopy(viewResolvers);
			this.localeResolver = localeResolver;
		}

		private static <T> List<T> unmodifiableCopy(List<? extends T> list) {
			return Collections.unmodifiableList(new ArrayList<>(list));
		}

		@Override
		public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
			return this.messageReaders::stream;
		}

		@Override
		public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
			return this.messageWriters::stream;
		}

		@Override
		public Supplier<Stream<ViewResolver>> viewResolvers() {
			return this.viewResolvers::stream;
		}

		@Override
		public Supplier<Function<ServerRequest, Optional<Locale>>> localeResolver() {
			return () -> this.localeResolver;
		}
	}

}
