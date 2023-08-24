package no.nav.foreldrepenger.mottak.vurderfagsystem.fp;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.*;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VurderFagsystemTjenesteImplTest {

    private VurderFagsystemFellesTjeneste vurderFagsystemTjeneste;
    @Mock
    private BehandlingRepository behandlingRepositoryMock;
    @Mock
    private FamilieHendelseRepository grunnlagRepository;

    @Mock
    private FagsakRepository fagsakRepositoryMock;

    private Fagsak fagsakFødselFP = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker(), null, ÅPEN_SAKSNUMMER_2);
    private Fagsak fagsakAnnenPartFP = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
    private Fagsak fpFagsakUdefinert = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker());

    private Fagsak fagsakSpyFP = spy(Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker()));

    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjenesteMock;
    @Mock
    private BehandlingRepositoryProvider repositoryProvider;
    private VurderFagsystemFellesUtils fellesUtils;

    private FagsakTjeneste fagsakTjeneste;
    private VurderFagsystemTjeneste tjenesteFP;

    @BeforeEach
    public void setUp() {
        lenient().when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        lenient().when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(grunnlagRepository);
        lenient().when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepositoryMock);
        var familieTjeneste = new FamilieHendelseTjeneste(null, grunnlagRepository);
        fellesUtils = new VurderFagsystemFellesUtils(repositoryProvider, familieTjeneste, mottatteDokumentTjenesteMock, null, null);
        fagsakTjeneste = new FagsakTjeneste(repositoryProvider.getFagsakRepository(), repositoryProvider.getSøknadRepository(), null);
        tjenesteFP = new VurderFagsystemTjenesteImpl(fellesUtils);
    }

    @Test
    void nesteStegSkalVæreVLHvisSakErFlaggetSkalBehandlesAvInfotrygd() {
        var vfData = byggVurderFagsystem(BehandlingTema.FORELDREPENGER_FØDSEL, false);
        vfData.setDokumentTypeId(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL);

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker());
        fagsak.setStengt(true);

        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));
        lenient().when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.emptyList());

        var result = toVurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);

        vfData.setStrukturertSøknad(true);
        result = toVurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void nesteStegSkalVæreManuellHvisEndringPåSakFlaggetSkalBehandlesAvInfotrygd() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker(), RelasjonsRolleType.MORA, ÅPEN_SAKSNUMMER_1);
        fagsak.setStengt(true);
        var vfData = byggVurderFagsystem(BehandlingTema.FORELDREPENGER_FØDSEL, true);
        vfData.setDokumentTypeId(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD);
        vfData.setSaksnummer(fagsak.getSaksnummer());

        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));
        when(fagsakRepositoryMock.hentSakGittSaksnummer(any(), anyBoolean())).thenReturn(Optional.of(fagsak));
        lenient().when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.emptyList());

        var result = toVurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    void nesteStegSkalVæreVLHvisEndringMedSaksnummer() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker(), RelasjonsRolleType.MORA, ÅPEN_SAKSNUMMER_1);
        var vfData = byggVurderFagsystem(BehandlingTema.FORELDREPENGER_FØDSEL, true);
        vfData.setDokumentTypeId(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD);
        vfData.setSaksnummer(fagsak.getSaksnummer());

        when(fagsakRepositoryMock.hentSakGittSaksnummer(any(), anyBoolean())).thenReturn(Optional.of(fagsak));
        lenient().when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(List.of(fagsak));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        var result = toVurderFagsystem(vfData);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(ÅPEN_SAKSNUMMER_1));
    }

    private BehandlendeFagsystem toVurderFagsystem(VurderFagsystem vfData) {
        return vurderFagsystemTjeneste.vurderFagsystem(vfData);

    }

    @Test
    void nesteStegSkalVæreVLHvisIngenSakPasserMenDetFinnesEnAvsluttetSak() {
        var terminDatdato = LocalDate.of(2017, 7, 1);

        var vfData = byggVurderFagsystemMedTermin(terminDatdato.plusYears(1), BehandlingTema.FORELDREPENGER_FØDSEL, true);

        List<Fagsak> saksliste = new ArrayList<>();
        var fagsak = buildFagsak(AVSLT_NY_FAGSAK_ID_1, true, FagsakYtelseType.FORELDREPENGER);
        var behandling = byggBehandlingFødsel(fagsak);
        behandling.avsluttBehandling();
        var behandlingOpt = Optional.of(behandling);
        saksliste.add(fagsak);
        lenient().when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var grunnlag = byggFødselGrunnlag(terminDatdato, null);
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandlingOpt);
        lenient().when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(Collections.emptyList());
        lenient().when(grunnlagRepository.hentAggregat(behandling.getId())).thenReturn(grunnlag);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandlingOpt.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        var result = toVurderFagsystem(vfData);
        assertThat(result.getSaksnummer()).isEmpty();
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void skalReturnereTrueNårFagÅrsakTypeErUdefinertOgBehandlingTemaErForeldrePenger() {
        var behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));

        lenient().when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        var grunnlag = byggFødselGrunnlag(null, null);
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));

        assertThat(FagsakYtelseType.FORELDREPENGER.equals(fpFagsakUdefinert.getYtelseType()) && fpFagsakUdefinert.erÅpen()).isTrue();
    }

    @Test
    void skalReturnereManuellBehandlingNårFlereÅpneSakerFinnesPåBruker() {
        var fagsystem = byggVurderFagsystem(BehandlingTema.FORELDREPENGER, true);
        when(fagsakRepositoryMock.hentJournalpost(any())).thenReturn(Optional.empty());

        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(buildFagsakMedUdefinertRelasjon(ÅPEN_FAGSAK_ID_1, false));
        saksliste.add(buildFagsakMedUdefinertRelasjon(ÅPEN_FAGSAK_ID_2, false));

        lenient().when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(ÅPEN_FAGSAK_ID_1))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_1), 10));

        lenient().when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(ÅPEN_FAGSAK_ID_2))
                .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_2), 12));
        lenient().when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        var behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        when(behandlingRepositoryMock.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(List.of(behandling.get()));
        var grunnlag = byggFødselGrunnlag(null, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        var result = toVurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    void skalReturnereVedtaksløsningMedSaksnummerNårEnSakFinnesMedStatusOpprett() {
        var terminDatdato = LocalDate.of(2019, 1, 1);
        var fagsystem = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.FORELDREPENGER_FØDSEL, true);
        when(fagsakRepositoryMock.hentJournalpost(any())).thenReturn(Optional.empty());

        lenient().when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.singletonList(buildFagsakMedUdefinertRelasjon(123L, false)));

        var behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        var grunnlag = byggFødselGrunnlag(terminDatdato, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        var result = toVurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isNotEmpty();
    }

    @Test
    void skalReturnereVedtaksløsningMedSaksnummerNårEnSakFinnesMedStatusLøpende() {
        var terminDatdato = LocalDate.of(2019, 1, 1);
        var fagsystem = byggVurderFagsystemMedTermin(terminDatdato, BehandlingTema.FORELDREPENGER_FØDSEL, true);
        when(fagsakRepositoryMock.hentJournalpost(any())).thenReturn(Optional.empty());

        lenient().when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.singletonList(buildFagsakMedUdefinertRelasjon(123L, false)));
        when(fagsakSpyFP.getStatus()).thenReturn(FagsakStatus.LØPENDE);

        var behandling = Optional.of(byggBehandlingUdefinert(fpFagsakUdefinert));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        var grunnlag = byggFødselGrunnlag(terminDatdato, null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())).thenReturn(Optional.of(grunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        var result = toVurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isNotEmpty();
    }

    @Test
    void skalReturnereVLMedSaksnummerNårSaksnummerFraSøknadFinnesIVL() {
        var fagsystem = byggVurderFagsystemMedAnnenPart(BehandlingTema.FORELDREPENGER_FØDSEL, ANNEN_PART_ID, ÅPEN_SAKSNUMMER_1,
                BRUKER_AKTØR_ID, JOURNALPOST_ID, BARN_TERMINDATO, BARN_FØDSELSDATO);

        when(fagsakRepositoryMock.hentSakGittSaksnummer(ÅPEN_SAKSNUMMER_1, false)).thenReturn(Optional.of(fagsakFødselFP));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        var result = toVurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValue(ÅPEN_SAKSNUMMER_1);
    }

    @Test
    void skalReturnereManuellBehandlingNårSaksnummrFraSøknadIkkeFinnesIVLOgAnnenPartIkkeHarSakForSammeBarnIVL() {
        var fagsystem = byggVurderFagsystemMedAnnenPart(BehandlingTema.FORELDREPENGER_FØDSEL, ANNEN_PART_ID, ÅPEN_SAKSNUMMER_1,
                BRUKER_AKTØR_ID, JOURNALPOST_ID, BARN_TERMINDATO, BARN_FØDSELSDATO);

        lenient().when(fagsakRepositoryMock.hentSakGittSaksnummer(ÅPEN_SAKSNUMMER_1)).thenReturn(Optional.empty());
        lenient().when(fagsakRepositoryMock.hentForBruker(ANNEN_PART_ID)).thenReturn(Collections.singletonList(fagsakAnnenPartFP));

        var behandling = Optional.of(byggBehandlingFødsel(fagsakFødselFP));
        lenient().when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(fagsakFødselFP.getId())).thenReturn(behandling);

        var familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusDays(10), BARN_FØDSELSDATO.minusDays(10));
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        var result = toVurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        assertThat(result.getSaksnummer()).isEmpty();
    }

    @Test
    void skalReturnereVLNårSøknadIkkeHarSaksnummmerOgAnnenPartHarSakForSammeBarnIVL() {
        var fagsystem = byggVurderFagsystemMedAnnenPart(BehandlingTema.FORELDREPENGER_FØDSEL, ANNEN_PART_ID, null, BRUKER_AKTØR_ID,
                JOURNALPOST_ID, BARN_TERMINDATO, BARN_FØDSELSDATO);

        lenient().when(fagsakRepositoryMock.hentSakGittSaksnummer(ÅPEN_SAKSNUMMER_1)).thenReturn(Optional.empty());
        lenient().when(fagsakRepositoryMock.hentForBruker(ANNEN_PART_ID)).thenReturn(Collections.singletonList(fagsakAnnenPartFP));

        var annenPartSakId = 222L;
        fagsakAnnenPartFP.setId(annenPartSakId);
        var behandling = Optional.of(byggBehandlingFødsel(fagsakAnnenPartFP));
        lenient().when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(annenPartSakId)).thenReturn(behandling);
        var familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO, BARN_FØDSELSDATO);
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        var result = toVurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isEmpty();
    }

    @Test
    void skalReturnereVLNårSøknadIkkeHarSaksnummerOgAnnenPartIkkeHarSakForSammeBarnIVL() {
        var fagsystem = byggVurderFagsystemMedAnnenPart(BehandlingTema.FORELDREPENGER_FØDSEL, ANNEN_PART_ID, null, BRUKER_AKTØR_ID,
                JOURNALPOST_ID, BARN_TERMINDATO, BARN_FØDSELSDATO);

        lenient().when(fagsakRepositoryMock.hentSakGittSaksnummer(ÅPEN_SAKSNUMMER_1)).thenReturn(Optional.empty());
        lenient().when(fagsakRepositoryMock.hentForBruker(ANNEN_PART_ID)).thenReturn(Collections.singletonList(fagsakAnnenPartFP));

        var annenPartSakId = 222L;
        fagsakAnnenPartFP.setId(annenPartSakId);
        var behandling = Optional.of(byggBehandlingFødsel(fagsakAnnenPartFP));
        lenient().when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(annenPartSakId)).thenReturn(behandling);
        var familieHendelseGrunnlag = byggFødselGrunnlag(BARN_TERMINDATO.minusMonths(10),
                BARN_FØDSELSDATO.minusMonths(10));
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlag));
        vurderFagsystemTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(tjenesteFP));

        var result = toVurderFagsystem(fagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isEmpty();
    }
}
