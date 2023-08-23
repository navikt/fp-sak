package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder.formatString;

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

    public void opprettHistorikkinnslag(VurderUttakDokumentasjonDto dto,
                                        List<OppgittPeriodeEntitet> eksisterendePerioder) {
        var builder = historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_UTTAK_DOKUMENTASJON);
        for (var periode : dto.getVurderingBehov()) {
            var nyvurdering = VurderUttakDokumentasjonOppdaterer.mapVurdering(periode);
            var eksisterendeVurdering = finnEksisterendePerioder(eksisterendePerioder, periode.fom(), periode.tom())
                .map(OppgittPeriodeEntitet::getDokumentasjonVurdering).orElse(null);
            if (nyvurdering != null && (eksisterendeVurdering == null || erEndringerIperiode(nyvurdering, eksisterendeVurdering))) {
                opprettAvklaring(builder, nyvurdering, periode, eksisterendeVurdering);
            }
        }
    }

    private void opprettAvklaring(HistorikkInnslagTekstBuilder builder,
                                  DokumentasjonVurdering nyVurdering,
                                  DokumentasjonVurderingBehovDto periode,
                                  DokumentasjonVurdering eksisterendeVurdering) {
        var fraVerdi = Objects.equals(eksisterendeVurdering, nyVurdering) ? null : eksisterendeVurdering;
        var tekstperiode = String.format("%s - %s", formatString(periode.fom()), formatString(periode.tom()));
        builder.medEndretFelt(HistorikkEndretFeltType.UTTAKPERIODE_DOK_AVKLARING, tekstperiode,
            Optional.ofNullable(fraVerdi).map(Kodeverdi::getNavn).orElse(null), nyVurdering.getNavn());
    }

    private boolean erEndringerIperiode(DokumentasjonVurdering nyVurdering,
                                        DokumentasjonVurdering eksisterendeVurdering) {
        return !Objects.equals(nyVurdering, eksisterendeVurdering);
    }

    private Optional<OppgittPeriodeEntitet> finnEksisterendePerioder(List<OppgittPeriodeEntitet> eksisterendePerioder,
                                                                            LocalDate fom,
                                                                            LocalDate tom) {
        return eksisterendePerioder.stream()
            .filter(ep -> !ep.getFom().isBefore(fom) && !ep.getTom().isAfter(tom))
            .findFirst();
    }
}
