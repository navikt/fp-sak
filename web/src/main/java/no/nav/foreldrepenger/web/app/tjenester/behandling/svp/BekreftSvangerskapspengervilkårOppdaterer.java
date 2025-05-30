package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftSvangerskapspengervilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftSvangerskapspengervilkårOppdaterer implements AksjonspunktOppdaterer<BekreftSvangerskapspengervilkårDto> {

    private HistorikkinnslagRepository historikkinnslagRepository;

    BekreftSvangerskapspengervilkårOppdaterer() {
        //cdi
    }

    @Inject
    public BekreftSvangerskapspengervilkårOppdaterer(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
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
                .medFremoverHopp(AksjonspunktOppdateringTransisjon.AVSLAG_VILKÅR)
                .leggTilManueltAvslåttVilkår(VilkårType.SVANGERSKAPSPENGERVILKÅR, avslagsårsak)
                .medTotrinn()
                .build();
        }
    }

    private void lagHistorikkinnslag(AksjonspunktOppdaterParameter param, String begrunnelse, boolean vilkårOppfylt) {
        var tilVerdi = vilkårOppfylt ? "Vilkåret er oppfylt" : "Vilkåret er ikke oppfylt";
        var builder = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(param.getFagsakId())
            .medTittel(SkjermlenkeType.PUNKT_FOR_SVANGERSKAPSPENGER)
            .addLinje(new HistorikkinnslagLinjeBuilder().til("Svangerskapsvilkåret", tilVerdi));
        if (begrunnelse != null && !begrunnelse.isEmpty()) {
            builder.addLinje(begrunnelse);
        }
        historikkinnslagRepository.lagre(builder.build());
    }
}
