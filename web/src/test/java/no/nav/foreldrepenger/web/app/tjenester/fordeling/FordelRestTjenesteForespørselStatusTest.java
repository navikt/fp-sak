package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusRequest;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusRequest.Forespørsel;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusRequest.YtelseType;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusResponse;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusResponse.Årsak;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusVurderingTjeneste;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.exception.FunksjonellException;

/**
 * Dekker kun endepunktets wiring (delegering + ABAC), uten behov for EntityManager/Docker.
 * Selve vurderingslogikken er dekket av ForespørselStatusVurderingTjenesteTest.
 */
@ExtendWith(MockitoExtension.class)
class FordelRestTjenesteForespørselStatusTest {

    private static final String SAKSNUMMER_1 = "1234567890";
    private static final String SAKSNUMMER_2 = "2234567890";
    private static final String ORGNR = "999999999";
    private static final String ANNET_ORGNR = "888888888";

    @Mock
    private SaksbehandlingDokumentmottakTjeneste dokumentmottakTjenesteMock;
    @Mock
    private FagsakTjeneste fagsakTjenesteMock;
    @Mock
    private OpprettSakTjeneste opprettSakTjenesteMock;
    @Mock
    private BehandlingRepositoryProvider behandlingRepositoryProviderMock;
    @Mock
    private VurderFagsystemFellesTjeneste vurderFagsystemTjenesteMock;
    @Mock
    private SakInfoDtoTjeneste sakInfoDtoTjenesteMock;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjenesteMock;
    @Mock
    private ForespørselStatusVurderingTjeneste forespørselStatusVurderingTjenesteMock;

    @Test
    void forespoerselStatus_delegerer_til_vurderingstjenesten_og_returnerer_svaret_uendret() {
        var tjeneste = new FordelRestTjeneste(dokumentmottakTjenesteMock, fagsakTjenesteMock, opprettSakTjenesteMock,
            behandlingRepositoryProviderMock, vurderFagsystemTjenesteMock, sakInfoDtoTjenesteMock, skjæringstidspunktTjenesteMock,
            forespørselStatusVurderingTjenesteMock);
        var request = new ForespørselStatusRequest(List.of(new Forespørsel(SAKSNUMMER_1, ORGNR, YtelseType.FORELDREPENGER)));
        var forventetSvar = List.of(ForespørselStatusResponse.av(SAKSNUMMER_1, ORGNR, Årsak.MANGLER_INNTEKTSMELDING));
        when(forespørselStatusVurderingTjenesteMock.vurder(request)).thenReturn(forventetSvar);

        var svar = tjeneste.forespørselStatus(request);

        assertThat(svar).isEqualTo(forventetSvar);
        verify(forespørselStatusVurderingTjenesteMock).vurder(request);
    }

    @Test
    void abac_supplier_legger_til_det_ene_saksnummeret_i_batchen() {
        var request = new ForespørselStatusRequest(
            List.of(new Forespørsel(SAKSNUMMER_1, ORGNR, YtelseType.FORELDREPENGER), new Forespørsel(SAKSNUMMER_1, ANNET_ORGNR, YtelseType.FORELDREPENGER)));

        var abacDataAttributter = new FordelRestTjeneste.ForespørselStatusRequestAbacDataSupplier().apply(request);

        assertThat(abacDataAttributter.getVerdier(AppAbacAttributtType.SAKSNUMMER)).containsExactly(SAKSNUMMER_1);
    }

    @Test
    void abac_supplier_kaster_funksjonell_exception_ved_flere_ulike_saksnummer_i_batchen() {
        var request = new ForespørselStatusRequest(
            List.of(new Forespørsel(SAKSNUMMER_1, ORGNR, YtelseType.FORELDREPENGER), new Forespørsel(SAKSNUMMER_2, ORGNR, YtelseType.FORELDREPENGER)));

        assertThatThrownBy(() -> new FordelRestTjeneste.ForespørselStatusRequestAbacDataSupplier().apply(request))
            .isInstanceOf(FunksjonellException.class);
    }

}
