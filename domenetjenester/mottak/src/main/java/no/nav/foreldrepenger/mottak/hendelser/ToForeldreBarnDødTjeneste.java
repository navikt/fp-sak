package no.nav.foreldrepenger.mottak.hendelser;

import static no.nav.vedtak.util.FPDateUtil.iDag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;

/**
 *   Denne klassen brukes til å finne ut hvilken forelder som skal få opprettet en revurdering først dersom barnet dør,
 *   og begge foreldre har avsluttet behandling. Dette er for å hindre feilutbetaling.
 *   Dette gjøres ved å finne ut hvem som har nærmest uttak.
 */
@ApplicationScoped
public class ToForeldreBarnDødTjeneste {

    private UttakRepository uttakRepository;
    private static final int ANTALL_DAGER_I_ET_ÅR = 365;
    // Dette bufferet brukes til å prioritere uttak som ligger fremover i tid
    // Et uttak som ligger f.eks 10 dager frem i tid regnes som "nærmere" enn ett uttak som ligger 1 dag tilbake i tid
    private static final int BUFFER = 14;

    ToForeldreBarnDødTjeneste() {
        // for CDI proxy
    }

    @Inject
    public ToForeldreBarnDødTjeneste(UttakRepository uttakRepository){
        this.uttakRepository = uttakRepository;
    }

    public Behandling finnBehandlingSomSkalRevurderes(Behandling behandlingF1, Behandling behandlingF2){
        List<UttakResultatPeriodeEntitet> uttaksPerioderF1 = finnPerioderMedUtbetalingsProsentUlik0(behandlingF1);
        if(hentAktivPeriodeHvisFinnes(uttaksPerioderF1).isPresent()){
            return behandlingF1;
        }
        List<UttakResultatPeriodeEntitet> uttaksPerioderF2 = finnPerioderMedUtbetalingsProsentUlik0(behandlingF2);
        if(hentAktivPeriodeHvisFinnes(uttaksPerioderF2).isPresent()){
            return behandlingF2;
        }
        return finnBehandlingMedNærmesteUttak(behandlingF1, uttaksPerioderF1,behandlingF2,uttaksPerioderF2);
    }

    private List<UttakResultatPeriodeEntitet> finnPerioderMedUtbetalingsProsentUlik0(Behandling behandling){
        return uttakRepository.hentUttakResultat(behandling.getId())
            .getGjeldendePerioder()
            .getPerioder().stream().filter(this::uttakPeriodeHarUtbetalingsprosentStørreEnn0)
            .collect(Collectors.toList());
    }

    private boolean uttakPeriodeHarUtbetalingsprosentStørreEnn0(UttakResultatPeriodeEntitet periode){
        return periode.getAktiviteter().stream().anyMatch(aktivitet -> aktivitet.getUtbetalingsprosent().compareTo(BigDecimal.ZERO) > 0);
    }

    private Optional<UttakResultatPeriodeEntitet> hentAktivPeriodeHvisFinnes(List<UttakResultatPeriodeEntitet> perioder){
        return perioder.stream().filter(this::erAktivNå).findFirst();
    }

    private boolean erAktivNå(UttakResultatPeriodeEntitet periode){
        LocalDate iDag = iDag();
        return periode.getFom().isBefore(iDag) && periode.getTom().isAfter(iDag);
    }

    private Behandling finnBehandlingMedNærmesteUttak(Behandling behandlingF1, List<UttakResultatPeriodeEntitet> uttaksPerioderF1,
                                                      Behandling behandlingF2, List<UttakResultatPeriodeEntitet> uttaksPerioderF2){
        List<LocalDate> uttaksGrenserF1 = finnUttaksGrenser(uttaksPerioderF1);
        List<LocalDate> uttaksGrenserF2 = finnUttaksGrenser(uttaksPerioderF2);
        List<Integer> avstanderF1 = uttaksGrenserF1.stream().map(this::avstandTilNåMedBuffer).collect(Collectors.toList());
        List<Integer> avstanderF2 = uttaksGrenserF2.stream().map(this::avstandTilNåMedBuffer).collect(Collectors.toList());
        Integer minValueF1 = Collections.min(avstanderF1);
        Integer minValueF2 = Collections.min(avstanderF2);
        return minValueF1 < minValueF2 ? behandlingF1 : behandlingF2;
    }

    private List<LocalDate> finnUttaksGrenser(List<UttakResultatPeriodeEntitet> uttaksPerioder) {
        List<LocalDate> uttaksGrenser = new ArrayList<>();
        for(UttakResultatPeriodeEntitet periode : uttaksPerioder){
            uttaksGrenser.add(periode.getFom());
            uttaksGrenser.add(periode.getTom());
        }
        return uttaksGrenser;
    }

    private Integer avstandTilNåMedBuffer(LocalDate date){
        LocalDate iDag = iDag();
        if(date.isBefore(iDag)){
            date = date.minusDays(BUFFER);
        }
        return Math.abs((iDag.getYear() - date.getYear())*ANTALL_DAGER_I_ET_ÅR + iDag.getDayOfYear() - date.getDayOfYear());
    }
}
