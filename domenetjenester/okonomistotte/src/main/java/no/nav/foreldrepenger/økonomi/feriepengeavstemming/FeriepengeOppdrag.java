package no.nav.foreldrepenger.økonomi.feriepengeavstemming;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;

import java.time.LocalDate;

public class FeriepengeOppdrag {
    private ØkonomiKodeKlassifik kodeKlassifik;
    private LocalDate utbetalesFom;
    private String mottakerPerson;
    private String mottakerRefusjon;
    private long sats;

    public FeriepengeOppdrag(ØkonomiKodeKlassifik kodeKlassifik, LocalDate utbetalesFom, long sats, String mottakerPerson, String mottakerRefusjon) {
        this.kodeKlassifik = kodeKlassifik;
        this.utbetalesFom = utbetalesFom;
        this.sats = sats;
        this.mottakerPerson = mottakerPerson;
        this.mottakerRefusjon = mottakerRefusjon;
    }

    public ØkonomiKodeKlassifik getKodeKlassifik() {
        return kodeKlassifik;
    }

    public LocalDate getUtbetalesFom() {
        return utbetalesFom;
    }

    public long getSats() {
        return sats;
    }

    public String getMottakerPerson() {
        return mottakerPerson;
    }

    public String getMottakerRefusjon() {
        return mottakerRefusjon;
    }

}
