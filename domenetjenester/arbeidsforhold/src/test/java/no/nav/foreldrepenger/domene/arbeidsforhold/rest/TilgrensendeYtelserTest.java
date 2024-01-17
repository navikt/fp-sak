package no.nav.foreldrepenger.domene.arbeidsforhold.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.TilgrensendeYtelser;

class TilgrensendeYtelserTest {
    private static final LocalDate I_DAG = LocalDate.now();

    @Test
    void skal_sortere_null_first() {
        var tilgrensendeYtelser = Arrays.asList(
                lagTilgrensendeYtelser(I_DAG.minusYears(3)),
                lagTilgrensendeYtelser(I_DAG.minusDays(2)),
                lagTilgrensendeYtelser(I_DAG.plusWeeks(3)),
                lagTilgrensendeYtelser(I_DAG),
                lagTilgrensendeYtelser(null),
                lagTilgrensendeYtelser(I_DAG.plusYears(2)),
                lagTilgrensendeYtelser(I_DAG.minusMonths(1)),
                lagTilgrensendeYtelser(null),
                lagTilgrensendeYtelser(I_DAG.minusYears(1)));

        tilgrensendeYtelser.sort(Comparator.naturalOrder());

        assertThat(tilgrensendeYtelser.get(0).periodeFra()).isNull();
        assertThat(tilgrensendeYtelser.get(1).periodeFra()).isNull();
        assertThat(tilgrensendeYtelser.get(2).periodeFra()).isEqualTo(I_DAG.plusYears(2));
        assertThat(tilgrensendeYtelser.get(3).periodeFra()).isEqualTo(I_DAG.plusWeeks(3));
        assertThat(tilgrensendeYtelser.get(4).periodeFra()).isEqualTo(I_DAG);
        assertThat(tilgrensendeYtelser.get(5).periodeFra()).isEqualTo(I_DAG.minusDays(2));
        assertThat(tilgrensendeYtelser.get(6).periodeFra()).isEqualTo(I_DAG.minusMonths(1));
        assertThat(tilgrensendeYtelser.get(7).periodeFra()).isEqualTo(I_DAG.minusYears(1));
        assertThat(tilgrensendeYtelser.get(8).periodeFra()).isEqualTo(I_DAG.minusYears(3));
    }

    private TilgrensendeYtelser lagTilgrensendeYtelser(LocalDate periodeFraDato) {
        return new TilgrensendeYtelser(RelatertYtelseType.UDEFINERT, periodeFraDato, null, null, null, null);
    }
}
