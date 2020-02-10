package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

public class InntektArbeidYtelseAggregat extends BaseEntitet {

    private UUID uuid;

    @ChangeTracked
    private Set<AktørInntekt> aktørInntekt = new LinkedHashSet<>();

    @ChangeTracked
    private Set<AktørArbeid> aktørArbeid = new LinkedHashSet<>();

    @ChangeTracked
    private Set<AktørYtelse> aktørYtelse = new LinkedHashSet<>();

    InntektArbeidYtelseAggregat() {
        // hibernate
    }

    InntektArbeidYtelseAggregat(UUID angittEksternReferanse, LocalDateTime angittOpprettetTidspunkt) {
        setOpprettetTidspunkt(angittOpprettetTidspunkt);
        uuid = angittEksternReferanse;
    }

    /** copy constructor men med angitt referanse og tidspunkt. Hvis unikt kan denne instansen brukes til lagring. */
    InntektArbeidYtelseAggregat(UUID eksternReferanse, LocalDateTime opprettetTidspunkt, InntektArbeidYtelseAggregat kopierFra) {
        this.setAktørInntekt(kopierFra.getAktørInntekt().stream().map(ai -> {
            AktørInntekt aktørInntekt = new AktørInntekt(ai);
            return aktørInntekt;
        }).collect(Collectors.toList()));

        this.setAktørArbeid(kopierFra.getAktørArbeid().stream().map(aktørArbied -> {
            AktørArbeid aktørArbeid = new AktørArbeid(aktørArbied);
            return aktørArbeid;
        }).collect(Collectors.toList()));

        this.setAktørYtelse(kopierFra.getAktørYtelse().stream().map(ay -> {
            AktørYtelse aktørYtelse = new AktørYtelse(ay);
            return aktørYtelse;
        }).collect(Collectors.toList()));

        setOpprettetTidspunkt(opprettetTidspunkt);
        this.uuid = eksternReferanse;

    }

    /**
     * Copy constructor - inklusiv angitt referanse og opprettet tid. Brukes for immutable copy internt i minne. Hvis lagres vil gi unik
     * constraint exception.
     */
    InntektArbeidYtelseAggregat(InntektArbeidYtelseAggregat kopierFra) {
        this(kopierFra.getEksternReferanse(), kopierFra.getOpprettetTidspunkt(), kopierFra);
    }

    /** Identifisere en immutable instans av grunnlaget unikt og er egnet for utveksling (eks. til abakus eller andre systemer) */
    public UUID getEksternReferanse() {
        return uuid;
    }

    public Collection<AktørInntekt> getAktørInntekt() {
        return Collections.unmodifiableSet(aktørInntekt);
    }

    void setAktørInntekt(Collection<AktørInntekt> aktørInntekt) {
        this.aktørInntekt = new LinkedHashSet<>(aktørInntekt);
    }

    void leggTilAktørInntekt(AktørInntekt aktørInntekt) {
        this.aktørInntekt.add(aktørInntekt);
    }

    void leggTilAktørArbeid(AktørArbeid aktørArbeid) {
        this.aktørArbeid.add(aktørArbeid);
    }

    void leggTilAktørYtelse(AktørYtelse aktørYtelse) {
        this.aktørYtelse.add(aktørYtelse);
    }

    public Collection<AktørArbeid> getAktørArbeid() {
        return Collections.unmodifiableSet(aktørArbeid);
    }

    void setAktørArbeid(Collection<AktørArbeid> aktørArbeid) {
        this.aktørArbeid = new LinkedHashSet<>(aktørArbeid);
    }

    public Collection<AktørYtelse> getAktørYtelse() {
        return Collections.unmodifiableSet(aktørYtelse);
    }

    void setAktørYtelse(Collection<AktørYtelse> aktørYtelse) {
        this.aktørYtelse = new LinkedHashSet<>(aktørYtelse);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof InntektArbeidYtelseAggregat)) {
            return false;
        }
        InntektArbeidYtelseAggregat other = (InntektArbeidYtelseAggregat) obj;
        return Objects.equals(this.getAktørInntekt(), other.getAktørInntekt())
            && Objects.equals(this.getAktørArbeid(), other.getAktørArbeid())
            && Objects.equals(this.getAktørYtelse(), other.getAktørYtelse());
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørInntekt, aktørArbeid, aktørYtelse);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "aktørInntekt=" + aktørInntekt +
            ", aktørArbeid=" + aktørArbeid +
            ", aktørYtelse=" + aktørYtelse +
            '>';
    }

}
