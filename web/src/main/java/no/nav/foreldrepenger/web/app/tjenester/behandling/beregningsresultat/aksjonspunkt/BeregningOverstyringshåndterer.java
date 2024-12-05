package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.ytelse.beregning.es.BeregnYtelseTjenesteES;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringBeregningDto.class, adapter = Overstyringshåndterer.class)
public class BeregningOverstyringshåndterer implements Overstyringshåndterer<OverstyringBeregningDto> {

    private LegacyESBeregningRepository beregningRepository;
    private Historikkinnslag2Repository historikkinnslagRepository;
    private BeregnYtelseTjenesteES beregnTjeneste;

    BeregningOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningOverstyringshåndterer(LegacyESBeregningRepository beregningRepository,
                                          Historikkinnslag2Repository historikkinnslagRepository,
                                          BeregnYtelseTjenesteES beregnTjeneste) {
        this.beregningRepository = beregningRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.beregnTjeneste = beregnTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringBeregningDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        beregnTjeneste.overstyrTilkjentYtelseForEngangsstønad(behandling, dto.getBeregnetTilkjentYtelse());
        return OppdateringResultat.utenOverhopp();
    }

    @Override
    public void lagHistorikkInnslag(OverstyringBeregningDto dto, Behandling behandling) {
        lagHistorikkInnslagForOverstyrtBeregning(behandling, dto.getBegrunnelse(), dto.getBeregnetTilkjentYtelse());
    }

    private void lagHistorikkInnslagForOverstyrtBeregning(Behandling behandling, String begrunnelse, Long tilBeregning) {
        var behandlingId = behandling.getId();
        var sisteBeregning = beregningRepository.getSisteBeregning(behandlingId);
        if (sisteBeregning.isPresent()) {
            var fraBeregning = sisteBeregning.get().getOpprinneligBeregnetTilkjentYtelse();
            var historikkinnslag = new Historikkinnslag2.Builder()
                .medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medFagsakId(behandling.getFagsakId())
                .medBehandlingId(behandlingId)
                .medTittel(SkjermlenkeType.BEREGNING_ENGANGSSTOENAD)
                .addLinje(fraTilEquals("__Overstyrt beregning:__ Beløpet", fraBeregning, tilBeregning))
                .addLinje(begrunnelse)
                .build();
            historikkinnslagRepository.lagre(historikkinnslag);
        }
    }
}
