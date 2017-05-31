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

package org.springframework.http.codec;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;

/**
 * {@code HttpMessageWriter} for {@code "text/event-stream"} responses.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServerSentEventHttpMessageWriter implements HttpMessageWriter<Object> {

	private static final List<MediaType> WRITABLE_MEDIA_TYPES =
			Collections.singletonList(MediaType.TEXT_EVENT_STREAM);


	private final Encoder<?> encoder;


	/**
	 * Constructor without an {@code Encoder}. In this mode only {@code String}
	 * is supported for event data to be encoded.
	 */
	public ServerSentEventHttpMessageWriter() {
		this(null);
	}

	/**
	 * Constructor with JSON {@code Encoder} for encoding objects.
	 * Support for {@code String} event data is built-in.
	 * @param encoder the Encoder to use (may be {@code null})
	 */
	public ServerSentEventHttpMessageWriter(Encoder<?> encoder) {
		this.encoder = encoder;
	}


	/**
	 * Return the configured {@code Encoder}, if any.
	 */
	@Nullable
	public Encoder<?> getEncoder() {
		return this.encoder;
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return WRITABLE_MEDIA_TYPES;
	}


	@Override
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		return (mediaType == null || MediaType.TEXT_EVENT_STREAM.includes(mediaType) ||
				ServerSentEvent.class.isAssignableFrom(elementType.resolve(Object.class)));
	}

	@Override
	public Mono<Void> write(Publisher<?> input, ResolvableType elementType, @Nullable MediaType mediaType,
			ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		message.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
		return message.writeAndFlushWith(encode(input, message.bufferFactory(), elementType, hints));
	}

	private Flux<Publisher<DataBuffer>> encode(Publisher<?> input, DataBufferFactory factory,
			ResolvableType elementType, Map<String, Object> hints) {

		ResolvableType valueType = (ServerSentEvent.class.isAssignableFrom(elementType.getRawClass()) ?
				elementType.getGeneric() : elementType);

		return Flux.from(input).map(element -> {

			ServerSentEvent<?> sse = (element instanceof ServerSentEvent ?
					(ServerSentEvent<?>) element : ServerSentEvent.builder().data(element).build());

			StringBuilder sb = new StringBuilder();
			if (sse.id() != null) {
				writeField("id", sse.id(), sb);
			}
			if (sse.event() != null) {
				writeField("event", sse.event(), sb);
			}
			if (sse.retry() != null) {
				writeField("retry", sse.retry().toMillis(), sb);
			}
			if (sse.comment() != null) {
				sb.append(':').append(sse.comment().replaceAll("\\n", "\n:")).append("\n");
			}
			if (sse.data() != null) {
				sb.append("data:");
			}

			return Flux.concat(encodeText(sb, factory),
					encodeData(sse.data(), valueType, factory, hints),
					encodeText("\n", factory));
		});
	}

	private void writeField(String fieldName, Object fieldValue, StringBuilder stringBuilder) {
		stringBuilder.append(fieldName);
		stringBuilder.append(':');
		stringBuilder.append(fieldValue.toString());
		stringBuilder.append("\n");
	}

	@SuppressWarnings("unchecked")
	private <T> Flux<DataBuffer> encodeData(T data, ResolvableType valueType,
			DataBufferFactory factory, Map<String, Object> hints) {

		if (data == null) {
			return Flux.empty();
		}

		if (data instanceof String) {
			String text = (String) data;
			return Flux.from(encodeText(text.replaceAll("\\n", "\ndata:") + "\n", factory));
		}

		if (this.encoder == null) {
			return Flux.error(new CodecException("No SSE encoder configured and the data is not String."));
		}

		return ((Encoder<T>) this.encoder)
				.encode(Mono.just(data), factory, valueType, MediaType.TEXT_EVENT_STREAM, hints)
				.concatWith(encodeText("\n", factory));
	}

	private Mono<DataBuffer> encodeText(CharSequence text, DataBufferFactory bufferFactory) {
		byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = bufferFactory.allocateBuffer(bytes.length).write(bytes);
		return Mono.just(buffer);
	}

	@Override
	public Mono<Void> write(Publisher<?> input, @Nullable ResolvableType actualType, ResolvableType elementType,
			@Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response,
			Map<String, Object> hints) {

		Map<String, Object> allHints = new HashMap<>();
		allHints.putAll(getEncodeHints(actualType, elementType, mediaType, request, response));
		allHints.putAll(hints);

		return write(input, elementType, mediaType, response, allHints);
	}

	private Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType,
			MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		if (this.encoder instanceof HttpMessageEncoder) {
			HttpMessageEncoder<?> httpEncoder = (HttpMessageEncoder<?>) this.encoder;
			return httpEncoder.getEncodeHints(actualType, elementType, mediaType, request, response);
		}
		return Collections.emptyMap();
	}

}
