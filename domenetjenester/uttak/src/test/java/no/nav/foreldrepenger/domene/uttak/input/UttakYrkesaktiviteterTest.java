package no.nav.foreldrepenger.domene.uttak.input;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class UttakYrkesaktiviteterTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private UttakRepositoryProvider repoProvider = new UttakRepositoryProvider(repoRule.getEntityManager());

    @Test
    public void stillingsprosent_ved_flere_overlappende_aktivitetsavtaler_på_dato_skal_velge_stilling_med_maks() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repoProvider);
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
        var input = new UttakInput(BehandlingReferanse.fra(behandling, dato), iayGrunnlag, null)
            .medBeregningsgrunnlagStatuser(Set.of(new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, arbeidsgiver, null)));
        var uttakYrkesaktiviteter = new UttakYrkesaktiviteter(input);

        assertThat(uttakYrkesaktiviteter.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, null, dato)).isEqualTo(aktivStillingsprosent);
    }

}
