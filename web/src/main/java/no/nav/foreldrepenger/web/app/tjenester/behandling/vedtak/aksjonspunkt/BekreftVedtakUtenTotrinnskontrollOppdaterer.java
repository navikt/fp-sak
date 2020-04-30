package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftVedtakUtenTotrinnskontrollDto.class, adapter=AksjonspunktOppdaterer.class)
class BekreftVedtakUtenTotrinnskontrollOppdaterer extends AbstractVedtaksbrevOverstyringshåndterer implements AksjonspunktOppdaterer<BekreftVedtakUtenTotrinnskontrollDto> {

    BekreftVedtakUtenTotrinnskontrollOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftVedtakUtenTotrinnskontrollOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                       HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                                       OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                                       VedtakTjeneste vedtakTjeneste,
                                                       BehandlingDokumentRepository behandlingDokumentRepository) {
        super(repositoryProvider, historikkApplikasjonTjeneste, opprettToTrinnsgrunnlag, vedtakTjeneste, behandlingDokumentRepository);
    }

    @Override
    public OppdateringResultat oppdater(BekreftVedtakUtenTotrinnskontrollDto dto, AksjonspunktOppdaterParameter param) {
        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            oppdaterFritekstVedtaksbrev(dto, param, builder);
            builder.medFremoverHopp(FellesTransisjoner.FREMHOPP_TIL_FATTE_VEDTAK);
        } else {
            fjernFritekstBrevHvisEksisterer(param.getBehandlingId());
        }
        return builder.build();
    }
}
