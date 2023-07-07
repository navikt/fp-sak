package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringSokersOpplysingspliktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringSokersOpplysingspliktDto.class, adapter = Overstyringshåndterer.class)
public class SøkersOpplysningspliktOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringSokersOpplysingspliktDto> {

    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    SøkersOpplysningspliktOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public SøkersOpplysningspliktOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                       InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST);
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;

    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringSokersOpplysingspliktDto dto) {
        leggTilEndretFeltIHistorikkInnslag(dto.getBegrunnelse(), dto.getErVilkarOk());
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringSokersOpplysingspliktDto dto, Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {

        var utfall = dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        inngangsvilkårTjeneste.overstyrAksjonspunktForSøkersopplysningsplikt(behandling.getId(), utfall, kontekst);

        var builder = OppdateringResultat.utenTransisjon();
        if (VilkårUtfallType.OPPFYLT.equals(utfall)) {
            // Rydd opp i aksjonspunkt tidligere opprettet i forbindelse med overstyring av søkers opplysningsplikt
            behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL)
                .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
            return builder.build();
        }
        builder.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR).medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL, AksjonspunktStatus.OPPRETTET);
        return builder.build();
    }

    private void leggTilEndretFeltIHistorikkInnslag(String begrunnelse, boolean vilkårOppfylt) {
        var tilVerdi = vilkårOppfylt ? HistorikkEndretFeltVerdiType.OPPFYLT : HistorikkEndretFeltVerdiType.IKKE_OPPFYLT;

        var tekstBuilder = getHistorikkAdapter().tekstBuilder();
        if (begrunnelse != null) {
            tekstBuilder.medBegrunnelse(begrunnelse);
        }
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.SOKERSOPPLYSNINGSPLIKT, null, tilVerdi)
            .medSkjermlenke(SkjermlenkeType.OPPLYSNINGSPLIKT);

    }
}
