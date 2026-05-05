package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.KontrollerAAPKombinertATFLDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = KontrollerAAPKombinertATFLDto.class, adapter = AksjonspunktOppdaterer.class)
public class KontrollerAAPKombinertATFLOppdaterer implements AksjonspunktOppdaterer<KontrollerAAPKombinertATFLDto> {

    private HistorikkinnslagRepository historikkinnslagRepository;

    protected KontrollerAAPKombinertATFLOppdaterer() {
        // CDI
    }

    @Inject
    public KontrollerAAPKombinertATFLOppdaterer(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(KontrollerAAPKombinertATFLDto dto, AksjonspunktOppdaterParameter param) {
        if (dto.getErBeregningenKorrekt() == null || !dto.getErBeregningenKorrekt()) {
            throw new IllegalStateException("Aksjonspunktet for AAP kombinert med AT/FL ble bekreftet uten at beregningen er godkjent, ugyldig tilstand");
        }
        lagHistorikk(param.getRef(), dto);
        return OppdateringResultat.utenTransisjon().build();
    }

    private void lagHistorikk(BehandlingReferanse ref, KontrollerAAPKombinertATFLDto dto) {
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.BEREGNING_FORELDREPENGER)
            .addLinje("Beregning for AAP kombinert med AT/FL er godkjent")
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
