package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class VurderOmSakSkalTilInfotrygd {

    private Instance<LocalDate> nyeBeregningsregler;

    VurderOmSakSkalTilInfotrygd() {
        // CDI
    }

    @Inject
    public VurderOmSakSkalTilInfotrygd(@KonfigVerdi(value = "dato.for.nye.beregningsregler", defaultVerdi = "2019-01-01") Instance<LocalDate> nyeBeregningsregler) {
        this.nyeBeregningsregler = nyeBeregningsregler;
    }

    public boolean skalForeldrepengersakBehandlesAvInfotrygd(Skjæringstidspunkt skjæringstidspunkter) {
        LocalDate skjæringstidspunkt = skjæringstidspunkter.getUtledetSkjæringstidspunkt();
        return skjæringstidspunkt.isBefore(nyeBeregningsregler.get());
    }
}
