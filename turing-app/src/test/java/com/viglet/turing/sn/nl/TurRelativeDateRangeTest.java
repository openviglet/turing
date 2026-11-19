/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.sn.nl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/**
 * T1117 — relative dates pinned against a fixed today (2026-10-24, the day the
 * gap was reported).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurRelativeDateRangeTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "posts from last year         | last year      | 2025-01-01 | 2025-12-31",
            "Posts from LAST YEAR         | LAST YEAR      | 2025-01-01 | 2025-12-31",
            "notícias do ano passado      | do ano passado | 2025-01-01 | 2025-12-31",
            "events this year             | this year      | 2026-01-01 | 2026-10-24",
            "news this month              | this month     | 2026-10-01 | 2026-10-24",
            "vagas deste mês              | deste mês      | 2026-10-01 | 2026-10-24",
            "vagas neste mês              | neste mês      | 2026-10-01 | 2026-10-24",
            "articles from last month     | last month     | 2026-09-01 | 2026-09-30",
            "posts since March            | since March    | 2026-03-01 | 2026-10-24",
            "posts since december         | since december | 2025-12-01 | 2026-10-24",
            "vagas desde março            | desde março    | 2026-03-01 | 2026-10-24",
            "posts since 2024             | since 2024     | 2024-01-01 | 2026-10-24",
            "the last 7 days              | last 7 days    | 2026-10-17 | 2026-10-24",
            "nos últimos 3 meses          | últimos 3 meses | 2026-07-24 | 2026-10-24",
            "posts from the past week     | past week      | 2026-10-17 | 2026-10-24",
    })
    void resolvesAgainstToday(String text, String phrase, String start, String end) {
        var resolved = TurRelativeDateRange.resolve(text, TODAY);
        if ("NONE".equals(phrase)) {
            assertThat(resolved).isEmpty();
            return;
        }
        assertThat(resolved).hasValueSatisfying(range -> {
            assertThat(range.phrase()).isEqualTo(phrase);
            assertThat(range.start()).isEqualTo(LocalDate.parse(start));
            assertThat(range.end()).isEqualTo(LocalDate.parse(end));
        });
    }

    /** T1299 — a Thursday, the day an events calendar asked for November. */
    private static final LocalDate NOV_19 = LocalDate.of(2026, 11, 19);

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "palestras em novembro        | em novembro         | 2026-11-01 | 2026-11-30",
            "eventos de dezembro          | de dezembro         | 2026-12-01 | 2026-12-31",
            "cursos em outubro            | em outubro          | 2027-10-01 | 2027-10-31",
            "talks in March               | in March            | 2027-03-01 | 2027-03-31",
            "charlas en marzo             | en marzo            | 2027-03-01 | 2027-03-31",
            "eventos de novembro de 2025  | de novembro de 2025 | 2025-11-01 | 2025-11-30",
            "o que tem amanhã             | amanhã              | 2026-11-20 | 2026-11-20",
            "events tomorrow              | tomorrow            | 2026-11-20 | 2026-11-20",
            "shows este fim de semana     | este fim de semana  | 2026-11-21 | 2026-11-22",
            "eventos na semana que vem    | na semana que vem   | 2026-11-23 | 2026-11-29",
            "palestras na próxima semana  | na próxima semana   | 2026-11-23 | 2026-11-29",
            "next week                    | next week           | 2026-11-23 | 2026-11-29",
            "cursos no mês que vem        | no mês que vem      | 2026-12-01 | 2026-12-31",
            "el mes que viene             | el mes que viene    | 2026-12-01 | 2026-12-31",
            "nos próximos 15 dias         | próximos 15 dias    | 2026-11-19 | 2026-12-04",
            "the next 2 weeks             | next 2 weeks        | 2026-11-19 | 2026-12-03",
            "palestra do professor Maio   | NONE                | -          | -",
            "o livro Agosto               | NONE                | -          | -",
    })
    void resolvesMonthsAndWhatIsToCome(String text, String phrase, String start, String end) {
        var resolved = TurRelativeDateRange.resolve(text, NOV_19);
        if ("NONE".equals(phrase)) {
            assertThat(resolved).isEmpty();
            return;
        }
        assertThat(resolved).hasValueSatisfying(range -> {
            assertThat(range.phrase()).isEqualTo(phrase);
            assertThat(range.start()).isEqualTo(LocalDate.parse(start));
            assertThat(range.end()).isEqualTo(LocalDate.parse(end));
        });
    }

    @Test
    void aWeekendAskedOnASundayIsThatSunday() {
        assertThat(TurRelativeDateRange.resolve("this weekend", LocalDate.of(2026, 11, 22)))
                .hasValueSatisfying(range -> {
                    assertThat(range.start()).isEqualTo(LocalDate.of(2026, 11, 22));
                    assertThat(range.end()).isEqualTo(LocalDate.of(2026, 11, 22));
                });
    }

    @Test
    void textWithoutARelativeDateResolvesToNothing() {
        assertThat(TurRelativeDateRange.resolve("posts about php", TODAY)).isEmpty();
        assertThat(TurRelativeDateRange.resolve("the yearly report", TODAY)).isEmpty();
    }
}
