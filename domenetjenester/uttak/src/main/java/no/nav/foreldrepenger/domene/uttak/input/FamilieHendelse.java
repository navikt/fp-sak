package no.nav.foreldrepenger.domene.uttak.input;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FamilieHendelse {

    private final LocalDate termindato;
    private final LocalDate fødselsdato;
    private final LocalDate omsorgsovertakelse;
    private final List<Barn> barna;
    private final int antallBarn;
    private final LocalDate ankomstNorge;
    private final boolean stebarnsadopsjon;

    private FamilieHendelse(List<Barn> barna,
                            int antallBarn,
                            LocalDate termindato,
                            LocalDate fødselsdato,
                            LocalDate omsorgsovertakelse,
                            LocalDate ankomstNorge,
                            boolean stebarnsadopsjon) {
        this.termindato = termindato;
        this.fødselsdato = fødselsdato;
        this.omsorgsovertakelse = omsorgsovertakelse;
        this.barna = barna;
        this.antallBarn = antallBarn;
        this.ankomstNorge = ankomstNorge;
        this.stebarnsadopsjon = stebarnsadopsjon;
    }

    public static FamilieHendelse forFødsel(LocalDate termindato,
                                            LocalDate fødselsdato,
                                            List<Barn> barna,
                                            int antallBarn) {
        return new FamilieHendelse(barna, antallBarn, termindato, fødselsdato, null, null, false);
    }

    public static FamilieHendelse forAdopsjonOmsorgsovertakelse(LocalDate omsorgsovertakelse,
                                                                List<Barn> barna,
                                                                int antallBarn,
                                                                LocalDate ankomstNorge,
                                                                boolean stebarnsadopsjon) {
        Objects.requireNonNull(omsorgsovertakelse, "omsorgsovertakelse");
        return new FamilieHendelse(barna, antallBarn, null, null, omsorgsovertakelse, ankomstNorge, stebarnsadopsjon);
    }

    public Optional<LocalDate> getTermindato() {
        return Optional.ofNullable(termindato);
    }

    public Optional<LocalDate> getFødselsdato() {
        return Optional.ofNullable(fødselsdato);
    }

    public List<Barn> getBarna() {
        return barna;
    }

    public Optional<LocalDate> getOmsorgsovertakelse() {
        return Optional.ofNullable(omsorgsovertakelse);
    }

    public int getAntallBarn() {
        return antallBarn;
    }

    public LocalDate getFamilieHendelseDato() {
        if (getOmsorgsovertakelse().isPresent()) {
            return getOmsorgsovertakelse().get();
        }
        if (getFødselsdato().isPresent()) {
            return getFødselsdato().get();
        }
        return getTermindato().orElseThrow(() -> new IllegalStateException("Mangler familiehendelse"));
    }

    public Optional<LocalDate> getAnkomstNorge() {
        return Optional.ofNullable(ankomstNorge);
    }

    public boolean erStebarnsadopsjon() {
        return stebarnsadopsjon;
    }

    public boolean erAlleBarnDøde() {
        var barna = getBarna();
        if (barna.isEmpty()) {
            return false;
        }
        return barna.stream().allMatch(b -> b.getDødsdato().isPresent());
    }

    public boolean gjelderFødsel() {
        return getFødselsdato().isPresent() || getTermindato().isPresent();
    }
}
