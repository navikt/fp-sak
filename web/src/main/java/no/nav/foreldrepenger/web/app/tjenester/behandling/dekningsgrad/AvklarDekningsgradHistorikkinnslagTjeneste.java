package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class AvklarDekningsgradHistorikkinnslagTjeneste {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    @Inject
    public AvklarDekningsgradHistorikkinnslagTjeneste(HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    AvklarDekningsgradHistorikkinnslagTjeneste() {
        //CDI
    }


    public void opprettHistorikkinnslag(AvklarDekingsgradDto dto) {
        historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse())
            .medSkjermlenke(SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER)
            .medEndretFelt(HistorikkEndretFeltType.DEKNINGSGRAD, null, dto.avklartDekningsgrad());
    }
}
