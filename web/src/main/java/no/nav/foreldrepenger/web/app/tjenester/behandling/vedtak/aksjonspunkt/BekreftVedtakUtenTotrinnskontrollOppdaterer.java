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
@DtoTilServiceAdapter(dto = BekreftVedtakUtenTotrinnskontrollDto.class, adapter = AksjonspunktOppdaterer.class)
class BekreftVedtakUtenTotrinnskontrollOppdaterer extends AbstractVedtaksbrevOverstyringshåndterer implements AksjonspunktOppdaterer<BekreftVedtakUtenTotrinnskontrollDto> {

    @Inject
    public BekreftVedtakUtenTotrinnskontrollOppdaterer(BehandlingRepository behandlingRepository,
                                                       BehandlingsresultatRepository behandlingsresultatRepository,
                                                       HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                                       VedtakTjeneste vedtakTjeneste,
                                                       BehandlingDokumentRepository behandlingDokumentRepository,
                                                       OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag) {
        super(behandlingRepository, behandlingsresultatRepository, historikkApplikasjonTjeneste, vedtakTjeneste, behandlingDokumentRepository,
            opprettToTrinnsgrunnlag);
    }

    BekreftVedtakUtenTotrinnskontrollOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(BekreftVedtakUtenTotrinnskontrollDto dto, AksjonspunktOppdaterParameter param) {
        return håndter(dto, param, utledToTrinn(dto));
    }

    static boolean utledToTrinn(VedtaksbrevOverstyringDto dto) {
        return dto.isSkalBrukeOverstyrendeFritekstBrev();
    }
}
