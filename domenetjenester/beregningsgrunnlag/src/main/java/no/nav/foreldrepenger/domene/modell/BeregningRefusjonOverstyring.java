package no.nav.foreldrepenger.domene.modell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;


public class BeregningRefusjonOverstyring {

    private Arbeidsgiver arbeidsgiver;
    private LocalDate førsteMuligeRefusjonFom;
    private List<BeregningRefusjonPeriode> refusjonPerioder = new ArrayList<>();


    BeregningRefusjonOverstyring() {
        // Hibernate
    }

    public BeregningRefusjonOverstyring(Arbeidsgiver arbeidsgiver, LocalDate førsteMuligeRefusjonFom) {
        Objects.requireNonNull(arbeidsgiver);
        Objects.requireNonNull(førsteMuligeRefusjonFom);
        this.førsteMuligeRefusjonFom = førsteMuligeRefusjonFom;
        this.arbeidsgiver = arbeidsgiver;
    }

    public BeregningRefusjonOverstyring(Arbeidsgiver arbeidsgiver,
                                        LocalDate førsteMuligeRefusjonFom,
                                        List<BeregningRefusjonPeriode> refusjonPerioder) {
        this.arbeidsgiver = arbeidsgiver;
        this.førsteMuligeRefusjonFom = førsteMuligeRefusjonFom;
        this.refusjonPerioder = refusjonPerioder;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public LocalDate getFørsteMuligeRefusjonFom() {
        return førsteMuligeRefusjonFom;
    }

    public List<BeregningRefusjonPeriode> getRefusjonPerioder() {
        return refusjonPerioder;
    }
}
