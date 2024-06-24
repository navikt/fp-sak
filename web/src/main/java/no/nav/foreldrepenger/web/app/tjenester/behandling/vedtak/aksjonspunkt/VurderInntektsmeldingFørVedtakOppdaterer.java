package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurdereInntektsmeldingFørVedtakDto.class, adapter=AksjonspunktOppdaterer.class)
class VurderInntektsmeldingFørVedtakOppdaterer implements AksjonspunktOppdaterer<VurdereInntektsmeldingFørVedtakDto> {

    private HistorikkTjenesteAdapter historikkAdapter;

    VurderInntektsmeldingFørVedtakOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderInntektsmeldingFørVedtakOppdaterer(HistorikkTjenesteAdapter historikkAdapter) {
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(VurdereInntektsmeldingFørVedtakDto dto, AksjonspunktOppdaterParameter param) {
        var tekstBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.OPPGAVE_VEDTAK)
            .medBegrunnelse("Vurder beregningsgrunnlag og klage ref ny inntektsmelding", true);

        var innslag = new Historikkinnslag();
        innslag.setType(HistorikkinnslagType.OPPGAVE_VEDTAK);
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setBehandlingId(param.getBehandlingId());
        tekstBuilder.build(innslag);
        historikkAdapter.lagInnslag(innslag);
        return OppdateringResultat.utenOverhopp();
    }
}
