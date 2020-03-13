package no.nav.foreldrepenger.domene.medlem.kontrollerfakta;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.konfig.Tid;

public class AksjonspunktutlederForAvklarStartdatoForForeldrepengeperiodenTest {
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);

    private YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();

    @Spy
    private AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden utleder = new AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden(
        iayTjeneste,
        ytelsesFordelingRepository);

    private Skjæringstidspunkt lagSkjæringstidspunkt(LocalDate dato) {
        return Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(dato).medFørsteUttaksdato(dato).build();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_fordi_startdatoer_samsvarer() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        LocalDate fødselsdato = LocalDate.now().minusDays(1);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        Behandling behandling = lagre(scenario);
        opprettArbeidsforhold(behandling, fødselsdato, Tid.TIDENES_ENDE);

        opprettOppgittFordeling(fødselsdato, behandling);
        opprettInntektsmelding(fødselsdato, behandling);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling, fødselsdato));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private Behandling lagre(ScenarioMorSøkerForeldrepenger scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_opprette_aksjonspunkt_for_aktivt_arbeidsforhold_er_løpende_når_startdatoene_ikke_samsvarer() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        LocalDate fødselsdato = LocalDate.now().minusDays(1);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        Behandling behandling = lagre(scenario);
        opprettArbeidsforhold(behandling, fødselsdato.minusMonths(1), fødselsdato.plusMonths(3L));

        opprettOppgittFordeling(fødselsdato, behandling);
        opprettInntektsmelding(fødselsdato.plusDays(2L), behandling);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling, fødselsdato));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AVKLAR_STARTDATO_FOR_FORELDREPENGEPERIODEN);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_for_aktivt_arbeidsforhold_er_løpende_når_startdatoene_samsvarer() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        LocalDate fødselsdato = LocalDate.now();

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        Behandling behandling = lagre(scenario);
        opprettArbeidsforhold(behandling, fødselsdato.minusMonths(1), fødselsdato.plusMonths(3L));

        opprettOppgittFordeling(fødselsdato, behandling);
        opprettInntektsmelding(fødselsdato, behandling);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling, LocalDate.now()));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private AksjonspunktUtlederInput lagInput(Behandling behandling, LocalDate dato) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, lagSkjæringstidspunkt(dato)));
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_fordi_startdatoene_på_alle_løpende_arbeidsforhold_samsvarer_med_oppgitt_av_bruker() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        LocalDate fødselsdato = LocalDate.now();

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        Behandling behandling = lagre(scenario);
        opprettArbeidsforhold(behandling, fødselsdato, Tid.TIDENES_ENDE);

        opprettOppgittFordeling(fødselsdato, behandling);
        opprettInntektsmelding(fødselsdato.plusDays(2L), behandling);
        opprettInntektsmelding(fødselsdato, behandling);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling, LocalDate.now()));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_fordi_startdatoene_skal_tolkes_som_påfølgende_mandag() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        LocalDate fødselsdato = endreDatoTilLørdag(LocalDate.now());

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        Behandling behandling = lagre(scenario);
        opprettArbeidsforhold(behandling, fødselsdato, Tid.TIDENES_ENDE);

        opprettOppgittFordeling(fødselsdato, behandling);
        opprettInntektsmelding(fødselsdato.plusDays(1L), behandling);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling, LocalDate.now()));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_fordi_startdatoene_på_alle_løpende_arbeidsforhold_ikke_samsvarer_med_oppgitt_av_bruker() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        LocalDate fødselsdato = endreDatoHvisLørdagEllerSøndag(LocalDate.now());

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        Behandling behandling = lagre(scenario);

        opprettArbeidsforhold(behandling, fødselsdato.minusDays(2), Tid.TIDENES_ENDE);
        opprettOppgittFordeling(fødselsdato, behandling);
        opprettInntektsmelding(fødselsdato.plusDays(2L), behandling);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagInput(behandling, fødselsdato));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AVKLAR_STARTDATO_FOR_FORELDREPENGEPERIODEN);
    }

    LocalDate endreDatoHvisLørdagEllerSøndag(LocalDate dato) {
        if (dato.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
            return dato.plusDays(2L);
        } else if (dato.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            return dato.plusDays(1L);
        }
        return dato;
    }

    LocalDate endreDatoTilLørdag(LocalDate dato) {
        if (dato.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            return dato.plusDays(5L);
        } else if (dato.getDayOfWeek().equals(DayOfWeek.TUESDAY)) {
            return dato.plusDays(4L);
        } else if (dato.getDayOfWeek().equals(DayOfWeek.WEDNESDAY)) {
            return dato.plusDays(3L);
        } else if (dato.getDayOfWeek().equals(DayOfWeek.THURSDAY)) {
            return dato.plusDays(2L);
        } else if (dato.getDayOfWeek().equals(DayOfWeek.FRIDAY)) {
            return dato.plusDays(1L);
        }
        return dato;
    }

    void opprettArbeidsforhold(Behandling behandling, LocalDate fom, LocalDate tom) {
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørArbeid = builder.getAktørArbeidBuilder(behandling.getAktørId());
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("45345"))
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medProsentsats(BigDecimal.TEN)
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)));
        aktørArbeid.leggTilYrkesaktivitet(yrkesaktivitet);
        builder.leggTilAktørArbeid(aktørArbeid);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    void opprettOppgittFordeling(LocalDate fødselsdato, Behandling behandling) {
        OppgittPeriodeEntitet periode1 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(6))
            .build();

        OppgittPeriodeEntitet periode2 = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(fødselsdato.plusWeeks(6).plusDays(1), fødselsdato.plusWeeks(10))
            .build();

        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(List.of(periode1, periode2), true);
        ytelsesFordelingRepository.lagre(behandling.getId(), fordeling);
    }

    void opprettInntektsmelding(LocalDate fødselsdato, Behandling behandling) {

        InntektsmeldingBuilder inntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        inntektsmeldingBuilder.medStartDatoPermisjon(fødselsdato);
        inntektsmeldingBuilder.medBeløp(BigDecimal.TEN);
        inntektsmeldingBuilder.medInnsendingstidspunkt(LocalDateTime.now());

        inntektsmeldingBuilder.medArbeidsgiver(Arbeidsgiver.virksomhet("45345"));

        inntektsmeldingTjeneste.lagreInntektsmelding(behandling.getFagsak().getSaksnummer(), behandling.getId(), inntektsmeldingBuilder);
    }
}
