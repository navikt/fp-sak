package no.nav.foreldrepenger.domene.uttak.input;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class UttakYrkesaktiviteterTest {

    @Test
    void stillingsprosent_ved_flere_overlappende_aktivitetsavtaler_på_dato_skal_velge_stilling_med_seneste_fom() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(new UttakRepositoryStubProvider());
        var dato = LocalDate.of(2020, 5, 4);
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var ansettelsesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(dato.minusYears(5)));
        var aktivitetsAvtale1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMed(dato.minusYears(1).plusDays(1)))
            .medProsentsats(BigDecimal.valueOf(30));
        var aktivStillingsprosent = BigDecimal.valueOf(100);
        var aktivitetsAvtale2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMed(dato.minusYears(2)))
            .medProsentsats(aktivStillingsprosent);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .leggTilAktivitetsAvtale(aktivitetsAvtale1)
            .leggTilAktivitetsAvtale(aktivitetsAvtale2);
        var aktørArbeid = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(behandling.getAktørId())
            .leggTilYrkesaktivitet(yrkesaktivitet
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver));
        var iayAggregat = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(aktørArbeid);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(iayAggregat)
            .build();
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(dato).build();
        var input = new UttakInput(BehandlingReferanse.fra(behandling), stp, iayGrunnlag, null)
            .medBeregningsgrunnlagStatuser(Set.of(new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, arbeidsgiver, null)));
        var uttakYrkesaktiviteter = new UttakYrkesaktiviteter(input);

        assertThat(uttakYrkesaktiviteter.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, null, dato)).isEqualTo(BigDecimal.valueOf(30));
    }

    @Test
    void skal_tåle_null_stillingsprosent_på_aktivitetsavtale() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(new UttakRepositoryStubProvider());
        var dato = LocalDate.of(2020, 5, 4);
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var ansettelsesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(dato.minusYears(5)));
        var aktivitetsavtaleUtenStillingsprosent = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medSisteLønnsendringsdato(LocalDate.now())
            .medPeriode(DatoIntervallEntitet.fraOgMed(dato.minusYears(5)));
        var aktivitetsavtaleMedStillingsprosent = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medSisteLønnsendringsdato(LocalDate.now())
            .medProsentsats(BigDecimal.TEN)
            .medPeriode(DatoIntervallEntitet.fraOgMed(dato.minusYears(5)));
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .leggTilAktivitetsAvtale(aktivitetsavtaleUtenStillingsprosent)
            .leggTilAktivitetsAvtale(aktivitetsavtaleMedStillingsprosent);
        var aktørArbeid = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(behandling.getAktørId())
            .leggTilYrkesaktivitet(yrkesaktivitet
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver));
        var iayAggregat = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(aktørArbeid);
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(iayAggregat)
            .build();
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(dato).build();
        var input = new UttakInput(BehandlingReferanse.fra(behandling), stp, iayGrunnlag, null)
            .medBeregningsgrunnlagStatuser(Set.of(new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, arbeidsgiver, null)));
        var uttakYrkesaktiviteter = new UttakYrkesaktiviteter(input);

        assertThat(uttakYrkesaktiviteter.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, null, dato)).isEqualTo(BigDecimal.TEN);
    }

}
