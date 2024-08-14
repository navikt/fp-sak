package no.nav.foreldrepenger.behandling;

import java.time.LocalDate;
import java.util.Optional;

public record FamilieHendelseDato(LocalDate termindato, LocalDate fødselsdato, LocalDate omsorgsovertakelse) {

    public static FamilieHendelseDato forFødsel(LocalDate termindato, LocalDate fødselsdato) {
        return new FamilieHendelseDato(termindato, fødselsdato, null);
    }

    public static FamilieHendelseDato forAdopsjonOmsorg(LocalDate omsorgsovertakelse) {
        return new FamilieHendelseDato(null, null, omsorgsovertakelse);
    }

    public LocalDate familieHendelseDato() {
        return Optional.ofNullable(omsorgsovertakelse)
            .or(() -> Optional.ofNullable(fødselsdato))
            .or(() -> Optional.ofNullable(termindato))
            .orElse(null);
    }

    public boolean gjelderFødsel() {
        return omsorgsovertakelse == null;
    }

    public boolean gjelderAdopsjonOmsorg() {
        return omsorgsovertakelse != null;
    }

}
