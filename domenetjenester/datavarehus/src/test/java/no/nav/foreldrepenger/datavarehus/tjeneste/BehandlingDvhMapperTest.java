package no.nav.foreldrepenger.datavarehus.tjeneste;

import static java.time.Month.JANUARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VenteGruppe;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(JpaExtension.class)
class BehandlingDvhMapperTest {

    private static final long VEDTAK_ID = 1L;
    private static final String BEHANDLENDE_ENHET = "behandlendeEnhet";
    private static final String ANSVARLIG_BESLUTTER = "ansvarligBeslutter";
    private static final String ANSVARLIG_SAKSBEHANDLER = "ansvarligSaksbehandler";
    private static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
    private static final Saksnummer SAKSNUMMER = new Saksnummer("12345");
    private static final LocalDateTime OPPRETTET_TID = LocalDateTime.now();
    private static final String KLAGE_BEGRUNNELSE = "Begrunnelse for klagevurdering er bla.bla.bla.";
    private static final String BEHANDLENDE_ENHET_ID = "1234";

    @Test
    void skal_mappe_familiehendelse_til_behandling_dvh_ikke_vedtatt() {
        var mottattTidspunkt = lagMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        var scenario = opprettFørstegangssøknadScenario();
        var behandling = byggBehandling(scenario, BehandlingResultatType.OPPHØR, false);
        var fødselsdato = LocalDate.of(2017, JANUARY, 1);
        var grunnlag = byggHendelseGrunnlag(fødselsdato, fødselsdato);
        var behandlingsresultat = scenario.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var dvh = BehandlingDvhMapper.map(behandling, behandlingsresultat, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(grunnlag), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), null, null, l -> UUID.randomUUID());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getFamilieHendelseType()).isEqualTo(FamilieHendelseType.FØDSEL.getKode());
        assertThat(dvh.getMottattTid()).isEqualTo(mottattTidspunkt.get(0).getMottattTidspunkt());
    }

    @Test
    void skal_mappe_til_behandling_dvh_uten_vedtak() {
        var mottattTidspunkt = lagMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        var scenario = opprettFørstegangssøknadScenario();
        var behandling = byggBehandling(scenario, BehandlingResultatType.IKKE_FASTSATT, false);

        var behandlingsresultat = scenario.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var dvh = BehandlingDvhMapper.map(behandling, behandlingsresultat, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), null, null, l -> UUID.randomUUID());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getAnsvarligBeslutter()).isEqualTo(ANSVARLIG_BESLUTTER);
        assertThat(dvh.getAnsvarligSaksbehandler()).isEqualTo(ANSVARLIG_SAKSBEHANDLER);
        assertThat(dvh.getBehandlendeEnhet()).isEqualTo(BEHANDLENDE_ENHET);
        assertThat(dvh.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(dvh.getBehandlingUuid()).isEqualTo(behandling.getUuid());
        assertThat(dvh.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.IKKE_FASTSATT.getKode());
        assertThat(dvh.getBehandlingStatus()).isEqualTo(BehandlingStatus.OPPRETTET.getKode());
        assertThat(dvh.getBehandlingType()).isEqualTo(BehandlingType.FØRSTEGANGSSØKNAD.getKode());
        assertThat(dvh.getSaksnummer()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(dvh.getAktørId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(dvh.getYtelseType()).isEqualTo(behandling.getFagsakYtelseType().getKode());
        assertThat(dvh.getFunksjonellTid()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        assertThat(dvh.getUtlandstilsnitt()).isEqualTo("NASJONAL");
        assertThat(dvh.getMottattTid()).isEqualTo(mottattTidspunkt.get(0).getMottattTidspunkt());
        assertThat(dvh.getFoersteStoenadsdag()).isNull();
    }

    @Test
    void skal_mappe_til_behandling_dvh_foerste_stoenadsdag(EntityManager entityManager) {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now())
            .medAdopsjon(familieHendelseBuilder.getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        var behandling = scenario.lagre(repositoryProvider);
        entityManager.persist(behandling.getBehandlingsresultat());

        var stp = LocalDate.now();

        var mottattDokument = lagMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var dvh = BehandlingDvhMapper.map(behandling, behandlingsresultat, mottattDokument, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(stp), List.of(), Optional.empty(), Optional.empty(), null, null, l -> UUID.randomUUID());
        assertThat(dvh.getFoersteStoenadsdag()).isEqualTo(stp);

        var behandlingsresultat2 = repositoryProvider.getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var dvh2 = BehandlingDvhMapper.map(behandling, behandlingsresultat2, mottattDokument, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(stp.plusDays(1)), List.of(), Optional.of(stp), Optional.empty(), null, null, l -> UUID.randomUUID());
        assertThat(dvh2.getFoersteStoenadsdag()).isEqualTo(stp.plusDays(1));
        assertThat(dvh2.getForventetOppstartTid()).isEqualTo(stp);

    }

    @Test
    void skal_mappe_til_behandling_dvh_ikke_vedtatt() {
        var mottattTidspunkt = lagMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        var scenario = opprettFørstegangssøknadScenario();
        var behandling = byggBehandling(scenario, BehandlingResultatType.OPPHØR, false);

        var behandlingsresultat = scenario.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var dvh = BehandlingDvhMapper.map(behandling, behandlingsresultat, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), null, null, l -> UUID.randomUUID());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getMottattTid()).isEqualTo(mottattTidspunkt.get(0).getMottattTidspunkt());
    }

    @Test
    void skal_mappe_ventestatus() {
        var mottattTidspunkt = lagMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        var scenario = opprettFørstegangssøknadScenario();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD, BehandlingStegType.VURDER_KOMPLETT_BEH);
        var behandling = byggBehandling(scenario, BehandlingResultatType.OPPHØR, false);

        var behandlingsresultat = scenario.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var dvh = BehandlingDvhMapper.map(behandling, behandlingsresultat, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), null, null, l -> UUID.randomUUID());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getBehandlingStatus()).isEqualTo(VenteGruppe.VenteKategori.VENT_TIDLIG.name());
        assertThat(dvh.getMottattTid()).isEqualTo(mottattTidspunkt.get(0).getMottattTidspunkt());
    }


    @Test
    void skal_mappe_til_behandling_dvh_vedtatt() {
        var mottattTidspunkt = lagMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);;
        var scenario = opprettFørstegangssøknadScenario();
        var behandling = byggBehandling(scenario, BehandlingResultatType.AVSLÅTT, true);

        var behandlingsresultat = scenario.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var dvh = BehandlingDvhMapper.map(behandling, behandlingsresultat, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), null, null, l -> UUID.randomUUID());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getMottattTid()).isEqualTo(mottattTidspunkt.get(0).getMottattTidspunkt());
    }

    @Test
    void skal_mappe_til_behandling_dvh_ferdig() {
        var mottattTidspunkt = lagMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        var scenario = opprettFørstegangssøknadScenario();
        var behandling = byggBehandling(scenario, BehandlingResultatType.AVSLÅTT, true);

        var behandlingsresultat = scenario.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var dvh = BehandlingDvhMapper.map(behandling, behandlingsresultat, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), null, null, l -> UUID.randomUUID());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getMottattTid()).isEqualTo(mottattTidspunkt.get(0).getMottattTidspunkt());
    }

    @Test
    void skal_mappe_til_behandling_dvh_ikke_ferdig() {
        var mottattTidspunkt = lagMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        var scenario = opprettFørstegangssøknadScenario();
        var behandling = byggBehandling(scenario, BehandlingResultatType.AVSLÅTT, false);

        var behandlingsresultat = scenario.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var dvh = BehandlingDvhMapper.map(behandling, behandlingsresultat, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), null, null, l -> UUID.randomUUID());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getMottattTid()).isEqualTo(mottattTidspunkt.get(0).getMottattTidspunkt());
    }

    @Test
    void skal_mappe_til_behandling_dvh_abrutt() {
        var mottattTidspunkt = lagMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        var scenario = opprettFørstegangssøknadScenario();
        var behandling = byggBehandling(scenario, BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET, true);

        var behandlingsresultat = scenario.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var dvh = BehandlingDvhMapper.map(behandling, behandlingsresultat, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), null, null, l -> UUID.randomUUID());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getMottattTid()).isEqualTo(mottattTidspunkt.get(0).getMottattTidspunkt());
    }

    @Test
    void skal_mappe_til_behandling_dvh_ikke_abrutt() {
        var mottattTidspunkt = lagMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        var scenario = opprettFørstegangssøknadScenario();
        var behandling = byggBehandling(scenario, BehandlingResultatType.IKKE_FASTSATT, false);

        var behandlingsresultat = scenario.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var dvh = BehandlingDvhMapper.map(behandling, behandlingsresultat, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), null, null, l -> UUID.randomUUID());
        assertThat(dvh).isNotNull();
        assertThat(dvh.getMottattTid()).isEqualTo(mottattTidspunkt.get(0).getMottattTidspunkt());
    }

    @Test
    void mapping_klage_med_påklagd_behandling() {
        var mottattDokument = lagMottattDokument(DokumentTypeId.KLAGE_DOKUMENT);
        var scenarioMorSøkerEngangsstønad = opprettFørstegangssøknadScenario();
        var behandling = byggBehandling(scenarioMorSøkerEngangsstønad, BehandlingResultatType.AVSLÅTT, true);

        var scenarioKlageEngangsstønad = opprettKlageScenario(scenarioMorSøkerEngangsstønad,
            KlageMedholdÅrsak.NYE_OPPLYSNINGER, KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE);
        var klageBehandling = scenarioKlageEngangsstønad.lagMocked();
        var klageRepository = scenarioKlageEngangsstønad.getKlageRepository();
        klageRepository.settPåklagdBehandlingId(klageBehandling.getId(), behandling.getId());

        var klageVurderingResultat = klageRepository.hentKlageResultatHvisEksisterer(klageBehandling.getId());

        var behandlingsresultat = scenarioKlageEngangsstønad.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(klageBehandling.getId());
        var dvh = BehandlingDvhMapper.map(klageBehandling, behandlingsresultat, mottattDokument, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), klageVurderingResultat, Optional.empty(), Optional.empty(), List.of(),
            Optional.empty(), Optional.empty(), null, KlageHjemmel.ENGANGS.getKabal(), l -> behandling.getUuid());

        assertThat(dvh.getRelatertBehandlingUuid()).as(
            "Forventer at relatert behandling på klagen er satt itl orginalbehandlingen vi klager på")
            .isEqualTo(behandling.getUuid());
        assertThat(dvh.getKlageHjemmel()).isEqualTo(KlageHjemmel.ENGANGS.getKabal());
        assertThat(dvh.getMottattTid()).isEqualTo(mottattDokument.get(0).getMottattTidspunkt());
    }

    @Test
    void mapping_klage_uten_påklagd_behandling() {
        var mottattTidspunkt = lagMottattDokument(DokumentTypeId.KLAGE_DOKUMENT);
        var scenarioMorSøkerEngangsstønad = opprettFørstegangssøknadScenario();

        var scenarioKlageEngangsstønad = opprettKlageScenario(scenarioMorSøkerEngangsstønad,
            KlageMedholdÅrsak.NYE_OPPLYSNINGER, KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE);
        var klageBehandling = scenarioKlageEngangsstønad.lagMocked();
        var klageRepository = scenarioKlageEngangsstønad.getKlageRepository();

        var klageVurderingResultat = klageRepository.hentKlageResultatHvisEksisterer(klageBehandling.getId());

        var behandlingsresultat = scenarioKlageEngangsstønad.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(klageBehandling.getId());
        var dvh = BehandlingDvhMapper.map(klageBehandling, behandlingsresultat, mottattTidspunkt, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), klageVurderingResultat, Optional.empty(), Optional.empty(), List.of(),
            Optional.empty(), Optional.empty(), null, KlageHjemmel.ENGANGS.getKabal(), l -> UUID.randomUUID());

        assertThat(dvh.getRelatertBehandlingUuid()).as(
            "Forventer at relatert behandling på klagen ikke blir satt når det ikke er påklagd ett vedtak.").isNull();
        assertThat(dvh.getKlageHjemmel()).isEqualTo(KlageHjemmel.ENGANGS.getKabal());
        assertThat(dvh.getMottattTid()).isEqualTo(mottattTidspunkt.get(0).getMottattTidspunkt());
    }

    @Test
    void skal_mappe_vedtak_id() {
        var mottattTidspunkt = lagMottattDokument(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        var scenario = opprettFørstegangssøknadScenario();
        var behandling = byggBehandling(scenario, BehandlingResultatType.INNVILGET, true);
        var behandlingsresultat = scenario.mockBehandlingRepositoryProvider()
            .getBehandlingsresultatRepository()
            .hent(behandling.getId());
        var vedtaksTid = LocalDateTime.now();
        var behandlingVedtak = BehandlingVedtak.builder()
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medBehandlingsresultat(behandlingsresultat)
            .medVedtakstidspunkt(vedtaksTid)
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .build();
        behandlingVedtak.setId(VEDTAK_ID);

        var dvh = BehandlingDvhMapper.map(behandling, behandlingsresultat, mottattTidspunkt,
            Optional.of(behandlingVedtak), Optional.of(LocalDate.now()), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(),
            Optional.empty(), Optional.empty(), null, null, l -> UUID.randomUUID());

        assertThat(dvh.getMottattTid()).isEqualTo(mottattTidspunkt.get(0).getMottattTidspunkt());
        assertThat(dvh.getVedtakTid()).isEqualTo(vedtaksTid);
    }

    private ScenarioKlageEngangsstønad opprettKlageScenario(AbstractTestScenario<?> abstractTestScenario,
                                                            KlageMedholdÅrsak klageMedholdÅrsak,
                                                            KlageVurderingOmgjør klageVurderingOmgjør) {
        var scenario = ScenarioKlageEngangsstønad.forMedholdNFP(abstractTestScenario);
        return scenario.medKlageMedholdÅrsak(klageMedholdÅrsak)
            .medKlageVurderingOmgjør(klageVurderingOmgjør)
            .medKlageHjemmel(KlageHjemmel.ENGANGS)
            .medBegrunnelse(KLAGE_BEGRUNNELSE)
            .medBehandlendeEnhet(BEHANDLENDE_ENHET_ID);
    }

    private Behandling byggBehandling(ScenarioMorSøkerEngangsstønad morSøkerEngangsstønad,
                                      BehandlingResultatType behandlingResultatType,
                                      boolean avsluttetFagsak) {
        var behandling = morSøkerEngangsstønad.lagMocked();
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet(BEHANDLENDE_ENHET, null));
        opprettBehandlingsresultat(behandling, behandlingResultatType);
        setFaksak(behandling, avsluttetFagsak);

        behandling.setOpprettetTidspunkt(OPPRETTET_TID);
        behandling.setOpprettetAv("OpprettetAv");
        return behandling;
    }

    private ScenarioMorSøkerEngangsstønad opprettFørstegangssøknadScenario() {
        return ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBruker(BRUKER_AKTØR_ID, NavBrukerKjønn.KVINNE)
            .medSaksnummer(SAKSNUMMER);
    }

    private void opprettBehandlingsresultat(Behandling behandling, BehandlingResultatType behandlingResultatType) {
        Behandlingsresultat.builder().medBehandlingResultatType(behandlingResultatType).buildFor(behandling);
    }

    private void setFaksak(Behandling behandling, boolean avsluttet) {
        if (avsluttet) {
            behandling.getFagsak().setAvsluttet();
        }
    }

    private static FamilieHendelseGrunnlagEntitet byggHendelseGrunnlag(LocalDate fødselsdato,
                                                                       LocalDate oppgittFødselsdato) {
        var hendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        if (oppgittFødselsdato != null) {
            hendelseBuilder.medFødselsDato(oppgittFødselsdato);
        }
        return FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medSøknadVersjon(hendelseBuilder)
            .medBekreftetVersjon(FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
                .medFødselsDato(fødselsdato))
            .build();
    }

    private List<MottattDokument> lagMottattDokument(DokumentTypeId dokumentTypeId) {
        var mottattDokument = Mockito.mock(MottattDokument.class);
        when(mottattDokument.getDokumentType()).thenReturn(dokumentTypeId);
        when(mottattDokument.getMottattTidspunkt()).thenReturn(LocalDateTime.now().minusDays(3));
        when(mottattDokument.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now());
        when(mottattDokument.getJournalpostId()).thenReturn(new JournalpostId("123"));
        return List.of(mottattDokument);
    }
}
