package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.RettighetType;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyrOmsorgOgRettDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrOmsorgOgRettDto.class, adapter = AksjonspunktOppdaterer.class)
public class OverstyrOmsorgOgRettOppdaterer implements AksjonspunktOppdaterer<OverstyrOmsorgOgRettDto> {
    private static final Logger LOG = LoggerFactory.getLogger(OverstyrOmsorgOgRettOppdaterer.class);

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private HistorikkinnslagRepository historikkRepository;

    OverstyrOmsorgOgRettOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public OverstyrOmsorgOgRettOppdaterer(YtelseFordelingTjeneste ytelseFordelingTjeneste, HistorikkinnslagRepository historikkRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.historikkRepository = historikkRepository;
    }

    @Override
    public OppdateringResultat oppdater(OverstyrOmsorgOgRettDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var gjeldendeRettighet = hentGjeldendeRettighetType(behandlingId);
        var nyOverstyrtRettighet = dto.getRettighetType();
        LOG.info("Overstyrer omsorg og rett for behandlingId {} fra {} til {}", param.getRef().behandlingId(), gjeldendeRettighet, nyOverstyrtRettighet);
        ytelseFordelingTjeneste.endreOverstyrtRettighet(behandlingId, nyOverstyrtRettighet);
        oppretHistorikkinnslag(param, gjeldendeRettighet, nyOverstyrtRettighet);
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(true).build();
    }

    private void oppretHistorikkinnslag(AksjonspunktOppdaterParameter param, RettighetType gammel, RettighetType ny) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(param.getFagsakId())
            .medBehandlingId(param.getRef().behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OMSORG_OG_RETT)
            .addLinje("Overstyring av omsorg og rett")
            .addLinje(HistorikkinnslagLinjeBuilder.fraTilEquals("Omsorg og rett", gammel.name(), ny.name()))
            .build();
        historikkRepository.lagre(historikkinnslag);
    }

    public RettighetType hentGjeldendeRettighetType(Long behandlingId) {
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        if (yfa.getOverstyrtRettighetType().isEmpty()) {
            return yfa.getOverstyrtRettighetType().get();
        }

        return Optional.of(yfa.getOverstyrtRettighet())
            .orElse(Optional.of(yfa.getOppgittRettighet()))
            .map(OverstyrOmsorgOgRettOppdaterer::tilRettighetType)
            .orElseThrow(() -> new IllegalStateException("Finner ikke eksisterende rettighet type på behanlding ved overstyring av omsorg og rett! Skal ikke være lov!"));
    }

    public static RettighetType tilRettighetType(OppgittRettighetEntitet oppgittRettighetEntitet) {
        if (Boolean.TRUE.equals(oppgittRettighetEntitet.getHarAleneomsorgForBarnet())) {
            return RettighetType.ALENEOMSORG;
        } else if (Boolean.TRUE.equals(oppgittRettighetEntitet.getMorMottarUføretrygd())) {
            return RettighetType.BARE_FAR_RETT_MOR_UFØR;
        } else if (Boolean.TRUE.equals(oppgittRettighetEntitet.getAnnenForelderRettEØSNullable())) {
            return RettighetType.BEGGE_RETT_EØS;
        } else if (Boolean.TRUE.equals(oppgittRettighetEntitet.getHarAnnenForeldreRett())) {
            return RettighetType.BEGGE_RETT;
        }
        return RettighetType.BARE_SØKER_RETT;
    }

}
