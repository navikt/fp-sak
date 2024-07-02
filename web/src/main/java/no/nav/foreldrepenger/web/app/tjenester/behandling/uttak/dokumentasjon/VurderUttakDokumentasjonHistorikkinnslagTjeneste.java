package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import static no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder.formatString;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class VurderUttakDokumentasjonHistorikkinnslagTjeneste {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    @Inject
    public VurderUttakDokumentasjonHistorikkinnslagTjeneste(HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    VurderUttakDokumentasjonHistorikkinnslagTjeneste() {
        //CDI
    }

    public void opprettHistorikkinnslag(String begrunnelse, List<OppgittPeriodeEntitet> eksisterendePerioder , List<OppgittPeriodeEntitet> nyFordeling) {
        var builder = historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse)
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_UTTAK_DOKUMENTASJON);
        for (var periode : nyFordeling) {
            var eksisterendeInnslag = finnEksisterendePerioder(eksisterendePerioder, periode.getFom(), periode.getTom())
                .map(OppgittPeriodeEntitet::getDokumentasjonVurdering)
                .orElse(null);

            var nyttInnslag = periode.getDokumentasjonVurdering();
            if (nyttInnslag != null && !nyttInnslag.equals(eksisterendeInnslag)) {
                opprettAvklaring(builder, periode, eksisterendeInnslag);
            }
        }
    }

    private void opprettAvklaring(HistorikkInnslagTekstBuilder builder,
                                  OppgittPeriodeEntitet oppdatertPeriode,
                                  DokumentasjonVurdering eksisterendeDokumentasjonVurdering) {
        var tekstperiode = String.format("%s - %s", formatString(oppdatertPeriode.getFom()), formatString(oppdatertPeriode.getTom()));
        var fraVerdi = Optional.ofNullable(eksisterendeDokumentasjonVurdering).map(VurderUttakDokumentasjonHistorikkinnslagTjeneste::formaterStreng).orElse(null);
        var nyVerdi = formaterStreng(oppdatertPeriode.getDokumentasjonVurdering());
        builder.medEndretFelt(HistorikkEndretFeltType.UTTAKPERIODE_DOK_AVKLARING, tekstperiode, fraVerdi, nyVerdi);
    }

    private static String formaterStreng(DokumentasjonVurdering dokumentasjonVurdering) {
        if (DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT.equals(dokumentasjonVurdering.type()) && dokumentasjonVurdering.morsStillingsprosent() != null) {
            return String.format("%s (%s%% arbeid)", dokumentasjonVurdering.type().getNavn(), dokumentasjonVurdering.morsStillingsprosent());
        }
        return dokumentasjonVurdering.type().getNavn();
    }

    private Optional<OppgittPeriodeEntitet> finnEksisterendePerioder(List<OppgittPeriodeEntitet> eksisterendePerioder, LocalDate fom, LocalDate tom) {
        return eksisterendePerioder.stream().filter(ep -> !ep.getFom().isBefore(fom) && !ep.getTom().isAfter(tom)).findFirst();
    }
}
