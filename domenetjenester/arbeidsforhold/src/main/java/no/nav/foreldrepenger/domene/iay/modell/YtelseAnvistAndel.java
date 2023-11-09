package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Objects;
import java.util.Optional;

import no.nav.abakus.iaygrunnlag.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;


public class YtelseAnvistAndel extends BaseEntitet implements IndexKey {


    @ChangeTracked
    private Arbeidsgiver arbeidsgiver;

    @ChangeTracked
    private InternArbeidsforholdRef arbeidsforholdRef;

    @ChangeTracked
    private Beløp dagsats;

    @ChangeTracked
    private Stillingsprosent utbetalingsgradProsent;

    @ChangeTracked
    private Stillingsprosent refusjonsgradProsent;

    @ChangeTracked
    private Inntektskategori inntektskategori = Inntektskategori.UDEFINERT;

    public YtelseAnvistAndel() {
        // hibernate
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }


    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public void setArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    public Beløp getDagsats() {
        return dagsats;
    }

    public void setDagsats(Beløp dagsats) {
        this.dagsats = dagsats;
    }

    public Stillingsprosent getUtbetalingsgradProsent() {
        return utbetalingsgradProsent;
    }

    public void setUtbetalingsgradProsent(Stillingsprosent utbetalingsgradProsent) {
        this.utbetalingsgradProsent = utbetalingsgradProsent;
    }

    public Stillingsprosent getRefusjonsgradProsent() {
        return refusjonsgradProsent;
    }

    public void setRefusjonsgradProsent(Stillingsprosent refusjonsgradProsent) {
        this.refusjonsgradProsent = refusjonsgradProsent;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (YtelseAnvistAndel) o;
        return Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            dagsats.equals(that.dagsats) &&
            inntektskategori == that.inntektskategori &&
            utbetalingsgradProsent.equals(that.utbetalingsgradProsent) &&
            refusjonsgradProsent.equals(that.refusjonsgradProsent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, dagsats, inntektskategori, utbetalingsgradProsent, refusjonsgradProsent);
    }

    @Override
    public String toString() {
        return "YtelseAnvistAndel{" +
            "arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", dagsats=" + dagsats +
            ", utbetalingsgradProsent=" + utbetalingsgradProsent +
            ", refusjonsgradProsent=" + refusjonsgradProsent +
            ", inntektskategori=" + inntektskategori +
            '}';
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(arbeidsgiver);
    }
}
