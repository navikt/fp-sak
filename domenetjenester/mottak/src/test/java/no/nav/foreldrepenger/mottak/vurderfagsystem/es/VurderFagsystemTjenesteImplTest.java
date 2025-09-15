package no.nav.foreldrepenger.mottak.vurderfagsystem.es;

import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.buildFagsak;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.byggBehandlingFødsel;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.byggFødselGrunnlag;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.lagNavBruker;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.AVSLT_GAMMEL_FAGSAK_ID_1;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.AVSLT_NY_FAGSAK_ID_1;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.AVSLT_NY_FAGSAK_ID_2;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.BARN_FØDSELSDATO;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.BARN_TERMINDATO;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggBehandlingMedEndretDato;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggBehandlingUdefinert;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggVurderFagsystem;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggVurderFagsystemMedTermin;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.fagsakFødselMedId;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ÅPEN_FAGSAK_ID_1;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ÅPEN_FAGSAK_ID_2;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ÅPEN_SAKSNUMMER_1;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ÅPEN_SAKSNUMMER_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(MockitoExtension.class)
class VurderFagsystemTjenesteImplTest {

    private VurderFagsystemFellesTjeneste vurderFagsystemTjeneste;
    @Mock
    private BehandlingRepository behandlingRepositoryMock;
    @Mock
    private FamilieHendelseRepository grunnlagRepository;
    @Mock
    private BehandlingVedtakRepository behandlingVedtakRepositoryMock;
    @Mock
    private FagsakRepository fagsakRepositoryMock;
    @Mock
    private NavBrukerTjeneste brukerTjeneste;

    private Fagsak fagsakFødselES = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, lagNavBruker(), null, ÅPEN_SAKSNUMMER_1);
    private Fagsak fagsakAdopsjonES = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
    private Fagsak fpFagsakUdefinert = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker(), null, ÅPEN_SAKSNUMMER_2);

    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjenesteMock;
    @Mock
    private BehandlingRepositoryProvider repositoryProvider;
    private VurderFagsystemFellesUtils fellesUtils;

    private FagsakTjeneste fagsakTjeneste;
    private VurderFagsystemTjeneste tjenesteES;

    @BeforeEach
    void setUp() {
        lenient().when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        lenient().when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(grunnlagRepository);
        lenient().when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepositoryMock);
        lenient().when(repositoryProvider.getBehandlingVedtakRepository()).thenReturn(behandlingVedtakRepositoryMock);
        fagsakFødselES.setId(1L);
        fagsakAdopsjonES.setId(2L);
        var familieTjeneste = new FamilieHendelseTjeneste(null, grunnlagRepository);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        fellesUtils = new VurderFagsystemFellesUtils(repositoryProvider, familieTjeneste, mottatteDokumentTjenesteMock, null, null,
            fagsakRelasjonTjeneste);
        fagsakTjeneste = new FagsakTjeneste(repositoryProvider.getFagsakRepository(), repositoryProvider.getSøknadRepository());
        tjenesteES = new VurderFagsystemTjenesteESImpl(fellesUtils);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES), brukerTjeneste);
    }

    @Test
    void nesteStegSkalVæreManuellVurderingHvisBrukerIkkeHarSakIVlForUstrukturertDokument() {
        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.emptyList());

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    void ustrukturertForsendelseSkalKnyttesTilBrukersNyesteÅpneSakHvisSlikFinnesOgJournalføres() {
        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        var b1 = byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_1), 10);
        var b2 = byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_2), 12);
        var b3 = byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_NY_FAGSAK_ID_1), 2);

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(ÅPEN_FAGSAK_ID_1)).thenReturn(b1);
        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(ÅPEN_FAGSAK_ID_2)).thenReturn(b2);
        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(AVSLT_NY_FAGSAK_ID_1)).thenReturn(b3);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(ÅPEN_FAGSAK_ID_1)).thenReturn(List.of(b1.get()));
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(ÅPEN_FAGSAK_ID_2)).thenReturn(Collections.emptyList());
        lenient().when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(AVSLT_NY_FAGSAK_ID_1)).thenReturn(Collections.emptyList());

        var familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(buildFagsak(ÅPEN_FAGSAK_ID_1, false, FagsakYtelseType.ENGANGSTØNAD));
        saksliste.add(buildFagsak(ÅPEN_FAGSAK_ID_2, false, FagsakYtelseType.ENGANGSTØNAD));
        saksliste.add(buildFagsak(AVSLT_NY_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(ÅPEN_SAKSNUMMER_1));

    }

    @Test
    void ustrukturertDokumentSkalKnyttesTilBrukersNyesteÅpneSakHvisSlikFinnesOgJournalføres() {
        var vfData = byggVurderFagsystem(BehandlingTema.UDEFINERT, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        var b1 = byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_1), 10);

        lenient().when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(ÅPEN_FAGSAK_ID_1)).thenReturn(b1);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(ÅPEN_FAGSAK_ID_1)).thenReturn(List.of(b1.get()));

        var familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(buildFagsak(ÅPEN_FAGSAK_ID_1, false, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(ÅPEN_SAKSNUMMER_1));
    }

    @Test
    void ustrukturertDokumentSkalTilManuellJournalføringHvisIngenÅpenSak() {
        var vfData = byggVurderFagsystem(BehandlingTema.UDEFINERT, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        var b1 = byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_1), 10);

        lenient().when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(ÅPEN_FAGSAK_ID_1)).thenReturn(b1);
        lenient().when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(ÅPEN_FAGSAK_ID_1)).thenReturn(Collections.emptyList());

        var familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(buildFagsak(ÅPEN_FAGSAK_ID_1, false, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    void ustrukturertForsendelseSkalKnyttesTilBrukersNyesteÅpneSakHvisUspesifikk() {
        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        var b1 = byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_1), 10);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(ÅPEN_FAGSAK_ID_1)).thenReturn(List.of(b1.get()));
        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(buildFagsak(ÅPEN_FAGSAK_ID_1, false, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(ÅPEN_SAKSNUMMER_1));
    }

    @Test
    void ustrukturertForsendelseSkalSendesTilManuellBehandlingHvisNyesteAvsluttedeSakErNyereEnn3mndOgÅpenSakIkkeFinnes() {
        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(AVSLT_NY_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_NY_FAGSAK_ID_1), 40));

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(AVSLT_NY_FAGSAK_ID_2))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_NY_FAGSAK_ID_2), 52));

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(AVSLT_GAMMEL_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_GAMMEL_FAGSAK_ID_1), 200));

        var familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksliste = new ArrayList<>();

        saksliste.add(buildFagsak(AVSLT_NY_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));
        saksliste.add(buildFagsak(AVSLT_NY_FAGSAK_ID_2, true, FagsakYtelseType.ENGANGSTØNAD));
        saksliste.add(buildFagsak(AVSLT_GAMMEL_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);

    }

    @Test
    void klageSkalSendesTilManuellBehandlingHvisIngenSaker() {
        var vfData = byggVurderFagsystem(BehandlingTema.UDEFINERT, false);
        vfData.setDokumentTypeId(DokumentTypeId.KLAGE_DOKUMENT);

        lenient().when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(any())).thenReturn(Optional.empty());

        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.empty());
        List<Fagsak> saksliste = new ArrayList<>();

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    void klageSkalSendesTilManuellBehandlingHvisFlereSaker() {
        var vfData = byggVurderFagsystem(BehandlingTema.UDEFINERT, false);
        vfData.setDokumentTypeId(DokumentTypeId.KLAGE_DOKUMENT);

        fagsakFødselES.setOpprettetTidspunkt(LocalDateTime.now().minusWeeks(5));
        fpFagsakUdefinert.setOpprettetTidspunkt(LocalDateTime.now().minusWeeks(6));

        var behandlingES = Optional.of(byggBehandlingFødsel(fagsakFødselES));
        var behandlingFP = Optional.of(byggBehandlingFødsel(fpFagsakUdefinert));
        when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakFødselES.getId())).thenReturn(behandlingES);
        when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fpFagsakUdefinert.getId())).thenReturn(behandlingFP);
        var vedtak = mock(BehandlingVedtak.class);
        when(vedtak.getVedtakstidspunkt()).thenReturn(LocalDateTime.now().minusDays(1));
        when(behandlingVedtakRepositoryMock.hentForBehandling(any())).thenReturn(vedtak);

        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.empty());
        var saksliste = List.of(fagsakFødselES, fpFagsakUdefinert);

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    void klageSkalSendesFordelesTilNyesteHvisFlereSakerOgKunEnNyere() {
        var vfData = byggVurderFagsystem(BehandlingTema.UDEFINERT, false);
        vfData.setDokumentTypeId(DokumentTypeId.KLAGE_DOKUMENT);

        fagsakFødselES.setOpprettetTidspunkt(LocalDateTime.now().minusMonths(15));
        fpFagsakUdefinert.setOpprettetTidspunkt(LocalDateTime.now().minusWeeks(4));

        var behandlingES = Optional.of(byggBehandlingFødsel(fagsakFødselES));
        var behandlingFP = Optional.of(byggBehandlingFødsel(fpFagsakUdefinert));
        when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakFødselES.getId())).thenReturn(behandlingES);
        when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fpFagsakUdefinert.getId())).thenReturn(behandlingFP);
        var vedtak = mock(BehandlingVedtak.class);
        when(vedtak.getVedtakstidspunkt()).thenReturn(LocalDateTime.now().minusDays(1));
        when(behandlingVedtakRepositoryMock.hentForBehandling(any())).thenReturn(vedtak);

        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.empty());
        var saksliste = List.of(fagsakFødselES, fpFagsakUdefinert);

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isEqualTo(Optional.of(fpFagsakUdefinert.getSaksnummer()));
    }

    @Test
    void klageMedBehandlingTemaSkalFordelesHvisFlereSakerMedUlikYtelse() {
        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.KLAGE_DOKUMENT);

        var behandlingES = Optional.of(byggBehandlingFødsel(fagsakFødselES));
        when(behandlingRepositoryMock.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakFødselES.getId())).thenReturn(behandlingES);
        var vedtak = mock(BehandlingVedtak.class);
        when(vedtak.getVedtakstidspunkt()).thenReturn(LocalDateTime.now().minusDays(1));
        when(behandlingVedtakRepositoryMock.hentForBehandling(any())).thenReturn(vedtak);

        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.empty());
        var saksliste = List.of(fagsakFødselES, fpFagsakUdefinert);

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void ustrukturertSøknadSkalSendesTilManuellBehandlingHvisIngenSaker() {
        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);

        lenient().when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.empty());

        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.empty());
        List<Fagsak> saksliste = new ArrayList<>();

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void ustrukturertForsendelseSkalSendesTilManuellBehandlingHvisÅpenSakIkkeFinnesOgAvsluttedeSakerErEldreEnn3mndOgNyereEnn10Mnd() {
        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(AVSLT_NY_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_NY_FAGSAK_ID_1), 140));

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(AVSLT_GAMMEL_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_GAMMEL_FAGSAK_ID_1), 452));

        var familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksliste = new ArrayList<>();

        saksliste.add(buildFagsak(AVSLT_NY_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));
        saksliste.add(buildFagsak(AVSLT_GAMMEL_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    void nyVLSakSkalOpprettesForUstrukturertSøknadDersomBrukerIkkerHarÅpenSakNyesteAvsluttedeSakErEldreEnn10mnd() {
        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(AVSLT_GAMMEL_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_GAMMEL_FAGSAK_ID_1), 340));

        var familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksListe = new ArrayList<>();

        saksListe.add(buildFagsak(AVSLT_GAMMEL_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksListe);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void nesteStegSkalVæreInfotrygdDersomEksisterendeSakerGjelderAnnetBehandlingTema() {
        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_ADOPSJON, false);
        vfData.setDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON);

        lenient().when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(AVSLT_GAMMEL_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_GAMMEL_FAGSAK_ID_1), 340));

        List<Fagsak> saksListe = new ArrayList<>();

        saksListe.add(buildFagsak(AVSLT_GAMMEL_FAGSAK_ID_1, true, FagsakYtelseType.FORELDREPENGER));
        saksListe.add(buildFagsak(ÅPEN_FAGSAK_ID_1, false, FagsakYtelseType.FORELDREPENGER));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksListe);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void nesteStegSkalVæreHentOgVurderInfotrygdHvisPassendeSakIkkeFinnesForStukturertDokument() {
        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void strukturertSøknadIngenMatchedeEllerPassendeFagsakSkalOppretteNyFagsakHvisOpprettSakVedBehovErSatt() {
        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);
        vfData.setOpprettSakVedBehov(true);
        when(brukerTjeneste.hentEllerOpprettFraAktørId(vfData.getAktørId())).thenReturn(lagNavBruker());
        var nySak = new Saksnummer("123456789");
        when(fagsakRepositoryMock.genererNyttSaksnummer()).thenReturn(nySak);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(saksnr -> assertThat(saksnr).isEqualTo(nySak));
    }

    @Test
    void nesteStegSkalVæreOpprettGSakOppgaveHvisMerEnnEnSakPasserForStukturertDokument() {
        var terminDatdato = LocalDate.of(2017, 7, 1);

        var vfData = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);
        vfData.setOpprettSakVedBehov(true); // Skal ikke overkjøre MANUELL_VURDERING!

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(List.of(fagsakFødselES, fagsakFødselES));

        var behandling = Optional.of(byggBehandlingFødsel(fagsakFødselES));
        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(behandling);
        var grunnlag = byggFødselGrunnlag(terminDatdato, null);
        lenient().when(grunnlagRepository.hentAggregat(behandling.get().getId())).thenReturn(grunnlag);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    void nesteStegSkalVæreTilJournalføringeHvisAkkurattEnÅpenSakPasserForStukturertDokument() {
        var terminDatdato = LocalDate.of(2017, 7, 1);

        var vfData = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(fagsakFødselES);
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var behandling = Optional.of(byggBehandlingFødsel(fagsakFødselES));
        var grunnlag = byggFødselGrunnlag(terminDatdato, null);
        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(behandling);
        lenient().when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(List.of(behandling.get()));

        lenient().when(grunnlagRepository.hentAggregat(behandling.get().getId())).thenReturn(grunnlag);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void nesteStegSkalVæreVLMedSnrHvisAkkurattEnÅpenSakUtenBehandlingPasserForStukturertDokument() {
        var terminDatdato = LocalDate.of(2017, 7, 1);

        var vfData = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(fagsakFødselES);
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);
        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.empty());
        lenient().when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(Collections.emptyList());

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(fagsakFødselES.getSaksnummer()));
    }

    @Test
    void nesteStegSkalVLMedSNRHvisEnSakMedLukketBehandlingSakPasserForStukturertDokument() {
        var terminDatdato = LocalDate.of(2017, 7, 1);

        var vfData = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(fagsakFødselES);
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var behandling = Optional.of(byggBehandlingFødsel(fagsakFødselES));
        behandling.get().avsluttBehandling();
        var grunnlag = byggFødselGrunnlag(terminDatdato, null);
        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(behandling);
        lenient().when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(Collections.emptyList());
        lenient().when(grunnlagRepository.hentAggregat(behandling.get().getId())).thenReturn(grunnlag);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(fagsakFødselES.getSaksnummer()));
    }

    @Test
    void nesteStegSkalVæreHentÅVurderInfotrygdSakHvisIngenSakPasserForStukturertDokument() {

        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        List<Fagsak> saksliste = new ArrayList<>();
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void nesteStegSkalVæreHentOgVurderInfotrygdSakHvisBrukerHarSakIVLMenDenIkkePasserForStukturertDokument() {

        var vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        List<Fagsak> fagsakListe = new ArrayList<>();
        fagsakListe.add(fagsakFødselES);
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(fagsakListe);

        var terminDatDato = LocalDate.of(2018, 7, 1);

        var behandling = Optional.of(byggBehandlingFødsel(fagsakFødselES));
        var grunnlag = byggFødselGrunnlag(terminDatDato, null);
        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(behandling);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));

        var result = vurderFagsystemTjeneste.vurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void skalReturnereTrueNårFagÅrsakTypeErUdefinertOgBehandlingTemaErForeldrePenger() {
        var behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));

        lenient().when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(behandling);
        var grunnlag = byggFødselGrunnlag(null, null);
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));

        assertThat(FagsakYtelseType.FORELDREPENGER.equals(fpFagsakUdefinert.getYtelseType()) && fpFagsakUdefinert.erÅpen()).isTrue();
    }

    @Test
    void skalReturnereInfotrygdBehandlingNårIngenÅpneSakerFinnesPåBrukerForEngangsstønad() {
        var fagsystem = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD, true);
        fagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.emptyList());

        var result = vurderFagsystemTjeneste.vurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }
}
