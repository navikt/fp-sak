package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.modell;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto.DokumentertBarnDto;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;

public class FødselStatus implements UidentifisertBarn, Comparable<FødselStatus> {
    private final LocalDate fødselsdato;
    private final LocalDate dødsdato;
    private final Integer barnNummer;

    public FødselStatus(UidentifisertBarn barn) {
        this.fødselsdato = barn.getFødselsdato();
        this.dødsdato = barn.getDødsdato().orElse(null);
        this.barnNummer = barn.getBarnNummer();
    }

    public FødselStatus(DokumentertBarnDto barn, Integer barnNummer) {
        this.fødselsdato = barn.fødselsdato();
        this.dødsdato = barn.dødsdato();
        this.barnNummer = barnNummer;
    }

    public FødselStatus(DokumentertBarnDto barn) {
        this(barn, 0);
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
    public Integer getBarnNummer() {
        return barnNummer;
    }

    public String formaterLevetid() {
        return getDødsdato().map(d -> String.format("f. %s - d. %s", format(fødselsdato), format(d)))
            .orElseGet(() -> String.format("f. %s", format(fødselsdato)));
    }

    public static Optional<FødselStatus> safeGet(List<FødselStatus> list, int index) {
        return (index < list.size()) ? Optional.ofNullable(list.get(index)) : Optional.empty();
    }

    @Override
    public int hashCode() {
        return Objects.hash(fødselsdato, dødsdato, barnNummer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FødselStatus that)) {
            return false;
        }
        return Objects.equals(fødselsdato, that.fødselsdato) && Objects.equals(dødsdato, that.dødsdato);
    }

    @Override
    public int compareTo(FødselStatus other) {
        return UidentifisertBarn.FØDSEL_COMPARATOR.compare(this, other);
    }
}
