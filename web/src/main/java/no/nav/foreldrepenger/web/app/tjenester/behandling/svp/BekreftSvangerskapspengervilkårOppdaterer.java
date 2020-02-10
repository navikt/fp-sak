package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftSvangerskapspengervilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftSvangerskapspengervilkårOppdaterer implements AksjonspunktOppdaterer<BekreftSvangerskapspengervilkårDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    BekreftSvangerskapspengervilkårOppdaterer() {
        //cdi
    }

    @Inject
    public BekreftSvangerskapspengervilkårOppdaterer(HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    @Override
    public OppdateringResultat oppdater(BekreftSvangerskapspengervilkårDto dto, AksjonspunktOppdaterParameter param) {
        boolean vilkårOppfylt = dto.getAvslagskode() == null;
        lagHistorikkinnslag(dto.getBegrunnelse(), vilkårOppfylt);
        VilkårResultat.Builder vilkårBuilder = param.getVilkårResultatBuilder();
        if (vilkårOppfylt) {
            //TODO ikke gå til totrinn når innvilgesbrev fungerer
            vilkårBuilder.leggTilVilkårResultatManueltOppfylt(VilkårType.SVANGERSKAPSPENGERVILKÅR);
            return OppdateringResultat.utenTransisjon().medTotrinn().build();
        } else {
            Avslagsårsak avslagsårsak = Avslagsårsak.fraKode(dto.getAvslagskode());
            vilkårBuilder.leggTilVilkårResultatManueltIkkeOppfylt(VilkårType.SVANGERSKAPSPENGERVILKÅR, avslagsårsak);
            return OppdateringResultat.medFremoverHoppTotrinn(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
        }
    }

    private void lagHistorikkinnslag(String begrunnelse,
                                     boolean vilkårOppfylt) {
        HistorikkEndretFeltVerdiType tilVerdi = vilkårOppfylt ? HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT;

        historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse)
            .medEndretFelt(HistorikkEndretFeltType.SVANGERSKAPSPENGERVILKÅRET, null, tilVerdi)
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_SVANGERSKAPSPENGER);
    }
}
