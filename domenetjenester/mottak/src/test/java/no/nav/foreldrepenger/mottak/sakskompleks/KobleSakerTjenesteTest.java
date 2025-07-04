package no.nav.foreldrepenger.mottak.sakskompleks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;

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
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@ExtendWith(MockitoExtension.class)
class KobleSakerTjenesteTest extends EntityManagerAwareTest {

    private static final AktørId MOR_AKTØR_ID = AktørId.dummy();

    private static final AktørId FAR_AKTØR_ID = AktørId.dummy();

    private static final AktørId BARN_AKTØR_ID = AktørId.dummy();
    private static final PersonIdent BARN_IDENT = new PersonIdent(new FiktiveFnr().nesteBarnFnr());
    private static FødtBarnInfo BARN_FBI;
    private static final LocalDate ELDRE_BARN_FØDT = LocalDate.of(2006, 6, 6);
    private static final LocalDate BARN_FØDT = LocalDate.of(2018, 3, 3);

    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private KobleSakerTjeneste kobleSakTjeneste;

    @BeforeEach
    public void oppsett() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        personinfoAdapter = mock(PersoninfoAdapter.class);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        var famHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        kobleSakTjeneste = new KobleSakerTjeneste(repositoryProvider, personinfoAdapter, famHendelseTjeneste, fagsakRelasjonTjeneste);
    }

    @Test
    void finn_mors_fagsak_dersom_termin_og_gjensidig_oppgitt_søknad() {
        // Oppsett
        settOppPDLStrukturer(false);

        var behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), FAR_AKTØR_ID);
        var behandlingFar = opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak)
            .isPresent()
            .hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    void finn_mors_fagsak_dersom_mor_søker_termin_får_bekreftet_fødsel_og_gjensidig_oppgitt_søknad() {
        // Oppsett
        settOppPDLStrukturer(false);

        var behandlingMor = opprettBehandlingMorSøkerFødselTerminFødsel(LocalDate.now().plusWeeks(1), LocalDate.now(), FAR_AKTØR_ID);
        var behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak)
            .isPresent()
            .hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    void finn_mors_fagsak_dersom_mor_søker_termin_og_gjensidig_oppgitt_søknad_ved_tidlig_fødsel_uten_at_barnet_er_registrert_i_TPS() {
        // Oppsett
        settOppPDLStrukturer(true, false);

        var behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now().plusWeeks(19), FAR_AKTØR_ID);
        var behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak)
            .isPresent()
            .hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    void finn_mors_fagsak_dersom_mor_søker_termin_og_gjensidig_oppgitt_søknad_ved_for_tidlig_fødsel() {
        // Oppsett
        settOppPDLStrukturer(true);

        opprettBehandlingMorSøkerFødselTermin(LocalDate.now().plusWeeks(15), FAR_AKTØR_ID);
        var behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isPresent();
    }

    @Test
    void finn_ikke_mors_nye_fagsak_ved_ulike_kull() {
        // Oppsett
        settOppPDLStrukturer(true);

        opprettBehandlingMorSøkerFødselTermin(LocalDate.now().plusWeeks(16), FAR_AKTØR_ID);
        var behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now().minusWeeks(30), MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isNotPresent();
    }


    @Test
    void finn_mors_fagsak_dersom_mor_søker_termin_får_bekreftet_fødsel_og_gjensidig_oppgitt_søknad_ved_forsinket_fødsel_uten_at_barnet_er_registrert_i_TPS() {
        // Oppsett
        settOppPDLStrukturer(true, false);

        var behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now().minusWeeks(4), FAR_AKTØR_ID);
        var behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak)
            .isPresent()
            .hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    void finn_riktig_mors_fagsak_ved_ulike_kull() {
        // Oppsett
        settOppPDLStrukturer(true);

        opprettBehandlingMorSøkerFødselTermin(LocalDate.now().minusYears(1), FAR_AKTØR_ID);
        var behandlingMor2 = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), FAR_AKTØR_ID);
        var behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now().plusWeeks(1), MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak)
            .isPresent()
            .hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor2.getFagsak()));
    }

    @Test
    void finn_mors_fagsak_dersom_termin_og_en_part_oppgir_annen_part() {
        // Oppsett
        settOppPDLStrukturer(false);

        var behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), null);
        var behandlingFar = opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak)
            .isPresent()
            .hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    void finner_ikke_mors_fagsak_dersom_termin_og_ikke_oppgir_annen_part() {
        // Oppsett
        settOppPDLStrukturer(false);

        opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), null);
        var behandlingFar = opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate.now(), null);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isNotPresent();
    }

    @Test
    void finn_mors_fagsak_dersom_surrogati_adopsjon() {
        // Oppsett
        settOppPDLSurrogatiStrukturer();

        var behandlingMor = opprettBehandlingMorSøkerFødselTerminBekreftetFødsel(LocalDate.now(), null);
        var behandlingFar = opprettBehandlingMedAdopsjonAvEktefellesBarn(LocalDate.now(), MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak)
            .isPresent()
            .hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    void finn_ikke_mors_fagsak_dersom_surrogati_adopsjon_og_skrivefeil_dato() {
        // Oppsett
        settOppPDLSurrogatiStrukturer();

        opprettBehandlingMorSøkerFødselTerminBekreftetFødsel(LocalDate.now().minusWeeks(8), null);
        var behandlingFar = opprettBehandlingMedAdopsjonAvEktefellesBarn(LocalDate.now().plusWeeks(8), MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak).isNotPresent();
    }

    @Test
    void finn_mors_fagsak_dersom_termin_og_en_part_oppgir_annen_part_og_andre_oppgir_tredje_part() {
        // Oppsett
        settOppPDLStrukturer(false);

        var behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), BARN_AKTØR_ID);
        var behandlingFar = opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak)
            .isPresent()
            .hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    void finn_mors_fagsak_dersom_fødsel_og_gjensidig_oppgitt_søknad() {
        // Oppsett
        settOppPDLStrukturer(false);

        var behandlingMor = opprettBehandlingMorSøkerFødselRegistrertPDL(BARN_FØDT, 1, FAR_AKTØR_ID);
        var behandlingFar = opprettBehandlingFarSøkerFødselRegistrertIPDL(BARN_FØDT, 1, MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        assertThat(morsSak)
            .isPresent()
            .hasValueSatisfying(it -> assertThat(it).isEqualTo(behandlingMor.getFagsak()));
    }

    @Test
    void finn_ingen_fagsak_dersom_mor_allerede_koblet() {
        // Oppsett
        settOppPDLStrukturer(false);

        var behandlingMor = opprettBehandlingMorSøkerFødselRegistrertPDL(BARN_FØDT, 1, FAR_AKTØR_ID);
        var behandlingFar = opprettBehandlingFarSøkerFødselRegistrertIPDL(BARN_FØDT, 1, MOR_AKTØR_ID);
        fagsakRelasjonTjeneste.kobleFagsaker(behandlingMor.getFagsak(), behandlingFar.getFagsak());

        var nybehandlingFar = opprettBehandlingFarSøkerFødselRegistrertIPDL(BARN_FØDT, 1, MOR_AKTØR_ID);

        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(nybehandlingFar);

        assertThat(morsSak).isNotPresent();
    }

    @Test
    void mor_søker_far_har_gammel_sak() {
        // Oppsett
        settOppPDLStrukturer(false);

        var behandlingMor = opprettBehandlingMorSøkerFødselTermin(LocalDate.now(), FAR_AKTØR_ID);
        opprettBehandlingFarSøkerFødselRegistrertIPDL(ELDRE_BARN_FØDT, 1, MOR_AKTØR_ID);

        var farsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingMor);

        assertThat(farsSak).isNotPresent();
    }

    @Test
    void finn_ikke_fars_fagsak_dersom_det_finnes_ikke_relasjon_i_pdl() {
        // Oppsett
        settOppPDLStrukturer(true);

        var behandlingMor = opprettBehandlingMorSøkerFødselRegistrertPDL(BARN_FØDT, 1, FAR_AKTØR_ID);

        var relatertSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingMor);

        assertThat(relatertSak).isNotPresent();
    }

    @Test
    void finner_to_fagsaker_på_mor_og_lar_være_å_koble_til_noen_av_dem() {
        // Arrange
        settOppPDLStrukturer(false);

        opprettBehandlingMorSøkerFødselTerminFødsel(LocalDate.now(), LocalDate.now(), FAR_AKTØR_ID);
        opprettBehandlingMorSøkerFødselTerminFødsel(LocalDate.now(), LocalDate.now(), FAR_AKTØR_ID);
        var behandlingFar = opprettBehandlingMedOppgittFødselOgBehandlingType(LocalDate.now(), MOR_AKTØR_ID);

        // Act
        var morsSak = kobleSakTjeneste.finnRelatertFagsakDersomRelevant(behandlingFar);

        // Assert
        assertThat(morsSak).isEmpty();
    }

    private Behandling opprettBehandlingMorSøkerFødselTermin(LocalDate termindato, AktørId annenPart) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
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
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
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
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
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
        var søker = scenario.opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .kvinne(MOR_AKTØR_ID, SivilstandType.GIFT)
            .statsborgerskap(Landkoder.NOR)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private void leggTilFarSøker(ScenarioFarSøkerForeldrepenger scenario) {
        var søker = scenario.opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .mann(FAR_AKTØR_ID, SivilstandType.GIFT)
            .statsborgerskap(Landkoder.NOR)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private Behandling opprettBehandlingMorSøkerFødselRegistrertPDL(LocalDate fødselsdato, int antallBarn, AktørId annenPart) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(MOR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Ola Dunk");
        scenario.medSøknadHendelse()
            .medFødselsDato(fødselsdato)
            .medAntallBarn(antallBarn);
        leggTilMorSøker(scenario);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medFørsteUttaksdato(fødselsdato.minusWeeks(3))
            .build();
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);
        scenario.medOppgittDekningsgrad(Dekningsgrad._100);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingFarSøkerFødselRegistrertIPDL(LocalDate fødseldato, int antallBarnSøknad, AktørId annenPart) {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
        scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Kari Dunk");
        scenario.medSøknadHendelse()
            .medFødselsDato(fødseldato)
            .medAntallBarn(antallBarnSøknad);
        leggTilFarSøker(scenario);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedOppgittTerminOgBehandlingType(LocalDate termindato, AktørId annenPart) {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
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
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
        if (annenPart != null) {
            scenario.medSøknadAnnenPart().medAktørId(annenPart).medNavn("Kari Dunk");
        }
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        leggTilFarSøker(scenario);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling opprettBehandlingMedAdopsjonAvEktefellesBarn(LocalDate fødseldato, AktørId annenPart) {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(FAR_AKTØR_ID);
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

    private void settOppPDLSurrogatiStrukturer() {
        BARN_FBI = new FødtBarnInfo.Builder().medIdent(BARN_IDENT).medFødselsdato(BARN_FØDT).build();
        lenient().when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), eq(MOR_AKTØR_ID), any())).thenReturn(List.of(BARN_FBI));
        lenient().when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), eq(FAR_AKTØR_ID), any())).thenReturn(List.of());
        lenient().when(personinfoAdapter.finnAktørIdForForeldreTil(FagsakYtelseType.FORELDREPENGER, BARN_IDENT)).thenReturn(List.of(MOR_AKTØR_ID));
    }

    private void settOppPDLStrukturer(boolean medKunMor) {
        settOppPDLStrukturer(medKunMor, true);
    }

    private void settOppPDLStrukturer(boolean medKunMor, boolean nyfødtbarnEriTPS) {
        BARN_FBI = new FødtBarnInfo.Builder().medIdent(BARN_IDENT).medFødselsdato(BARN_FØDT).build();
        lenient().when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), eq(MOR_AKTØR_ID), any())).thenReturn(List.of());
        lenient().when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), eq(FAR_AKTØR_ID), any())).thenReturn(List.of());
        if(nyfødtbarnEriTPS) {
            if (medKunMor) {
                lenient().when(personinfoAdapter.finnAktørIdForForeldreTil(FagsakYtelseType.FORELDREPENGER, BARN_IDENT)).thenReturn(List.of(MOR_AKTØR_ID));
            } else {
                lenient().when(personinfoAdapter.finnAktørIdForForeldreTil(FagsakYtelseType.FORELDREPENGER, BARN_IDENT)).thenReturn(List.of(MOR_AKTØR_ID, FAR_AKTØR_ID));
            }
            lenient().when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), eq(MOR_AKTØR_ID), any())).thenReturn(List.of(BARN_FBI));
        }

    }
}
