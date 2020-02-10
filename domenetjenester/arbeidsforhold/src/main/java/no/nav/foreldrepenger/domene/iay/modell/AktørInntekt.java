package no.nav.foreldrepenger.domene.iay.modell;


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class AktørInntekt extends BaseEntitet implements IndexKey {

    private AktørId aktørId;

    @ChangeTracked
    private Set<Inntekt> inntekt = new LinkedHashSet<>();

    public AktørInntekt() {
        //hibernate
    }

    /**
     * Deep copy ctor
     */
    AktørInntekt(AktørInntekt aktørInntekt) {
        this.aktørId = aktørInntekt.getAktørId();

        this.inntekt = aktørInntekt.inntekt.stream().map(i -> {
            var inntekt = new Inntekt(i);
            inntekt.setAktørInntekt(this);
            return inntekt;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(getAktørId());
    }

    /**
     * Aktøren inntekten er relevant for
     * @return aktørid
     */
    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    /** Get alle inntekter samlet (ufiltrert). */
    public Collection<Inntekt> getInntekt() {
        return List.copyOf(inntekt);
    }

    public boolean hasValues() {
        return aktørId != null || inntekt != null;
    }

    InntektBuilder getInntektBuilder(InntektsKilde inntektsKilde, Opptjeningsnøkkel nøkkel) {
        Optional<Inntekt> inntektOptional = getInntekt()
            .stream()
            .filter(i -> inntektsKilde.equals(i.getInntektsKilde()))
            .filter(i -> i.getArbeidsgiver() != null && new Opptjeningsnøkkel(i.getArbeidsgiver()).matcher(nøkkel)
                || inntektsKilde.equals(InntektsKilde.SIGRUN)).findFirst();
        InntektBuilder oppdatere = InntektBuilder.oppdatere(inntektOptional);
        if (!oppdatere.getErOppdatering()) {
            oppdatere.medInntektsKilde(inntektsKilde);
        }
        return oppdatere;
    }

    public InntektBuilder getInntektBuilderForYtelser(InntektsKilde inntektsKilde) {
        Optional<Inntekt> inntektOptional = getInntekt()
            .stream()
            .filter(i -> i.getArbeidsgiver() == null)
            .filter(i -> inntektsKilde.equals(i.getInntektsKilde()))
            .filter(i -> i.getAlleInntektsposter().stream()
                .anyMatch(post -> post.getInntektspostType().equals(InntektspostType.YTELSE)))
            .findFirst();
        InntektBuilder oppdatere = InntektBuilder.oppdatere(inntektOptional);
        if (!oppdatere.getErOppdatering()) {
            oppdatere.medInntektsKilde(inntektsKilde);
        }
        return oppdatere;
    }

    void leggTilInntekt(Inntekt inntekt) {
        this.inntekt.add(inntekt);
        inntekt.setAktørInntekt(this);
    }

    void fjernInntekterFraKilde(InntektsKilde inntektsKilde) {
        this.inntekt.removeIf(it -> it.getInntektsKilde().equals(inntektsKilde));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof AktørInntekt)) {
            return false;
        }
        AktørInntekt other = (AktørInntekt) obj;
        return Objects.equals(this.getAktørId(), other.getAktørId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "aktørId=" + aktørId +
            ", inntekt=" + inntekt +
            '>';
    }

}
