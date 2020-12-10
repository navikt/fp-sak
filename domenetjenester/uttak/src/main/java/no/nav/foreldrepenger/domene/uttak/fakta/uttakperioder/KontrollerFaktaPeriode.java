package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;

public class KontrollerFaktaPeriode {

    private final boolean tidligOppstart;
    private final boolean bekreftet;
    private final UttakPeriodeVurderingType vurdering;
    private final OppgittPeriodeEntitet oppgittPeriode;
    private final List<PeriodeUttakDokumentasjonEntitet> dokumentertePerioder;

    private KontrollerFaktaPeriode(OppgittPeriodeEntitet oppgittPeriode,
                                   boolean bekreftet,
                                   boolean tidligOppstart,
                                   List<PeriodeUttakDokumentasjonEntitet> dokumentertePerioder,
                                   UttakPeriodeVurderingType periodeVurderingType) {
        this.bekreftet = bekreftet;
        this.vurdering = periodeVurderingType;
        this.oppgittPeriode = oppgittPeriode;
        this.tidligOppstart = tidligOppstart;
        this.dokumentertePerioder = dokumentertePerioder;
    }

    private static KontrollerFaktaPeriode ubekreftet(OppgittPeriodeEntitet oppgittPeriode, boolean tidligOppstart) {
        return new KontrollerFaktaPeriode(oppgittPeriode, false, tidligOppstart,
            Collections.emptyList(), oppgittPeriode.getPeriodeVurderingType());
    }

    public static KontrollerFaktaPeriode ubekreftet(OppgittPeriodeEntitet oppgittPeriode) {
        return ubekreftet(oppgittPeriode, false);
    }

    /**
     * Tiltenkt bruk ved aksjonspunkt når far/medmor søker før uke 7
     */
    public static KontrollerFaktaPeriode ubekreftetTidligOppstart(OppgittPeriodeEntitet søknadsperiode) {
        return ubekreftet(søknadsperiode, true);
    }

    public static KontrollerFaktaPeriode automatiskBekreftet(OppgittPeriodeEntitet oppgittPeriode, UttakPeriodeVurderingType vurderingType) {
        return new KontrollerFaktaPeriode(oppgittPeriode, true, false,
            Collections.emptyList(), vurderingType);
    }

    public static KontrollerFaktaPeriode manueltAvklart(OppgittPeriodeEntitet oppgittPeriode, List<PeriodeUttakDokumentasjonEntitet> dokumentertPeriode) {
        return new KontrollerFaktaPeriode(oppgittPeriode, true, false,
            dokumentertPeriode, oppgittPeriode.getPeriodeVurderingType());
    }

    public boolean erBekreftet() {
        return bekreftet;
    }

    public List<PeriodeUttakDokumentasjonEntitet> getDokumentertePerioder() {
        return dokumentertePerioder;
    }

    public UttakPeriodeVurderingType getVurdering() {
        return vurdering;
    }

    public boolean isTidligOppstart() {
        return tidligOppstart;
    }

    public OppgittPeriodeEntitet getOppgittPeriode() {
        return oppgittPeriode;
    }
}
