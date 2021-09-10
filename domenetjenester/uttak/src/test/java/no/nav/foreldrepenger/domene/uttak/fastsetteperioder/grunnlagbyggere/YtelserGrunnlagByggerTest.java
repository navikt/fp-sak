package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet.fraOgMedTilOgMed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerInnleggelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.PeriodeMedBarnInnlagt;

class YtelserGrunnlagByggerTest {

    @Test
    void tom_ytelser_hvis_ikke_pleiepenger() {
        var input = new UttakInput(mock(BehandlingReferanse.class), null, new ForeldrepengerGrunnlag());

        var ytelser = new YtelserGrunnlagBygger().byggGrunnlag(input);
        assertThat(ytelser.pleiepenger()).isEmpty();
    }

    @Test
    void legger_pleiepenger_med_innleggelse() {
        var periode1Fom = LocalDate.now();
        var periode1Tom = LocalDate.now().plusWeeks(1);
        var periode1 = new PleiepengerInnleggelseEntitet.Builder()
            .medPeriode(fraOgMedTilOgMed(periode1Fom, periode1Tom));
        var periode2Fom = LocalDate.now().plusWeeks(2);
        var periode2Tom = LocalDate.now().plusWeeks(3);
        var periode2 = new PleiepengerInnleggelseEntitet.Builder()
            .medPeriode(fraOgMedTilOgMed(periode2Fom, periode2Tom));
        var pleiepengerGrunnlag = PleiepengerGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medInnleggelsePerioder(new PleiepengerPerioderEntitet.Builder()
                .leggTil(periode1)
                .leggTil(periode2))
            .build();
        var input = new UttakInput(mock(BehandlingReferanse.class), null,
            new ForeldrepengerGrunnlag().medPleiepengerGrunnlag(pleiepengerGrunnlag));

        var ytelser = new YtelserGrunnlagBygger().byggGrunnlag(input);

        assertThat(ytelser.pleiepenger()).isPresent();
        var perioder = ytelser.pleiepenger().orElseThrow().perioder();
        assertThat(perioder).hasSize(2);
        assertThat(perioder).containsExactlyInAnyOrder(new PeriodeMedBarnInnlagt(periode1Fom, periode1Tom),
            new PeriodeMedBarnInnlagt(periode2Fom, periode2Tom));
    }

}
