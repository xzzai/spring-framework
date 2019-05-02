/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.reactive;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.junit.Assert.*;

/**
 * Tests for {@link TransactionalOperator}.
 *
 * @author Mark Paluch
 */
public class TransactionalOperatorTests {

	ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);


	@Test
	public void commitWithMono() {
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Mono.just(true).as(operator::transactional)
				.as(StepVerifier::create)
				.expectNext(true)
				.verifyComplete();
		assertTrue(tm.commit);
		assertFalse(tm.rollback);
	}

	@Test
	public void rollbackWithMono() {
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Mono.error(new IllegalStateException()).as(operator::transactional)
				.as(StepVerifier::create)
				.verifyError(IllegalStateException.class);
		assertFalse(tm.commit);
		assertTrue(tm.rollback);
	}

	@Test
	public void commitWithFlux() {
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Flux.just(true).as(operator::transactional)
				.as(StepVerifier::create)
				.expectNext(true)
				.verifyComplete();
		assertTrue(tm.commit);
		assertFalse(tm.rollback);
	}

	@Test
	public void rollbackWithFlux() {
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		Flux.error(new IllegalStateException()).as(operator::transactional)
				.as(StepVerifier::create)
				.verifyError(IllegalStateException.class);
		assertFalse(tm.commit);
		assertTrue(tm.rollback);
	}

}
