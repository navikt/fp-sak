package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftSvangerskapspengervilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftSvangerskapspengervilkårOppdaterer implements AksjonspunktOppdaterer<BekreftSvangerskapspengervilkårDto> {

    private Historikkinnslag2Repository historikkinnslag2Repository;

    BekreftSvangerskapspengervilkårOppdaterer() {
        //cdi
    }

    @Inject
    public BekreftSvangerskapspengervilkårOppdaterer(Historikkinnslag2Repository historikkinnslag2Repository) {
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    @Override
    public OppdateringResultat oppdater(BekreftSvangerskapspengervilkårDto dto, AksjonspunktOppdaterParameter param) {
        var vilkårOppfylt = dto.getAvslagskode() == null;
        lagHistorikkinnslag(param, dto.getBegrunnelse(), vilkårOppfylt);
        if (vilkårOppfylt) {
            return OppdateringResultat.utenTransisjon().leggTilManueltOppfyltVilkår(VilkårType.SVANGERSKAPSPENGERVILKÅR).build();
        } else {
            var avslagsårsak = Avslagsårsak.fraDefinertKode(dto.getAvslagskode())
                .orElseThrow(() -> new FunksjonellException("FP-MANGLER-ÅRSAK", "Ugyldig avslagsårsak", "Velg gyldig avslagsårsak"));

            return OppdateringResultat.utenTransisjon()
                .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
                .leggTilManueltAvslåttVilkår(VilkårType.SVANGERSKAPSPENGERVILKÅR, avslagsårsak)
                .medTotrinn()
                .build();
        }
    }

    private void lagHistorikkinnslag(AksjonspunktOppdaterParameter param, String begrunnelse, boolean vilkårOppfylt) {
        var tilVerdi = vilkårOppfylt ? "Vilkåret er oppfylt" : "Vilkåret er ikke oppfylt";
        historikkinnslag2Repository.lagre(new Historikkinnslag2.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(param.getFagsakId())
            .medTittel(SkjermlenkeType.PUNKT_FOR_SVANGERSKAPSPENGER)
            .addlinje(new HistorikkinnslagLinjeBuilder().til("Svangerskapsvilkåret", tilVerdi))
            .addLinje(begrunnelse)
            .build());
    }
}
