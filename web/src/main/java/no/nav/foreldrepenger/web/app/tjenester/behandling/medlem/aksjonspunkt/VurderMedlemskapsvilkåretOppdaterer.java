package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderMedlemskapDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderMedlemskapsvilkåretOppdaterer implements AksjonspunktOppdaterer<VurderMedlemskapDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    public VurderMedlemskapsvilkåretOppdaterer(HistorikkTjenesteAdapter historikkAdapter, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    VurderMedlemskapsvilkåretOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(VurderMedlemskapDto dto, AksjonspunktOppdaterParameter param) {
        var nyttUtfall = dto.getAvslagskode() == null ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

        if (VilkårUtfallType.OPPFYLT.equals(nyttUtfall)) {
            return oppfyltResultat();
        }
        if (!VilkårType.MEDLEMSKAPSVILKÅRET.getAvslagsårsaker().contains(dto.getAvslagskode())) {
            throw new IllegalArgumentException("Ugyldig avslagsårsak for medlemskapsvilkåret");
        }
        if (erOpphørEtterStp(dto, param.getBehandlingId())) {
            //TODD lagre opphørsdato
            return oppfyltResultat();
        }
        return OppdateringResultat.utenTransisjon()
            .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
            .leggTilManueltAvslåttVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, dto.getAvslagskode())
            .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
            .build();

    }

    private boolean erOpphørEtterStp(VurderMedlemskapDto dto, Long behandlingId) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
        return dto.getOpphørFom() != null && dto.getOpphørFom().isAfter(stp);
    }

    private static OppdateringResultat oppfyltResultat() {
        return new OppdateringResultat.Builder().leggTilManueltOppfyltVilkår(VilkårType.MEDLEMSKAPSVILKÅRET).build();
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, VilkårUtfallType nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
                .medEndretFelt(HistorikkEndretFeltType.MEDLEMSKAPSVILKÅRET, null, nyVerdi);

        historikkAdapter.tekstBuilder()
                .medBegrunnelse(begrunnelse, param.erBegrunnelseEndret())
                .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP);
    }
}
