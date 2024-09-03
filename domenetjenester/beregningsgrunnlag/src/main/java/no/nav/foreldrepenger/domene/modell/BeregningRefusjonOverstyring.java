package no.nav.foreldrepenger.domene.modell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;


public class BeregningRefusjonOverstyring {

    private Arbeidsgiver arbeidsgiver;
    private LocalDate førsteMuligeRefusjonFom;
    private boolean erFristUtvidet;
    private List<BeregningRefusjonPeriode> refusjonPerioder = new ArrayList<>();

    public BeregningRefusjonOverstyring(Arbeidsgiver arbeidsgiver,
                                        LocalDate førsteMuligeRefusjonFom,
                                        boolean erFristUtvidet,
                                        List<BeregningRefusjonPeriode> refusjonPerioder) {
        Objects.requireNonNull(arbeidsgiver);
        this.arbeidsgiver = arbeidsgiver;
        if (erFristUtvidet) {
            Objects.requireNonNull(førsteMuligeRefusjonFom);
            this.førsteMuligeRefusjonFom = førsteMuligeRefusjonFom;
        }
        this.erFristUtvidet = erFristUtvidet;
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

    public boolean isErFristUtvidet() {
        return erFristUtvidet;
    }
}
