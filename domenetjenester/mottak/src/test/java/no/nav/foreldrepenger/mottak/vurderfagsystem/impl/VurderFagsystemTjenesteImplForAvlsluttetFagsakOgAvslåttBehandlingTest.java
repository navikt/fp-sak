package no.nav.foreldrepenger.mottak.vurderfagsystem.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.DokumentPersistererTjeneste;
import no.nav.foreldrepenger.mottak.publiserer.publish.MottattDokumentPersistertPubliserer;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.es.VurderFagsystemTjenesteESImpl;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class VurderFagsystemTjenesteImplForAvlsluttetFagsakOgAvslåttBehandlingTest {

    private static final Period FRIST_INNSENDING_PERIODE = Period.ofWeeks(6);

    private final LocalDate DATO_ETTER_FRISTEN = LocalDate.now().minus(FRIST_INNSENDING_PERIODE.plusDays(2));
    private final LocalDate DATO_FØR_FRISTEN = LocalDate.now().minus(FRIST_INNSENDING_PERIODE.minusDays(2));
    private final AktørId AKTØR_ID = AktørId.dummy();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private VurderFagsystemFellesTjeneste vurderFagsystemFellesTjeneste;

    @Before
    public void setUp() {
        MottatteDokumentRepository mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        DokumentPersistererTjeneste dokumentPersistererTjeneste = new DokumentPersistererTjeneste(mock(MottattDokumentPersistertPubliserer.class));

        MottatteDokumentTjeneste mottatteDokumentTjeneste =
            new MottatteDokumentTjeneste(FRIST_INNSENDING_PERIODE, dokumentPersistererTjeneste, mottatteDokumentRepository, repositoryProvider);

        VurderFagsystemFellesUtils fellesUtils = new VurderFagsystemFellesUtils(repositoryProvider, mottatteDokumentTjeneste, null, null);

        var fagsakTjeneste = new FagsakTjeneste(repositoryProvider, null);
        var tjenesteES = new VurderFagsystemTjenesteESImpl(fellesUtils);

        vurderFagsystemFellesTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));
    }

    @Test
    public void skalTilManuellVurderingHvisBehandlingErAvslåttPgaManglendeDokOgInnsendtDokErEtterFristForInnsending() {
        opprettBehandling(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.AVSLÅTT, Avslagsårsak.MANGLENDE_DOKUMENTASJON, VedtakResultatType.AVSLAG, DATO_ETTER_FRISTEN);
        VurderFagsystem vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vfData.setStrukturertSøknad(false);

        //Act
        BehandlendeFagsystem resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        //Assert
        assertThat(resultat.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(resultat.getSaksnummer()).isEmpty();
    }

    @Test
    public void skalReturnereVedtaksløsningMedSaksnummerVurderingHvisBehandlingErAvslåttPgaManglendeDokOgInnsendtDokErFørFristForInnsending() {
        //Arrange
        Behandling behandling = opprettBehandling(BehandlingType.REVURDERING, BehandlingResultatType.AVSLÅTT, Avslagsårsak.MANGLENDE_DOKUMENTASJON, VedtakResultatType.AVSLAG, DATO_FØR_FRISTEN);
        VurderFagsystem vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vfData.setStrukturertSøknad(false);

        //Act
        BehandlendeFagsystem resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        //Assert
        assertThat(resultat.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(resultat.getSaksnummer()).isEqualTo(Optional.of(behandling.getFagsak().getSaksnummer()));
    }

    @Test
    public void skalReturnereVedtaksløsningMedSaksnummerVurderingHvisEttersendelsePåAngittSak() {
        //Arrange
        Behandling behandling = opprettBehandling(BehandlingType.REVURDERING, BehandlingResultatType.AVSLÅTT, Avslagsårsak.MANGLENDE_DOKUMENTASJON, VedtakResultatType.AVSLAG, DATO_FØR_FRISTEN);
        VurderFagsystem vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vfData.setStrukturertSøknad(false);
        vfData.setSaksnummer(behandling.getFagsak().getSaksnummer());

        //Act
        BehandlendeFagsystem resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        //Assert
        assertThat(resultat.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(resultat.getSaksnummer()).isEqualTo(Optional.of(behandling.getFagsak().getSaksnummer()));
    }

    @Test
    public void skalTilManuellVurderingHvisBehandlingIkkeErAvslåttPgaManglendeDokOgInnsendtDokErFørFristForInnsending() {
        opprettBehandling(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.AVSLÅTT, Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING, VedtakResultatType.AVSLAG, DATO_FØR_FRISTEN);
        VurderFagsystem vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setStrukturertSøknad(false);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);

        //Act
        BehandlendeFagsystem resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        //Assert
        assertThat(resultat.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(resultat.getSaksnummer()).isEmpty();
    }

    @Test
    public void skalTilManuellVurderingHvisBehandlingIkkeErAvslåttPgaManglendeDokOgInnsendtDokErEtterFristForInnsending() {
        opprettBehandling(BehandlingType.REVURDERING, BehandlingResultatType.AVSLÅTT, Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING, VedtakResultatType.AVSLAG, DATO_ETTER_FRISTEN);
        VurderFagsystem vfData = opprettVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL);
        vfData.setStrukturertSøknad(false);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);

        //Act
        BehandlendeFagsystem resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        //Assert
        assertThat(resultat.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(resultat.getSaksnummer()).isEmpty();
    }

    @Test
    public void skalTilManuellVurderingHvisBehandlingstypeErKlage() {
        var behandling = opprettBehandling(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingResultatType.AVSLÅTT, Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING, VedtakResultatType.AVSLAG, DATO_FØR_FRISTEN);
        VurderFagsystem vfData = opprettVurderFagsystem(BehandlingTema.UDEFINERT);
        vfData.setDokumentTypeId(DokumentTypeId.KLAGE_DOKUMENT);
        //Act
        BehandlendeFagsystem resultat = vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

        //Assert
        assertThat(resultat.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(resultat.getSaksnummer()).isPresent();
        assertThat(resultat.getSaksnummer().get()).isEqualTo(behandling.getFagsak().getSaksnummer());
    }


    private Behandling opprettBehandling(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType, Avslagsårsak avslagsårsak, VedtakResultatType vedtakResultatType, LocalDate vedtaksdato) {
        ScenarioMorSøkerEngangsstønad scenarioES = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(AKTØR_ID)
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

        Behandling behandling = scenarioES.lagre(repositoryProvider);

        behandling.getFagsak().setAvsluttet();
        behandling.avsluttBehandling();

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        return behandling;
    }

    private VurderFagsystem opprettVurderFagsystem(BehandlingTema behandlingTema) {
        LocalDate terminDatdato = LocalDate.of(2017, 7, 1);
        VurderFagsystem vfData = new VurderFagsystem();
        vfData.setBehandlingTema(behandlingTema);
        vfData.setAktørId(AKTØR_ID);
        vfData.setStrukturertSøknad(true);
        vfData.setBarnTermindato(terminDatdato);
        return vfData;
    }
}

