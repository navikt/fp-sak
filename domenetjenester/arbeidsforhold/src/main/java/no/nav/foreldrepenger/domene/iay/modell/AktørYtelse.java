package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
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
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
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
        this.ytelser = aktørYtelse.getAlleYtelser().stream().map(ytelse -> {
            var yt = new Ytelse(ytelse);
            return yt;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
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

    YtelseBuilder getYtelseBuilderForType(Fagsystem fagsystem, RelatertYtelseType type, Saksnummer saksnummer, DatoIntervallEntitet periode,
            Optional<LocalDate> tidligsteAnvistFom) {
        // OBS kan være flere med samme Saksnummer+FOM: Konvensjon ifm satsjustering
        var aktuelleYtelser = getAlleYtelser().stream()
                .filter(ya -> ya.getKilde().equals(fagsystem) && ya.getRelatertYtelseType().equals(type) && (saksnummer.equals(ya.getSaksnummer())
                        && periode.getFomDato().equals(ya.getPeriode().getFomDato())))
                .collect(Collectors.toList());
        var ytelse = aktuelleYtelser.stream()
                .filter(ya -> periode.equals(ya.getPeriode()))
                .findFirst();
        if (ytelse.isEmpty() && !aktuelleYtelser.isEmpty()) {
            // Håndtere endret TOM-dato som regel ifm at ytelsen er opphørt. Hvis flere med
            // samme FOM-dato sjekk anvist-fom
            if (tidligsteAnvistFom.isPresent()) {
                ytelse = aktuelleYtelser.stream()
                        .filter(yt -> yt.getYtelseAnvist().stream().anyMatch(ya -> tidligsteAnvistFom.get().equals(ya.getAnvistFOM())))
                        .findFirst();
            }
            if (ytelse.isEmpty()) {
                ytelse = aktuelleYtelser.stream().filter(yt -> yt.getYtelseAnvist().isEmpty()).findFirst();
            }
        }
        return YtelseBuilder.oppdatere(ytelse).medYtelseType(type).medKilde(fagsystem).medSaksnummer(saksnummer);
    }

    YtelseBuilder getYtelseBuilderForType(Fagsystem fagsystem, RelatertYtelseType type, TemaUnderkategori typeKategori,
            DatoIntervallEntitet periode) {
        var ytelse = getAlleYtelser().stream()
                .filter(ya -> ya.getKilde().equals(fagsystem) && ya.getRelatertYtelseType().equals(type)
                        && ya.getBehandlingsTema().equals(typeKategori) && (periode.getFomDato().equals(ya.getPeriode().getFomDato())))
                .findFirst();
        return YtelseBuilder.oppdatere(ytelse).medYtelseType(type).medKilde(fagsystem).medPeriode(periode);
    }

    void leggTilYtelse(Ytelse ytelse) {
        this.ytelser.add(ytelse);
    }

    void fjernYtelse(Ytelse ytelse) {
        this.ytelser.remove(ytelse);
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
