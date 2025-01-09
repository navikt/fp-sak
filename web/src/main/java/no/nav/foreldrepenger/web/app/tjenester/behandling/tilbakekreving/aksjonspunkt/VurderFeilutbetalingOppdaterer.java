package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFeilutbetalingDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFeilutbetalingOppdaterer implements AksjonspunktOppdaterer<VurderFeilutbetalingDto> {

    static final Set<TilbakekrevingVidereBehandling> LOVLIGE_VALG = Set.of(TilbakekrevingVidereBehandling.IGNORER_TILBAKEKREVING, TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING);
    private TilbakekrevingRepository repository;
    private TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger;
    private BehandlingRepository behandlingRepository;

    VurderFeilutbetalingOppdaterer() {
        //CDI proxy
    }

    @Inject
    public VurderFeilutbetalingOppdaterer(TilbakekrevingRepository repository, TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger,
                                          BehandlingRepository behandlingRepository) {
        this.repository = repository;
        this.historikkInnslagBygger = historikkInnslagBygger;
        this.behandlingRepository = behandlingRepository;
    }

    private static void valider(VurderFeilutbetalingDto dto) {
        if (!LOVLIGE_VALG.contains(dto.getVidereBehandling())) {
            throw new IllegalArgumentException("Verdien " + dto.getVidereBehandling() + " er ikke blant lovlige verdier: " + LOVLIGE_VALG);
        }
    }

    @Override
    public OppdateringResultat oppdater(VurderFeilutbetalingDto dto, AksjonspunktOppdaterParameter param) {
        valider(dto);
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var forrigeValg = repository.hent(behandlingId);
        var valg = TilbakekrevingValg.utenMulighetForInntrekk(dto.getVidereBehandling(), dto.getVarseltekst());
        repository.lagre(behandling, valg);

        historikkInnslagBygger.byggHistorikkinnslag(param.getRef(), forrigeValg, valg, dto.getBegrunnelse());

        return OppdateringResultat.utenOverhopp();
    }
}
