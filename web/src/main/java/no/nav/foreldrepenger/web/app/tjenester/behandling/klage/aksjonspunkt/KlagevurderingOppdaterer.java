package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktkontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.events.KlageOppdatertEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = KlageVurderingResultatAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class KlagevurderingOppdaterer implements AksjonspunktOppdaterer<KlageVurderingResultatAksjonspunktDto> {
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingEventPubliserer eventPubliserer;
    private KlageHistorikkinnslag klageHistorikkinnslag;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste;
    private BehandlingRepository behandlingRepository;

    KlagevurderingOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public KlagevurderingOppdaterer(KlageHistorikkinnslag klageHistorikkinnslag,
                                    BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                    BehandlingEventPubliserer eventPubliserer,
                                    AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste,
                                    KlageVurderingTjeneste klageVurderingTjeneste,
                                    BehandlingRepository behandlingRepository) {
        this.klageHistorikkinnslag = klageHistorikkinnslag;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.eventPubliserer = eventPubliserer;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.aksjonspunktkontrollTjeneste = aksjonspunktkontrollTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(KlageVurderingResultatAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var skriveLås = behandlingRepository.taSkriveLås(param.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var aksjonspunktDefinisjon = dto.getAksjonspunktDefinisjon();
        var totrinn = håndterToTrinnsBehandling(behandling, skriveLås, aksjonspunktDefinisjon, dto.getKlageVurdering());

        håndterKlageVurdering(dto, behandling, skriveLås, aksjonspunktDefinisjon);

        klageHistorikkinnslag.opprettHistorikkinnslagVurdering(behandling, aksjonspunktDefinisjon, dto, dto.getBegrunnelse());
        oppdatereDatavarehus(dto, behandling, aksjonspunktDefinisjon);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private void håndterKlageVurdering(KlageVurderingResultatAksjonspunktDto dto, Behandling behandling,
                                       BehandlingLås lås, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var vurdertAv = erNfpAksjonspunkt(aksjonspunktDefinisjon) ? KlageVurdertAv.NFP : KlageVurdertAv.NK;
        var builder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(behandling, vurdertAv)
            .medKlageVurdering(dto.getKlageVurdering())
            .medKlageVurderingOmgjør(dto.getKlageVurderingOmgjør())
            .medKlageMedholdÅrsak(dto.getKlageMedholdÅrsak())
            .medKlageHjemmel(dto.getKlageHjemmel())
            .medBegrunnelse(dto.getBegrunnelse())
            .medFritekstTilBrev(dto.getFritekstTilBrev());

        klageVurderingTjeneste.oppdaterBekreftetVurderingAksjonspunkt(behandling, lås, builder, vurdertAv);
    }

    private boolean håndterToTrinnsBehandling(Behandling behandling, BehandlingLås skriveLås,
                                              AksjonspunktDefinisjon aksjonspunktDefinisjon, KlageVurdering klageVurdering) {
        if (erNfpAksjonspunkt(aksjonspunktDefinisjon) && KlageVurderingTjeneste.skalBehandlesAvKlageInstans(KlageVurdertAv.NFP, klageVurdering)) {
            // Må fjerne totrinnsbehandling i tilfeller hvor totrinn er satt for NFP (klagen ikke er innom NK),
            // beslutter sender behandlingen tilbake til NFP, og NFP deretter gjør et valgt som sender
            // behandlingen til NK. Da skal ikke aksjonspunkt NFP totrinnsbehandles.
            fjernToTrinnsBehandling(behandling, skriveLås, aksjonspunktDefinisjon);
        }
        return erNfpAksjonspunkt(aksjonspunktDefinisjon) && !KlageVurderingTjeneste.skalBehandlesAvKlageInstans(KlageVurdertAv.NFP, klageVurdering);
    }

    private void fjernToTrinnsBehandling(Behandling behandling, BehandlingLås skriveLås, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
        if (aksjonspunkt.isToTrinnsBehandling()) {
            aksjonspunktkontrollTjeneste.setAksjonspunkterToTrinn(behandling, skriveLås, List.of(aksjonspunkt), false);
        }
    }

    private void oppdatereDatavarehus(KlageVurderingResultatAksjonspunktDto dto, Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var klageVurdering = dto.getKlageVurdering();
        if (erNfpAksjonspunkt(aksjonspunktDefinisjon) && KlageVurderingTjeneste.skalBehandlesAvKlageInstans(KlageVurdertAv.NFP, klageVurdering)) {
            behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, BehandlendeEnhetTjeneste.getKlageInstans(),
                HistorikkAktør.VEDTAKSLØSNINGEN, ""); //Det er ikke behov for en begrunnelse i dette tilfellet.
        } else {
            eventPubliserer.publiserBehandlingEvent(new KlageOppdatertEvent(behandling));
        }
    }

    private boolean erNfpAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.equals(aksjonspunktDefinisjon);
    }
}
