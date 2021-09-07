package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet.fraOgMedTilOgMed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerInnleggelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.ytelser.PleiepengerPeriode;

class YtelserGrunnlagByggerTest {

    @Test
    void tom_ytelser_hvis_ikke_pleiepenger() {
        var input = new UttakInput(mock(BehandlingReferanse.class), iay(), new ForeldrepengerGrunnlag());

        var ytelser = new YtelserGrunnlagBygger().byggGrunnlag(input);
        assertThat(ytelser.pleiepenger()).isEmpty();
    }

    @Test
    void legger_pleiepenger_med_innleggelse() {
        var innleggelseFom = LocalDate.now();
        var innleggelseTom = LocalDate.now().plusWeeks(1);
        var innleggelse = new PleiepengerInnleggelseEntitet.Builder()
            .medPeriode(fraOgMedTilOgMed(innleggelseFom, innleggelseTom));
        var pleiepengerGrunnlag = PleiepengerGrunnlagEntitet.Builder.oppdatere(Optional.empty())
            .medInnleggelsePerioder(new PleiepengerPerioderEntitet.Builder()
                .leggTil(innleggelse))
            .build();
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty());
        var aktørYtelseBuilder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty())
            .leggTilYtelse(ytelseBuilder
                .medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
                    .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(innleggelseFom, innleggelseTom))
                    .medUtbetalingsgradProsent(BigDecimal.TEN)
                    .build())
                .medYtelseAnvist(ytelseBuilder.getAnvistBuilder()
                    .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(innleggelseTom.plusDays(1), innleggelseTom.plusWeeks(2)))
                    .medUtbetalingsgradProsent(BigDecimal.TEN)
                    .build())
                .medKilde(Fagsystem.K9SAK)
                .medYtelseType(RelatertYtelseType.PLEIEPENGER_SYKT_BARN));
        var input = new UttakInput(mock(BehandlingReferanse.class), iay(aktørYtelseBuilder),
            new ForeldrepengerGrunnlag().medPleiepengerGrunnlag(pleiepengerGrunnlag));

        var ytelser = new YtelserGrunnlagBygger().byggGrunnlag(input);

        assertThat(ytelser.pleiepenger()).isPresent();
        var perioder = ytelser.pleiepenger().orElseThrow().perioder();
        assertThat(perioder).hasSize(2);
        assertThat(perioder).containsExactlyInAnyOrder(new PleiepengerPeriode(innleggelseFom, innleggelseTom, true),
            new PleiepengerPeriode(innleggelseTom.plusDays(1), innleggelseTom.plusWeeks(2), false));
    }

    private InntektArbeidYtelseGrunnlag iay() {
        return iay(InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty()));
    }

    private InntektArbeidYtelseGrunnlag iay(InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder) {
        var inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
            .leggTilAktørYtelse(aktørYtelseBuilder);
        return InntektArbeidYtelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medData(inntektArbeidYtelseAggregatBuilder)
            .build();
    }
}
