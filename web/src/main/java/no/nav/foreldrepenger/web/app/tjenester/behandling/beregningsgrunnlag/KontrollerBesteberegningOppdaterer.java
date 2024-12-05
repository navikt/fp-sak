package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

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
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.KontrollerBesteberegningDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = KontrollerBesteberegningDto.class, adapter = AksjonspunktOppdaterer.class)
public class KontrollerBesteberegningOppdaterer implements AksjonspunktOppdaterer<KontrollerBesteberegningDto> {

    private Historikkinnslag2Repository historikkinnslagRepository;

    protected KontrollerBesteberegningOppdaterer() {
        // CDI
    }

    @Inject
    public KontrollerBesteberegningOppdaterer(Historikkinnslag2Repository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(KontrollerBesteberegningDto dto, AksjonspunktOppdaterParameter param) {
        if (dto.getBesteberegningErKorrekt() == null || !dto.getBesteberegningErKorrekt()) {
            throw new IllegalStateException("Feil: Besteberegningen er ikke godkjent, ugyldig tilstand");
        }
        lagHistorikk(param.getRef(), dto);
        return OppdateringResultat.utenTransisjon().build();
    }

    private void lagHistorikk(BehandlingReferanse ref, KontrollerBesteberegningDto dto) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.BESTEBEREGNING)
            .addLinje(fraTilEquals("Godkjenning av automatisk besteberegning", null, dto.getBesteberegningErKorrekt()))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
