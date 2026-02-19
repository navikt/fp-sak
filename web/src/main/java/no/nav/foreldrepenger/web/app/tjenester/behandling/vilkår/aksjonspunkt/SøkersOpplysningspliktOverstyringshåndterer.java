package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringSokersOpplysingspliktDto;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringSokersOpplysingspliktDto.class, adapter = Overstyringshåndterer.class)
public class SøkersOpplysningspliktOverstyringshåndterer implements Overstyringshåndterer<OverstyringSokersOpplysingspliktDto> {

    private HistorikkinnslagRepository historikkinnslagRepository;
    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    SøkersOpplysningspliktOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public SøkersOpplysningspliktOverstyringshåndterer(HistorikkinnslagRepository historikkinnslagRepository,
                                                       InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;

    }

    @Override
    public void lagHistorikkInnslag(OverstyringSokersOpplysingspliktDto dto, BehandlingReferanse ref) {
        leggTilEndretFeltIHistorikkInnslag(ref.fagsakId(), ref.behandlingId(), dto);
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringSokersOpplysingspliktDto dto, BehandlingReferanse ref) {

        if (!dto.getErVilkårOk() && ref.erRevurdering()) {
            throw new FunksjonellException("FP-093925", "Kan ikke avslå revurdering med opplysningsplikt.",
                "Overstyr ett av de andre vilkårene.");
        }
        var utfall = dto.getErVilkårOk() ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        inngangsvilkårTjeneste.overstyrAksjonspunktForSøkersopplysningsplikt(ref.behandlingId(), utfall);

        var builder = OppdateringResultat.utenTransisjon();
        if (VilkårUtfallType.OPPFYLT.equals(utfall)) {
            return builder.build();
        }
        builder.medFremoverHopp(AksjonspunktOppdateringTransisjon.AVSLAG_VILKÅR);
        return builder.build();
    }

    private void leggTilEndretFeltIHistorikkInnslag(Long fagsakId, Long behandlingId, OverstyringSokersOpplysingspliktDto dto) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medTittel(SkjermlenkeType.OPPLYSNINGSPLIKT)
            .addLinje(fraTilEquals("Søkers opplysningsplikt", null, dto.getErVilkårOk() ? "oppfylt" : "ikke oppfylt"))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
