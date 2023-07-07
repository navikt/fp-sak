package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslåVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer extends AbstractVedtaksbrevOverstyringshåndterer implements AksjonspunktOppdaterer<ForeslåVedtakAksjonspunktDto> {

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(BehandlingRepository behandlingRepository,
                                               BehandlingsresultatRepository behandlingsresultatRepository,
                                               HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                               OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                               VedtakTjeneste vedtakTjeneste,
                                               BehandlingDokumentRepository behandlingDokumentRepository) {
        super(behandlingRepository, behandlingsresultatRepository, historikkApplikasjonTjeneste, vedtakTjeneste, behandlingDokumentRepository,
            opprettToTrinnsgrunnlag);
    }

    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(ForeslåVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        return håndter(dto, param, true);
    }
}
