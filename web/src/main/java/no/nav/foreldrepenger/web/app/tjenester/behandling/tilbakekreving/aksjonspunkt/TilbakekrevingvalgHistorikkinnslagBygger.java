package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class TilbakekrevingvalgHistorikkinnslagBygger {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    protected TilbakekrevingvalgHistorikkinnslagBygger() {
        // For CDI proxy
    }

    @Inject
    public TilbakekrevingvalgHistorikkinnslagBygger(HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    public void byggHistorikkinnslag(Long behandlingId, Optional<TilbakekrevingValg> forrigeValg, TilbakekrevingValg tilbakekrevingValg, String begrunnelse) {
        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setBehandlingId(behandlingId);
        innslag.setType(HistorikkinnslagType.TILBAKEKREVING_VIDEREBEHANDLING);

        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder().medSkjermlenke(SkjermlenkeType.FAKTA_OM_SIMULERING);
        tekstBuilder.medBegrunnelse(begrunnelse);
        tekstBuilder.medHendelse(innslag.getType());

        Boolean forrigeTilbakekrevingVilkårOppfylt = null;
        Boolean forrigeGrunnerTilReduksjon = null;
        TilbakekrevingVidereBehandling forrigeTilbakekrevingVidereBehandling = null;
        if (forrigeValg.isPresent()) {
            TilbakekrevingValg forrigeValgObj = forrigeValg.get();
            forrigeTilbakekrevingVilkårOppfylt = forrigeValgObj.getErTilbakekrevingVilkårOppfylt();
            forrigeGrunnerTilReduksjon = forrigeValgObj.getGrunnerTilReduksjon();
            forrigeTilbakekrevingVidereBehandling = forrigeValgObj.getVidereBehandling();
        }
        if (tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt() != null) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT, forrigeTilbakekrevingVilkårOppfylt,
                tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt());
        }

        if (tilbakekrevingValg.getGrunnerTilReduksjon() != null) {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON, forrigeGrunnerTilReduksjon, tilbakekrevingValg.getGrunnerTilReduksjon());
        }
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_VIDERE_BEHANDLING, forrigeTilbakekrevingVidereBehandling, tilbakekrevingValg.getVidereBehandling());

        tekstBuilder.build(innslag);
        historikkTjenesteAdapter.lagInnslag(innslag);

    }
}
