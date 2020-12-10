package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerAktivitetskravDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerAktivitetskravPeriodeDto;

@ApplicationScoped
public class KontrollerAktivitetskravHistorikkinnslagTjeneste {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    @Inject
    public KontrollerAktivitetskravHistorikkinnslagTjeneste(HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    KontrollerAktivitetskravHistorikkinnslagTjeneste() {
        //CDI
    }

    public void opprettHistorikkinnslag(Long behandlingId,
                                        KontrollerAktivitetskravDto dto,
                                        List<AktivitetskravPeriodeEntitet> eksisterendePerioder) {
        var builder = historikkTjenesteAdapter.tekstBuilder();
        for (var periode : dto.getAvklartePerioder()) {
            var eksisterendePeriode = finnEksisterendePerioder(eksisterendePerioder, periode.getFom(),
                periode.getTom());
            if (eksisterendePeriode.isPresent()) {
                if (erEndringerIperiode(periode, eksisterendePeriode.get())) {
                    opprettDel(builder, periode, eksisterendePeriode.get().getAvklaring());
                }
            } else {
                opprettDel(builder, periode, null);
            }
        }
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingId, HistorikkinnslagType.AVKLART_AKTIVITETSKRAV);
    }

    private void opprettDel(HistorikkInnslagTekstBuilder builder,
                            KontrollerAktivitetskravPeriodeDto periode,
                            KontrollerAktivitetskravAvklaring eksisterendeVerdi) {
        builder.ferdigstillHistorikkinnslagDel();
        builder.medOpplysning(HistorikkOpplysningType.UTTAK_PERIODE_FOM, periode.getFom());
        builder.medOpplysning(HistorikkOpplysningType.UTTAK_PERIODE_TOM, periode.getTom());
        builder.medEndretFelt(HistorikkEndretFeltType.AKTIVITETSKRAV_AVKLARING, null, eksisterendeVerdi, periode.getAvklaring());
        builder.medBegrunnelse(periode.getBegrunnelse());
        builder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_AKTIVITETSKRAV);
    }

    private boolean erEndringerIperiode(KontrollerAktivitetskravPeriodeDto periode,
                                        AktivitetskravPeriodeEntitet aktivitetskravPeriodeEntitet) {
        return !periode.getAvklaring().equals(aktivitetskravPeriodeEntitet.getAvklaring()) || !periode.getBegrunnelse()
            .equals(aktivitetskravPeriodeEntitet.getBegrunnelse());
    }

    private Optional<AktivitetskravPeriodeEntitet> finnEksisterendePerioder(List<AktivitetskravPeriodeEntitet> eksisterendePerioder,
                                                                            LocalDate fom,
                                                                            LocalDate tom) {
        return eksisterendePerioder.stream()
            .filter(ep -> ep.getTidsperiode().getFomDato().isEqual(fom) && ep.getTidsperiode().getTomDato().isEqual(tom))
            .findFirst();
    }
}
