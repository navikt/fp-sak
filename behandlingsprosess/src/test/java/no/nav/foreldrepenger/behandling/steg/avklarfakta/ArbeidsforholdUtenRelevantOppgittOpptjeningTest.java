package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import static no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt.ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class ArbeidsforholdUtenRelevantOppgittOpptjeningTest {

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    @Test
    public void skal_returne_true_hvis_ingen_IAY() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), Optional.empty());
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }

    @Test
    public void skal_returne_true_hvis_ingen_oppgitt_opptjening() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }

    @Test
    public void skal_returne_true_hvis_arbeidsforhold_er_utenlandsk() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny()
                .leggTilOppgittArbeidsforhold(OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
                        .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                        .medPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()))
                        .medErUtenlandskInntekt(true));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }

    @Test
    public void skal_returne_true_hvis_arbeidsforhold_ikke_er_aktivt_på_stp() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var skjæringstidspunkt = LocalDate.now();
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder
                .oppdatere(Optional.empty())
                .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                        .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                        .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny().medPeriode(
                                DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(2), skjæringstidspunkt.minusYears(2))))
                        .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")))
                .medAktørId(behandling.getAktørId());
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(skjæringstidspunkt, behandling),
                iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }

    @Test
    public void skal_returne_false_hvis_arbeidsforhold_er_aktivt_på_stp() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var skjæringstidspunkt = LocalDate.now();
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder
                .oppdatere(Optional.empty())
                .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                        .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                        .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(2), skjæringstidspunkt)))
                        .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")))
                .medAktørId(behandling.getAktørId());
        iayBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(skjæringstidspunkt, behandling),
                iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_annen_aktivitet_med_militær_eller_siviltjeneste() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        // Arrange
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        var oppgittOpptjeningBuilder = opprettOppgittOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_annen_aktivitet_med_vartpenger_ventelønn() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        // Arrange
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        var oppgittOpptjeningBuilder = opprettOppgittOpptjening(ArbeidType.VENTELØNN_VARTPENGER);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_annen_aktivitet_med_etterlønn_sluttpakke() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        var oppgittOpptjeningBuilder = opprettOppgittOpptjening(ArbeidType.ETTERLØNN_SLUTTPAKKE);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_oppgitt_opptjening_med_frilans() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny()
                .leggTilFrilansOpplysninger(new OppgittFrilans());
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_oppgitt_opptjening_med_selvstendig_næringsdrivende() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(1), LocalDate.now());
        var egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        egenNæringBuilder.medPeriode(periode);
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny()
                .leggTilEgneNæringer(List.of(egenNæringBuilder));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_true_hvis_uten_relevant_oppgitt_opptjening() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        var oppgittOpptjeningBuilder = OppgittOpptjeningBuilder.ny();
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }

    @Test
    public void skal_returne_true_hvis_man_bare_har_sykepenger() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        var iayYtelse = iayBuilder.getAktørYtelseBuilder(behandling.getAktørId());
        var ytelseBuilder = iayYtelse.getYtelselseBuilderForType(Fagsystem.ARENA, RelatertYtelseType.SYKEPENGER,
                new Saksnummer("999999999"));
        var sykePeriode = DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(7));
        ytelseBuilder.medPeriode(sykePeriode);
        iayYtelse.leggTilYtelse(ytelseBuilder);
        iayBuilder.leggTilAktørYtelse(iayYtelse);

        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isTrue();
    }

    @Test
    public void skal_returne_false_hvis_man_bare_har_aap() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        var iayYtelse = iayBuilder.getAktørYtelseBuilder(behandling.getAktørId());
        var ytelseBuilder = iayYtelse.getYtelselseBuilderForType(Fagsystem.ARENA, RelatertYtelseType.ARBEIDSAVKLARINGSPENGER,
                new Saksnummer("999999999"));
        var sykePeriode = DatoIntervallEntitet.fraOgMed(LocalDate.now().minusDays(1));
        ytelseBuilder.medPeriode(sykePeriode);
        iayYtelse.leggTilYtelse(ytelseBuilder);
        iayBuilder.leggTilAktørYtelse(iayYtelse);

        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    @Test
    public void skal_returne_false_hvis_man_bare_har_dp() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var iayBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
        var iayYtelse = iayBuilder.getAktørYtelseBuilder(behandling.getAktørId());
        var ytelseBuilder = iayYtelse.getYtelselseBuilderForType(Fagsystem.ARENA, RelatertYtelseType.DAGPENGER,
                new Saksnummer("999999999"));
        var sykePeriode = DatoIntervallEntitet.fraOgMed(LocalDate.now().minusDays(1));
        ytelseBuilder.medPeriode(sykePeriode);
        iayYtelse.leggTilYtelse(ytelseBuilder);
        iayBuilder.leggTilAktørYtelse(iayYtelse);

        // Act
        var erUtenRelevantOppgittOpptjening = erUtenRelevantOppgittOpptjening(lagInput(behandling), iayTjeneste.finnGrunnlag(behandling.getId()));
        // Assert
        assertThat(erUtenRelevantOppgittOpptjening).isFalse();
    }

    private OppgittOpptjeningBuilder opprettOppgittOpptjening(ArbeidType arbeidType) {
        var periode = DatoIntervallEntitet.fraOgMed(LocalDate.now());
        var annenAktivitet = new OppgittAnnenAktivitet(periode, arbeidType);
        return OppgittOpptjeningBuilder.ny().leggTilAnnenAktivitet(annenAktivitet);
    }

    private AksjonspunktUtlederInput lagInput(LocalDate skjæringstidspunkt, Behandling behandling) {
        var stp = Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(skjæringstidspunkt).build();
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, stp));
    }

    private AksjonspunktUtlederInput lagInput(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling));
    }

}
