package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.svp;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerInntektsmelding;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.KompletthetssjekkerTestUtil;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp.KompletthetsjekkerFelles;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp.KompletthetssjekkerSøknadFørstegangsbehandlingImpl;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp.KompletthetssjekkerSøknadImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
public class KompletthetsjekkerImplTest extends EntityManagerAwareTest {

    private static final LocalDate STARTDATO_PERMISJON = LocalDate.now().plusWeeks(1);
    private BehandlingRepositoryProvider repositoryProvider;

    private KompletthetssjekkerTestUtil testUtil;

    @Mock
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjenesteMock;
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
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.anyLong()))
            .thenReturn(skjæringstidspunkt);
        lenient().when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(any(), anyBoolean()))
            .thenReturn(new HashMap<>());
        lenient().when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(any(), anyBoolean()))
            .thenReturn(new HashMap<>());

        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        KompletthetssjekkerSøknadImpl kompletthetssjekkerSøknadImpl = new KompletthetssjekkerSøknadFørstegangsbehandlingImpl(
            dokumentArkivTjeneste, repositoryProvider, Period.parse("P4W"));
        KompletthetssjekkerInntektsmelding kompletthetssjekkerInntektsmelding = new KompletthetssjekkerInntektsmeldingImpl(
            inntektsmeldingArkivTjeneste);
        KompletthetsjekkerFelles kompletthetsjekkerFelles = new KompletthetsjekkerFelles(repositoryProvider,
            dokumentBestillerApplikasjonTjenesteMock, dokumentBehandlingTjenesteMock);
        SøknadRepository søknadRepository = repositoryProvider.getSøknadRepository();
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
        kompletthetsjekkerImpl = new KompletthetsjekkerImpl(kompletthetssjekkerSøknadImpl,
            kompletthetssjekkerInntektsmelding, inntektsmeldingTjeneste, kompletthetsjekkerFelles, søknadRepository,
            behandlingRepository);
        testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);
    }

    @Test
    public void skal_sende_brev_når_inntektsmelding_mangler() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        mockManglendeInntektsmeldingGrunnlag();
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(1),
            STARTDATO_PERMISJON);
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(Collections.emptyList());

        // Act
        KompletthetResultat kompletthetResultat = kompletthetsjekkerImpl.vurderEtterlysningInntektsmelding(
            lagRef(behandling, STARTDATO_PERMISJON));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.getVentefrist().toLocalDate()).isEqualTo(LocalDate.now().plusWeeks(3));
        verify(dokumentBestillerApplikasjonTjenesteMock, times(1)).bestillDokument(any(), any(), Mockito.anyBoolean());
    }

    @Test
    public void skal_ikke_sende_brev_når_inntektsmelding_mangler_men_sak_er_migrert_fra_infotrygd() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        behandling.setMigrertKilde(Fagsystem.INFOTRYGD);
        mockManglendeInntektsmeldingGrunnlag();
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(1),
            STARTDATO_PERMISJON);
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(Collections.emptyList());

        // Act
        KompletthetResultat kompletthetResultat = kompletthetsjekkerImpl.vurderEtterlysningInntektsmelding(
            lagRef(behandling, STARTDATO_PERMISJON));

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.getVentefrist().toLocalDate()).isEqualTo(LocalDate.now().plusWeeks(3));
        verify(dokumentBestillerApplikasjonTjenesteMock, never()).bestillDokument(any(), any(), Mockito.anyBoolean());
    }

    private BehandlingReferanse lagRef(Behandling behandling, LocalDate stpDate) {
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stpDate).build();
        return BehandlingReferanse.fra(behandling, stp);
    }

    private void mockManglendeInntektsmeldingGrunnlag() {
        HashMap<Arbeidsgiver, Set<InternArbeidsforholdRef>> manglendeInntektsmeldinger = new HashMap<>();
        manglendeInntektsmeldinger.put(Arbeidsgiver.virksomhet("1"), new HashSet<>());
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(any(), anyBoolean())).thenReturn(
            manglendeInntektsmeldinger);
    }
}
