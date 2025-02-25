/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.server.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.undertow.util.HeaderMap;
import org.apache.tomcat.util.http.MimeHeaders;
import org.eclipse.jetty.http.HttpFields;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.http.support.Netty4HeadersAdapter;
import org.springframework.http.support.Netty5HeadersAdapter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Tests for {@code HeadersAdapters} {@code MultiValueMap} implementations.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
class HeadersAdaptersTests {

	@ParameterizedHeadersTest
	void getWithUnknownHeaderShouldReturnNull(MultiValueMap<String, String> headers) {
		assertThat(headers.get("Unknown")).isNull();
	}

	@ParameterizedHeadersTest
	void getFirstWithUnknownHeaderShouldReturnNull(MultiValueMap<String, String> headers) {
		assertThat(headers.getFirst("Unknown")).isNull();
	}

	@ParameterizedHeadersTest
	void sizeWithMultipleValuesForHeaderShouldCountHeaders(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("TestHeader", "second");
		assertThat(headers).hasSize(1);
	}

	@ParameterizedHeadersTest
	void keySetShouldNotDuplicateHeaderNames(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("OtherHeader", "test");
		headers.add("TestHeader", "second");
		assertThat(headers.keySet()).hasSize(2);
	}

	@ParameterizedHeadersTest
	void containsKeyShouldBeCaseInsensitive(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		assertThat(headers.containsKey("testheader")).isTrue();
	}

	@ParameterizedHeadersTest
	void addShouldKeepOrdering(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("TestHeader", "second");
		assertThat(headers.getFirst("TestHeader")).isEqualTo("first");
		assertThat(headers.get("TestHeader")).first().isEqualTo("first");
	}

	@ParameterizedHeadersTest
	void putShouldOverrideExisting(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.put("TestHeader", List.of("override"));
		assertThat(headers.getFirst("TestHeader")).isEqualTo("override");
		assertThat(headers.get("TestHeader")).hasSize(1);
	}

	@ParameterizedHeadersTest
	void nullValuesShouldNotFail(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", null);
		assertThat(headers.getFirst("TestHeader")).isNull();
		headers.set("TestHeader", null);
		assertThat(headers.getFirst("TestHeader")).isNull();
	}

	@ParameterizedHeadersTest
	void shouldReflectChangesOnKeyset(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		assertThat(headers.keySet()).hasSize(1);
		headers.keySet().removeIf("TestHeader"::equals);
		assertThat(headers.keySet()).isEmpty();
	}

	@ParameterizedHeadersTest
	void shouldFailIfHeaderRemovedFromKeyset(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		assertThat(headers.keySet()).hasSize(1);
		Iterator<String> names = headers.keySet().iterator();
		assertThat(names.hasNext()).isTrue();
		assertThat(names.next()).isEqualTo("TestHeader");
		names.remove();
		assertThatThrownBy(names::remove).isInstanceOf(IllegalStateException.class);
	}

	@ParameterizedHeadersTest
	void testHeadersOutput(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("testHeader", "second");
		MultiValueMap<String, String> multiValueMap =
				CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH));
		headers.forEach(multiValueMap::addAll);
		assertThat(multiValueMap.toString()).isEqualToIgnoringCase("{testheader=[first, second]}");
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest
	@MethodSource("headers")
	@interface ParameterizedHeadersTest {
	}

	static Stream<Arguments> headers() {
		return Stream.of(
				argumentSet("Map", CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH))),
				argumentSet("Netty", new Netty4HeadersAdapter(new DefaultHttpHeaders())),
				argumentSet("Netty", new Netty5HeadersAdapter(io.netty5.handler.codec.http.headers.HttpHeaders.newHeaders())),
				argumentSet("Tomcat", new TomcatHeadersAdapter(new MimeHeaders())),
				argumentSet("Undertow", new UndertowHeadersAdapter(new HeaderMap())),
				argumentSet("Jetty", new JettyHeadersAdapter(HttpFields.build()))
		);
	}

}
