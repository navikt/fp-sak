package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerInntektsmelding;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.KompletthetssjekkerTestUtil;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
public class KompletthetsjekkerImplTest extends EntityManagerAwareTest {

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

    private KompletthetsjekkerImpl kompletthetsjekkerImpl;
    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(STARTDATO_PERMISJON)
            .build();

    @BeforeEach
    public void before() {
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.anyLong())).thenReturn(skjæringstidspunkt);
        lenient().when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(any(), anyBoolean())).thenReturn(
                new HashMap<>());
        lenient().when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(any(), anyBoolean())).thenReturn(
                new HashMap<>());

        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        KompletthetssjekkerSøknadImpl kompletthetssjekkerSøknadImpl = new KompletthetssjekkerSøknadFørstegangsbehandlingImpl(
                dokumentArkivTjeneste, repositoryProvider, Period.parse("P4W"));
        KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding = new KompletthetssjekkerInntektsmelding(
                inntektsmeldingArkivTjeneste);
        KompletthetsjekkerFelles kompletthetsjekkerFelles = new KompletthetsjekkerFelles(repositoryProvider,
                dokumentBestillerTjenesteMock, dokumentBehandlingTjenesteMock, kompletthetssjekkerInntektsmelding, inntektsmeldingTjeneste);
        søknadRepository = repositoryProvider.getSøknadRepository();
        kompletthetsjekkerImpl = new KompletthetsjekkerImpl(kompletthetssjekkerSøknadImpl,
                kompletthetssjekkerInntektsmelding, kompletthetsjekkerFelles);
        testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);
    }

    @Test
    public void skal_finne_at_kompletthet_er_oppfylt() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        // Act
        KompletthetResultat kompletthetResultat = kompletthetsjekkerImpl.vurderForsendelseKomplett(lagRef(behandling));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
        assertThat(kompletthetResultat.getVentefrist()).isNull();
    }

    @Test
    public void skal_finne_at_kompletthet_ikke_er_oppfylt_når_inntektsmelding_mangler() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmelding();
        testUtil.byggOgLagreSøknadMedNyOppgittFordeling(behandling, false);
        behandling.setStatus(BehandlingStatus.UTREDES);

        // Act
        KompletthetResultat kompletthetResultat = kompletthetsjekkerImpl.vurderForsendelseKomplett(lagRef(behandling));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.getVentefrist().toLocalDate()).isEqualTo(
                søknadRepository.hentSøknad(behandling.getId()).getMottattDato().plusWeeks(1));
    }

    @Test
    public void skal_ikke_sende_brev_når_inntektsmelding_finnes() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmeldingKompletthet();
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(2),
                STARTDATO_PERMISJON);
        lenient().when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(
                List.of(InntektsmeldingBuilder.builder().build()));

        // Act
        KompletthetResultat kompletthetResultat = kompletthetsjekkerImpl.vurderEtterlysningInntektsmelding(
                lagRef(behandling, STARTDATO_PERMISJON));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
        assertThat(kompletthetResultat.getVentefrist()).isNull();
        verify(dokumentBestillerTjenesteMock, never()).bestillDokument(any(), any(), Mockito.anyBoolean());
    }

    @Test
    public void skal_ikke_sende_brev_når_frister_passert() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmeldingKompletthet();
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(3), LocalDate.now());
        lenient().when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(
                List.of(InntektsmeldingBuilder.builder().build()));

        // Act
        KompletthetResultat kompletthetResultat = kompletthetsjekkerImpl.vurderEtterlysningInntektsmelding(
                lagRef(behandling, LocalDate.now()));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
        assertThat(kompletthetResultat.getVentefrist()).isNull();
        verify(dokumentBestillerTjenesteMock, never()).bestillDokument(any(), any(), Mockito.anyBoolean());
    }

    @Test
    public void skal_ikke_sende_brev_når_etterlysning_sendt() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmeldingKompletthet();
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(4),
                STARTDATO_PERMISJON);
        when(dokumentBehandlingTjenesteMock.erDokumentBestilt(any(), any())).thenReturn(true);

        // Act
        KompletthetResultat kompletthetResultat = kompletthetsjekkerImpl.vurderEtterlysningInntektsmelding(
                lagRef(behandling, STARTDATO_PERMISJON));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.getVentefrist().toLocalDate()).isEqualTo(STARTDATO_PERMISJON);
        verify(dokumentBestillerTjenesteMock, never()).bestillDokument(any(), any(), Mockito.anyBoolean());
    }

    @Test
    public void skal_etterlyse_mer_enn_3ukerfør() {
        // Arrange
        LocalDate stp = LocalDate.now().plusDays(2).plusWeeks(3);
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmeldingKompletthet();
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(1), stp);
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(Collections.emptyList());

        // Act
        KompletthetResultat kompletthetResultat = kompletthetsjekkerImpl.vurderEtterlysningInntektsmelding(
                lagRef(behandling, stp));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.getVentefrist().toLocalDate()).isEqualTo(LocalDate.now().plusWeeks(3));
        verify(dokumentBestillerTjenesteMock, times(1)).bestillDokument(any(), any(), Mockito.anyBoolean());

        // Act 2
        stp = LocalDate.now().plusWeeks(3);
        KompletthetResultat kompletthetResultat2 = kompletthetsjekkerImpl.vurderEtterlysningInntektsmelding(
                lagRef(behandling, stp));

        // Assert
        assertThat(kompletthetResultat2.erOppfylt()).isFalse();
        assertThat(kompletthetResultat2.getVentefrist().toLocalDate()).isEqualTo(stp);
        verify(dokumentBestillerTjenesteMock, times(2)).bestillDokument(any(), any(), Mockito.anyBoolean());
    }

    @Test
    public void skal_sende_brev_når_inntektsmelding_mangler() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmeldingKompletthet();
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(1),
                STARTDATO_PERMISJON);
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(Collections.emptyList());

        // Act
        KompletthetResultat kompletthetResultat = kompletthetsjekkerImpl.vurderEtterlysningInntektsmelding(
                lagRef(behandling, STARTDATO_PERMISJON));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.getVentefrist().toLocalDate()).isEqualTo(LocalDate.now().plusWeeks(3));
        verify(dokumentBestillerTjenesteMock, times(1)).bestillDokument(any(), any(), Mockito.anyBoolean());
    }

    @Test
    public void skal_finne_at_kompletthet_ikke_er_oppfylt_når_vedlegg_til_søknad_mangler() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        opprettSøknadMedPåkrevdVedlegg(behandling);
        behandling.setStatus(BehandlingStatus.UTREDES);
        // Act
        KompletthetResultat kompletthetResultat = kompletthetsjekkerImpl.vurderForsendelseKomplett(lagRef(behandling));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.getVentefrist().toLocalDate()).isEqualTo(
                søknadRepository.hentSøknad(behandling.getId()).getMottattDato().plusWeeks(3));
    }

    @Test
    public void skal_finne_at_kompletthet_er_oppfylt_når_vedlegg_til_søknad_finnes_i_joark() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        opprettSøknadMedPåkrevdVedlegg(behandling);

        // Act
        KompletthetResultat kompletthetResultat = kompletthetsjekkerImpl.vurderForsendelseKomplett(lagRef(behandling));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isTrue();
        assertThat(kompletthetResultat.getVentefrist()).isNull();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return BehandlingReferanse.fra(behandling, stp);
    }

    private BehandlingReferanse lagRef(Behandling behandling, LocalDate stpDate) {
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stpDate).build();
        return BehandlingReferanse.fra(behandling, stp);
    }

    @Test
    public void skal_returnere_hvilke_vedlegg_som_mangler() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmeldingKompletthet();
        opprettSøknadMedPåkrevdVedlegg(behandling);

        // Act
        List<ManglendeVedlegg> manglendeVedlegg = kompletthetsjekkerImpl.utledAlleManglendeVedleggForForsendelse(
                lagRef(behandling));

        // Assert
        assertThat(manglendeVedlegg).hasSize(2);
        List<DokumentTypeId> koder = manglendeVedlegg.stream()
                .map(ManglendeVedlegg::getDokumentType)
                .collect(Collectors.toList());
        assertThat(koder).containsExactlyInAnyOrder(DokumentTypeId.DOK_INNLEGGELSE, DokumentTypeId.INNTEKTSMELDING);
    }

    private void opprettSøknadMedPåkrevdVedlegg(Behandling behandling) {
        testUtil.byggOgLagreSøknadMedNyOppgittFordeling(behandling, false);
        SøknadEntitet søknad = new SøknadEntitet.Builder(søknadRepository.hentSøknad(behandling.getId()),
                false).leggTilVedlegg(
                        new SøknadVedleggEntitet.Builder().medSkjemanummer(KODE_INNLEGGELSE)
                                .medErPåkrevdISøknadsdialog(true)
                                .build())
                        .build();
        søknadRepository.lagreOgFlush(behandling, søknad);
    }

    private void mockManglendeInntektsmelding() {
        HashMap<Arbeidsgiver, Set<EksternArbeidsforholdRef>> manglendeInntektsmeldinger = new HashMap<>();
        manglendeInntektsmeldinger.put(Arbeidsgiver.virksomhet("1"), new HashSet<>());
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(any(), anyBoolean())).thenReturn(
                manglendeInntektsmeldinger);
    }

    private void mockManglendeInntektsmeldingKompletthet() {
        HashMap<Arbeidsgiver, Set<InternArbeidsforholdRef>> manglendeInntektsmeldinger = new HashMap<>();
        manglendeInntektsmeldinger.put(Arbeidsgiver.virksomhet("1"), new HashSet<>());
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(any(), anyBoolean())).thenReturn(
                manglendeInntektsmeldinger);
    }
}
