package no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.FpInntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetssjekkerInntektsmelding;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.KompletthetssjekkerTestUtil;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp.KompletthetsjekkerFelles;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp.KompletthetssjekkerSøknadFørstegangsbehandlingImpl;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.impl.fp.KompletthetssjekkerSøknadImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
class KompletthetsjekkerImplTest extends EntityManagerAwareTest {

    private static final LocalDate STARTDATO_PERMISJON = LocalDate.now().plusWeeks(1);
    private BehandlingRepositoryProvider repositoryProvider;

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
    @Mock
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;
    @Mock
    private SvangerskapspengerRepository svangerskapspengerRepository;

    private KompletthetsjekkerImpl kompletthetsjekkerImpl;
    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
        .medUtledetSkjæringstidspunkt(STARTDATO_PERMISJON)
        .build();

    @BeforeEach
    public void before() {
        lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.anyLong()))
            .thenReturn(skjæringstidspunkt);
        lenient().when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(any(), any()))
            .thenReturn(new HashMap<>());
        lenient().when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(any(), any()))
            .thenReturn(new HashMap<>());

        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        KompletthetssjekkerSøknadImpl kompletthetssjekkerSøknadImpl = new KompletthetssjekkerSøknadFørstegangsbehandlingImpl(
            dokumentArkivTjeneste, repositoryProvider, Period.parse("P4W"));
        var kompletthetssjekkerInntektsmelding = new KompletthetssjekkerInntektsmelding(
            inntektsmeldingArkivTjeneste, svangerskapspengerRepository);
        var kompletthetsjekkerFelles = new KompletthetsjekkerFelles(repositoryProvider,
            dokumentBestillerTjenesteMock, dokumentBehandlingTjenesteMock, kompletthetssjekkerInntektsmelding, inntektsmeldingTjeneste,
            fpInntektsmeldingTjeneste);
        kompletthetsjekkerImpl = new KompletthetsjekkerImpl(kompletthetssjekkerSøknadImpl, kompletthetsjekkerFelles);
        testUtil = new KompletthetssjekkerTestUtil(repositoryProvider);
    }

    @Test
    void skal_sende_brev_når_inntektsmelding_mangler() {
        // Arrange
        var behandling =  ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagre(repositoryProvider);
        mockManglendeInntektsmeldingGrunnlag();
        testUtil.byggOgLagreFørstegangsSøknadMedMottattdato(behandling, LocalDate.now().minusWeeks(1),
            STARTDATO_PERMISJON);
        when(inntektsmeldingTjeneste.hentInntektsmeldinger(any(), any())).thenReturn(Collections.emptyList());

        // Act
        var kompletthetResultat = kompletthetsjekkerImpl.vurderEtterlysningInntektsmelding(
            lagRef(behandling), Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(STARTDATO_PERMISJON).build());

        // Assert
        assertThat(kompletthetResultat.erOppfylt()).isFalse();
        assertThat(kompletthetResultat.ventefrist().toLocalDate()).isEqualTo(LocalDate.now().plusWeeks(3));
        verify(dokumentBestillerTjenesteMock, times(1)).bestillDokument(any(DokumentBestilling.class));
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    private void mockManglendeInntektsmeldingGrunnlag() {
        var manglendeInntektsmeldinger = new HashMap<Arbeidsgiver, Set<InternArbeidsforholdRef>>();
        manglendeInntektsmeldinger.put(Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG), new HashSet<>());
        when(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(any(), any())).thenReturn(
            manglendeInntektsmeldinger);
    }
}
