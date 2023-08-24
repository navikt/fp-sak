package no.nav.foreldrepenger.domene.iay.modell;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

import java.time.LocalDate;
import java.util.*;

import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.*;

public class ArbeidsforholdOverstyring extends BaseEntitet implements IndexKey {

    private Arbeidsgiver arbeidsgiver;

    private InternArbeidsforholdRef arbeidsforholdRef;

    private InternArbeidsforholdRef nyArbeidsforholdRef;

    @ChangeTracked
    private ArbeidsforholdHandlingType handling = ArbeidsforholdHandlingType.UDEFINERT;

    private String begrunnelse;

    private String navn;

    private Stillingsprosent stillingsprosent;

    private List<ArbeidsforholdOverstyrtePerioder> arbeidsforholdOverstyrtePerioder = new ArrayList<>();

    private BekreftetPermisjon bekreftetPermisjon = new BekreftetPermisjon();

    ArbeidsforholdOverstyring() {
    }

    ArbeidsforholdOverstyring(ArbeidsforholdOverstyring arbeidsforholdOverstyringEntitet) {
        this.arbeidsgiver = arbeidsforholdOverstyringEntitet.getArbeidsgiver();
        this.arbeidsforholdRef = arbeidsforholdOverstyringEntitet.arbeidsforholdRef;
        this.handling = arbeidsforholdOverstyringEntitet.getHandling();
        this.nyArbeidsforholdRef = arbeidsforholdOverstyringEntitet.nyArbeidsforholdRef;
        this.bekreftetPermisjon = arbeidsforholdOverstyringEntitet.bekreftetPermisjon;
        this.navn = arbeidsforholdOverstyringEntitet.getArbeidsgiverNavn();
        this.stillingsprosent = arbeidsforholdOverstyringEntitet.getStillingsprosent();
        this.begrunnelse = arbeidsforholdOverstyringEntitet.getBegrunnelse();
        leggTilOverstyrtePerioder(arbeidsforholdOverstyringEntitet);
    }

    private void leggTilOverstyrtePerioder(ArbeidsforholdOverstyring arbeidsforholdOverstyringEntitet) {
        for (var overstyrtePerioderEntitet : arbeidsforholdOverstyringEntitet.getArbeidsforholdOverstyrtePerioder()) {
            var perioderEntitet = new ArbeidsforholdOverstyrtePerioder(overstyrtePerioderEntitet);
            perioderEntitet.setArbeidsforholdOverstyring(this);
            this.arbeidsforholdOverstyrtePerioder.add(perioderEntitet);
        }
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef != null ? arbeidsforholdRef : InternArbeidsforholdRef.nullRef();
    }

    void setArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    public ArbeidsforholdHandlingType getHandling() {
        return handling;
    }

    void setHandling(ArbeidsforholdHandlingType handling) {
        this.handling = handling;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    void setBeskrivelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    void leggTilOverstyrtPeriode(LocalDate fom, LocalDate tom) {
        var overstyrtPeriode = new ArbeidsforholdOverstyrtePerioder();
        overstyrtPeriode.setPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        overstyrtPeriode.setArbeidsforholdOverstyring(this);
        arbeidsforholdOverstyrtePerioder.add(overstyrtPeriode);
    }

    public List<ArbeidsforholdOverstyrtePerioder> getArbeidsforholdOverstyrtePerioder() {
        return arbeidsforholdOverstyrtePerioder;
    }

    public InternArbeidsforholdRef getNyArbeidsforholdRef() {
        return nyArbeidsforholdRef;
    }

    void setNyArbeidsforholdRef(InternArbeidsforholdRef nyArbeidsforholdRef) {
        this.nyArbeidsforholdRef =
            nyArbeidsforholdRef != null && !InternArbeidsforholdRef.nullRef().equals(nyArbeidsforholdRef) ? nyArbeidsforholdRef : null;
    }

    public Optional<BekreftetPermisjon> getBekreftetPermisjon() {
        if (bekreftetPermisjon.getStatus().equals(BekreftetPermisjonStatus.UDEFINERT)) {
            return Optional.empty();
        }
        return Optional.of(bekreftetPermisjon);
    }

    void setBekreftetPermisjon(BekreftetPermisjon bekreftetPermisjon) {
        this.bekreftetPermisjon = bekreftetPermisjon;
    }

    public boolean erOverstyrt() {
        return !Objects.equals(ArbeidsforholdHandlingType.BRUK, handling)
            || Objects.equals(ArbeidsforholdHandlingType.BRUK, handling) && !Objects.equals(bekreftetPermisjon.getStatus(),
            BekreftetPermisjonStatus.UDEFINERT);
    }

    public boolean kreverIkkeInntektsmelding() {
        return Set.of(LAGT_TIL_AV_SAKSBEHANDLER, BRUK_UTEN_INNTEKTSMELDING,
                BRUK_MED_OVERSTYRT_PERIODE, INNTEKT_IKKE_MED_I_BG).contains(handling);
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(arbeidsgiver, arbeidsforholdRef);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArbeidsforholdOverstyring that)) {
            return false;
        }
        return Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
                Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidsforholdRef);
    }

    @Override
    public String toString() {
        return "ArbeidsforholdOverstyringEntitet{" +
                "arbeidsgiver=" + arbeidsgiver +
                ", arbeidsforholdRef=" + arbeidsforholdRef +
                ", handling=" + handling +
                '}';
    }

    public Stillingsprosent getStillingsprosent() {
        return stillingsprosent;
    }

    public String getArbeidsgiverNavn() {
        return navn;
    }

    void setNavn(String navn) {
        this.navn = navn;
    }

    void setStillingsprosent(Stillingsprosent stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }
}
