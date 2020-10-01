package no.nav.foreldrepenger.mottak.vurderfagsystem.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil;
import no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class VurderFagsystemTjenesteImplTest {

    public static final long FAGSAK_EN_ID = 111L;
    public static final long FAGSAK_TO_ID = 222L;
    private static AktørId BRUKER_ID = AktørId.dummy();

    private VurderFagsystemFellesTjeneste tjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository grunnlagRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;

    @Before
    public void setUp() throws Exception {
        fagsakRepository = mock(FagsakRepository.class);
        behandlingRepository = mock(BehandlingRepository.class);
        grunnlagRepository = mock(FamilieHendelseRepository.class);
        svangerskapspengerRepository = mock(SvangerskapspengerRepository.class);
        mottatteDokumentTjeneste = mock(MottatteDokumentTjeneste.class);

        BehandlingRepositoryProvider repositoryProvider = mock(BehandlingRepositoryProvider.class);
        when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(grunnlagRepository);
        when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepository);
        when(repositoryProvider.getSvangerskapspengerRepository()).thenReturn(svangerskapspengerRepository);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        var fagsakTjeneste = new FagsakTjeneste(repositoryProvider.getFagsakRepository(), repositoryProvider.getSøknadRepository(), null);
        var fellesUtils = new VurderFagsystemFellesUtils(repositoryProvider, mottatteDokumentTjeneste, null, null);
        var svpTjeneste = new VurderFagsystemTjenesteImpl(fellesUtils, repositoryProvider);
        tjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(svpTjeneste));
    }

    @Test
    public void skalReturnereSjekkInfotrygdHvisBrukerIkkeHarSakIVL() {
        VurderFagsystem vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.INNTEKTSMELDING);
        vurderFagsystem.setÅrsakInnsendingInntektsmelding("");
        vurderFagsystem.setVirksomhetsnummer(VurderFagsystemTestUtils.VIRKSOMHETSNUMMER);

        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Collections.emptyList());

        BehandlendeFagsystem result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    public void skalReturnereManuellBehandlingdHvisBrukerHarMerEnnEnSakIVLMedåpenFørstegangsbehandling() {
        VurderFagsystem vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.INNTEKTSMELDING);
        vurderFagsystem.setÅrsakInnsendingInntektsmelding("");
        vurderFagsystem.setVirksomhetsnummer(VurderFagsystemTestUtils.VIRKSOMHETSNUMMER);

        Fagsak fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        Fagsak fagsak2 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_TO_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Arrays.asList(fagsak1, fagsak2));

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        Behandling behandling2 = Behandling.forFørstegangssøknad(fagsak2).build();
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.singletonList(behandling1));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_TO_ID)).thenReturn(Collections.singletonList(behandling2));

        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(eq(FAGSAK_EN_ID))).thenReturn(Optional.of(behandling1));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(eq(FAGSAK_TO_ID))).thenReturn(Optional.of(behandling2));

        FamilieHendelseGrunnlagEntitet familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(2), null);
        FamilieHendelseGrunnlagEntitet familieHendelse2 = BehandlingslagerTestUtil.byggFødselGrunnlag(null, LocalDate.now().minusYears(1));

        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling2.getId())).thenReturn(Optional.of(familieHendelse2));

        BehandlendeFagsystem result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    public void skalReturnereManuellBehandlingHvisBrukerHarAvsluttetSakIVLMedFamiliehendelseIPerioden() {
        VurderFagsystem vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setStrukturertSøknad(true);

        Fagsak fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, true, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Arrays.asList(fagsak1));

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));

        FamilieHendelseGrunnlagEntitet familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(2), null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));

        BehandlendeFagsystem result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    public void skalReturnereVLMedSakHvisBrukerHarÅpenSakIVLMedMatchendeFH() {
        VurderFagsystem vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setBarnTermindato(LocalDate.now().plusMonths(2));

        vurderFagsystem.setStrukturertSøknad(true);

        Fagsak fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Arrays.asList(fagsak1));

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));

        FamilieHendelseGrunnlagEntitet familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(2), null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));

        BehandlendeFagsystem result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer().filter(fagsak1.getSaksnummer()::equals)).isPresent();
    }

    @Test
    public void skalReturnereManuellMedSakHvisBrukerHarAvsluttetSakIVLMedMatchendeFH() {
        VurderFagsystem vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setBarnTermindato(LocalDate.now().plusMonths(2));

        vurderFagsystem.setStrukturertSøknad(true);

        Fagsak fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, true, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Arrays.asList(fagsak1));

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));

        FamilieHendelseGrunnlagEntitet familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(2), null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));

        BehandlendeFagsystem result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    public void skalReturnereVLHvisBrukerHarÅpenSakIVLMedIkkeMatchendeFH() {
        VurderFagsystem vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setBarnTermindato(LocalDate.now().plusMonths(6));

        vurderFagsystem.setStrukturertSøknad(true);

        Fagsak fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Arrays.asList(fagsak1));

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));

        FamilieHendelseGrunnlagEntitet familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().minusMonths(6), null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));

        BehandlendeFagsystem result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).isEmpty();
    }

    @Test
    public void skalReturnereVedtaksløsningenForInntektsmeldingHvisBrukerHarSakIVLMedFamiliehendelseUtenforPerioden() {
        VurderFagsystem vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.INNTEKTSMELDING);
        vurderFagsystem.setÅrsakInnsendingInntektsmelding("");
        vurderFagsystem.setVirksomhetsnummer(VurderFagsystemTestUtils.VIRKSOMHETSNUMMER);

        Fagsak fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        Fagsak fagsak2 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_TO_ID, true, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Arrays.asList(fagsak1, fagsak2));
        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        Behandling behandling2 = Behandling.forFørstegangssøknad(fagsak2).build();
        behandling1.avsluttBehandling();
        behandling2.avsluttBehandling();
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.singletonList(behandling1));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_TO_ID)).thenReturn(Collections.singletonList(behandling2));

        FamilieHendelseGrunnlagEntitet familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().minusMonths(8), null);
        FamilieHendelseGrunnlagEntitet familieHendelse2 = BehandlingslagerTestUtil.byggFødselGrunnlag(null, LocalDate.now().minusYears(1));
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling2.getId())).thenReturn(Optional.of(familieHendelse2));

        BehandlendeFagsystem result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    public void skalReturnereVLMedSaksnummerHvisBrukerHarNøyaktigEnSakMedÅpenFørstegangsbehandlingSomVenterPåSøknad() {
        VurderFagsystem vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setStrukturertSøknad(true);

        Fagsak fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Collections.singletonList(fagsak1));

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.singletonList(behandling1));
        FamilieHendelseGrunnlagEntitet familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(3), null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));

        SvpGrunnlagEntitet svpGrunnlagEntitet = new SvpGrunnlagEntitet.Builder().medBehandlingId(1L).build();

        when(svangerskapspengerRepository.hentGrunnlag(behandling1.getId())).thenReturn(Optional.of(svpGrunnlagEntitet));

        BehandlendeFagsystem result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
    }

    @Test
    public void skalReturnereManuellVurderingHvisBrukerHarNøyaktigEnSakMedÅpenFørstegangsbehandlingSomAlleredeHarSøknadOgDetKommerEnNySøknad() {
        VurderFagsystem vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER);
        vurderFagsystem.setStrukturertSøknad(true);

        Fagsak fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Collections.singletonList(fagsak1));

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.singletonList(behandling1));
        FamilieHendelseGrunnlagEntitet familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(3), null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));
        when(svangerskapspengerRepository.hentGrunnlag(behandling1.getId())).thenReturn(Optional.empty());

        BehandlendeFagsystem result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer().get()).isEqualTo(fagsak1.getSaksnummer());
    }

    @Test
    public void skalReturnereVLForVedleggNårÅpenBehandling() {
        VurderFagsystem vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vurderFagsystem.setStrukturertSøknad(true);

        Fagsak fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, false, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Collections.singletonList(fagsak1));

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.singletonList(behandling1));
        FamilieHendelseGrunnlagEntitet familieHendelse1 = BehandlingslagerTestUtil.byggFødselGrunnlag(LocalDate.now().plusMonths(3), null);
        when(grunnlagRepository.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelse1));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));
        when(svangerskapspengerRepository.hentGrunnlag(behandling1.getId())).thenReturn(Optional.empty());

        BehandlendeFagsystem result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer().get()).isEqualTo(fagsak1.getSaksnummer());
    }

    @Test
    public void skalReturnereManuellVurderingForVedleggNårHenlagt() {
        VurderFagsystem vurderFagsystem = lagVurderfagsystem();
        vurderFagsystem.setDokumentTypeId(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        vurderFagsystem.setStrukturertSøknad(false);

        Fagsak fagsak1 = BehandlingslagerTestUtil.buildFagsak(FAGSAK_EN_ID, true, FagsakYtelseType.SVANGERSKAPSPENGER);
        when(fagsakRepository.hentForBruker(BRUKER_ID)).thenReturn(Collections.singletonList(fagsak1));

        Behandling behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_EN_ID)).thenReturn(Collections.emptyList());
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_EN_ID)).thenReturn(Optional.of(behandling1));
        when(svangerskapspengerRepository.hentGrunnlag(behandling1.getId())).thenReturn(Optional.empty());
        when(mottatteDokumentTjeneste.erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(any())).thenReturn(false);
        when(mottatteDokumentTjeneste.harFristForInnsendingAvDokGåttUt(any())).thenReturn(true);

        BehandlendeFagsystem result = tjeneste.vurderFagsystem(vurderFagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    private VurderFagsystem lagVurderfagsystem() {
        VurderFagsystem vurderFagsystem = new VurderFagsystem();
        vurderFagsystem.setBehandlingTema(BehandlingTema.SVANGERSKAPSPENGER);
        vurderFagsystem.setAktørId(BRUKER_ID);
        vurderFagsystem.setForsendelseMottattTidspunkt(LocalDateTime.now().minusDays(2));
        return vurderFagsystem;
    }

}
