package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import java.util.Optional;
import java.util.Set;

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
@DtoTilServiceAdapter(dto = VurderFeilutbetalingDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFeilutbetalingOppdaterer implements AksjonspunktOppdaterer<VurderFeilutbetalingDto> {

    static final Set<TilbakekrevingVidereBehandling> LOVLIGE_VALG = Set.of(TilbakekrevingVidereBehandling.IGNORER_TILBAKEKREVING, TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD);
    private TilbakekrevingRepository repository;
    private TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger;

    VurderFeilutbetalingOppdaterer() {
        //CDI proxy
    }

    @Inject
    public VurderFeilutbetalingOppdaterer(TilbakekrevingRepository repository, TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger) {
        this.repository = repository;
        this.historikkInnslagBygger = historikkInnslagBygger;
    }

    private static void valider(VurderFeilutbetalingDto dto) {
        if (!LOVLIGE_VALG.contains(dto.getVidereBehandling())) {
            throw new IllegalArgumentException("Verdien " + dto.getVidereBehandling() + " er ikke blant lovlige verdier: " + LOVLIGE_VALG);
        }
    }

    @Override
    public OppdateringResultat oppdater(VurderFeilutbetalingDto dto, AksjonspunktOppdaterParameter param) {
        valider(dto);
        Behandling behandling = param.getBehandling();
        Long behandlingId = param.getBehandlingId();
        Optional<TilbakekrevingValg> forrigeValg = repository.hent(behandlingId);
        TilbakekrevingValg valg = TilbakekrevingValg.utenMulighetForInntrekk(dto.getVidereBehandling(), dto.getVarseltekst());
        repository.lagre(behandling, valg);

        historikkInnslagBygger.byggHistorikkinnslag(behandlingId, forrigeValg, valg, dto.getBegrunnelse());

        return OppdateringResultat.utenOveropp();
    }
}
