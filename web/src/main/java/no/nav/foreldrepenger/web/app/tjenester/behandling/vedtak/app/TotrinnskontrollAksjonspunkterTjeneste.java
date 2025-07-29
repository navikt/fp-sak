package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModellTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderingspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollAksjonspunkterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollSkjermlenkeContextDto;

@ApplicationScoped
public class TotrinnskontrollAksjonspunkterTjeneste {

    private TotrinnsaksjonspunktDtoTjeneste totrinnsaksjonspunktDtoTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingModellTjeneste behandlingModellTjeneste;

    protected TotrinnskontrollAksjonspunkterTjeneste() {
        //for CDI-proxy
    }

    @Inject
    public TotrinnskontrollAksjonspunkterTjeneste(TotrinnsaksjonspunktDtoTjeneste totrinnsaksjonspunktDtoTjeneste,
                                                  TotrinnTjeneste totrinnTjeneste,
                                                  BehandlingModellTjeneste behandlingModellTjeneste) {
        this.totrinnsaksjonspunktDtoTjeneste = totrinnsaksjonspunktDtoTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
        this.behandlingModellTjeneste = behandlingModellTjeneste;
    }

    public List<TotrinnskontrollSkjermlenkeContextDto> hentTotrinnsSkjermlenkeContext(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        var skjermlenkeContext = new ArrayList<TotrinnskontrollSkjermlenkeContextDto>();
        var aksjonspunkter = behandling.getAksjonspunkterMedTotrinnskontroll();
        Map<SkjermlenkeType, List<TotrinnskontrollAksjonspunkterDto>> skjermlenkeMap = new EnumMap<>(SkjermlenkeType.class);
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
        return sorterPåBehandlingsrekkefølge(behandlingModellTjeneste, behandling.getFagsakYtelseType(), behandling.getType(), skjermlenkeContext);
    }

    public List<TotrinnskontrollSkjermlenkeContextDto> hentTotrinnsvurderingSkjermlenkeContext(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        var skjermlenkeContext = new ArrayList<TotrinnskontrollSkjermlenkeContextDto>();
        var totrinnaksjonspunktvurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling.getId());
        Map<SkjermlenkeType, List<TotrinnskontrollAksjonspunkterDto>> skjermlenkeMap = new EnumMap<>(SkjermlenkeType.class);
        for (var vurdering : totrinnaksjonspunktvurderinger) {
            lagTotrinnsaksjonspunkt(behandling, behandlingsresultat, skjermlenkeMap, vurdering);
        }
        for (var skjermlenke : skjermlenkeMap.entrySet()) {
            var context = new TotrinnskontrollSkjermlenkeContextDto(skjermlenke.getKey().getKode(),
                skjermlenke.getValue());
            skjermlenkeContext.add(context);
        }
        return sorterPåBehandlingsrekkefølge(behandlingModellTjeneste, behandling.getFagsakYtelseType(), behandling.getType(), skjermlenkeContext);
    }

    static List<TotrinnskontrollSkjermlenkeContextDto> sorterPåBehandlingsrekkefølge(BehandlingModellTjeneste behandlingModellTjeneste,
                                                                                     FagsakYtelseType ytelseType,
                                                                                     BehandlingType behandlingType,
                                                                                     List<TotrinnskontrollSkjermlenkeContextDto> skjermlenkeContext) {
        return skjermlenkeContext.stream().sorted((o1, o2) -> {
                var stegA = tidligstLøstSteg(behandlingModellTjeneste, ytelseType, behandlingType, o1.getTotrinnskontrollAksjonspunkter());
                var stegB = tidligstLøstSteg(behandlingModellTjeneste, ytelseType, behandlingType, o2.getTotrinnskontrollAksjonspunkter());
                if (stegA == stegB) {
                    return 0;
                }
                return behandlingModellTjeneste.erStegAEtterStegB(ytelseType, behandlingType, stegA, stegB) ? 1 : -1;
            })
            .toList();
    }

    private static BehandlingStegType tidligstLøstSteg(BehandlingModellTjeneste behandlingModellTjeneste,
                                                       FagsakYtelseType ytelseType,
                                                       BehandlingType behandlingType,
                                                       List<TotrinnskontrollAksjonspunkterDto> totrinnskontrollAksjonspunkter) {
        return totrinnskontrollAksjonspunkter.stream()
            .map(tt -> AksjonspunktDefinisjon.fraKode(tt.getAksjonspunktKode()))
            .min((aksjonspunktDefinisjonA, aksjonspunktDefinisjonB) -> {
                var stegA = aksjonspunktDefinisjonA.getBehandlingSteg();
                var stegB = aksjonspunktDefinisjonB.getBehandlingSteg();
                if (stegA.equals(stegB)) {
                    if (aksjonspunktDefinisjonA.getVurderingspunktType() == aksjonspunktDefinisjonB.getVurderingspunktType()) {
                        return 0;
                    }
                    return aksjonspunktDefinisjonA.getVurderingspunktType() == VurderingspunktType.INN ? -1 : 1;
                }
                return behandlingModellTjeneste.erStegAFørStegB(ytelseType, behandlingType, stegA, stegB) ? -1 : 1;
            })
            .map(AksjonspunktDefinisjon::getBehandlingSteg).orElseThrow();
    }

    private void lagTotrinnsaksjonspunkt(Behandling behandling,
                                         Behandlingsresultat behandlingsresultat,
                                         Map<SkjermlenkeType, List<TotrinnskontrollAksjonspunkterDto>> skjermlenkeMap,
                                         Totrinnsvurdering vurdering) {
        var totrinnsAksjonspunkt = totrinnsaksjonspunktDtoTjeneste.lagTotrinnskontrollAksjonspunktDto(vurdering,
            behandling);
        var skjermlenkeType = SkjermlenkeType.finnSkjermlenkeType(vurdering.getAksjonspunktDefinisjon(), behandling,
            behandlingsresultat);
        if (SkjermlenkeType.totrinnsSkjermlenke(skjermlenkeType)) {
            var aksjonspktContextListe = skjermlenkeMap.computeIfAbsent(skjermlenkeType, k -> new ArrayList<>());
            aksjonspktContextListe.add(totrinnsAksjonspunkt);
        }
    }
}
