package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

public class SakPersonerDto {

    private PersonDto bruker;
    private PersonDto annenPart;
    private SakHendelseDto familiehendelse;
    private boolean utlandskAnnenPart;

    public SakPersonerDto() {
        // Injiseres i test
    }

    public SakPersonerDto(PersonDto bruker, PersonDto annenPart, SakHendelseDto familiehendelse, boolean utlandskAnnenPart) {
        this.bruker = bruker;
        this.annenPart = annenPart;
        this.familiehendelse = familiehendelse;
        this.utlandskAnnenPart = utlandskAnnenPart;
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

    public boolean isUtlandskAnnenPart() {
        return utlandskAnnenPart;
    }
}
