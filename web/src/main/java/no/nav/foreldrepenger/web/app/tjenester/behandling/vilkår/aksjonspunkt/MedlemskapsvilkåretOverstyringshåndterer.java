package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt.MedlemskapAksjonspunktFellesTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringMedlemskapsvilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringMedlemskapsvilkåretDto.class, adapter = Overstyringshåndterer.class)
public class MedlemskapsvilkåretOverstyringshåndterer implements Overstyringshåndterer<OverstyringMedlemskapsvilkåretDto> {

    private MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste;
    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    @Inject
    public MedlemskapsvilkåretOverstyringshåndterer(MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste,
                                                    InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        this.medlemskapAksjonspunktFellesTjeneste = medlemskapAksjonspunktFellesTjeneste;
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
    }

    MedlemskapsvilkåretOverstyringshåndterer() {
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringMedlemskapsvilkåretDto dto,
                                                  Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {
        var avslagsårsak = Avslagsårsak.fraKode(dto.getAvslagskode());
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var utfall = medlemskapAksjonspunktFellesTjeneste.oppdater(behandlingReferanse,
            avslagsårsak, dto.getOpphørFom(), dto.getBegrunnelse(), SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP);
        inngangsvilkårTjeneste.overstyrAksjonspunkt(behandling.getId(), VilkårType.MEDLEMSKAPSVILKÅRET, utfall,
            avslagsårsak == null ? Avslagsårsak.UDEFINERT : avslagsårsak, kontekst);
        if (VilkårUtfallType.OPPFYLT.equals(utfall)) {
            return OppdateringResultat.utenOverhopp();
        } else {
            return OppdateringResultat.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR);
        }
    }
}
