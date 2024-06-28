package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PeriodeMedGradering(LocalDate fom, LocalDate tom, BigDecimal arbeidsprosent,
                                  AktivitetStatus aktivitetStatus, Arbeidsgiver arbeidsgiver) {
}
