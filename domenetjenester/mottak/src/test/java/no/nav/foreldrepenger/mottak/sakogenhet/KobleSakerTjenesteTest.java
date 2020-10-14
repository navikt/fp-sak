package no.nav.foreldrepenger.mottak.sakogenhet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
@ExtendWith(MockitoExtension.class)
public class KobleSakerTjenesteTest extends EntityManagerAwareTest {

    private static AktørId MOR_AKTØR_ID = AktørId.dummy();

    private static AktørId FAR_AKTØR_ID = AktørId.dummy();

    private static AktørId BARN_AKTØR_ID = AktørId.dummy();
    private static PersonIdent BARN_IDENT = new PersonIdent(new FiktiveFnr().nesteBarnFnr());
    private static FødtBarnInfo BARN_FBI;
    private static LocalDate ELDRE_BARN_FØDT = LocalDate.of(2006, 6, 6);
    private static LocalDate BARN_FØDT = LocalDate.of(2018, 3, 3);

    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private PersoninfoAdapter personinfoAdapter;
    private KobleSakerTjeneste kobleSakTjeneste;

    @BeforeEach
    public void oppsett() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        personinfoAdapter = mock(PersoninfoAdapter.class);
        FagsakRelasjonTjeneste fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider.getFagsakRelasjonRepository(), null,
            repositoryProvider.getFagsakRepository());
        var famHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        kobleSakTjeneste = new KobleSakerTjeneste(repositoryProvider, personinfoAdapter, famHendelseTjeneste, fagsakRelasjonTjeneste);
    }

    @Test
    public void finn_mors_fagsak_dersom_termin_og_gjensidig_oppgitt_søknad() {
        // Oppsett
        settOppTpsStrukturer(false);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), FAR_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isPresent();
        assertThat(morsSak).hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    public void finn_mors_fagsak_dersom_mor_søker_termin_får_bekreftet_fødsel_og_gjensidig_oppgitt_søknad() {
        // Oppsett
        settOppTpsStrukturer(false);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTerminFødsel(LocalDate.now().plusWeeks(1), LocalDate.now(), FAR_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isPresent();
        assertThat(morsSak).hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    public void finn_mors_fagsak_dersom_mor_søker_termin_og_gjensidig_oppgitt_søknad_ved_tidlig_fødsel_uten_at_barnet_er_registrert_i_TPS() {
        // Oppsett
        settOppTpsStrukturer(true, false);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now().plusWeeks(19), FAR_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isPresent();
        assertThat(morsSak).hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    public void finn_mors_fagsak_dersom_mor_søker_termin_og_gjensidig_oppgitt_søknad_ved_for_tidlig_fødsel() {
        // Oppsett
        settOppTpsStrukturer(true);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now().plusWeeks(15), FAR_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isPresent();
    }

    @Test
    public void finn_ikke_mors_nye_fagsak_ved_ulike_kull() {
        // Oppsett
        settOppTpsStrukturer(true);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now().plusWeeks(16), FAR_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now().minusWeeks(30), MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isNotPresent();
    }


    @Test
    public void finn_mors_fagsak_dersom_mor_søker_termin_får_bekreftet_fødsel_og_gjensidig_oppgitt_søknad_ved_forsinket_fødsel_uten_at_barnet_er_registrert_i_TPS() {
        // Oppsett
        settOppTpsStrukturer(true, false);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now().minusWeeks(4), FAR_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isPresent();
        assertThat(morsSak).hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    public void finn_riktig_mors_fagsak_ved_ulike_kull() {
        // Oppsett
        settOppTpsStrukturer(true);

        Behandling behandlingMor1 = opprettBehandlingMorSøkerFødselTermin(LocalDate.now().minusYears(1), FAR_AKTØR_ID);
        Behandling behandlingMor2 = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), FAR_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now().plusWeeks(1), MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isPresent();
        assertThat(morsSak).hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor2.getFagsak()));
    }

    @Test
    public void finn_mors_fagsak_dersom_termin_og_en_part_oppgir_annen_part() {
        // Oppsett
        settOppTpsStrukturer(false);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), null);
        Behandling behandlingFar = opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isPresent();
        assertThat(morsSak).hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    public void finner_ikke_mors_fagsak_dersom_termin_og_ikke_oppgir_annen_part() {
        // Oppsett
        settOppTpsStrukturer(false);

        @SuppressWarnings("unused")
        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), null);
        Behandling behandlingFar = opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate.now(), null);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isNotPresent();
    }

    @Test
    public void finn_mors_fagsak_dersom_surrogati_adopsjon() {
        // Oppsett
        settOppTpsSurrogatiStrukturer();

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTerminBekreftetFødsel(LocalDate.now(), null);
        Behandling behandlingFar = opprettBehandlingMedAdopsjonAvEktefellesBarn(LocalDate.now(), MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isPresent();
        assertThat(morsSak).hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    public void finn_ikke_mors_fagsak_dersom_surrogati_adopsjon_og_skrivefeil_dato() {
        // Oppsett
        settOppTpsSurrogatiStrukturer();

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTerminBekreftetFødsel(LocalDate.now().minusWeeks(8), null);
        Behandling behandlingFar = opprettBehandlingMedAdopsjonAvEktefellesBarn(LocalDate.now().plusWeeks(8), MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isNotPresent();
    }

    @Test
    public void finn_mors_fagsak_dersom_termin_og_en_part_oppgir_annen_part_og_andre_oppgir_tredje_part() {
        // Oppsett
        settOppTpsStrukturer(false);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), BARN_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isPresent();
        assertThat(morsSak).hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    public void finn_mors_fagsak_dersom_fødsel_og_gjensidig_oppgitt_søknad() {
        // Oppsett
        settOppTpsStrukturer(false);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselRegistrertTPS(BARN_FØDT, 1, FAR_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingFarSøkerFødselRegistrertITps(BARN_FØDT, 1, MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isPresent();
        assertThat(morsSak).hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    public void finn_ingen_fagsak_dersom_mor_allerede_koblet() {
        // Oppsett
        settOppTpsStrukturer(false);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselRegistrertTPS(BARN_FØDT, 1, FAR_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingFarSøkerFødselRegistrertITps(BARN_FØDT, 1, MOR_AKTØR_ID);
        fagsakRelasjonRepository.kobleFagsaker(behandlingMor.getFagsak(), behandlingFar.getFagsak(), behandlingMor);

        Behandling nybehandlingFar = opprettBehandlingFarSøkerFødselRegistrertITps(BARN_FØDT, 1, MOR_AKTØR_ID);

        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(nybehandlingFar);

        assertThat(morsSak).isNotPresent();
    }

    @Test
    public void mor_søker_far_har_gammel_sak() {
        // Oppsett
        settOppTpsStrukturer(false);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), FAR_AKTØR_ID);
        opprettBehandlingFarSøkerFødselRegistrertITps(ELDRE_BARN_FØDT, 1, MOR_AKTØR_ID);

        Optional<Fagsak> farsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingMor);

        assertThat(farsSak).isNotPresent();
    }

    @Test
    public void finn_ikke_fars_fagsak_dersom_det_finnes_ikke_relasjon_i_tps() {
        // Oppsett
        settOppTpsStrukturer(true);

        Behandling behandlingMor = opprettBehandlingMorSøkerFødselRegistrertTPS(BARN_FØDT, 1, FAR_AKTØR_ID);

        Optional<Fagsak> relatertSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingMor);

        assertThat(relatertSak).isNotPresent();
    }

    @Test
    public void finner_to_fagsaker_på_mor_og_lar_være_å_koble_til_noen_av_dem() {
        // Arrange
        settOppTpsStrukturer(false);

        opprettBehandlingMorSøkerFødselTerminFødsel(LocalDate.now(), LocalDate.now(), FAR_AKTØR_ID);
        opprettBehandlingMorSøkerFødselTerminFødsel(LocalDate.now(), LocalDate.now(), FAR_AKTØR_ID);
        Behandling behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        // Act
        Optional<Fagsak> morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        // Assert
        assertThat(morsSak).isEmpty();
    }

    private Behandling opprettBehandlingMorSøkerFødselTermin(LocalDate termindato, AktørId annenPart) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Ola Dunk");
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"))
            .medAntallBarn(1);

        leggTilMorSøker(scenario);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMorSøkerFødselTerminFødsel(LocalDate termindato, LocalDate fødselsdato, AktørId annenPart) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Ola Dunk");
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"))
            .medAntallBarn(1);
        scenario.medOverstyrtHendelse()
            .medFødselsDato(fødselsdato)
            .medAntallBarn(1)
            .medTerminbekreftelse(scenario.medOverstyrtHendelse().getTerminbekreftelseBuilder()
                .medUtstedtDato(LocalDate.now())
                .medTermindato(termindato)
                .medNavnPå("LEGEN MIN"));

        leggTilMorSøker(scenario);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMorSøkerFødselTerminBekreftetFødsel(LocalDate termindato, AktørId annenPart) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Ola Dunk");
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"))
            .medAntallBarn(1);
        scenario.medBekreftetHendelse().medFødselsDato(termindato).medAntallBarn(1);

        leggTilMorSøker(scenario);
        return scenario.lagre(repositoryProvider);
    }

    private void leggTilMorSøker(ScenarioMorSøkerForeldrepenger scenario) {
        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .kvinne(MOR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
            .statsborgerskap(Landkoder.NOR)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private void leggTilFarSøker(ScenarioFarSøkerForeldrepenger scenario) {
        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .mann(FAR_AKTØR_ID, SivilstandType.GIFT, Region.NORDEN)
            .statsborgerskap(Landkoder.NOR)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private Behandling opprettBehandlingMorSøkerFødselRegistrertTPS(LocalDate fødselsdato, int antallBarn, AktørId annenPart) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Ola Dunk");
        scenario.medSøknadHendelse()
            .medFødselsDato(fødselsdato)
            .medAntallBarn(antallBarn);
        leggTilMorSøker(scenario);
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medFørsteUttaksdato(fødselsdato.minusWeeks(3))
            .build();
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);
        scenario.medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingFarSøkerFødselRegistrertITps(LocalDate fødseldato, int antallBarnSøknad, AktørId annenPart) {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Kari Dunk");
        scenario.medSøknadHendelse()
            .medFødselsDato(fødseldato)
            .medAntallBarn(antallBarnSøknad);
        leggTilFarSøker(scenario);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate termindato, AktørId annenPart) {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
        if (annenPart != null) {
            scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Kari Dunk");
        }
        scenario.medSøknadHendelse().medAntallBarn(1).medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(LocalDate.now())
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"));
        leggTilFarSøker(scenario);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate fødselsdato, AktørId annenPart) {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
        if (annenPart != null) {
            scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Kari Dunk");
        }
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        leggTilFarSøker(scenario);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedAdopsjonAvEktefellesBarn(LocalDate fødseldato, AktørId annenPart) {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
        if (annenPart != null) {
            scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Kari Dunk");
        }
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(LocalDate.now())
            .medAdoptererAlene(false)
            .medErEktefellesBarn(true))
            .medFødselsDato(fødseldato);
        leggTilFarSøker(scenario);
        return scenario.lagre(repositoryProvider);
    }

    private void settOppTpsSurrogatiStrukturer() {
        BARN_FBI = new FødtBarnInfo.Builder().medIdent(BARN_IDENT).medFødselsdato(BARN_FØDT).build();
        lenient().when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(eq(MOR_AKTØR_ID), any())).thenReturn(List.of(BARN_FBI));
        lenient().when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(eq(FAR_AKTØR_ID), any())).thenReturn(List.of());
        lenient().when(personinfoAdapter.finnAktørIdForForeldreTil(BARN_IDENT)).thenReturn(List.of(MOR_AKTØR_ID));
    }

    private void settOppTpsStrukturer(boolean medKunMor) {
        settOppTpsStrukturer(medKunMor, true);
    }

    private void settOppTpsStrukturer(boolean medKunMor, boolean nyfødtbarnEriTPS) {
        BARN_FBI = new FødtBarnInfo.Builder().medIdent(BARN_IDENT).medFødselsdato(BARN_FØDT).build();
        lenient().when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(eq(MOR_AKTØR_ID), any())).thenReturn(List.of());
        lenient().when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(eq(FAR_AKTØR_ID), any())).thenReturn(List.of());
        if(nyfødtbarnEriTPS) {
            if (medKunMor) {
                lenient().when(personinfoAdapter.finnAktørIdForForeldreTil(BARN_IDENT)).thenReturn(List.of(MOR_AKTØR_ID));
            } else {
                lenient().when(personinfoAdapter.finnAktørIdForForeldreTil(BARN_IDENT)).thenReturn(List.of(MOR_AKTØR_ID, FAR_AKTØR_ID));
            }
            lenient().when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(eq(MOR_AKTØR_ID), any())).thenReturn(List.of(BARN_FBI));
        }

    }
}
