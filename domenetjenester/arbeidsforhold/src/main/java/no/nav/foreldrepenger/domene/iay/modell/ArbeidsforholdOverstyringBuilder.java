package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

public class ArbeidsforholdOverstyringBuilder {

    private final ArbeidsforholdOverstyring kladd;
    private final boolean oppdatering;

    private ArbeidsforholdOverstyringBuilder(ArbeidsforholdOverstyring kladd, boolean oppdatering) {
        this.kladd = kladd;
        this.oppdatering = oppdatering;
    }

    static ArbeidsforholdOverstyringBuilder ny() {
        return new ArbeidsforholdOverstyringBuilder(new ArbeidsforholdOverstyring(), false);
    }

    static ArbeidsforholdOverstyringBuilder oppdatere(ArbeidsforholdOverstyring oppdatere) {
        return new ArbeidsforholdOverstyringBuilder(new ArbeidsforholdOverstyring(oppdatere), true);
    }

    public static ArbeidsforholdOverstyringBuilder oppdatere(Optional<ArbeidsforholdOverstyring> oppdatere) {
        return oppdatere.map(ArbeidsforholdOverstyringBuilder::oppdatere).orElseGet(ArbeidsforholdOverstyringBuilder::ny);
    }

    public ArbeidsforholdOverstyringBuilder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        kladd.setArbeidsgiver(arbeidsgiver);
        return this;
    }

    public ArbeidsforholdOverstyringBuilder medArbeidsforholdRef(InternArbeidsforholdRef ref) {
        kladd.setArbeidsforholdRef(ref);
        return this;
    }

    public ArbeidsforholdOverstyringBuilder medNyArbeidsforholdRef(InternArbeidsforholdRef ref) {
        kladd.setNyArbeidsforholdRef(ref);
        return this;
    }

    public ArbeidsforholdOverstyringBuilder medHandling(ArbeidsforholdHandlingType type) {
        kladd.setHandling(type);
        return this;
    }

    public ArbeidsforholdOverstyringBuilder medAngittArbeidsgiverNavn(String navn) {
        kladd.setNavn(navn);
        return this;
    }

    public ArbeidsforholdOverstyringBuilder medAngittStillingsprosent(Stillingsprosent stillingsprosent) {
        kladd.setStillingsprosent(stillingsprosent);
        return this;
    }

    public ArbeidsforholdOverstyringBuilder medBeskrivelse(String beskrivelse) {
        kladd.setBeskrivelse(beskrivelse);
        return this;
    }

    public ArbeidsforholdOverstyringBuilder leggTilOverstyrtPeriode(LocalDate fom, LocalDate tom){
        kladd.leggTilOverstyrtPeriode(fom, tom);
        return this;
    }

    public ArbeidsforholdOverstyringBuilder medBekreftetPermisjon(BekreftetPermisjon bekreftetPermisjon){
        kladd.setBekreftetPermisjon(bekreftetPermisjon);
        return this;
    }

    public ArbeidsforholdOverstyring build() {
        return kladd;
    }

    boolean isOppdatering() {
        return oppdatering;
    }

}
