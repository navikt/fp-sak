package no.nav.foreldrepenger.mottak.vurderfagsystem.fp;

import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.buildFagsak;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.byggBehandlingFødsel;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.byggFødselGrunnlag;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.lagNavBruker;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ANNEN_PART_ID;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.AVSLT_NY_FAGSAK_ID_1;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.BARN_FØDSELSDATO;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.BARN_TERMINDATO;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.BRUKER_AKTØR_ID;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.JOURNALPOST_ID;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.buildFagsakMedUdefinertRelasjon;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggBehandlingMedEndretDato;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggBehandlingUdefinert;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggVurderFagsystem;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggVurderFagsystemMedAnnenPart;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggVurderFagsystemMedTermin;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.fagsakFødselMedId;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ÅPEN_FAGSAK_ID_1;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ÅPEN_FAGSAK_ID_2;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ÅPEN_SAKSNUMMER_1;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ÅPEN_SAKSNUMMER_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
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

    private Fagsak fagsakFødselFP = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker(), null, ÅPEN_SAKSNUMMER_2);
    private Fagsak fagsakAnnenPartFP = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
    private Fagsak fpFagsakUdefinert = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker());

    private Fagsak fagsakSpyFP = spy(Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker()));

    private MottatteDokumentTjeneste mottatteDokumentTjenesteMock ;
    private BehandlingRepositoryProvider repositoryProvider;
    private VurderFagsystemFellesUtils fellesUtils;

    private FagsakTjeneste fagsakTjeneste;
    private VurderFagsystemTjeneste tjenesteFP;

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
        fellesUtils = new VurderFagsystemFellesUtils(repositoryProvider, mottatteDokumentTjenesteMock, null, null);
        fagsakTjeneste = new FagsakTjeneste(repositoryProvider, null);
        tjenesteFP = new VurderFagsystemTjenesteImpl(fellesUtils, repositoryProvider);

    }

    @Test
    public void nesteStegSkalVæreVLHvisSakErFlaggetSkalBehandlesAvInfotrygd() throws Exception {
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.FORELDREPENGER_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL);

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker());
        fagsak.setSkalTilInfotrygd(true);

        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.emptyList());

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);

        vfData.setStrukturertSøknad(true);
        result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    public void nesteStegSkalVæreManuellHvisEndringPåSakFlaggetSkalBehandlesAvInfotrygd() throws Exception {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker(), RelasjonsRolleType.MORA, ÅPEN_SAKSNUMMER_1);
        fagsak.setSkalTilInfotrygd(true);
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.FORELDREPENGER_FØDSEL, true);
        vfData.setDokumentTypeId(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD);
        vfData.setSaksnummer(fagsak.getSaksnummer());

        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));
        when(fagsakRepositoryMock.hentSakGittSaksnummer(any(), anyBoolean())).thenReturn(Optional.of(fagsak));
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.emptyList());

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    public void nesteStegSkalVæreVLHvisEndringMedSaksnummer() throws Exception {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker(), RelasjonsRolleType.MORA, ÅPEN_SAKSNUMMER_1);
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.FORELDREPENGER_FØDSEL, true);
        vfData.setDokumentTypeId(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD);
        vfData.setSaksnummer(fagsak.getSaksnummer());

        when(fagsakRepositoryMock.hentSakGittSaksnummer(any(), anyBoolean())).thenReturn(Optional.of(fagsak));
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(List.of(fagsak));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(ÅPEN_SAKSNUMMER_1));
    }

    private BehandlendeFagsystem toVurderFagsystem(VurderFagsystem vfData) {
        return vurderFagsystemTjeneste.vurderFagsystem(vfData);

    }

    @Test
    public void nesteStegSkalVæreVLHvisIngenSakPasserMenDetFinnesEnAvsluttetSak() {
        LocalDate terminDatdato = LocalDate.of(2017, 7, 1);

        VurderFagsystem vfData = byggVurderFagsystemMedTermin(terminDatdato.plusYears(1), BehandlingTema.FORELDREPENGER_FØDSEL, true);

        List<Fagsak> saksliste = new ArrayList<>();
        Fagsak fagsak = buildFagsak(AVSLT_NY_FAGSAK_ID_1, true, FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = byggBehandlingFødsel(fagsak);
        behandling.avsluttBehandling();
        Optional<Behandling> behandlingOpt = Optional.of(behandling);
        saksliste.add(fagsak);
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        final FamilieHendelseGrunnlagEntitet grunnlag = byggFødselGrunnlag(terminDatdato, null);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandlingOpt);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(Collections.emptyList());
        when(grunnlagRepository.hentAggregat(behandling.getId())).thenReturn(grunnlag);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandlingOpt.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getSaksnummer()).isEmpty();
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    public void skalReturnereTrueNårFagÅrsakTypeErUdefinertOgBehandlingTemaErForeldrePenger() {
        Optional<Behandling> behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        final FamilieHendelseGrunnlagEntitet grunnlag = byggFødselGrunnlag(null, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));

        assertThat(fpFagsakUdefinert.getYtelseType().gjelderForeldrepenger() && fpFagsakUdefinert.erÅpen()).isTrue();
    }

    @Test
    public void skalReturnereManuellBehandlingNårFlereÅpneSakerFinnesPåBruker() {
        VurderFagsystem fagsystem = byggVurderFagsystem(BehandlingTema.FORELDREPENGER, true);
        when(fagsakRepositoryMock.hentJournalpost(any())).thenReturn(Optional.empty());

        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(buildFagsakMedUdefinertRelasjon(ÅPEN_FAGSAK_ID_1, false));
        saksliste.add(buildFagsakMedUdefinertRelasjon(ÅPEN_FAGSAK_ID_2, false));

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(ÅPEN_FAGSAK_ID_1))
            .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_1), 10));

        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(ÅPEN_FAGSAK_ID_2))
            .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_2), 12));
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        Optional<Behandling> behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(List.of(behandling.get()));
        final FamilieHendelseGrunnlagEntitet grunnlag = byggFødselGrunnlag(null, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        BehandlendeFagsystem result = toVurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    public void skalReturnereVedtaksløsningMedSaksnummerNårEnSakFinnesMedStatusOpprett() {
        LocalDate terminDatdato = LocalDate.of(2019, 1, 1);
        VurderFagsystem fagsystem = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.FORELDREPENGER_FØDSEL, true);
        when(fagsakRepositoryMock.hentJournalpost(any())).thenReturn(Optional.empty());

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.singletonList(buildFagsakMedUdefinertRelasjon(123L, false)));

        Optional<Behandling> behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        final FamilieHendelseGrunnlagEntitet grunnlag = byggFødselGrunnlag(terminDatdato, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        BehandlendeFagsystem result = toVurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isNotEmpty();
    }

    @Test
    public void skalReturnereVedtaksløsningMedSaksnummerNårEnSakFinnesMedStatusLøpende() {
        LocalDate terminDatdato = LocalDate.of(2019, 1, 1);
        VurderFagsystem fagsystem = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.FORELDREPENGER_FØDSEL, true);
        when(fagsakRepositoryMock.hentJournalpost(any())).thenReturn(Optional.empty());

        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.singletonList(buildFagsakMedUdefinertRelasjon(123L, false)));
        when(fagsakSpyFP.getStatus()).thenReturn(FagsakStatus.LØPENDE);

        Optional<Behandling> behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        final FamilieHendelseGrunnlagEntitet grunnlag = byggFødselGrunnlag(terminDatdato, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        BehandlendeFagsystem result = toVurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isNotEmpty();
    }

    @Test
    public void skalReturnereVLMedSaksnummerNårSaksnummerFraSøknadFinnesIVL() {
        VurderFagsystem fagsystem = byggVurderFagsystemMedAnnenPart(BehandlingTema.FORELDREPENGER_FØDSEL, ANNEN_PART_ID, ÅPEN_SAKSNUMMER_1, BRUKER_AKTØR_ID, JOURNALPOST_ID, BARN_TERMINDATO, BARN_FØDSELSDATO);

        when(fagsakRepositoryMock.hentSakGittSaksnummer(ÅPEN_SAKSNUMMER_1, false)).thenReturn(Optional.of(fagsakFødselFP));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        BehandlendeFagsystem result = toVurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValue(ÅPEN_SAKSNUMMER_1);
    }

    @Test
    public void skalReturnereManuellBehandlingNårSaksnummrFraSøknadIkkeFinnesIVLOgAnnenPartIkkeHarSakForSammeBarnIVL() {
        VurderFagsystem fagsystem = byggVurderFagsystemMedAnnenPart(BehandlingTema.FORELDREPENGER_FØDSEL, ANNEN_PART_ID, ÅPEN_SAKSNUMMER_1, BRUKER_AKTØR_ID, JOURNALPOST_ID, BARN_TERMINDATO, BARN_FØDSELSDATO);

        when(fagsakRepositoryMock.hentSakGittSaksnummer(ÅPEN_SAKSNUMMER_1)).thenReturn(Optional.empty());
        when(fagsakRepositoryMock.hentForBruker(ANNEN_PART_ID)).thenReturn(Collections.singletonList(fagsakAnnenPartFP));

        Optional<Behandling> behandling = Optional.of(byggBehandlingFødsel(fagsakFødselFP));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(fagsakFødselFP.getId())).thenReturn(behandling);

        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(10), BARN_FØDSELSDATO.minusDays(10));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        BehandlendeFagsystem result = toVurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(result.getSaksnummer()).isEmpty();
    }

    @Test
    public void skalReturnereVLNårSøknadIkkeHarSaksnummmerOgAnnenPartHarSakForSammeBarnIVL() {
        VurderFagsystem fagsystem = byggVurderFagsystemMedAnnenPart(BehandlingTema.FORELDREPENGER_FØDSEL, ANNEN_PART_ID, null, BRUKER_AKTØR_ID, JOURNALPOST_ID, BARN_TERMINDATO, BARN_FØDSELSDATO);

        when(fagsakRepositoryMock.hentSakGittSaksnummer(ÅPEN_SAKSNUMMER_1)).thenReturn(Optional.empty());
        when(fagsakRepositoryMock.hentForBruker(ANNEN_PART_ID)).thenReturn(Collections.singletonList(fagsakAnnenPartFP));

        long annenPartSakId = 222L;
        fagsakAnnenPartFP.setId(annenPartSakId);
        Optional<Behandling> behandling = Optional.of(byggBehandlingFødsel(fagsakAnnenPartFP));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(annenPartSakId)).thenReturn(behandling);
        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO, BARN_FØDSELSDATO);
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        BehandlendeFagsystem result = toVurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isEmpty();
    }

    @Test
    public void skalReturnereInfotrygdNårSøknadIkkeHarSaksnummerOgAnnenPartIkkeHarSakForSammeBarnIVL() {
        VurderFagsystem fagsystem = byggVurderFagsystemMedAnnenPart(BehandlingTema.FORELDREPENGER_FØDSEL, ANNEN_PART_ID, null, BRUKER_AKTØR_ID, JOURNALPOST_ID, BARN_TERMINDATO, BARN_FØDSELSDATO);

        when(fagsakRepositoryMock.hentSakGittSaksnummer(ÅPEN_SAKSNUMMER_1)).thenReturn(Optional.empty());
        when(fagsakRepositoryMock.hentForBruker(ANNEN_PART_ID)).thenReturn(Collections.singletonList(fagsakAnnenPartFP));

        long annenPartSakId = 222L;
        fagsakAnnenPartFP.setId(annenPartSakId);
        Optional<Behandling> behandling = Optional.of(byggBehandlingFødsel(fagsakAnnenPartFP));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(annenPartSakId)).thenReturn(behandling);
        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusMonths(10), BARN_FØDSELSDATO.minusMonths(10));
        when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        BehandlendeFagsystem result = toVurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VURDER_INFOTRYGD);
        assertThat(result.getSaksnummer()).isEmpty();
    }
}
