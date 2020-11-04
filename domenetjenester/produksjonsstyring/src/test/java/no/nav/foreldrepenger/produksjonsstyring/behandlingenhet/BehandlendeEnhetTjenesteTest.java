package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon.Builder;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.event.BehandlingEnhetEventPubliserer;

public class BehandlendeEnhetTjenesteTest extends EntityManagerAwareTest {

    private static final AktørId MOR_AKTØR_ID = AktørId.dummy();

    private static final AktørId FAR_AKTØR_ID = AktørId.dummy();

    private static final OrganisasjonsEnhet enhetNormal = new OrganisasjonsEnhet("4849", "NAV Tromsø");
    private static final OrganisasjonsEnhet enhetKode6 = new OrganisasjonsEnhet("2103", "NAV Viken");

    private BehandlingRepositoryProvider repositoryProvider;
    private EnhetsTjeneste enhetsTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @BeforeEach
    public void oppsett() {
        enhetsTjeneste = mock(EnhetsTjeneste.class);
        var eventPubliserer = mock(BehandlingEnhetEventPubliserer.class);
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        behandlendeEnhetTjeneste = new BehandlendeEnhetTjeneste(enhetsTjeneste, eventPubliserer, repositoryProvider);
    }

    @Test
    public void finn_mors_enhet_normal_sak() {
        // Oppsett
        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), FAR_AKTØR_ID);
        when(enhetsTjeneste.hentEnhetSjekkKunAktør(any(), any())).thenReturn(enhetNormal);

        OrganisasjonsEnhet morEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandlingMor.getFagsak());

        assertThat(morEnhet.getEnhetId()).isEqualTo(enhetNormal.getEnhetId());
    }

    @Test
    public void finn_mors_enhet_annenpart_kode6() {
        // Oppsett
        when(enhetsTjeneste.hentEnhetSjekkKunAktør(any(), any())).thenReturn(enhetNormal);
        when(enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(any(), any(), any(), any())).thenReturn(Optional.empty());

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), FAR_AKTØR_ID);

        OrganisasjonsEnhet morEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandlingMor.getFagsak());

        when(enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(any(), any(), any(), any())).thenReturn(Optional.of(enhetKode6));
        Optional<OrganisasjonsEnhet> nyEnhet = behandlendeEnhetTjeneste.sjekkEnhetEtterEndring(behandlingMor);

        assertThat(morEnhet.getEnhetId()).isEqualTo(enhetNormal.getEnhetId());
        assertThat(nyEnhet).hasValueSatisfying(enhet -> assertThat(enhet.getEnhetId()).isEqualTo(enhetKode6.getEnhetId()));
    }

    @Test
    public void finn_enhet_etter_kobling_far_relasjon_kode6() {
        // Oppsett
        when(enhetsTjeneste.hentEnhetSjekkKunAktør(any(), any())).thenReturn(enhetNormal);
        when(enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(any(), any(), any(), any())).thenReturn(Optional.empty());

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselRegistrertTPS(LocalDate.now(),1,  FAR_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingFarSøkerFødselRegistrertITps(LocalDate.now(), 1, MOR_AKTØR_ID);
        behandlingFar.setBehandlendeEnhet(enhetKode6);

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandlingMor.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(behandlingMor.getFagsak(), behandlingFar.getFagsak(), behandlingMor);

        when(enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(any(), any(), any(), any())).thenReturn(Optional.of(enhetKode6));
        when(enhetsTjeneste.enhetsPresedens(any(), any())).thenReturn(enhetKode6);

        Optional<OrganisasjonsEnhet> oppdatertEnhet = behandlendeEnhetTjeneste.endretBehandlendeEnhetEtterFagsakKobling(behandlingMor, repositoryProvider.getFagsakRelasjonRepository().finnRelasjonFor(behandlingMor.getFagsak()));

        assertThat(oppdatertEnhet).isPresent();
        assertThat(oppdatertEnhet).hasValueSatisfying(it -> assertThat(it.getEnhetId()).isEqualTo(enhetKode6.getEnhetId()));
    }



    private Behandling opprettBehandlingMorSøkerFødselTermin(LocalDate termindato, AktørId annenPart) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Ola Dunk");
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMorSøkerFødselRegistrertTPS(LocalDate fødselsdato, int antallBarn, AktørId annenPart) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Ola Dunk");
        scenario.medSøknadHendelse()
            .medFødselsDato(fødselsdato)
            .medAntallBarn(antallBarn);
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        return scenario.lagre(repositoryProvider);
    }


    private Behandling opprettBehandlingFarSøkerFødselRegistrertITps(LocalDate fødseldato, int antallBarnSøknad, AktørId annenPart) {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Kari Dunk");
        scenario.medSøknadHendelse()
            .medFødselsDato(fødseldato)
            .medAntallBarn(antallBarnSøknad);
        leggTilSøker(scenario, NavBrukerKjønn.MANN);
        return scenario.lagre(repositoryProvider);
    }


    private void leggTilSøker(AbstractTestScenario<?> scenario, NavBrukerKjønn kjønn) {
        Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, kjønn, Region.UDEFINERT)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

}
