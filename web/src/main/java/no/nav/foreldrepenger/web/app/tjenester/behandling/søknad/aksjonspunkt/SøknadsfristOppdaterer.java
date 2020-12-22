package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKNADSFRISTVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad.VM_5007;

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
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SoknadsfristAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class SøknadsfristOppdaterer implements AksjonspunktOppdaterer<SoknadsfristAksjonspunktDto> {

    private HistorikkTjenesteAdapter historikkAdapter;

    protected SøknadsfristOppdaterer() {
    }

    @Inject
    public SøknadsfristOppdaterer(HistorikkTjenesteAdapter historikkAdapter) {
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(SoknadsfristAksjonspunktDto dto, AksjonspunktOppdaterParameter  param) {
        historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.SOKNADSFRISTVILKARET, null, dto.getErVilkarOk() ? HistorikkEndretFeltVerdiType.OPPFYLT : HistorikkEndretFeltVerdiType.IKKE_OPPFYLT)
                .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
                .medSkjermlenke(SkjermlenkeType.SOEKNADSFRIST);

        if (dto.getErVilkarOk()) {
            return new OppdateringResultat.Builder()
                .leggTilVilkårResultat(SØKNADSFRISTVILKÅRET, VilkårUtfallType.OPPFYLT)
                .build();
        } else {
            return OppdateringResultat.utenTransisjon()
                .medFremoverHopp(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT)
                .leggTilAvslåttVilkårResultat(SØKNADSFRISTVILKÅRET,  Avslagsårsak.SØKT_FOR_SENT, VM_5007)
                .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
                .build();
        }
    }
}
