package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.rest.dto.KontrollerAAPKombinertATFLDto;

class KontrollerAAPKombinertATFLOppdatererTest {

    private final HistorikkinnslagRepository historikkinnslagRepository = mock(HistorikkinnslagRepository.class);
    private final KontrollerAAPKombinertATFLOppdaterer oppdaterer = new KontrollerAAPKombinertATFLOppdaterer(historikkinnslagRepository);

    @Test
    void skal_lagre_historikk_og_returnere_uten_transisjon() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var dto = new KontrollerAAPKombinertATFLDto("Beregningen er kontrollert og korrekt.");

        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkinnslagRepository).lagre(captor.capture());

        var historikkinnslag = captor.getValue();
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.getSkjermlenke()).isEqualTo(SkjermlenkeType.BEREGNING_FORELDREPENGER);
        assertThat(historikkinnslag.getTekstLinjer()).contains("Beregningen er kontrollert og korrekt.");
        assertThat(resultat.getEkstraAksjonspunktResultat()).isEmpty();
    }
}
