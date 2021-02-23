package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

public class SakPersonerDto {

    private PersonDto bruker;
    private PersonDto annenPart;
    private SakHendelseDto familiehendelse;

    public SakPersonerDto() {
        // Injiseres i test
    }

    public SakPersonerDto(PersonDto bruker, PersonDto annenPart, SakHendelseDto familiehendelse) {
        this.bruker = bruker;
        this.annenPart = annenPart;
        this.familiehendelse = familiehendelse;
    }

    public PersonDto getBruker() {
        return bruker;
    }

    public PersonDto getAnnenPart() {
        return annenPart;
    }

    public SakHendelseDto getFamiliehendelse() {
        return familiehendelse;
    }
}
