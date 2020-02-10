package no.nav.foreldrepenger.domene.uttak.input;

public class OriginalBehandling {

    private final FamilieHendelser familieHendelser;
    private final long id;

    public OriginalBehandling(long id, FamilieHendelser familieHendelser) {
        this.id = id;
        this.familieHendelser = familieHendelser;
    }

    public FamilieHendelser getFamilieHendelser() {
        return familieHendelser;
    }

    public Long getId() {
        return id;
    }
}
