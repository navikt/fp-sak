package no.nav.foreldrepenger.domene.uttak.fakta;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fakta.omsorg.AnnenForelderHarRettAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fakta.omsorg.BrukerHarAleneomsorgAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fakta.omsorg.BrukerHarOmsorgAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.AvklarFaktaUttakPerioderTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.AvklarHendelseAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.FørsteUttaksdatoAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.GraderingAktivitetUtenBGAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.GraderingUkjentAktivitetAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.SøknadsperioderMåKontrolleresAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

public class KontrollerFaktaUttakTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    private KontrollerFaktaUttakTjeneste tjeneste;

    private PersonopplysningerForUttak personopplysninger;

    @BeforeEach
    void setUp() {
        personopplysninger = mock(PersonopplysningerForUttak.class);
        var uttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());
        var annenForelderHarRettAksjonspunktUtleder = new AnnenForelderHarRettAksjonspunktUtleder(repositoryProvider,
            personopplysninger, uttakTjeneste);
        var avklarHendelseAksjonspunktUtleder = new AvklarHendelseAksjonspunktUtleder();
        var brukerHarAleneomsorgAksjonspunktUtleder = new BrukerHarAleneomsorgAksjonspunktUtleder(repositoryProvider,
            personopplysninger);
        var brukerHarOmsorgAksjonspunktUtleder = new BrukerHarOmsorgAksjonspunktUtleder(repositoryProvider,
            personopplysninger);
        var førsteUttaksdatoAksjonspunktUtleder = new FørsteUttaksdatoAksjonspunktUtleder(repositoryProvider);
        var graderingAktivitetUtenBGAksjonspunktUtleder = new GraderingAktivitetUtenBGAksjonspunktUtleder();
        var ytelseFordelingTjeneste = new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository());
        var graderingUkjentAktivitetAksjonspunktUtleder = new GraderingUkjentAktivitetAksjonspunktUtleder(
            ytelseFordelingTjeneste);
        var søknadsperioderMåKontrolleresAksjonspunktUtleder = new SøknadsperioderMåKontrolleresAksjonspunktUtleder(
            new AvklarFaktaUttakPerioderTjeneste(repositoryProvider.getYtelsesFordelingRepository()));
        var utledere = List.of(annenForelderHarRettAksjonspunktUtleder,
            avklarHendelseAksjonspunktUtleder,
            brukerHarAleneomsorgAksjonspunktUtleder, brukerHarOmsorgAksjonspunktUtleder,
            førsteUttaksdatoAksjonspunktUtleder, graderingAktivitetUtenBGAksjonspunktUtleder,
            graderingUkjentAktivitetAksjonspunktUtleder, søknadsperioderMåKontrolleresAksjonspunktUtleder);
        tjeneste = new KontrollerFaktaUttakTjeneste(utledere, ytelseFordelingTjeneste, personopplysninger);
    }

    @Test
    public void aksjonspunkt_dersom_far_søker_og_ikke_oppgitt_omsorg_til_barnet() {
        //Arrange
        var behandling = opprettBehandlingForFarSomSøker();
        //Act
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        YtelsespesifiktGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
            new FamilieHendelser().medBekreftetHendelse(familieHendelse));
        var input = new UttakInput(BehandlingReferanse.fra(behandling, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build()), null, fpGrunnlag);
        var aksjonspunktResultater = tjeneste.utledAksjonspunkter(input);

        //Assert
        assertThat(aksjonspunktResultater).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    @Test
    public void automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_oppgitt_rett_og_annenpart_uten_norsk_id() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        when(personopplysninger.oppgittAnnenpartUtenNorskID(ref)).thenReturn(true);

        tjeneste.avklarOmAnnenForelderHarRett(ref);

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository()
            .hentAggregat(behandling.getId())
            .getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRett).isPresent();
        assertThat(perioderAnnenforelderHarRett.get().getPerioder()).isEmpty();
    }

    @Test
    public void automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_ikke_oppgitt_rett_og_annenpart_uten_norsk_id() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(false, true, false));
        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        when(personopplysninger.oppgittAnnenpartUtenNorskID(ref)).thenReturn(true);

        tjeneste.avklarOmAnnenForelderHarRett(ref);

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository()
            .hentAggregat(behandling.getId())
            .getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRett).isEmpty();
    }

    @Test
    public void ikke_automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_annen_forelder_er_i_tps() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        when(personopplysninger.oppgittAnnenpartUtenNorskID(ref)).thenReturn(false);

        tjeneste.avklarOmAnnenForelderHarRett(ref);

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository()
            .hentAggregat(behandling.getId())
            .getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRett).isEmpty();
    }

    @Test
    public void ikke_automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_annen_forelder_ikke_finnes() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        var behandling = scenario.lagre(repositoryProvider);

        tjeneste.avklarOmAnnenForelderHarRett(BehandlingReferanse.fra(behandling));

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository()
            .hentAggregat(behandling.getId())
            .getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRett).isEmpty();
    }

    private Behandling opprettBehandlingForFarSomSøker() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        var rettighet = new OppgittRettighetEntitet(true, false, false);
        scenario.medOppgittRettighet(rettighet);

        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(LocalDate.now().plusWeeks(6), LocalDate.now().plusWeeks(10))
            .build();

        scenario.medFordeling(new OppgittFordelingEntitet(Collections.singletonList(periode), true));
        return scenario.lagre(repositoryProvider);
    }
}
