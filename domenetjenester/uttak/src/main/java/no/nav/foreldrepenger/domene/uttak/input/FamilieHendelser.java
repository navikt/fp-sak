package no.nav.foreldrepenger.domene.uttak.input;

import java.util.Optional;

public class FamilieHendelser {
    private FamilieHendelse søknadFamilieHendelse;
    private FamilieHendelse bekreftetFamilieHendelse;
    private FamilieHendelse overstyrtFamilieHendelse;

    public FamilieHendelser() {
    }

    public FamilieHendelser(FamilieHendelser familieHendelser) {
        søknadFamilieHendelse = familieHendelser.søknadFamilieHendelse;
        bekreftetFamilieHendelse = familieHendelser.bekreftetFamilieHendelse;
        overstyrtFamilieHendelse = familieHendelser.overstyrtFamilieHendelse;
    }

    public FamilieHendelser medSøknadHendelse(FamilieHendelse familieHendelse) {
        var nyeHendelser = new FamilieHendelser(this);
        nyeHendelser.søknadFamilieHendelse = familieHendelse;
        return nyeHendelser;
    }

    public FamilieHendelser medBekreftetHendelse(FamilieHendelse familieHendelse) {
        var nyeHendelser = new FamilieHendelser(this);
        nyeHendelser.bekreftetFamilieHendelse = familieHendelse;
        return nyeHendelser;
    }

    public FamilieHendelser medOverstyrtHendelse(FamilieHendelse familieHendelse) {
        var nyeHendelser = new FamilieHendelser(this);
        nyeHendelser.overstyrtFamilieHendelse = familieHendelse;
        return nyeHendelser;
    }

    public boolean gjelderTerminFødsel() {
        return getGjeldendeFamilieHendelse().gjelderFødsel();
    }

    public boolean erSøktTermin() {
        return søknadFamilieHendelse != null && søknadFamilieHendelse.getTermindato().isPresent() && søknadFamilieHendelse.getFødselsdato().isEmpty();
    }

    public FamilieHendelse getSøknadFamilieHendelse() {
        return søknadFamilieHendelse;
    }

    public Optional<FamilieHendelse> getBekreftetFamilieHendelse() {
        return Optional.ofNullable(bekreftetFamilieHendelse);
    }

    public Optional<FamilieHendelse> getOverstyrtFamilieHendelse() {
        return Optional.ofNullable(overstyrtFamilieHendelse);
    }

    public FamilieHendelse getGjeldendeFamilieHendelse() {
        return getOverstyrtFamilieHendelse().or(this::getBekreftetFamilieHendelse).orElseGet(() -> søknadFamilieHendelse);
    }

    public Optional<FamilieHendelse> getOverstyrtEllerBekreftet() {
        return getOverstyrtFamilieHendelse().or(this::getBekreftetFamilieHendelse);
    }
}
