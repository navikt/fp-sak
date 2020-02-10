package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFeilutbetalingOgInntrekkDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFeilutbetalingOgInntrekkOppdaterer implements AksjonspunktOppdaterer<VurderFeilutbetalingOgInntrekkDto> {

    private TilbakekrevingRepository repository;
    private TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger;

    VurderFeilutbetalingOgInntrekkOppdaterer() {
        //CDI proxy
    }

    @Inject
    public VurderFeilutbetalingOgInntrekkOppdaterer(TilbakekrevingRepository repository, TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger) {
        this.repository = repository;
        this.historikkInnslagBygger = historikkInnslagBygger;
    }

    @Override
    public OppdateringResultat oppdater(VurderFeilutbetalingOgInntrekkDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        validerInput(dto);

        boolean vilkårOppfylt = dto.getErTilbakekrevingVilkårOppfylt();
        Boolean grunnerTilReduksjon = dto.getGrunnerTilReduksjon();

        Optional<TilbakekrevingValg> forrigeValg = repository.hent(behandling.getId());
        TilbakekrevingValg valg = brukInntrekk(dto)
            ? TilbakekrevingValg.medMulighetForInntrekk(vilkårOppfylt, grunnerTilReduksjon, TilbakekrevingVidereBehandling.INNTREKK)
            : TilbakekrevingValg.medMulighetForInntrekk(vilkårOppfylt, grunnerTilReduksjon, dto.getVidereBehandling());

        repository.lagre(behandling, valg);
        historikkInnslagBygger.byggHistorikkinnslag(behandling.getId(), forrigeValg, valg, dto.getBegrunnelse());
        return OppdateringResultat.utenOveropp();
    }

    private static boolean brukInntrekk(VurderFeilutbetalingOgInntrekkDto dto) {
        return dto.getErTilbakekrevingVilkårOppfylt() && !dto.getGrunnerTilReduksjon();
    }

    private static void validerInput(VurderFeilutbetalingOgInntrekkDto dto) {
        boolean vilkårOppfylt = dto.getErTilbakekrevingVilkårOppfylt();
        Boolean grunnerTilReduksjon = dto.getGrunnerTilReduksjon();

        if (vilkårOppfylt && grunnerTilReduksjon == null) {
            throw new IllegalArgumentException("Må oppgi ja/nei på grunner til reduksjon når tilbakekrevingvilkår er oppfylt");
        }
        if (!vilkårOppfylt && grunnerTilReduksjon != null) {
            throw new IllegalArgumentException("Skal ikke oppgi om det er grunner til reduksjon når tilbakekrevingvilkår ikke er oppfylt");
        }

        if (brukInntrekk(dto)) {
            if (dto.getVidereBehandling() != null && !dto.getVidereBehandling().equals(TilbakekrevingVidereBehandling.INNTREKK)) {
                throw new IllegalArgumentException("Når tilbakekrevingsvilkår er oppfylt og det ikke er grunner til reduksjon, må videre behandling være inntrekk, men var " + dto.getVidereBehandling());
            }
        } else {
            if (!VurderFeilutbetalingOppdaterer.LOVLIGE_VALG.contains(dto.getVidereBehandling())) {
                throw new IllegalArgumentException("Verdien " + dto.getVidereBehandling() + " er ikke blant lovlige verdier: " + VurderFeilutbetalingOppdaterer.LOVLIGE_VALG);
            }
        }
    }


}
