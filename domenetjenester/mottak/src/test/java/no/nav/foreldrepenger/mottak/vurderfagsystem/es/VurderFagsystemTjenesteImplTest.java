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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class VurderFagsystemTjenesteImplTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private VurderFagsystemFellesTjeneste vurderFagsystemTjeneste;
    @Mock
    private BehandlingRepository behandlingRepositoryMock;
    private FamilieHendelseRepository grunnlagRepository;

    @Mock
    private FagsakRepository fagsakRepositoryMock;

    private Fagsak fagsakFødselES = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, lagNavBruker(), null, ÅPEN_SAKSNUMMER_1);
    private Fagsak fagsakAdopsjonES = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, null);
    private Fagsak fpFagsakUdefinert = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker());

    private MottatteDokumentTjeneste mottatteDokumentTjenesteMock;
    private BehandlingRepositoryProvider repositoryProvider;
    private VurderFagsystemFellesUtils fellesUtils;

    private FagsakTjeneste fagsakTjeneste;
    private VurderFagsystemTjeneste tjenesteES;

    @Before
    public void setUp() {
        mottatteDokumentTjenesteMock = Mockito.mock(MottatteDokumentTjeneste.class);
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        behandlingRepositoryMock = mock(BehandlingRepository.class);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        grunnlagRepository = mock(FamilieHendelseRepository.class);
        when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(grunnlagRepository);
        fagsakRepositoryMock = mock(FagsakRepository.class);
        when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepositoryMock);
        fagsakFødselES.setId(1L);
        fagsakAdopsjonES.setId(2L);
        var familieTjeneste = new FamilieHendelseTjeneste(null, grunnlagRepository);
        fellesUtils = new VurderFagsystemFellesUtils(repositoryProvider, familieTjeneste, mottatteDokumentTjenesteMock, null, null);
        fagsakTjeneste = new FagsakTjeneste(repositoryProvider.getFagsakRepository(), repositoryProvider.getSøknadRepository(), null);
        tjenesteES = new VurderFagsystemTjenesteESImpl(fellesUtils);
    }

    @Test
    public void nesteStegSkalVæreManuellVurderingHvisBrukerIkkeHarSakIVlForUstrukturertDokument() throws Exception {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);

        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.emptyList());

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    public void ustrukturertForsendelseSkalKnyttesTilBrukersNyesteÅpneSakHvisSlikFinnesOgJournalføres() {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        Optional<Behandling> b1 = byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_1), 10);
        Optional<Behandling> b2 = byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_2), 12);
        Optional<Behandling> b3 = byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_NY_FAGSAK_ID_1), 2);

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(ÅPEN_FAGSAK_ID_1)).thenReturn(b1);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(ÅPEN_FAGSAK_ID_2)).thenReturn(b2);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(AVSLT_NY_FAGSAK_ID_1)).thenReturn(b3);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(ÅPEN_FAGSAK_ID_1)).thenReturn(List.of(b1.get())); // NOSONAR
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(ÅPEN_FAGSAK_ID_2)).thenReturn(Collections.emptyList()); // NOSONAR
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(AVSLT_NY_FAGSAK_ID_1)).thenReturn(Collections.emptyList()); // NOSONAR
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(buildFagsak(ÅPEN_FAGSAK_ID_1, false, FagsakYtelseType.ENGANGSTØNAD));
        saksliste.add(buildFagsak(ÅPEN_FAGSAK_ID_2, false, FagsakYtelseType.ENGANGSTØNAD));
        saksliste.add(buildFagsak(AVSLT_NY_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(ÅPEN_SAKSNUMMER_1));

    }

    @Test
    public void ustrukturertDokumentSkalKnyttesTilBrukersNyesteÅpneSakHvisSlikFinnesOgJournalføres() {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.UDEFINERT, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        Optional<Behandling> b1 = byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_1), 10);

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(ÅPEN_FAGSAK_ID_1)).thenReturn(b1);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(ÅPEN_FAGSAK_ID_1)).thenReturn(List.of(b1.get())); // NOSONAR

        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(buildFagsak(ÅPEN_FAGSAK_ID_1, false, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(ÅPEN_SAKSNUMMER_1));
    }

    @Test
    public void ustrukturertDokumentSkalTilManuellJournalføringHvisIngenÅpenSak() {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.UDEFINERT, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        Optional<Behandling> b1 = byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_1), 10);

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(ÅPEN_FAGSAK_ID_1)).thenReturn(b1);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(ÅPEN_FAGSAK_ID_1)).thenReturn(Collections.emptyList()); // NOSONAR

        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(buildFagsak(ÅPEN_FAGSAK_ID_1, false, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    public void ustrukturertForsendelseSkalKnyttesTilBrukersNyesteÅpneSakHvisUspesifikk() {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        Optional<Behandling> b1 = byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_1), 10);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(ÅPEN_FAGSAK_ID_1)).thenReturn(List.of(b1.get())); // NOSONAR
        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(buildFagsak(ÅPEN_FAGSAK_ID_1, false, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(ÅPEN_SAKSNUMMER_1));
    }

    @Test
    public void ustrukturertForsendelseSkalSendesTilManuellBehandlingHvisNyesteAvsluttedeSakErNyereEnn3mndOgÅpenSakIkkeFinnes() throws Exception {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(AVSLT_NY_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_NY_FAGSAK_ID_1), 40));

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(AVSLT_NY_FAGSAK_ID_2))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_NY_FAGSAK_ID_2), 52));

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(AVSLT_GAMMEL_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_GAMMEL_FAGSAK_ID_1), 200));

        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksliste = new ArrayList<>();

        saksliste.add(buildFagsak(AVSLT_NY_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));
        saksliste.add(buildFagsak(AVSLT_NY_FAGSAK_ID_2, true, FagsakYtelseType.ENGANGSTØNAD));
        saksliste.add(buildFagsak(AVSLT_GAMMEL_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);

    }

    @Test
    public void ustrukturertVedleggSkalSendesTilManuellBehandlingHvisIngenSaker() throws Exception {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.KLAGE_DOKUMENT);

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.empty());

        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.empty());
        List<Fagsak> saksliste = new ArrayList<>();

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    public void ustrukturertSøknadSkalSendesTilManuellBehandlingHvisIngenSaker() throws Exception {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.empty());

        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.empty());
        List<Fagsak> saksliste = new ArrayList<>();

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    public void ustrukturertForsendelseSkalSendesTilManuellBehandlingHvisÅpenSakIkkeFinnesOgAvsluttedeSakerErEldreEnn3mndOgNyereEnn10Mnd()
            throws Exception {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO);

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(AVSLT_NY_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_NY_FAGSAK_ID_1), 140));

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(AVSLT_GAMMEL_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_GAMMEL_FAGSAK_ID_1), 452));

        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksliste = new ArrayList<>();

        saksliste.add(buildFagsak(AVSLT_NY_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));
        saksliste.add(buildFagsak(AVSLT_GAMMEL_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    public void nyVLSakSkalOpprettesForUstrukturertSøknadDersomBrukerIkkerHarÅpenSakNyesteAvsluttedeSakErEldreEnn10mnd() throws Exception {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(AVSLT_GAMMEL_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_GAMMEL_FAGSAK_ID_1), 340));

        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(340), BARN_FØDSELSDATO.minusDays(340));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        List<Fagsak> saksListe = new ArrayList<>();

        saksListe.add(buildFagsak(AVSLT_GAMMEL_FAGSAK_ID_1, true, FagsakYtelseType.ENGANGSTØNAD));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksListe);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    public void nesteStegSkalVæreInfotrygdDersomEksisterendeSakerGjelderAnnetBehandlingTema() throws Exception {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_ADOPSJON, false);
        vfData.setDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON);

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(AVSLT_GAMMEL_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(AVSLT_GAMMEL_FAGSAK_ID_1), 340));

        List<Fagsak> saksListe = new ArrayList<>();

        saksListe.add(buildFagsak(AVSLT_GAMMEL_FAGSAK_ID_1, true, FagsakYtelseType.FORELDREPENGER));
        saksListe.add(buildFagsak(ÅPEN_FAGSAK_ID_1, false, FagsakYtelseType.FORELDREPENGER));

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksListe);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    private BehandlendeFagsystem toVurderFagsystem(VurderFagsystem vfData) {
        return vurderFagsystemTjeneste.vurderFagsystem(vfData);

    }

    @Test
    public void nesteStegSkalVæreHentOgVurderInfotrygdHvisPassendeSakIkkeFinnesForStukturertDokument() throws Exception {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    public void nesteStegSkalVæreOpprettGSakOppgaveHvisMerEnnEnSakPasserForStukturertDokument() throws Exception {
        LocalDate terminDatdato = LocalDate.of(2017, 7, 1);

        VurderFagsystem vfData = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(fagsakFødselES);
        saksliste.add(fagsakFødselES);

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        Optional<Behandling> behandling = Optional.of(byggBehandlingFødsel(fagsakFødselES));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        final FamilieHendelseGrunnlagEntitet grunnlag = byggFødselGrunnlag(terminDatdato, null);
        when(grunnlagRepository.hentAggregat(behandling.get().getId())).thenReturn(grunnlag);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    public void nesteStegSkalVæreTilJournalføringeHvisAkkurattEnÅpenSakPasserForStukturertDokument() throws Exception {
        LocalDate terminDatdato = LocalDate.of(2017, 7, 1);

        VurderFagsystem vfData = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(fagsakFødselES);
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        Optional<Behandling> behandling = Optional.of(byggBehandlingFødsel(fagsakFødselES));
        final FamilieHendelseGrunnlagEntitet grunnlag = byggFødselGrunnlag(terminDatdato, null);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(List.of(behandling.get()));

        when(grunnlagRepository.hentAggregat(behandling.get().getId())).thenReturn(grunnlag);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    public void nesteStegSkalVæreVLMedSnrHvisAkkurattEnÅpenSakUtenBehandlingPasserForStukturertDokument() throws Exception {
        LocalDate terminDatdato = LocalDate.of(2017, 7, 1);

        VurderFagsystem vfData = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(fagsakFødselES);
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.empty());
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(Collections.emptyList());
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(fagsakFødselES.getSaksnummer()));
    }

    @Test
    public void nesteStegSkalVLMedSNRHvisEnSakMedLukketBehandlingSakPasserForStukturertDokument() throws Exception {
        LocalDate terminDatdato = LocalDate.of(2017, 7, 1);

        VurderFagsystem vfData = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(fagsakFødselES);
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        Optional<Behandling> behandling = Optional.of(byggBehandlingFødsel(fagsakFødselES));
        behandling.get().avsluttBehandling();
        final FamilieHendelseGrunnlagEntitet grunnlag = byggFødselGrunnlag(terminDatdato, null);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(Collections.emptyList());
        when(grunnlagRepository.hentAggregat(behandling.get().getId())).thenReturn(grunnlag);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(fagsakFødselES.getSaksnummer()));
    }

    @Test
    public void nesteStegSkalVæreHentÅVurderInfotrygdSakHvisIngenSakPasserForStukturertDokument() throws Exception {

        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        List<Fagsak> saksliste = new ArrayList<>();
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    public void nesteStegSkalVæreHentOgVurderInfotrygdSakHvisBrukerHarSakIVLMenDenIkkePasserForStukturertDokument() throws Exception {

        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD_FØDSEL, true);

        List<Fagsak> fagsakListe = new ArrayList<>();
        fagsakListe.add(fagsakFødselES);
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(fagsakListe);

        LocalDate terminDatDato = LocalDate.of(2018, 7, 1);

        Optional<Behandling> behandling = Optional.of(byggBehandlingFødsel(fagsakFødselES));
        final FamilieHendelseGrunnlagEntitet grunnlag = byggFødselGrunnlag(terminDatDato, null);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    public void skalReturnereTrueNårFagÅrsakTypeErUdefinertOgBehandlingTemaErForeldrePenger() {
        Optional<Behandling> behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        final FamilieHendelseGrunnlagEntitet grunnlag = byggFødselGrunnlag(null, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));

        assertThat(FagsakYtelseType.FORELDREPENGER.equals(fpFagsakUdefinert.getYtelseType()) && fpFagsakUdefinert.erÅpen()).isTrue();
    }

    @Test
    public void skalReturnereInfotrygdBehandlingNårIngenÅpneSakerFinnesPåBrukerForEngangsstønad() {
        VurderFagsystem fagsystem = byggVurderFagsystem(BehandlingTema.ENGANGSSTØNAD, true);
        fagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.emptyList());
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteES));

        BehandlendeFagsystem result = toVurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }
}
