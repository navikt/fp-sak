package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurdereInntektsmeldingFørVedtakDto.class, adapter=AksjonspunktOppdaterer.class)
class VurderInntektsmeldingFørVedtakOppdaterer implements AksjonspunktOppdaterer<VurdereInntektsmeldingFørVedtakDto> {

    private Historikkinnslag2Repository historikkinnslagRepository;

    VurderInntektsmeldingFørVedtakOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderInntektsmeldingFørVedtakOppdaterer(Historikkinnslag2Repository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurdereInntektsmeldingFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        lagHistorikkinnslag(param.getRef());
        return OppdateringResultat.utenOverhopp();
    }

    private void lagHistorikkinnslag(BehandlingReferanse behandlingReferanse) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(behandlingReferanse.fagsakId())
            .medBehandlingId(behandlingReferanse.behandlingId())
            .medTittel("Oppgave før vedtak")
            .addTekstlinje("Vurder beregningsgrunnlag og klage ref ny inntektsmelding")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
