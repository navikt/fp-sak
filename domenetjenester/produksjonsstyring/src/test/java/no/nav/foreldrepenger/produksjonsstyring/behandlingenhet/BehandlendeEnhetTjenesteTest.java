package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.event.BehandlingEnhetEventPubliserer;

@ExtendWith(MockitoExtension.class)
class BehandlendeEnhetTjenesteTest {

    private static final AktørId MOR_AKTØR_ID = AktørId.dummy();

    private static final AktørId FAR_AKTØR_ID = AktørId.dummy();

    private static final OrganisasjonsEnhet enhetNormal = new OrganisasjonsEnhet("4867", "NAV Foreldrepenger");
    private static final OrganisasjonsEnhet enhetKode6 = new OrganisasjonsEnhet("2103", "NAV Viken");

    @Mock
    private EnhetsTjeneste enhetsTjeneste;
    @Mock
    private FagsakEgenskapRepository egenskapRepository;
    @Mock
    private BehandlingEnhetEventPubliserer eventPubliserer;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @Test
    void finn_mors_enhet_normal_sak() {
        // Oppsett
        var behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), FAR_AKTØR_ID);
        when(enhetsTjeneste.hentEnhetSjekkKunAktør(any(), any())).thenReturn(enhetNormal);

        var morEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandlingMor.getFagsak());

        assertThat(morEnhet.enhetId()).isEqualTo(enhetNormal.enhetId());
    }

    @Test
    void finn_mors_enhet_annenpart_kode6() {
        // Oppsett
        when(enhetsTjeneste.hentEnhetSjekkKunAktør(any(), any())).thenReturn(enhetNormal);
        when(enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        var behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), FAR_AKTØR_ID);

        var morEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandlingMor.getFagsak());

        when(enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(any(), any(), any(), any(), any())).thenReturn(Optional.of(enhetKode6));
        var nyEnhet = behandlendeEnhetTjeneste.sjekkEnhetEtterEndring(behandlingMor);

        assertThat(morEnhet.enhetId()).isEqualTo(enhetNormal.enhetId());
        assertThat(nyEnhet).hasValueSatisfying(enhet -> assertThat(enhet.enhetId()).isEqualTo(enhetKode6.enhetId()));
    }

    @Test
    void finn_enhet_etter_kobling_far_relasjon_kode6() {
        // Oppsett
        lenient().when(enhetsTjeneste.hentEnhetSjekkKunAktør(any(), any())).thenReturn(enhetNormal);
        lenient().when(enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(any(), any(), any(), any(),any())).thenReturn(Optional.empty());

        var behandlingMor = opprettBehandlingMorSøkerFødselRegistrertTPS(LocalDate.now(),1,  FAR_AKTØR_ID);
        behandlingMor.setBehandlendeEnhet(enhetNormal);
        var behandlingFar = opprettBehandlingFarSøkerFødselRegistrertITps(LocalDate.now(), 1, MOR_AKTØR_ID);
        behandlingFar.setBehandlendeEnhet(enhetKode6);

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandlingMor.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(behandlingMor.getFagsak(), behandlingFar.getFagsak(), behandlingMor);

        lenient().when(enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(any(), any(), any(), any(), any())).thenReturn(Optional.of(enhetKode6));

        var oppdatertEnhet = behandlendeEnhetTjeneste.endretBehandlendeEnhetEtterFagsakKobling(behandlingMor);

        assertThat(oppdatertEnhet).isPresent();
        assertThat(oppdatertEnhet).hasValueSatisfying(it -> assertThat(it.enhetId()).isEqualTo(enhetKode6.enhetId()));
    }



    private Behandling opprettBehandlingMorSøkerFødselTermin(LocalDate termindato, AktørId annenPart) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Ola Dunk");
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        var behandling = scenario.lagMocked();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        behandlendeEnhetTjeneste = new BehandlendeEnhetTjeneste(enhetsTjeneste, eventPubliserer, repositoryProvider, egenskapRepository);
        return behandling;
    }

    private Behandling opprettBehandlingMorSøkerFødselRegistrertTPS(LocalDate fødselsdato, int antallBarn, AktørId annenPart) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Ola Dunk");
        scenario.medSøknadHendelse()
            .medFødselsDato(fødselsdato)
            .medAntallBarn(antallBarn);
        leggTilSøker(scenario, NavBrukerKjønn.KVINNE);
        var behandling = scenario.lagMocked();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        behandlendeEnhetTjeneste = new BehandlendeEnhetTjeneste(enhetsTjeneste, eventPubliserer, repositoryProvider, egenskapRepository);
        return behandling;
    }


    private Behandling opprettBehandlingFarSøkerFødselRegistrertITps(LocalDate fødseldato, int antallBarnSøknad, AktørId annenPart) {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Kari Dunk");
        scenario.medSøknadHendelse()
            .medFødselsDato(fødseldato)
            .medAntallBarn(antallBarnSøknad);
        leggTilSøker(scenario, NavBrukerKjønn.MANN);
        var behandling = scenario.lagMocked();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        behandlendeEnhetTjeneste = new BehandlendeEnhetTjeneste(enhetsTjeneste, eventPubliserer, repositoryProvider, egenskapRepository);
        return behandling;
    }


    private void leggTilSøker(AbstractTestScenario<?> scenario, NavBrukerKjønn kjønn) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var søker = builderForRegisteropplysninger
            .medPersonas()
            .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, kjønn)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

}
