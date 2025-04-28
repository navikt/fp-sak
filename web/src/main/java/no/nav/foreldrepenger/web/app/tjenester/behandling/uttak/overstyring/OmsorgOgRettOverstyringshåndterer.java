package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyrOmsorgOgRettDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrOmsorgOgRettDto.class, adapter = Overstyringshåndterer.class)
public class OmsorgOgRettOverstyringshåndterer implements Overstyringshåndterer<OverstyrOmsorgOgRettDto> {
    private static final Logger LOG = LoggerFactory.getLogger(OmsorgOgRettOverstyringshåndterer.class);

    private HistorikkinnslagRepository historikkRepository;
    private FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste;

    OmsorgOgRettOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public OmsorgOgRettOverstyringshåndterer(HistorikkinnslagRepository historikkRepository,
                                             FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste) {
        this.historikkRepository = historikkRepository;
        this.faktaOmsorgRettTjeneste = faktaOmsorgRettTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrOmsorgOgRettDto dto, BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var nyOverstyrtRettighet = dto.getRettighetstype();

        LOG.info("Overstyrer rettighetstype for behandlingId {} til {}", behandlingId, nyOverstyrtRettighet);
        faktaOmsorgRettTjeneste.overstyrRettighet(behandlingId, nyOverstyrtRettighet);
        oppretHistorikkinnslag(ref, nyOverstyrtRettighet, dto.getBegrunnelse());
        return OppdateringResultat.utenOverhopp();
    }

    private void oppretHistorikkinnslag(BehandlingReferanse ref,
                                        Rettighetstype nyOverstyrtRettighet,
                                        String begrunnelse) {
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OMSORG_OG_RETT)
            .addLinje(new HistorikkinnslagLinjeBuilder().til("Rettighetstype", tekstverdi(nyOverstyrtRettighet)))
            .addLinje(begrunnelse)
            .build();
        historikkRepository.lagre(historikkinnslag);
    }

    private String tekstverdi(Rettighetstype rettighetstype) {
        return switch (rettighetstype) {
            case ALENEOMSORG -> "Aleneomsorg";
            case BEGGE_RETT -> "Begge rett";
            case BEGGE_RETT_EØS -> "Begge rett EØS";
            case BARE_FAR_RETT -> "Bare far/medmor rett";
            case BARE_MOR_RETT -> "Bare mor rett";
            case BARE_FAR_RETT_MOR_UFØR -> "Bare far/medmor rett mor ufør";
            case null -> null;
        };
    }
}
