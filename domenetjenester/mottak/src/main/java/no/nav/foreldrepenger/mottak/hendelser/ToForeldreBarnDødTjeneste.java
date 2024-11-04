package no.nav.foreldrepenger.mottak.hendelser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

/**
 *   Denne klassen brukes til å finne ut hvilken forelder som skal få opprettet
 *   en revurdering først dersom barnet dør,
 *   og begge foreldre har avsluttet behandling. Dette er for å hindre feilutbetaling.
 *   Dette gjøres ved å finne ut hvem som har nærmest uttak.
 */
@ApplicationScoped
public class ToForeldreBarnDødTjeneste {

    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private static final int ANTALL_DAGER_I_ET_ÅR = 365;
    // Dette bufferet brukes til å prioritere uttak som ligger fremover i tid
    // Et uttak som ligger f.eks 10 dager frem i tid regnes som "nærmere" enn ett uttak som ligger 1 dag tilbake i tid
    private static final int BUFFER = 14;

    ToForeldreBarnDødTjeneste() {
        // for CDI proxy
    }

    @Inject
    public ToForeldreBarnDødTjeneste(ForeldrepengerUttakTjeneste uttakTjeneste){
        this.uttakTjeneste = uttakTjeneste;
    }

    public Behandling finnBehandlingSomSkalRevurderes(Behandling behandlingF1, Behandling behandlingF2) {
        var uttaksPerioderF1 = finnPerioderMedUtbetaling(behandlingF1);
        if (hentAktivPeriodeHvisFinnes(uttaksPerioderF1).isPresent()) {
            return behandlingF1;
        }
        var uttaksPerioderF2 = finnPerioderMedUtbetaling(behandlingF2);
        if (hentAktivPeriodeHvisFinnes(uttaksPerioderF2).isPresent()) {
            return behandlingF2;
        }
        if (uttaksPerioderF1.isEmpty() && !uttaksPerioderF2.isEmpty()) {
            return behandlingF2;
        }
        if (uttaksPerioderF2.isEmpty()) {
            return behandlingF1;
        }
        return finnBehandlingMedNærmesteUttak(behandlingF1, uttaksPerioderF1, behandlingF2, uttaksPerioderF2);
    }

    private List<ForeldrepengerUttakPeriode> finnPerioderMedUtbetaling(Behandling behandling) {
        return uttakTjeneste.hentUttakHvisEksisterer(behandling.getId())
            .map(uttakResultat -> uttakResultat
                .getGjeldendePerioder().stream().filter(ForeldrepengerUttakPeriode::harUtbetaling)
                .toList())
            .orElse(Collections.emptyList());
    }

    private Optional<ForeldrepengerUttakPeriode> hentAktivPeriodeHvisFinnes(List<ForeldrepengerUttakPeriode> perioder) {
        return perioder.stream().filter(this::erAktivNå).findFirst();
    }

    private boolean erAktivNå(ForeldrepengerUttakPeriode periode) {
        var iDag = LocalDate.now();
        return periode.getFom().isBefore(iDag) && periode.getTom().isAfter(iDag);
    }

    private Behandling finnBehandlingMedNærmesteUttak(Behandling behandlingF1, List<ForeldrepengerUttakPeriode> uttaksPerioderF1,
                                                      Behandling behandlingF2, List<ForeldrepengerUttakPeriode> uttaksPerioderF2) {
        var uttaksGrenserF1 = finnUttaksGrenser(uttaksPerioderF1);
        var uttaksGrenserF2 = finnUttaksGrenser(uttaksPerioderF2);
        var avstanderF1 = uttaksGrenserF1.stream().map(this::avstandTilNåMedBuffer).toList();
        var avstanderF2 = uttaksGrenserF2.stream().map(this::avstandTilNåMedBuffer).toList();
        var minValueF1 = Collections.min(avstanderF1);
        var minValueF2 = Collections.min(avstanderF2);
        return minValueF1 < minValueF2 ? behandlingF1 : behandlingF2;
    }

    private List<LocalDate> finnUttaksGrenser(List<ForeldrepengerUttakPeriode> uttaksPerioder) {
        List<LocalDate> uttaksGrenser = new ArrayList<>();
        for (var periode : uttaksPerioder) {
            uttaksGrenser.add(periode.getFom());
            uttaksGrenser.add(periode.getTom());
        }
        return uttaksGrenser;
    }

    private Integer avstandTilNåMedBuffer(LocalDate date) {
        var iDag = LocalDate.now();
        if (date.isBefore(iDag)) {
            date = date.minusDays(BUFFER);
        }
        return Math.abs((iDag.getYear() - date.getYear())*ANTALL_DAGER_I_ET_ÅR + iDag.getDayOfYear() - date.getDayOfYear());
    }
}
