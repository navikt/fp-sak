package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttaksperiodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttaksperioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@ApplicationScoped
public class EøsUttakFellesTjeneste {

    private EøsUttakRepository eøsUttakRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public EøsUttakFellesTjeneste(EøsUttakRepository eøsUttakRepository, HistorikkinnslagRepository historikkinnslagRepository) {
        this.eøsUttakRepository = eøsUttakRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    EøsUttakFellesTjeneste() {
        //CDI
    }

    public OppdateringResultat oppdater(BehandlingReferanse ref, List<EøsUttakPeriodeDto> perioder, String begrunnelse, boolean overstyring) {
        var behandlingId = ref.behandlingId();
        lagHistorikkinnslag(ref, perioder, begrunnelse, overstyring);
        var eøsUttaksperioderEntitet = new EøsUttaksperioderEntitet.Builder().leggTil(
                tilPerioderEntitet(perioder)) //lagrer tomme hvis ingen perioder for å vise saksbehandlers avklaring
            .build();
        eøsUttakRepository.lagreEøsUttak(behandlingId, eøsUttaksperioderEntitet);
        return OppdateringResultat.utenTransisjon().build();
    }

    private void lagHistorikkinnslag(BehandlingReferanse ref, List<EøsUttakPeriodeDto> perioder, String begrunnelse, boolean overstyring) {
        final Historikkinnslag historikkinnslag;
        if (perioder.isEmpty()) {
            historikkinnslag = historikkinnslagIngenPerioder(ref, begrunnelse, overstyring);
        } else {
            historikkinnslag = historikkinnslagRegistreringAvUttaksperioder(ref, perioder, begrunnelse, overstyring);
        }
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private List<EøsUttaksperiodeEntitet> tilPerioderEntitet(List<EøsUttakPeriodeDto> perioder) {
        return perioder.stream().map(EøsUttakFellesTjeneste::tilPeriodeEntitet).toList();
    }

    private static EøsUttaksperiodeEntitet tilPeriodeEntitet(EøsUttakPeriodeDto p) {
        return new EøsUttaksperiodeEntitet.Builder().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(p.fom(), p.tom()))
            .medTrekkdager(new Trekkdager(p.trekkdager()))
            .medTrekkonto(p.trekkonto())
            .build();
    }

    private static Historikkinnslag historikkinnslagIngenPerioder(BehandlingReferanse ref, String begrunnelse, boolean overstyring) {
        return new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_UTTAK_EØS)
            .addLinje(overstyring ? "Overstyrt vurdering:" : null)
            .addLinje("Avklart at annen forelder ikke har uttak i EØS")
            .addLinje(begrunnelse)
            .build();
    }

    private static Historikkinnslag historikkinnslagRegistreringAvUttaksperioder(BehandlingReferanse ref,
                                                                                 List<EøsUttakPeriodeDto> perioder,
                                                                                 String begrunnelse,
                                                                                 boolean overstyring) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        if (overstyring) {
            linjer.add(new HistorikkinnslagLinjeBuilder().tekst("Overstyrt vurdering:"));
        }
        linjer.add(new HistorikkinnslagLinjeBuilder().tekst("Registerert uttak for annen forelder i EØS"));
        for (var periode : perioder) {
            var trekkdager = new Trekkdager(periode.trekkdager());
            linjer.add(new HistorikkinnslagLinjeBuilder().tekst(String.format("%s - %s: Trekker", periode.fom(), periode.tom()))
                .bold(String.format("%s dager av %s", trekkdager, periode.trekkonto().getNavn().toLowerCase())));
        }
        return new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_UTTAK_EØS)
            .medLinjer(linjer)
            .addLinje(begrunnelse)
            .build();
    }
}
