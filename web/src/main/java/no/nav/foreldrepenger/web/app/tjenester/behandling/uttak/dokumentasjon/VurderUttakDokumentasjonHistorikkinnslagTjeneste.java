package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;

@ApplicationScoped
public class VurderUttakDokumentasjonHistorikkinnslagTjeneste {

    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public VurderUttakDokumentasjonHistorikkinnslagTjeneste(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    VurderUttakDokumentasjonHistorikkinnslagTjeneste() {
        //CDI
    }

    public void opprettHistorikkinnslag(BehandlingReferanse ref,
                                        String begrunnelse,
                                        List<OppgittPeriodeEntitet> eksisterendePerioder,
                                        List<OppgittPeriodeEntitet> nyFordeling) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        for (var periode : nyFordeling) {
            opprettAvklaring(eksisterendePerioder, periode).ifPresent(linjer::add);
        }
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OM_UTTAK_DOKUMENTASJON)
            .medLinjer(linjer)
            .addLinje(begrunnelse)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private static Optional<HistorikkinnslagLinjeBuilder> opprettAvklaring(List<OppgittPeriodeEntitet> eksisterendePerioder,
                                                                           OppgittPeriodeEntitet periode) {
        var eksisterendeInnslag = finnEksisterendePerioder(eksisterendePerioder, periode.getFom(), periode.getTom()).map(
            OppgittPeriodeEntitet::getDokumentasjonVurdering).orElse(null);
        var nyttInnslag = periode.getDokumentasjonVurdering();

        if (nyttInnslag == null || nyttInnslag.equals(eksisterendeInnslag)) {
            return Optional.empty();
        }

        var tekstperiode = HistorikkinnslagLinjeBuilder.format(periode.getTidsperiode());
        var fraVerdi = Optional.ofNullable(eksisterendeInnslag).map(VurderUttakDokumentasjonHistorikkinnslagTjeneste::formaterStreng).orElse(null);
        var nyVerdi = formaterStreng(nyttInnslag);
        return Optional.ofNullable(fraTilEquals(String.format("Avklart dokumentasjon for periode %s", tekstperiode), fraVerdi, nyVerdi));
    }

    private static String formaterStreng(DokumentasjonVurdering dokumentasjonVurdering) {
        if (DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT.equals(dokumentasjonVurdering.type())
            && dokumentasjonVurdering.morsStillingsprosent() != null) {
            return String.format("%s (%s%% arbeid)", dokumentasjonVurdering.type().getNavn(), dokumentasjonVurdering.morsStillingsprosent());
        }
        return dokumentasjonVurdering.type().getNavn();
    }

    private static Optional<OppgittPeriodeEntitet> finnEksisterendePerioder(List<OppgittPeriodeEntitet> eksisterendePerioder,
                                                                            LocalDate fom,
                                                                            LocalDate tom) {
        return eksisterendePerioder.stream().filter(ep -> !ep.getFom().isBefore(fom) && !ep.getTom().isAfter(tom)).findFirst();
    }
}
