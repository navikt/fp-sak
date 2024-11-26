package no.nav.foreldrepenger.historikk;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;

/**
 * RequestScoped fordi HistorikkInnslagTekstBuilder inneholder state og denne
 * deles på tvers av AksjonspunktOppdaterere.
 */
@RequestScoped
public class HistorikkTjenesteAdapter {

    private HistorikkRepository historikkRepository;
    private HistorikkInnslagTekstBuilder builder;

    @Inject
    public HistorikkTjenesteAdapter(HistorikkRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
        this.builder = new HistorikkInnslagTekstBuilder();
    }

    HistorikkTjenesteAdapter() {
        // for CDI proxy
    }

    public HistorikkInnslagTekstBuilder tekstBuilder() {
        return builder;
    }

    public void opprettHistorikkInnslag(Long behandlingId, HistorikkinnslagType hisType) {
        opprettHistorikkInnslag(behandlingId, hisType, HistorikkAktør.SAKSBEHANDLER);
    }

    private void opprettHistorikkInnslag(Long behandlingId, HistorikkinnslagType hisType, HistorikkAktør historikkAktør) {
        if (!builder.getHistorikkinnslagDeler().isEmpty() || builder.antallEndredeFelter() > 0 || builder.getErBegrunnelseEndret()
            || builder.getErGjeldendeFraSatt()) {

            var innslag = new Historikkinnslag();

            builder.medHendelse(hisType);
            innslag.setAktør(historikkAktør);
            innslag.setType(hisType);
            innslag.setBehandlingId(behandlingId);
            builder.build(innslag);

            resetBuilder();

            historikkRepository.lagre(innslag);
        }
    }

    private void resetBuilder() {
        builder = new HistorikkInnslagTekstBuilder();
    }

}
