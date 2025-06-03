package no.nav.foreldrepenger.domene.vedtak.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktkontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.domene.vedtak.VedtakAksjonspunktData;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class FatterVedtakAksjonspunkt {

    private VedtakTjeneste vedtakTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public FatterVedtakAksjonspunkt(AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste,
                                    VedtakTjeneste vedtakTjeneste,
                                    TotrinnTjeneste totrinnTjeneste,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                    BehandlingRepository behandlingRepository) {
        this.vedtakTjeneste = vedtakTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
        this.aksjonspunktkontrollTjeneste = aksjonspunktkontrollTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    FatterVedtakAksjonspunkt() {
        // CDI
    }

    public void oppdater(BehandlingReferanse behandlingReferanse, Collection<VedtakAksjonspunktData> aksjonspunkter) {
        var lås = behandlingRepository.taSkriveLås(behandlingReferanse.behandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.behandlingId());
        behandling.setAnsvarligBeslutter(KontekstHolder.getKontekst().getUid());

        List<Totrinnsvurdering> totrinnsvurderinger = new ArrayList<>();
        List<Aksjonspunkt> skalReåpnes = new ArrayList<>();

        for (var aks : aksjonspunkter) {
            var erTotrinnGodkjent = aks.isGodkjent();
            var aksjonspunkt = behandling.getAksjonspunktFor(aks.getAksjonspunktDefinisjon());
            if (!aks.isGodkjent()) {
                skalReåpnes.add(aksjonspunkt);
            }

            var koder = aks.getVurderÅrsakskoder();
            Collection<VurderÅrsak> vurderÅrsaker = koder.stream().map(VurderÅrsak::fraKode).collect(Collectors.toSet());

            var vurderingBuilder = new Totrinnsvurdering.Builder(behandling, aks.getAksjonspunktDefinisjon());
            vurderingBuilder.medGodkjent(erTotrinnGodkjent);
            vurderÅrsaker.forEach(vurderingBuilder::medVurderÅrsak);
            vurderingBuilder.medBegrunnelse(aks.getBegrunnelse());
            totrinnsvurderinger.add(vurderingBuilder.build());
        }
        totrinnTjeneste.settNyeTotrinnaksjonspunktvurderinger(totrinnsvurderinger);
        vedtakTjeneste.lagHistorikkinnslagFattVedtak(behandling);
        // Noe spesialhåndtering ifm totrinn og tilbakeføring fra FVED
        if (!skalReåpnes.isEmpty()) {
            aksjonspunktkontrollTjeneste.lagreAksjonspunkterReåpnet(behandling, lås, skalReåpnes);
            aksjonspunktkontrollTjeneste.setAksjonspunkterToTrinn(behandling, lås, skalReåpnes, true);
            // Litt spesialbehandling siden dette aksjonspunktet er ekstra sticky pga mange tilbakehopp - nå kan det løses på nytt
            if (skalReåpnes.stream().map(Aksjonspunkt::getAksjonspunktDefinisjon).anyMatch(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING::equals)) {
                inntektArbeidYtelseTjeneste.fjernSaksbehandletVersjon(behandling.getId());
            }
        }
    }

}
