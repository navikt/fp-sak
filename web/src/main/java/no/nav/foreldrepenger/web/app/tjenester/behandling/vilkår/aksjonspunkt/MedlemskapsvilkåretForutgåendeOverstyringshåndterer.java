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
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt.MedlemskapAksjonspunktFellesTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringForutgåendeMedlemskapsvilkårDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringForutgåendeMedlemskapsvilkårDto.class, adapter = Overstyringshåndterer.class)
public class MedlemskapsvilkåretForutgåendeOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringForutgåendeMedlemskapsvilkårDto> {

    private MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste;
    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    @Inject
    public MedlemskapsvilkåretForutgåendeOverstyringshåndterer(MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste,
                                                               InngangsvilkårTjeneste inngangsvilkårTjeneste,
                                                               HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        super(historikkTjenesteAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET);
        this.medlemskapAksjonspunktFellesTjeneste = medlemskapAksjonspunktFellesTjeneste;
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
    }

    MedlemskapsvilkåretForutgåendeOverstyringshåndterer() {
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringForutgåendeMedlemskapsvilkårDto dto,
                                                  Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            throw new IllegalArgumentException("Utviklerfeil: Prøver overstyre forutgående medlemskap for annen ytelse enn engangsstønad");
        }
        var avslagsårsak = Avslagsårsak.fraKode(dto.getAvslagskode());
        var utfall = medlemskapAksjonspunktFellesTjeneste.oppdaterForutgående(kontekst.getBehandlingId(), avslagsårsak, dto.getMedlemFom(),
            dto.getBegrunnelse(), SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP);
        inngangsvilkårTjeneste.overstyrAksjonspunkt(kontekst.getBehandlingId(), VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, utfall,
            avslagsårsak == null ? Avslagsårsak.UDEFINERT : avslagsårsak, kontekst);
        if (VilkårUtfallType.OPPFYLT.equals(utfall)) {
            return OppdateringResultat.utenOverhopp();
        } else {
            return OppdateringResultat.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR);
        }
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringForutgåendeMedlemskapsvilkårDto dto) {

    }
}
