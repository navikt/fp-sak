package no.nav.foreldrepenger.domene.modell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;


public class BeregningRefusjonOverstyring {

    private Arbeidsgiver arbeidsgiver;
    private LocalDate førsteMuligeRefusjonFom;

    BeregningRefusjonOverstyring() {
        // Hibernate
    }

    public BeregningRefusjonOverstyring(Arbeidsgiver arbeidsgiver, LocalDate førsteMuligeRefusjonFom) {
        Objects.requireNonNull(arbeidsgiver);
        Objects.requireNonNull(førsteMuligeRefusjonFom);
        this.førsteMuligeRefusjonFom = førsteMuligeRefusjonFom;
        this.arbeidsgiver = arbeidsgiver;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public LocalDate getFørsteMuligeRefusjonFom() {
        return førsteMuligeRefusjonFom;
    }
}
