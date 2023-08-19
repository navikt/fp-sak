package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollAksjonspunkterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollSkjermlenkeContextDto;

@ApplicationScoped
public class TotrinnskontrollAksjonspunkterTjeneste {

    private TotrinnsaksjonspunktDtoTjeneste totrinnsaksjonspunktDtoTjeneste;
    private TotrinnTjeneste totrinnTjeneste;

    protected TotrinnskontrollAksjonspunkterTjeneste() {
        //for CDI-proxy
    }

    @Inject
    public TotrinnskontrollAksjonspunkterTjeneste(TotrinnsaksjonspunktDtoTjeneste totrinnsaksjonspunktDtoTjeneste,
                                                  TotrinnTjeneste totrinnTjeneste) {
        this.totrinnsaksjonspunktDtoTjeneste = totrinnsaksjonspunktDtoTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    public List<TotrinnskontrollSkjermlenkeContextDto> hentTotrinnsSkjermlenkeContext(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        var skjermlenkeContext = new ArrayList<TotrinnskontrollSkjermlenkeContextDto>();
        var aksjonspunkter = behandling.getAksjonspunkterMedTotrinnskontroll();
        var skjermlenkeMap = new HashMap<SkjermlenkeType, List<TotrinnskontrollAksjonspunkterDto>>();
        var ttVurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling.getId());
        // Behandling er ikkje i fatte vedtak og har ingen totrinnsvurderinger -> returnerer tom liste
        if (!BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus()) && ttVurderinger.isEmpty()) {
            return Collections.emptyList();
        }
        for (var ap : aksjonspunkter) {
            var builder = new Totrinnsvurdering.Builder(behandling, ap.getAksjonspunktDefinisjon());
            var vurdering = ttVurderinger.stream()
                .filter(v -> v.getAksjonspunktDefinisjon().equals(ap.getAksjonspunktDefinisjon()))
                .findFirst();
            vurdering.ifPresent(ttVurdering -> {
                if (ttVurdering.isGodkjent()) {
                    builder.medGodkjent(ttVurdering.isGodkjent());
                }
            });
            lagTotrinnsaksjonspunkt(behandling, behandlingsresultat, skjermlenkeMap, builder.build());
        }
        for (var skjermlenke : skjermlenkeMap.entrySet()) {
            var context = new TotrinnskontrollSkjermlenkeContextDto(skjermlenke.getKey().getKode(),
                skjermlenke.getValue());
            skjermlenkeContext.add(context);
        }
        return skjermlenkeContext;
    }

    private void lagTotrinnsaksjonspunkt(Behandling behandling,
                                         Behandlingsresultat behandlingsresultat,
                                         Map<SkjermlenkeType, List<TotrinnskontrollAksjonspunkterDto>> skjermlenkeMap,
                                         Totrinnsvurdering vurdering) {
        var totrinnresultatOpt = totrinnTjeneste.hentTotrinngrunnlagHvisEksisterer(behandling.getId());
        var totrinnsAksjonspunkt = totrinnsaksjonspunktDtoTjeneste.lagTotrinnskontrollAksjonspunktDto(vurdering,
            behandling, totrinnresultatOpt);
        var skjermlenkeType = SkjermlenkeType.finnSkjermlenkeType(vurdering.getAksjonspunktDefinisjon(), behandling,
            behandlingsresultat);
        if (SkjermlenkeType.totrinnsSkjermlenke(skjermlenkeType)) {
            var aksjonspktContextListe = skjermlenkeMap.computeIfAbsent(skjermlenkeType, k -> new ArrayList<>());
            aksjonspktContextListe.add(totrinnsAksjonspunkt);
        }
    }

    public List<TotrinnskontrollSkjermlenkeContextDto> hentTotrinnsvurderingSkjermlenkeContext(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        var skjermlenkeContext = new ArrayList<TotrinnskontrollSkjermlenkeContextDto>();
        var totrinnaksjonspunktvurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling.getId());
        var skjermlenkeMap = new HashMap<SkjermlenkeType, List<TotrinnskontrollAksjonspunkterDto>>();
        for (var vurdering : totrinnaksjonspunktvurderinger) {
            lagTotrinnsaksjonspunkt(behandling, behandlingsresultat, skjermlenkeMap, vurdering);
        }
        for (var skjermlenke : skjermlenkeMap.entrySet()) {
            var context = new TotrinnskontrollSkjermlenkeContextDto(skjermlenke.getKey().getKode(),
                skjermlenke.getValue());
            skjermlenkeContext.add(context);
        }
        return skjermlenkeContext;
    }
}
