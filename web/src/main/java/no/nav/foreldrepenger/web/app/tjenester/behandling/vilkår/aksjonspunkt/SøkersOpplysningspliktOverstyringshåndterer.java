package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringSokersOpplysingspliktDto;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringSokersOpplysingspliktDto.class, adapter = Overstyringshåndterer.class)
public class SøkersOpplysningspliktOverstyringshåndterer implements Overstyringshåndterer<OverstyringSokersOpplysingspliktDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    SøkersOpplysningspliktOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public SøkersOpplysningspliktOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                       InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;

    }

    @Override
    public void lagHistorikkInnslag(OverstyringSokersOpplysingspliktDto dto, Behandling behandling) {
        leggTilEndretFeltIHistorikkInnslag(dto.getBegrunnelse(), dto.getErVilkarOk());
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringSokersOpplysingspliktDto dto, Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {

        if (!dto.getErVilkarOk() && behandling.erRevurdering()) {
            throw new FunksjonellException("FP-093925", "Kan ikke avslå revurdering med opplysningsplikt.",
                "Overstyr ett av de andre vilkårene.");
        }
        var utfall = dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        inngangsvilkårTjeneste.overstyrAksjonspunktForSøkersopplysningsplikt(behandling.getId(), utfall, kontekst);

        var builder = OppdateringResultat.utenTransisjon();
        if (VilkårUtfallType.OPPFYLT.equals(utfall)) {
            return builder.build();
        }
        builder.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR);
        return builder.build();
    }

    private void leggTilEndretFeltIHistorikkInnslag(String begrunnelse, boolean vilkårOppfylt) {
        var tilVerdi = vilkårOppfylt ? HistorikkEndretFeltVerdiType.OPPFYLT : HistorikkEndretFeltVerdiType.IKKE_OPPFYLT;

        var tekstBuilder = historikkAdapter.tekstBuilder();
        if (begrunnelse != null) {
            tekstBuilder.medBegrunnelse(begrunnelse);
        }
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.SOKERSOPPLYSNINGSPLIKT, null, tilVerdi)
            .medSkjermlenke(SkjermlenkeType.OPPLYSNINGSPLIKT);

    }
}
