package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.util.ArrayList;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;

@ApplicationScoped
public class TilbakekrevingvalgHistorikkinnslagBygger {

    private Historikkinnslag2Repository historikkinnslagRepository;

    protected TilbakekrevingvalgHistorikkinnslagBygger() {
        // For CDI proxy
    }

    @Inject
    public TilbakekrevingvalgHistorikkinnslagBygger(Historikkinnslag2Repository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void byggHistorikkinnslag(BehandlingReferanse ref, Optional<TilbakekrevingValg> forrigeValg, TilbakekrevingValg tilbakekrevingValg, String begrunnelse) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        if (tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt() != null) {
            var fraVerdi = forrigeValg.map(TilbakekrevingValg::getErTilbakekrevingVilkårOppfylt).orElse(null);
            linjer.add(fraTilEquals("Er vilkårene for tilbakekreving oppfylt", fraVerdi, tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt()));
        }
        if (tilbakekrevingValg.getGrunnerTilReduksjon() != null) {
            var fraVerdi = forrigeValg.map(TilbakekrevingValg::getGrunnerTilReduksjon).orElse(null);
            linjer.add(fraTilEquals("Er det særlige grunner til reduksjon", fraVerdi, tilbakekrevingValg.getGrunnerTilReduksjon()));
        }
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OM_SIMULERING)
            .medLinjer(linjer)
            .addLinje(fraTilEquals("Fastsett videre behandling", forrigeValg.map(TilbakekrevingValg::getVidereBehandling).orElse(null), tilbakekrevingValg.getVidereBehandling()))
            .addLinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);

    }
}
