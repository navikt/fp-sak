package no.nav.foreldrepenger.behandling.steg.beregnytelse.es;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class BarnInfo implements UidentifisertBarn {
    private final LocalDate fødselsdato;
    private final LocalDate dødsdato;
    private Integer barnNummer = null; // null = ikke opprettet

    public BarnInfo(Integer barnNummer, LocalDate fødselsdato, LocalDate dødsdato) {
        this.barnNummer = barnNummer;
        this.fødselsdato = fødselsdato;
        this.dødsdato = dødsdato;
    }

    @Override
    public Integer getBarnNummer() {
        return barnNummer;
    }

    @Override
    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    @Override
    public Optional<LocalDate> getDødsdato() {
        return Optional.ofNullable(dødsdato);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BarnInfo barnInfo)) {
            return false;
        }
        return Objects.equals(barnNummer, barnInfo.barnNummer) &&
                Objects.equals(fødselsdato, barnInfo.fødselsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barnNummer, fødselsdato);
    }
}
