package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class AktørYtelse extends BaseEntitet implements IndexKey {

    private AktørId aktørId;

    @ChangeTracked
    private Set<Ytelse> ytelser = new LinkedHashSet<>();

    public AktørYtelse() {
        // hibernate
    }

    /**
     * Deep copy ctor
     */
    AktørYtelse(AktørYtelse aktørYtelse) {
        this.aktørId = aktørYtelse.getAktørId();
        this.ytelser = aktørYtelse.getAlleYtelser().stream().map(ytelse -> new Ytelse(ytelse)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(getAktørId());
    }

    /**
     * Aktøren tilstøtende ytelser gjelder for
     *
     * @return aktørId
     */
    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    /**
     * Alle registrerte tilstøende ytelser (ufiltrert).
     */
    public Collection<Ytelse> getAlleYtelser() {
        return List.copyOf(ytelser);
    }

    boolean hasValues() {
        return (aktørId != null) || ((ytelser != null) && !ytelser.isEmpty());
    }

    YtelseBuilder getYtelseBuilderForType(Fagsystem fagsystem, RelatertYtelseType type, Saksnummer saksnummer) {
        var ytelse = getAlleYtelser().stream()
                .filter(ya -> ya.getKilde().equals(fagsystem) && ya.getRelatertYtelseType().equals(type) && (saksnummer.equals(ya.getSaksnummer())))
                .findFirst();
        return YtelseBuilder.oppdatere(ytelse).medYtelseType(type).medKilde(fagsystem).medSaksnummer(saksnummer);
    }

    void leggTilYtelse(Ytelse ytelse) {
        this.ytelser.add(ytelse);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AktørYtelse)) {
            return false;
        }
        var other = (AktørYtelse) obj;
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
                ", ytelser=" + ytelser +
                '>';
    }
}
