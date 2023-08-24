package no.nav.foreldrepenger.domene.iay.modell;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;

import java.util.*;
import java.util.stream.Collectors;

public class Inntekt extends BaseEntitet implements IndexKey {

    private AktørInntekt aktørInntekt;

    private Arbeidsgiver arbeidsgiver;

    private InntektsKilde inntektsKilde;

    /*
     * TODO: Bør InntektspostEntitet splittes? inneholder litt forskjellig felter
     * avhengig av kilde.
     */
    @ChangeTracked
    private Set<Inntektspost> inntektspost = new LinkedHashSet<>();

    Inntekt() {
        // hibernate
    }

    /**
     * Copy ctor
     */
    Inntekt(Inntekt inntektMal) {
        this.inntektsKilde = inntektMal.getInntektsKilde();
        this.arbeidsgiver = inntektMal.getArbeidsgiver();
        this.inntektspost = inntektMal.getAlleInntektsposter().stream().map(ip -> {
            var inntektspost = new Inntektspost(ip);
            inntektspost.setInntekt(this);
            return inntektspost;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(getArbeidsgiver(), getInntektsKilde());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Inntekt other)) {
            return false;
        }
        return Objects.equals(this.getInntektsKilde(), other.getInntektsKilde())
                && Objects.equals(this.getArbeidsgiver(), other.getArbeidsgiver());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInntektsKilde(), getArbeidsgiver());
    }

    /**
     * System (+ filter) som inntektene er hentet inn fra / med
     *
     * @return {@link InntektsKilde}
     */
    public InntektsKilde getInntektsKilde() {
        return inntektsKilde;
    }

    void setInntektsKilde(InntektsKilde inntektsKilde) {
        this.inntektsKilde = inntektsKilde;
    }

    /**
     * Utbetaler
     *
     * @return {@link Arbeidsgiver}
     */
    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    /**
     * Alle utbetalinger utført av utbetaler (ufiltrert).
     */
    public Collection<Inntektspost> getAlleInntektsposter() {
        return Collections.unmodifiableSet(inntektspost);
    }

    void leggTilInntektspost(Inntektspost inntektspost) {
        inntektspost.setInntekt(this);
        this.inntektspost.add(inntektspost);
    }

    public AktørInntekt getAktørInntekt() {
        return aktørInntekt;
    }

    void setAktørInntekt(AktørInntekt aktørInntekt) {
        this.aktørInntekt = aktørInntekt;
    }

    public InntektspostBuilder getInntektspostBuilder() {
        return InntektspostBuilder.ny();
    }

    public boolean hasValues() {
        return arbeidsgiver != null || inntektsKilde != null || inntektspost != null;
    }

}
