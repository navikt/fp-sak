package no.nav.foreldrepenger.mottak.vurderfagsystem.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil;
import no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(MockitoExtension.class)
class VurderFagsystemTjenesteImplTest {

    public static final long FAGSAK_EN_ID = 111L;
    public static final long FAGSAK_TO_ID = 222L;
    private static final AktørId BRUKER_ID = AktørId.dummy();

    private VurderFagsystemFellesTjeneste tjeneste;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private FamilieHendelseRepository grunnlagRepository;
    @Mock
    private SvangerskapspengerRepository svangerskapspengerRepository;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;

    @BeforeEach
    void setUp() {
        var repositoryProvider = mock(BehandlingRepositoryProvider.class);
        lenient().when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(grunnlagRepository);
        lenient().when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepository);
        lenient().when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        var fagsakTjeneste = new FagsakTjeneste(repositoryProvider.getFagsakRepository(), repositoryProvider.getSøknadRepository());
        var familieTjeneste = new FamilieHendelseTjeneste(null, grunnlagRepository);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        var fellesUtils = new VurderFagsystemFellesUtils(repositoryProvider, familieTjeneste, mottatteDokumentTjeneste, null, null,
            fagsakRelasjonTjeneste);
        var svpTjeneste = new VurderFagsystemTjenesteImpl(fellesUtils, behandlingRepository, svangerskapspengerRepository);
        tjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(svpTjeneste));
    }

    @Test
    void skalReturnereSjekkInfotrygdHvisBrukerIkkeHarSakIVL() {
        var vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.INNTEKTSMELDING);
        vurderFagsystem.setÅrsakInnsendingInntektsmelding("");
        vurderFagsystem.setVirksomhetsnummer(VurderFagsystemTestUtils.VIRKSOMHETSNUMMER);

        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Collections.emptyList());

        var result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void skalReturnereManuellBehandlingdHvisBrukerHarMerEnnEnSakIVLMedåpenFørstegangsbehandling() {
        var vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.INNTEKTSMELDING);
        vurderFagsystem.setÅrsakInnsendingInntektsmelding("");
        vurderFagsystem.setVirksomhetsnummer(VurderFagsystemTestUtils.VIRKSOMHETSNUMMER);

        var fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        var fagsak2 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_TO_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(List.of(fagsak1, fagsak2));

        var behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        var behandling2 = Behandling.forFørstegangssøknad(fagsak2).build();
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.singletonList(behandling1));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_TO_ID)).thenReturn(Collections.singletonList(behandling2));

        lenient().when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(eq(FAGSAK_EN_ID))).thenReturn(Optional.of(behandling1));
        lenient().when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(eq(FAGSAK_TO_ID))).thenReturn(Optional.of(behandling2));

        var familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(2), null);
        var familieHendelse2 = BehandlingslagerTestUtil.byggFødselGrunnlag(null, LocalDate.now().minusYears(1));

        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling2.getId())).thenReturn(Optional.of(familieHendelse2));

        var result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    void skalReturnereManuellBehandlingHvisBrukerHarAvsluttetSakIVLMedFamiliehendelseIPerioden() {
        var vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setStrukturertSøknad(true);

        var fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, true, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(List.of(fagsak1));

        var behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        lenient().when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));

        var familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(2), null);
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));

        var result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    void skalReturnereVLMedSakHvisBrukerHarÅpenSakIVLMedMatchendeFH() {
        var vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setBarnTermindato(LocalDate.now().plusMonths(2));

        vurderFagsystem.setStrukturertSøknad(true);

        var fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(List.of(fagsak1));

        var behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));

        var familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(2), null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));

        var result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer().filter(fagsak1.getSaksnummer()::equals)).isPresent();
    }

    @Test
    void skalReturnereManuellMedSakHvisBrukerHarAvsluttetSakIVLMedMatchendeFH() {
        var vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setBarnTermindato(LocalDate.now().plusMonths(2));

        vurderFagsystem.setStrukturertSøknad(true);

        var fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, true, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(List.of(fagsak1));

        var behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));

        var familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(2), null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));

        var result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    void skalReturnereVLHvisBrukerHarÅpenSakIVLMedIkkeMatchendeFH() {
        var vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setBarnTermindato(LocalDate.now().plusMonths(6));

        vurderFagsystem.setStrukturertSøknad(true);

        var fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(List.of(fagsak1));

        var behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));

        var familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().minusMonths(6), null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));

        var result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isEmpty();
    }

    @Test
    void skalReturnereVedtaksløsningenForInntektsmeldingHvisBrukerHarSakIVLMedFamiliehendelseUtenforPerioden() {
        var vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.INNTEKTSMELDING);
        vurderFagsystem.setÅrsakInnsendingInntektsmelding("");
        vurderFagsystem.setVirksomhetsnummer(VurderFagsystemTestUtils.VIRKSOMHETSNUMMER);

        var fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        var fagsak2 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_TO_ID, true, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(List.of(fagsak1, fagsak2));
        var behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        var behandling2 = Behandling.forFørstegangssøknad(fagsak2).build();
        behandling1.avsluttBehandling();
        behandling2.avsluttBehandling();
        lenient().when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.singletonList(behandling1));
        lenient().when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_TO_ID)).thenReturn(Collections.singletonList(behandling2));

        var familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().minusMonths(8), null);
        var familieHendelse2 = BehandlingslagerTestUtil.byggFødselGrunnlag(null, LocalDate.now().minusYears(1));
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling2.getId())).thenReturn(Optional.of(familieHendelse2));

        var result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void skalReturnereVLMedSaksnummerHvisBrukerHarNøyaktigEnSakMedÅpenFørstegangsbehandlingSomVenterPåSøknad() {
        var vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setStrukturertSøknad(true);

        var fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Collections.singletonList(fagsak1));

        var behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.singletonList(behandling1));
        var familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(3), null);
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));
        lenient().when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));

        var svpGrunnlagEntitet = new SvpGrunnlagEntitet.Builder().medBehandlingId(1L).build();

        lenient().when(svangerskapspengerRepository.hentGrunnlag(behandling1.getId())).thenReturn(Optional.of(svpGrunnlagEntitet));

        var result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    void skalReturnereManuellVurderingHvisBrukerHarNøyaktigEnSakMedÅpenFørstegangsbehandlingSomAlleredeHarSøknadOgDetKommerEnNySøknad() {
        var vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setStrukturertSøknad(true);

        var fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Collections.singletonList(fagsak1));

        var behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.singletonList(behandling1));
        var familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(3), null);
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));
        lenient().when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));
        lenient().when(svangerskapspengerRepository.hentGrunnlag(behandling1.getId())).thenReturn(Optional.empty());

        var result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).contains(fagsak1.getSaksnummer());
    }

    @Test
    void skalReturnereVLForVedleggNårÅpenBehandling() {
        var vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vurderFagsystem.setStrukturertSøknad(true);

        var fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Collections.singletonList(fagsak1));

        var behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.singletonList(behandling1));
        var familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(3), null);
        lenient().when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));
        lenient().when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));
        lenient().when(svangerskapspengerRepository.hentGrunnlag(behandling1.getId())).thenReturn(Optional.empty());

        var result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).contains(fagsak1.getSaksnummer());
    }

    @Test
    void skalReturnereManuellVurderingForVedleggNårHenlagt() {
        var vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vurderFagsystem.setStrukturertSøknad(false);

        var fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, true, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Collections.singletonList(fagsak1));

        var behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        lenient().when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.emptyList());
        lenient().when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));
        lenient().when(svangerskapspengerRepository.hentGrunnlag(behandling1.getId())).thenReturn(Optional.empty());
        lenient().when(mottatteDokumentTjeneste.erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(any())).thenReturn(false);
        lenient().when(mottatteDokumentTjeneste.harFristForInnsendingAvDokGåttUt(any())).thenReturn(true);

        var result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.behandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    private VurderFagsystem lagVurderfagsystem() {
        var vurderFagsystem = new VurderFagsystem();
        vurderFagsystem.setBehandlingTema(BehandlingTema.SVANGERSKAPSPENGER);
        vurderFagsystem.setAktørId(BRUKER_ID);
        vurderFagsystem.setForsendelseMottattTidspunkt(LocalDateTime.now().minusDays(2));
        return vurderFagsystem;
    }

}
