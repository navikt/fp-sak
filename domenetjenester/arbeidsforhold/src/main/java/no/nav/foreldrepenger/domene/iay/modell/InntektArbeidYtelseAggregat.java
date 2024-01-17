package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

public class InntektArbeidYtelseAggregat {

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

    /**
     * copy constructor men med angitt referanse og tidspunkt. Hvis unikt kan denne
     * instansen brukes til lagring.
     */
    InntektArbeidYtelseAggregat(UUID eksternReferanse, LocalDateTime opprettetTidspunkt, InntektArbeidYtelseAggregat kopierFra) {
        this.setAktørInntekt(kopierFra.getAktørInntekt().stream().map(AktørInntekt::new).toList());
        this.setAktørArbeid(kopierFra.getAktørArbeid().stream().map(AktørArbeid::new).toList());
        this.setAktørYtelse(kopierFra.getAktørYtelse().stream().map(AktørYtelse::new).toList());

        setOpprettetTidspunkt(opprettetTidspunkt);
        this.uuid = eksternReferanse;

    }

    /**
     * Identifisere en immutable instans av grunnlaget unikt og er egnet for
     * utveksling (eks. til abakus eller andre systemer)
     */
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
        }
        if (!(obj instanceof InntektArbeidYtelseAggregat other)) {
            return false;
        }
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
