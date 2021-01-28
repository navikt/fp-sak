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

    public OppdragMottakerPrÅr getMottaker() {
        return new OppdragMottakerPrÅr(mottakerPerson == null ? mottakerRefusjon : mottakerPerson, utbetalesFom, mottakerPerson != null);
    }

    public class OppdragMottakerPrÅr {
        private String mottakerId;
        private LocalDate utbetalesFom;
        private boolean erSøker;

        public OppdragMottakerPrÅr(String mottakerId, LocalDate utbetalesFom, boolean erSøker) {
            this.mottakerId = mottakerId;
            this.utbetalesFom = utbetalesFom;
            this.erSøker = erSøker;
        }

        public String getMottakerId() {
            return mottakerId;
        }

        public LocalDate getUtbetalesFom() {
            return utbetalesFom;
        }

        public boolean isErSøker() {
            return erSøker;
        }
    }

}
