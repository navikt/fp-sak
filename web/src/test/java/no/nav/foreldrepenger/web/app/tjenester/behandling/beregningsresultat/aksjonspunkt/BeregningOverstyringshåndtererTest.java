package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.aksjonspunkt;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;

import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeregningOverstyringshåndtererTest {

    @Mock
    private LegacyESBeregningRepository beregningRepository;
    @Mock
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Test
    void lagHistorikkInnslag() {
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagMocked();
        var beregning = new LegacyESBeregning(48500L, 1L, 48500L, LocalDateTime.now(), true, 2030L);
        when(beregningRepository.getSisteBeregning(behandling.getId())).thenReturn(Optional.of(beregning));

        var dto = new OverstyringBeregningDto(1001, "Dette er en begrunnelse");
        var håndterer = new BeregningOverstyringshåndterer(beregningRepository, historikkinnslagRepository, null);
        håndterer.lagHistorikkInnslag(dto, behandling);

        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkinnslagRepository, times(1)).lagre(captor.capture());
        var historikkinnslag = captor.getValue();

        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        assertThat(historikkinnslag.getSkjermlenke()).isEqualTo(SkjermlenkeType.BEREGNING_ENGANGSSTOENAD);
        assertThat(historikkinnslag.getLinjer().getFirst().getTekst()).isEqualTo("__Overstyrt beregning: Beløpet__ er endret fra 2 030 kr til __1 001 kr__.");
        assertThat(historikkinnslag.getLinjer().get(1).getTekst()).isEqualTo("Dette er en begrunnelse.");
    }
}
