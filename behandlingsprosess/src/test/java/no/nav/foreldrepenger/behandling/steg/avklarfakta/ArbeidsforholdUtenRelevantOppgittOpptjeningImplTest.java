package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class ArbeidsforholdUtenRelevantOppgittOpptjeningImplTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private Behandling behandling;

    @Before
    public void oppsett(){
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        this.behandling = scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_returne_true_hvis_ingen_IAY(){
        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), Optional.empty());
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }


    @Test
    public void skal_returne_true_hvis_ingen_oppgitt_opptjening(){
        // Arrange
        InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }

    @Test
    public void skal_returne_true_hvis_arbeidsforhold_er_utenlandsk(){
        // Arrange
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny()
            .leggTilOppgittArbeidsforhold(OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()))
                .medErUtenlandskInntekt(true));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }

    @Test
    public void skal_returne_true_hvis_arbeidsforhold_ikke_er_aktivt_på_stp(){
        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.now();
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(2), skjæringstidspunkt.minusYears(2))))
                .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")))
            .medAktørId(behandling.getAktørId());
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(skjæringstidspunkt), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }

    @Test
    public void skal_returne_false_hvis_arbeidsforhold_er_aktivt_på_stp(){
        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.now();
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(2), skjæringstidspunkt)))
                .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")))
            .medAktørId(behandling.getAktørId());
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(skjæringstidspunkt), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_annen_aktivitet_med_militær_eller_siviltjeneste(){
        // Arrange
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = opprettOppgittOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_annen_aktivitet_med_vartpenger_ventelønn(){
        // Arrange
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = opprettOppgittOpptjening(ArbeidType.VENTELØNN_VARTPENGER);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_annen_aktivitet_med_etterlønn_sluttpakke(){
        // Arrange
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = opprettOppgittOpptjening(ArbeidType.ETTERLØNN_SLUTTPAKKE);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_oppgitt_opptjening_med_frilans(){
        // Arrange
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny()
            .leggTilFrilansOpplysninger(new OppgittFrilans());
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_oppgitt_opptjening_med_selvstendig_næringsdrivende(){
        // Arrange
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(1), LocalDate.now());
        OppgittOpptjeningBuilder.EgenNæringBuilder egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        egenNæringBuilder.medPeriode(periode);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny()
            .leggTilEgneNæringer(Collections.singletonList(egenNæringBuilder));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_true_hvis_uten_relevant_oppgitt_opptjening(){
        // Arrange
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        OppgittOpptjeningBuilder oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }

    @Test
    public void skal_returne_true_hvis_man_bare_har_sykepenger(){
        // Arrange
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder iayYtelse = iayBuilder.getAktørYtelseBuilder(behandling.getAktørId());
        YtelseBuilder ytelseBuilder = iayYtelse.getYtelselseBuilderForType(Fagsystem.ARENA, RelatertYtelseType.SYKEPENGER, new Saksnummer("999999999"));
        DatoIntervallEntitet sykePeriode = DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(7));
        ytelseBuilder.medPeriode(sykePeriode);
        iayYtelse.leggTilYtelse(ytelseBuilder);
        iayBuilder.leggTilAktørYtelse(iayYtelse);

        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }

    @Test
    public void skal_returne_false_hvis_man_bare_har_aap(){
        // Arrange
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder iayYtelse = iayBuilder.getAktørYtelseBuilder(behandling.getAktørId());
        YtelseBuilder ytelseBuilder = iayYtelse.getYtelselseBuilderForType(Fagsystem.ARENA, RelatertYtelseType.ARBEIDSAVKLARINGSPENGER, new Saksnummer("999999999"));
        DatoIntervallEntitet sykePeriode = DatoIntervallEntitet.fraOgMed(LocalDate.now().minusDays(1));
        ytelseBuilder.medPeriode(sykePeriode);
        iayYtelse.leggTilYtelse(ytelseBuilder);
        iayBuilder.leggTilAktørYtelse(iayYtelse);

        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_man_bare_har_dp(){
        // Arrange
        InntektArbeidYtelseAggregatBuilder iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder iayYtelse = iayBuilder.getAktørYtelseBuilder(behandling.getAktørId());
        YtelseBuilder ytelseBuilder = iayYtelse.getYtelselseBuilderForType(Fagsystem.ARENA, RelatertYtelseType.DAGPENGER, new Saksnummer("999999999"));
        DatoIntervallEntitet sykePeriode = DatoIntervallEntitet.fraOgMed(LocalDate.now().minusDays(1));
        ytelseBuilder.medPeriode(sykePeriode);
        iayYtelse.leggTilYtelse(ytelseBuilder);
        iayBuilder.leggTilAktørYtelse(iayYtelse);

        // Act
        boolean erUtenRelevantOppgittOpptjening = ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(lagInput(), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }


    private OppgittOpptjeningBuilder opprettOppgittOpptjening(ArbeidType arbeidType) {
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMed(LocalDate.now());
        OppgittAnnenAktivitet annenAktivitet = new OppgittAnnenAktivitet(periode, arbeidType);
        return OppgittOpptjeningBuilder.ny().leggTilAnnenAktivitet(annenAktivitet);
    }

    private AksjonspunktUtlederInput lagInput(LocalDate skjæringstidspunkt) {
        Skjæringstidspunkt stp = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(skjæringstidspunkt).build();
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, stp));
    }

    private AksjonspunktUtlederInput lagInput() {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling));
    }

}
