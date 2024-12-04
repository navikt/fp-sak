package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

public abstract class InngangsvilkårOverstyringshåndterer<T extends OverstyringAksjonspunktDto> implements Overstyringshåndterer<T> {

    private VilkårType vilkårType;
    private InngangsvilkårTjeneste inngangsvilkårTjeneste;
    private Historikkinnslag2Repository historikkinnslag2Repository;

    protected InngangsvilkårOverstyringshåndterer() {
        // for CDI proxy
    }

    public InngangsvilkårOverstyringshåndterer(VilkårType vilkårType,
                                               InngangsvilkårTjeneste inngangsvilkårTjeneste,
                                               Historikkinnslag2Repository historikkinnslag2Repository) {
        this.vilkårType = vilkårType;
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    @Override
    public OppdateringResultat håndterOverstyring(T dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var utfall = dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        var avslagsårsak = dto.getErVilkarOk() ? Avslagsårsak.UDEFINERT : Avslagsårsak.fraDefinertKode(dto.getAvslagskode())
            .orElseThrow(() -> new FunksjonellException("FP-MANGLER-ÅRSAK", "Ugyldig avslagsårsak", "Velg gyldig avslagsårsak"));

        inngangsvilkårTjeneste.overstyrAksjonspunkt(behandling.getId(), vilkårType, utfall, avslagsårsak, kontekst);

        if (utfall.equals(VilkårUtfallType.IKKE_OPPFYLT)) {
            return OppdateringResultat.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR);
        }

        return OppdateringResultat.utenOverhopp();
    }

    protected void lagHistorikkInnslagForOverstyrtVilkår(Behandling behandling,
                                                         String begrunnelse,
                                                         boolean vilkårOppfylt,
                                                         SkjermlenkeType skjermlenkeType) {
        var tilVerdi = vilkårOppfylt ? "Vilkåret er oppfylt" : "Vilkåret er ikke oppfylt";
        var fraVerdi = vilkårOppfylt ? "Vilkåret er ikke oppfylt" : "Vilkåret er oppfylt";
        var historikkinnslag = new Historikkinnslag2.Builder().medTittel(skjermlenkeType)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .addlinje(fraTilEquals("Overstyrt vurdering: Utfallet", fraVerdi, tilVerdi))
            .addLinje(begrunnelse)
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }
}
