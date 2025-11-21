package no.nav.foreldrepenger.kompletthet.impl.fp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerImpl;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerSøknadTjeneste;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetssjekkerTestUtil;
import no.nav.foreldrepenger.kompletthet.impl.ManglendeInntektsmeldingTjeneste;
import no.nav.foreldrepenger.kompletthet.impl.ManglendeVedleggTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
class KompletthetsjekkerImplTest extends EntityManagerAwareTest {

    private static final LocalDate STARTDATO_PERMISJON = LocalDate.now().plusWeeks(1);
    private static final String KODE_INNLEGGELSE = "I000037";

    private BehandlingRepositoryProvider repositoryProvider;
    private SøknadRepository søknadRepository;

    private KompletthetssjekkerTestUtil testUtil;

    @Mock
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private DokumentBestillerTjeneste dokumentBestillerTjenesteMock;
    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock;
    @Mock
    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    private Kompletthetsjekker kompletthetsjekker;
    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(STARTDATO_PERMISJON)
            .build();
    HashMap<Arbeidsgiver, Set<InternArbeidsforholdRef>> manglendeInntektsmeldinger;

    @BeforeEach
    public void before() {
        manglendeInntektsmeldinger = new HashMap<>();
        manglendeInntektsmeldinger.put(Arbeidsgiver.virksomhet(KUNSTIG_ORG), new HashSet<>());

        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.anyLong())).thenReturn(skjæringstidspunkt);
        lenient().when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerForKompletthet(any(), any())).thenReturn(new HashMap<>());

        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        søknadRepository = repositoryProvider.getSøknadRepository();

        var manglendeVedleggTjeneste = new ManglendeVedleggTjeneste(repositoryProvider, dokumentArkivTjeneste);
        var kompletthetsjekkerSøknad = new KompletthetsjekkerSøknadTjeneste(repositoryProvider, manglendeVedleggTjeneste);
        var manglendeInntektsmeldingTjeneste = new ManglendeInntektsmeldingTjeneste(
            repositoryProvider,
            dokumentBehandlingTjenesteMock,
            inntektsmeldingArkivTjeneste,
            inntektsmeldingTjeneste
        );
        kompletthetsjekker = new KompletthetsjekkerImpl(repositoryProvider.getBehandlingRepository(), kompletthetsjekkerSøknad, manglendeInntektsmeldingTjeneste);
        testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);
    }

    @Test
    void skal_finne_at_kompletthet_er_oppfylt() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        // Act
        var kompletthetResultat = kompletthetsjekker.vurderForsendelseKomplett(lagRef(behandling), lagStp());

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
        assertThat(kompletthetResultat.ventefrist()).isNull();
    }

    @Test
    void skal_finne_at_kompletthet_ikke_er_oppfylt_når_inntektsmelding_mangler() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmelding();
        testUtil.byggOgLagreSøknadMedNyOppgittFordeling(behandling, false);
        behandling.setStatus(BehandlingStatus.UTREDES);

        // Act
        var kompletthetResultat = kompletthetsjekker.vurderForsendelseKomplett(lagRef(behandling), lagStp());

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.ventefrist().toLocalDate()).isEqualTo(
                søknadRepository.hentSøknad(behandling.getId()).getMottattDato().plusDays(10));
    }

    @Test
    void kompletthet_skal_være_oppfylt_når_det_ikke_mangler_noen_inntektsmelding() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmeldingKompletthet(Collections.emptyMap());
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(2),
                STARTDATO_PERMISJON);
        lenient().when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(
                List.of(InntektsmeldingBuilder.builder().build()));

        // Act
        var kompletthetResultat = kompletthetsjekker.vurderEtterlysningInntektsmelding(
                lagRef(behandling), lagStp(STARTDATO_PERMISJON));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
        assertThat(kompletthetResultat.ventefrist()).isNull();
    }

    @Test
    void skal_justere_frist_hvis_allerede_motttatt_inntektsmelding() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmeldingKompletthet(manglendeInntektsmeldinger);
        var stp = LocalDate.now();
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, stp.minusWeeks(3), stp);
        lenient().when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(
                List.of(InntektsmeldingBuilder.builder().medInnsendingstidspunkt(LocalDateTime.now().minusDays(10)).build()));

        // Act
        var kompletthetResultat = kompletthetsjekker.vurderEtterlysningInntektsmelding(lagRef(behandling) , lagStp(stp));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.ventefrist().toLocalDate()).isEqualTo(stp.plusWeeks(2));
    }

    @Test
    void skal_justere_ventefrist_ved_endret_skjæringstidspunkt() {
        // Arrange
        var stp = LocalDate.now().plusDays(2).plusWeeks(3);
        var søknadsdato = LocalDate.now().minusDays(10);
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmeldingKompletthet(manglendeInntektsmeldinger);
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, søknadsdato, stp);
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(Collections.emptyList());

        // Act
        var kompletthetResultat = kompletthetsjekker.vurderEtterlysningInntektsmelding(
                lagRef(behandling), lagStp(stp));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.ventefrist().toLocalDate()).isEqualTo(stp.plusWeeks(2));

        // Act 2
        stp = LocalDate.now().plusWeeks(2);
        var kompletthetResultat2 = kompletthetsjekker.vurderEtterlysningInntektsmelding(
                lagRef(behandling), lagStp(stp));

        // Assert
        assertThat(kompletthetResultat2.erOppfylt()).isFalse();
        assertThat(kompletthetResultat2.ventefrist().toLocalDate()).isEqualTo(stp.plusWeeks(2));
    }

    @Test
    void kompletthet_ikke_oppflyt_med_frist_utledet_fra_mottattdato() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmeldingKompletthet(manglendeInntektsmeldinger);
        var søknadsdato = LocalDate.now().minusDays(10);
        var stp = STARTDATO_PERMISJON;
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, søknadsdato, STARTDATO_PERMISJON);
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(Collections.emptyList());

        // Act
        var kompletthetResultat = kompletthetsjekker.vurderEtterlysningInntektsmelding(lagRef(behandling), lagStp(stp));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.ventefrist().toLocalDate()).isEqualTo(stp.plusWeeks(2));
    }

    @Test
    void skal_føre_til_oppflyt_kompletthet_når_inntektsmelding_mangler_for_privat_arbeidsgivere() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        HashMap<Arbeidsgiver, Set<InternArbeidsforholdRef>>  manglendeImPrivatArbGiver = new HashMap<>();
        manglendeImPrivatArbGiver.put(Arbeidsgiver.fra(AktørId.dummy()), new HashSet<>());
        mockManglendeInntektsmeldingKompletthet(manglendeImPrivatArbGiver);
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(1),
            STARTDATO_PERMISJON);

        // Act
        var kompletthetResultat = kompletthetsjekker.vurderEtterlysningInntektsmelding(
            lagRef(behandling), lagStp(STARTDATO_PERMISJON));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
        assertThat(kompletthetResultat.ventefrist()).isNull();
        verify(dokumentBestillerTjenesteMock, times(0)).bestillDokument(any(DokumentBestilling.class));
    }

    @Test
    void skal_finne_at_kompletthet_ikke_er_oppfylt_når_vedlegg_til_søknad_mangler() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        opprettSøknadMedPåkrevdVedlegg(behandling);
        behandling.setStatus(BehandlingStatus.UTREDES);
        // Act
        var kompletthetResultat = kompletthetsjekker.vurderForsendelseKomplett(lagRef(behandling), lagStp());

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.ventefrist().toLocalDate()).isEqualTo(
                søknadRepository.hentSøknad(behandling.getId()).getMottattDato().plusWeeks(1));
    }

    @Test
    void skal_finne_at_kompletthet_er_oppfylt_når_vedlegg_til_søknad_finnes_i_joark() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        opprettSøknadMedPåkrevdVedlegg(behandling);

        // Act
        var kompletthetResultat = kompletthetsjekker.vurderForsendelseKomplett(lagRef(behandling), lagStp());

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
        assertThat(kompletthetResultat.ventefrist()).isNull();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    private Skjæringstidspunkt lagStp() {
        return Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
    }

    private Skjæringstidspunkt lagStp(LocalDate stpDate) {
        return Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stpDate).build();
    }

    @Test
    void skal_returnere_hvilke_vedlegg_som_mangler() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        opprettSøknadMedPåkrevdVedlegg(behandling);

        // Act
        var manglendeVedlegg = kompletthetsjekker.utledAlleManglendeVedleggForForsendelse(
                lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).hasSize(1);
        var koder = manglendeVedlegg.stream()
                .map(ManglendeVedlegg::dokumentType)
                .toList();
        assertThat(koder).containsExactlyInAnyOrder(DokumentTypeId.DOK_INNLEGGELSE);
    }

    private void opprettSøknadMedPåkrevdVedlegg(Behandling behandling) {
        testUtil.byggOgLagreSøknadMedNyOppgittFordeling(behandling, false);
        var søknad = new SøknadEntitet.Builder(søknadRepository.hentSøknad(behandling.getId()),
                false).leggTilVedlegg(
                        new SøknadVedleggEntitet.Builder().medSkjemanummer(KODE_INNLEGGELSE)
                                .medErPåkrevdISøknadsdialog(true)
                                .build())
                        .build();
        søknadRepository.lagreOgFlush(behandling, søknad);
    }

    private void mockManglendeInntektsmelding() {
        var manglendeInntektsmeldingerSet = new HashMap<Arbeidsgiver, Set<InternArbeidsforholdRef>>();
        manglendeInntektsmeldingerSet.put(Arbeidsgiver.virksomhet("1"), new HashSet<>());
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerForKompletthet(any(), any())).thenReturn(
                manglendeInntektsmeldingerSet);
    }

    private void mockManglendeInntektsmeldingKompletthet(Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> manglendeIM) {
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerForKompletthet(any(), any())).thenReturn(
            manglendeIM);
    }
}
