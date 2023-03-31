package no.nav.foreldrepenger.familiehendelse.rest;

public class FamilieHendelseGrunnlagDto {
    private FamiliehendelseDto oppgitt;
    private FamiliehendelseDto gjeldende;
    private FamiliehendelseDto register;

    public FamiliehendelseDto getGjeldende() {
        return gjeldende;
    }

    public void setGjeldende(FamiliehendelseDto gjeldende) {
        this.gjeldende = gjeldende;
    }

    public FamiliehendelseDto getRegister() {
        return register;
    }

    public void setRegister(FamiliehendelseDto register) {
        this.register = register;
    }

    public FamiliehendelseDto getOppgitt() {
        return oppgitt;
    }

    public void setOppgitt(FamiliehendelseDto oppgitt) {
        this.oppgitt = oppgitt;
    }
}
