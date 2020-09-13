package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakManueltAksjonspuntDto.class, adapter=AksjonspunktOppdaterer.class)
class ForeslåVedtakManueltAksjonspunktOppdaterer extends AbstractVedtaksbrevOverstyringshåndterer implements AksjonspunktOppdaterer<ForeslaVedtakManueltAksjonspuntDto> {

    ForeslåVedtakManueltAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakManueltAksjonspunktOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                      HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                                      VedtakTjeneste vedtakTjeneste,
                                                      BehandlingDokumentRepository behandlingDokumentRepository) {
        super(repositoryProvider, historikkApplikasjonTjeneste, vedtakTjeneste, behandlingDokumentRepository);
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakManueltAksjonspuntDto dto, AksjonspunktOppdaterParameter param) {
        return standardHåndteringUtenTotrinn(dto, param);
    }
}
