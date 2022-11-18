package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.fakta.aktkrav.KontrollerAktivitetskravAksjonspunktUtleder;

public class KontrollerAktivitetskravStegTest {

    @Test
    public void skalReturnereResultatMedAksjonspunkt() {
        var resultat = kjørSteg(AksjonspunktDefinisjon.KONTROLLER_AKTIVITETSKRAV);
        assertThat(resultat.getAksjonspunktListe()).contains(AksjonspunktDefinisjon.KONTROLLER_AKTIVITETSKRAV);
    }

    @Test
    public void skalIkkeReturnereResultatMedAksjonspunkt() {
        var resultat = kjørSteg();
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    private BehandleStegResultat kjørSteg() {
        return kjørSteg(null);
    }

    private BehandleStegResultat kjørSteg(AksjonspunktDefinisjon forventet) {
        var utleder = mock(KontrollerAktivitetskravAksjonspunktUtleder.class);
        var uttakInputTjeneste = mock(UttakInputTjeneste.class);
        List<AksjonspunktDefinisjon> forventetAksjonspunkter = forventet == null ? List.of() : List.of(forventet);
        when(utleder.utledFor(any(), anyBoolean())).thenReturn(forventetAksjonspunkter);
        var steg = new KontrollerAktivitetskravSteg(utleder, uttakInputTjeneste, null);
        return steg.utførSteg(new BehandlingskontrollKontekst(1L, AktørId.dummy(), mock(BehandlingLås.class)));
    }

}
