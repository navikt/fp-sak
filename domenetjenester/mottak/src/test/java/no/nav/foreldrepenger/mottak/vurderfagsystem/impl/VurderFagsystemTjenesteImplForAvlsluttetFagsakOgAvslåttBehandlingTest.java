package no.nav.foreldrepenger.mottak.vurderfagsystem.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.MottattDokumentPersisterer;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.es.VurderFagsystemTjenesteESImpl;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

class VurderFagsystemTjenesteImplForAvlsluttetFagsakOgAvslåttBehandlingTest extends EntityManagerAwareTest {

    private static final Period FRIST_INNSENDING_PERIODE = Period.ofWeeks(6);

    private final LocalDate DATO_ETTER_FRISTEN = LocalDate.now().minus(FRIST_INNSENDING_PERIODE.plusDays(2));
    private final LocalDate DATO_FØR_FRISTEN = LocalDate.now().minus(FRIST_INNSENDING_PERIODE.minusDays(2));
    private final AktørId AKTØR_ID = AktørId.dummy();

    private BehandlingRepository behandlingRepository;
    private VurderFagsystemFellesTjeneste vurderFagsystemFellesTjeneste;
    BehandlingRepositoryProvider behandlingRepositoryProvider;

    @BeforeEach
    public void setUp() {
        behandlingRepository = new BehandlingRepository(getEntityManager());
        var mottatteDokumentRepository = new MottatteDokumentRepository(getEntityManager());
        var mottattDokumentPersisterer = new MottattDokumentPersisterer(mock(BehandlingEventPubliserer.class));

        behandlingRepositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var mottatteDokumentTjeneste = new MottatteDokumentTjeneste(FRIST_INNSENDING_PERIODE,
            mottattDokumentPersisterer,
                mottatteDokumentRepository, behandlingRepositoryProvider);

        var familieTjeneste = new FamilieHendelseTjeneste(null, behandlingRepositoryProvider.getFamilieHendelseRepository());
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(behandlingRepositoryProvider);
        var fellesUtils = new VurderFagsystemFellesUtils(behandlingRepositoryProvider, familieTjeneste, mottatteDokumentTjeneste, null, null,
            fagsakRelasjonTjeneste);

        var fagsakTjeneste = new FagsakTjeneste(new FagsakRepository(getEntityManager()),
                new SøknadRepository(getEntityManager(), behandlingRepository));
        var tjenesteES = new VurderFagsystemTjenesteESImpl(fellesUtils);

        vurderFagsystemFellesTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES), null);
    }

    @Test
    void skalTilManuellVurderingHvisBehandlingErAvslåttPgaManglendeDokOgInnsendtDokErEtterFristForInnsending() {
        opprettBehandling(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.AVSLÅTT, Avslagsårsak.MANGLENDE_DOKUMENTASJON,
                VedtakResultatType.AVSLAG, DATO_ETTER_FRISTEN);
        var vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vfData.setStrukturertSøknad(false);

        // Act
        var resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        // Assert
        assertThat(resultat.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(resultat.getSaksnummer()).isEmpty();
    }

    @Test
    void skalReturnereVedtaksløsningMedSaksnummerVurderingHvisBehandlingErAvslåttPgaManglendeDokOgInnsendtDokErFørFristForInnsending() {
        // Arrange
        var behandling = opprettBehandling(BehandlingType.REVURDERING, BehandlingResultatType.AVSLÅTT, Avslagsårsak.MANGLENDE_DOKUMENTASJON,
                VedtakResultatType.AVSLAG, DATO_FØR_FRISTEN);
        var vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vfData.setStrukturertSøknad(false);

        // Act
        var resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        // Assert
        assertThat(resultat.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(resultat.getSaksnummer()).isEqualTo(Optional.of(behandling.getSaksnummer()));
    }

    @Test
    void skalReturnereVedtaksløsningMedSaksnummerVurderingHvisEttersendelsePåAngittSak() {
        // Arrange
        var behandling = opprettBehandling(BehandlingType.REVURDERING, BehandlingResultatType.AVSLÅTT, Avslagsårsak.MANGLENDE_DOKUMENTASJON,
                VedtakResultatType.AVSLAG, DATO_FØR_FRISTEN);
        var vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vfData.setStrukturertSøknad(false);
        vfData.setSaksnummer(behandling.getSaksnummer());

        // Act
        var resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        // Assert
        assertThat(resultat.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(resultat.getSaksnummer()).isEqualTo(Optional.of(behandling.getSaksnummer()));
    }

    @Test
    void skalTilManuellVurderingHvisBehandlingIkkeErAvslåttPgaManglendeDokOgInnsendtDokErFørFristForInnsending() {
        opprettBehandling(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.AVSLÅTT, Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING,
                VedtakResultatType.AVSLAG, DATO_FØR_FRISTEN);
        var vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setStrukturertSøknad(false);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);

        // Act
        var resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        // Assert
        assertThat(resultat.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(resultat.getSaksnummer()).isEmpty();
    }

    @Test
    void skalTilManuellVurderingHvisBehandlingIkkeErAvslåttPgaManglendeDokOgInnsendtDokErEtterFristForInnsending() {
        opprettBehandling(BehandlingType.REVURDERING, BehandlingResultatType.AVSLÅTT, Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING,
                VedtakResultatType.AVSLAG, DATO_ETTER_FRISTEN);
        var vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setStrukturertSøknad(false);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);

        // Act
        var resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        // Assert
        assertThat(resultat.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(resultat.getSaksnummer()).isEmpty();
    }

    @Test
    void skalTilManuellVurderingHvisBehandlingstypeErKlage() {
        var behandling = opprettBehandling(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.AVSLÅTT,
                Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING, VedtakResultatType.AVSLAG, DATO_FØR_FRISTEN);
        var vfData = opprettVurderFagsystem(BehandlingTema.UDEFINERT);
        vfData.setDokumentTypeId(DokumentTypeId.KLAGE_DOKUMENT);
        // Act
        var resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        // Assert
        assertThat(resultat.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(resultat.getSaksnummer()).isPresent();
        assertThat(resultat.getSaksnummer()).contains(behandling.getSaksnummer());
    }

    private Behandling opprettBehandling(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType, Avslagsårsak avslagsårsak,
            VedtakResultatType vedtakResultatType, LocalDate vedtaksdato) {
        var scenarioES = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(AKTØR_ID)
                .medFagsakId(1234L)
                .medSaksnummer(new Saksnummer("2345"))
                .medBehandlingType(behandlingType);
        scenarioES.medBehandlingsresultat(Behandlingsresultat.builder()
                .medBehandlingResultatType(behandlingResultatType)
                .medAvslagsårsak(avslagsårsak));
        scenarioES.medBehandlingVedtak()
                .medVedtakstidspunkt(vedtaksdato.atStartOfDay())
                .medVedtakResultatType(vedtakResultatType)
                .medAnsvarligSaksbehandler("fornavn etternavn");

        var behandling = scenarioES.lagre(behandlingRepositoryProvider);

        behandling.getFagsak().setAvsluttet();
        behandling.avsluttBehandling();

        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        return behandling;
    }

    private VurderFagsystem opprettVurderFagsystem(BehandlingTema behandlingTema) {
        var terminDatdato = LocalDate.of(2017, 7, 1);
        var vfData = new VurderFagsystem();
        vfData.setBehandlingTema(behandlingTema);
        vfData.setAktørId(AKTØR_ID);
        vfData.setStrukturertSøknad(true);
        vfData.setBarnTermindato(terminDatdato);
        return vfData;
    }
}
