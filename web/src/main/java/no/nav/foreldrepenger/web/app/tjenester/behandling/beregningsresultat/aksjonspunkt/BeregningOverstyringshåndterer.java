package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.ytelse.beregning.es.BeregnYtelseTjenesteES;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringBeregningDto.class, adapter = Overstyringshåndterer.class)
public class BeregningOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringBeregningDto> {

    private LegacyESBeregningRepository beregningRepository;
    private BeregnYtelseTjenesteES beregnTjeneste;

    BeregningOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningOverstyringshåndterer(LegacyESBeregningRepository beregningRepository,
            HistorikkTjenesteAdapter historikkAdapter,
            BeregnYtelseTjenesteES beregnTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNING);
        this.beregningRepository = beregningRepository;
        this.beregnTjeneste = beregnTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringBeregningDto dto, Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {
        beregnTjeneste.overstyrTilkjentYtelseForEngangsstønad(behandling, dto.getBeregnetTilkjentYtelse());
        return OppdateringResultat.utenOveropp();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringBeregningDto dto) {
       lagHistorikkInnslagForOverstyrtBeregning(behandling.getId(), dto.getBegrunnelse(), dto.getBeregnetTilkjentYtelse());
    }

    private void lagHistorikkInnslagForOverstyrtBeregning(Long behandlingId, String begrunnelse, Long tilBeregning) {
        var sisteBeregning = beregningRepository.getSisteBeregning(behandlingId);
        if (sisteBeregning.isPresent()) {
            var fraBeregning = sisteBeregning.get().getOpprinneligBeregnetTilkjentYtelse();
            getHistorikkAdapter().tekstBuilder()
                .medHendelse(HistorikkinnslagType.OVERSTYRT)
                .medBegrunnelse(begrunnelse)
                .medSkjermlenke(SkjermlenkeType.BEREGNING_ENGANGSSTOENAD)
                .medEndretFelt(HistorikkEndretFeltType.OVERSTYRT_BEREGNING, fraBeregning, tilBeregning);
        }
    }
}
