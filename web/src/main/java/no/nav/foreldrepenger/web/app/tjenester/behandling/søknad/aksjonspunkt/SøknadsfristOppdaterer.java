package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

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
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
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

        Builder vilkårBuilder = param.getVilkårResultatBuilder();
        if (dto.getErVilkarOk()) {
            vilkårBuilder.leggTilVilkårResultatManueltOppfylt(VilkårType.SØKNADSFRISTVILKÅRET);

            return OppdateringResultat.utenOveropp();
        } else {
            vilkårBuilder.leggTilVilkårResultatManueltIkkeOppfylt(VilkårType.SØKNADSFRISTVILKÅRET, VM_5007, Avslagsårsak.SØKT_FOR_SENT);
            vilkårBuilder.medVilkårResultatType(VilkårResultatType.AVSLÅTT);

            return OppdateringResultat.medFremoverHopp(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
        }
    }
}
