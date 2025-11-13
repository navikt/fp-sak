package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.tilNullable;

import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

public abstract class InngangsvilkårOverstyringshåndterer<T extends OverstyringAksjonspunktDto> implements Overstyringshåndterer<T> {

    private VilkårType vilkårType;
    private InngangsvilkårTjeneste inngangsvilkårTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    protected InngangsvilkårOverstyringshåndterer() {
        // for CDI proxy
    }

    public InngangsvilkårOverstyringshåndterer(VilkårType vilkårType,
                                               InngangsvilkårTjeneste inngangsvilkårTjeneste,
                                               HistorikkinnslagRepository historikkinnslagRepository) {
        this.vilkårType = vilkårType;
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat håndterOverstyring(T dto, BehandlingReferanse ref) {
        var utfall = dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        var avslagsårsak = dto.getErVilkarOk() ? Avslagsårsak.UDEFINERT : Avslagsårsak.fraDefinertKode(dto.getAvslagskode())
            .orElseThrow(() -> new FunksjonellException("FP-MANGLER-ÅRSAK", "Ugyldig avslagsårsak", "Velg gyldig avslagsårsak"));

        inngangsvilkårTjeneste.overstyrAksjonspunkt(ref.behandlingId(), vilkårType, utfall, avslagsårsak);

        if (utfall.equals(VilkårUtfallType.IKKE_OPPFYLT)) {
            return OppdateringResultat.medFremoverHopp(AksjonspunktOppdateringTransisjon.AVSLAG_VILKÅR);
        }

        return OppdateringResultat.utenOverhopp();
    }

    protected void lagHistorikkInnslagForOverstyrtVilkår(BehandlingReferanse ref, T dto, SkjermlenkeType skjermlenkeType) {
        var utfall = dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT.getNavn() : VilkårUtfallType.IKKE_OPPFYLT.getNavn();
        var avslagsårsak = Optional.ofNullable(dto.getAvslagskode()).flatMap(Avslagsårsak::fraDefinertKode).map(Avslagsårsak::getNavn).orElse(null);
        var historikkinnslag = new Historikkinnslag.Builder().medTittel(skjermlenkeType)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(ref.behandlingId())
            .medFagsakId(ref.fagsakId())
            .addLinje(dto.getAksjonspunktDefinisjon().getNavn())
            .addLinje(tilNullable(vilkårType.getNavn(), utfall))
            .addLinje(tilNullable("Avslagsårsak", avslagsårsak))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
