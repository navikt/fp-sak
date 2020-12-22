package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.Foreldreansvarsvilkår1AksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.Foreldreansvarsvilkår2AksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OmsorgsvilkårAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.omsorg.OmsorghendelseTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

/**
 * Håndterer oppdatering av omsorgsvilkåret.
 */
public abstract class OmsorgsvilkårAksjonspunktOppdaterer implements AksjonspunktOppdaterer<AvslagbartAksjonspunktDto> {

    private VilkårType vilkårType;
    private HistorikkTjenesteAdapter historikkAdapter;
    private OmsorghendelseTjeneste omsorghendelseTjeneste;

    protected OmsorgsvilkårAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    public OmsorgsvilkårAksjonspunktOppdaterer(OmsorghendelseTjeneste omsorghendelseTjeneste, HistorikkTjenesteAdapter historikkAdapter, VilkårType vilkårType) {
        this.omsorghendelseTjeneste = omsorghendelseTjeneste;
        this.historikkAdapter = historikkAdapter;
        this.vilkårType = vilkårType;
    }

    @Override
    public OppdateringResultat oppdater(AvslagbartAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        OppdateringResultat.Builder resultatBuilder = OppdateringResultat.utenTransisjon();
        String aksjonspunktKode = dto.getKode();
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(aksjonspunktKode);
        var skjermlenkeType = HistorikkAksjonspunktAdapter.getSkjermlenkeType(vilkårType, aksjonspunktKode);
        historikkAdapter.tekstBuilder()
            .medEndretFelt(getTekstKode(), null, dto.getErVilkarOk() ? HistorikkEndretFeltVerdiType.OPPFYLT : HistorikkEndretFeltVerdiType.IKKE_OPPFYLT)
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(skjermlenkeType);

        // Rydd opp gjenopprettede aksjonspunkt på andre omsorgsvilkår ved eventuelt tilbakehopp
        omsorghendelseTjeneste.aksjonspunktOmsorgsvilkår(behandling, aksjonspunktDefinisjon, resultatBuilder);

        if (dto.getErVilkarOk()) {
            resultatBuilder.leggTilVilkårResultat(vilkårType, VilkårUtfallType.OPPFYLT);
            return resultatBuilder.medTotrinn().build();
        } else {
            resultatBuilder.leggTilAvslåttVilkårResultat(vilkårType, Avslagsårsak.fraKode(dto.getAvslagskode()));

            return resultatBuilder.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR).build();
        }
    }

    protected abstract HistorikkEndretFeltType getTekstKode();

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = Foreldreansvarsvilkår1AksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class Foreldreansvarsvilkår1Oppdaterer extends OmsorgsvilkårAksjonspunktOppdaterer {

        Foreldreansvarsvilkår1Oppdaterer() {
            // for CDI proxy
        }

        @Inject
        public Foreldreansvarsvilkår1Oppdaterer(OmsorghendelseTjeneste omsorghendelseTjeneste,
                                                HistorikkTjenesteAdapter historikkAdapter) {
            super(omsorghendelseTjeneste, historikkAdapter, VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD);
        }

        @Override
        protected HistorikkEndretFeltType getTekstKode() {
            return HistorikkEndretFeltType.FORELDREANSVARSVILKARET;
        }

    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = Foreldreansvarsvilkår2AksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class Foreldreansvarsvilkår2Oppdaterer extends OmsorgsvilkårAksjonspunktOppdaterer {

        Foreldreansvarsvilkår2Oppdaterer() {
            // for CDI proxy
        }

        @Inject
        public Foreldreansvarsvilkår2Oppdaterer(OmsorghendelseTjeneste omsorghendelseTjeneste,
                                                HistorikkTjenesteAdapter historikkAdapter) {
            super(omsorghendelseTjeneste, historikkAdapter, VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD);
        }

        @Override
        protected HistorikkEndretFeltType getTekstKode() {
            return HistorikkEndretFeltType.FORELDREANSVARSVILKARET;
        }
    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = OmsorgsvilkårAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class OmsorgsvilkårOppdaterer extends OmsorgsvilkårAksjonspunktOppdaterer {

        OmsorgsvilkårOppdaterer() {
            // for CDI proxy
        }

        @Inject
        public OmsorgsvilkårOppdaterer(OmsorghendelseTjeneste omsorghendelseTjeneste, HistorikkTjenesteAdapter historikkAdapter) {
            super(omsorghendelseTjeneste, historikkAdapter, VilkårType.OMSORGSVILKÅRET);
        }

        @Override
        protected HistorikkEndretFeltType getTekstKode() {
            return HistorikkEndretFeltType.OMSORGSVILKAR;
        }

    }
}
