package no.nav.foreldrepenger.behandlingslager.aktør;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;

@Entity(name = "Bruker")
@Table(name = "BRUKER")
public class NavBruker extends Person {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BRUKER")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Convert(converter = Språkkode.KodeverdiConverter.class)
    @Column(name = "sprak_kode", nullable = false)
    private Språkkode språkkode = Språkkode.NB;

    NavBruker() {
        super(null);
        // For Hibernate
    }

    private NavBruker(final AktørId aktørId, final Språkkode språkkode) {
        super(aktørId);
        this.språkkode = språkkode;
    }

    // Brukes til testformål der språk er irrelevant for formålet.
    public static NavBruker opprettNyNB(AktørId aktørId) {
        return new NavBruker(aktørId, Språkkode.NB);
    }

    public static NavBruker opprettNy(AktørId aktørId, Språkkode språk) {
        return new NavBruker(aktørId, språk);
    }

    public Long getId() {
        return id;
    }

    public Språkkode getSpråkkode() {
        return språkkode;
    }

    public void setSpråkkode(Språkkode språkkode) {
        this.språkkode = språkkode;
    }
}
