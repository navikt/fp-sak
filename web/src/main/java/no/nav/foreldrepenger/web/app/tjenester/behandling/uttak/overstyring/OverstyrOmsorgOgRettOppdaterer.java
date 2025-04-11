package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.RettighetType;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyrOmsorgOgRettDto;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrOmsorgOgRettDto.class, adapter = Overstyringshåndterer.class)
public class OverstyrOmsorgOgRettOppdaterer implements Overstyringshåndterer<OverstyrOmsorgOgRettDto> {
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
    public OppdateringResultat håndterOverstyring(OverstyrOmsorgOgRettDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var behandlingId = behandling.getId();
        var gjeldendeRettighet = hentGjeldendeRettighetType(behandlingId);
        var nyOverstyrtRettighet = dto.getRettighet();
        if (nyOverstyrtRettighet.equals(gjeldendeRettighet)) {
            throw new FunksjonellException("FP-919191", "Rettighet er allerede satt til " + nyOverstyrtRettighet.name() + " på behandlingen");
        }

        LOG.info("Overstyrer omsorg og rett for behandlingId {} fra {} til {}", behandlingId, gjeldendeRettighet, nyOverstyrtRettighet);
        ytelseFordelingTjeneste.overstyrRettighet(behandlingId, nyOverstyrtRettighet);
        oppretHistorikkinnslag(behandling, gjeldendeRettighet, nyOverstyrtRettighet);
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(true).build();
    }

    private void oppretHistorikkinnslag(Behandling behandling, RettighetType gammel, RettighetType ny) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.FAKTA_OMSORG_OG_RETT)
            .addLinje("Overstyring av omsorg og rett")
            .addLinje(HistorikkinnslagLinjeBuilder.fraTilEquals("Omsorg og rett", gammel.name(), ny.name()))
            .build();
        historikkRepository.lagre(historikkinnslag);
    }

    private RettighetType hentGjeldendeRettighetType(Long behandlingId) {
        return ytelseFordelingTjeneste.hentAggregat(behandlingId)
            .getGjeldendeRettighetstype()
            .orElseThrow(() -> new IllegalStateException("Finner ikke eksisterende rettighet type på behanlding ved overstyring av omsorg og rett! Skal ikke være lov!"));
    }
}
