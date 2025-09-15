package no.nav.foreldrepenger.domene.prosess;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PeriodeMedGradering(LocalDate fom, LocalDate tom, BigDecimal arbeidsprosent,
                                  AktivitetStatus aktivitetStatus, Arbeidsgiver arbeidsgiver) {
}
