package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
class ForespørselStatusVurderingTjenesteTest {

    private static final String SAKSNUMMER = "1234567890";
    private static final String ORGNR = "999999999";
    private static final String ANNET_ORGNR = "888888888";
    private static final Long FAGSAK_ID = 42L;

    @Mock
    private FagsakTjeneste fagsakTjenesteMock;
    @Mock
    private BehandlingRepository behandlingRepositoryMock;
    @Mock
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjenesteMock;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjenesteMock;

    private ForespørselStatusVurderingTjeneste tjeneste;

    @BeforeEach
    void setup() {
        tjeneste = new ForespørselStatusVurderingTjeneste(fagsakTjenesteMock, behandlingRepositoryMock, arbeidsforholdInntektsmeldingMangelTjenesteMock, skjæringstidspunktTjenesteMock);
        lenient().when(skjæringstidspunktTjenesteMock.getSkjæringstidspunkter(anyLong()))
            .thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build());
    }

    @Test
    void avsluttet_fagsak_gir_utgått() {
        var fagsak = lagFagsak(FagsakStatus.AVSLUTTET);
        when(fagsakTjenesteMock.finnFagsakGittSaksnummer(new Saksnummer(SAKSNUMMER), false)).thenReturn(Optional.of(fagsak));

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR)))

            .isEqualTo(ForespørselVurderingResultat.SETT_TIL_UTGÅTT);
    }

    @Test
    void ingen_arbeidsforhold_for_orgnummer_gir_utgått() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any(), any())).thenReturn(List.of());

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR)))

            .isEqualTo(ForespørselVurderingResultat.SETT_TIL_UTGÅTT);
    }

    @Test
    void minst_en_ikke_mottatt_gir_trenger_fortsatt() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any(), any())).thenReturn(
            List.of(lagIMStatus(ORGNR, InntektsmeldingStatus.MOTTATT), lagIMStatus(ORGNR, InntektsmeldingStatus.IKKE_MOTTAT)));

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR)))
            .isEqualTo(ForespørselVurderingResultat.TRENGER_FORTSATT_INNTEKTSMELDING);
    }

    @Test
    void alle_mottatt_gir_ferdig_siden_saken_fortsatt_løper() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any(), any())).thenReturn(
            List.of(lagIMStatus(ORGNR, InntektsmeldingStatus.MOTTATT), lagIMStatus(ORGNR, InntektsmeldingStatus.MOTTATT)));

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR)))

            .isEqualTo(ForespørselVurderingResultat.SETT_TIL_FERDIG);
    }

    @Test
    void avklart_ikke_paakrevd_gir_trengs_fortsatt_skal_ikke_lukkes_automatisk() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any(), any())).thenReturn(
            List.of(lagIMStatus(ORGNR, InntektsmeldingStatus.MOTTATT), lagIMStatus(ORGNR, InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD)));

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR)))
            .isEqualTo(ForespørselVurderingResultat.TRENGER_FORTSATT_INNTEKTSMELDING);
    }

    @Test
    void statuser_for_annet_orgnummer_filtreres_bort_og_gir_utgått() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any(), any())).thenReturn(
            List.of(lagIMStatus(ANNET_ORGNR, InntektsmeldingStatus.IKKE_MOTTAT)));

        assertThat(tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR)))

            .isEqualTo(ForespørselVurderingResultat.SETT_TIL_UTGÅTT);
    }

    @Test
    void feil_i_statusberegning_propagerer_og_gir_ikke_utgått() {
        var behandling = mock(Behandling.class);
        stubFagsakOgBehandling(behandling);
        when(arbeidsforholdInntektsmeldingMangelTjenesteMock.finnStatusForInntektsmeldingArbeidsforhold(any(), any()))
            .thenThrow(new IllegalStateException("Beregningsfeil"));

        assertThatThrownBy(() -> tjeneste.vurder(new ForespørselStatusRequest(SAKSNUMMER, ORGNR)))
            .isInstanceOf(IllegalStateException.class);
    }

    private void stubFagsakOgBehandling(Behandling behandling) {
        var fagsak = lagFagsak(FagsakStatus.LØPENDE);
        when(fagsakTjenesteMock.finnFagsakGittSaksnummer(new Saksnummer(SAKSNUMMER), false)).thenReturn(Optional.of(fagsak));
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(FAGSAK_ID)).thenReturn(Optional.of(behandling));
    }

    private Fagsak lagFagsak(FagsakStatus status) {
        var fagsak = mock(Fagsak.class);
        lenient().when(fagsak.getStatus()).thenReturn(status);
        lenient().when(fagsak.getId()).thenReturn(FAGSAK_ID);
        return fagsak;
    }

    private static ArbeidsforholdInntektsmeldingStatus lagIMStatus(String orgnummer, InntektsmeldingStatus status) {
        return new ArbeidsforholdInntektsmeldingStatus(Arbeidsgiver.virksomhet(orgnummer), InternArbeidsforholdRef.nullRef(), status);
    }
}
