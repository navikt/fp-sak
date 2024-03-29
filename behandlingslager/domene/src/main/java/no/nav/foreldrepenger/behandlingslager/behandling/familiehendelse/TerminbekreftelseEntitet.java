package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

/**
 * Entitetsklasse for terminbekreftelse.
 * <p>
 * Implementert iht. builder pattern (ref. "Effective Java, 2. ed." J.Bloch).
 * Non-public constructors og setters, dvs. immutable.
 * <p>
 * OBS: Legger man til nye felter så skal dette oppdateres mange steder:
 * builder, equals, hashcode etc.
 */
@Entity(name = "Terminbekreftelse")
@Table(name = "FH_TERMINBEKREFTELSE")
public class TerminbekreftelseEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TERMINBEKREFTELSE")
    private Long id;

    @ChangeTracked
    @Column(name = "termindato", nullable = false)
    private LocalDate termindato;

    @ChangeTracked
    @Column(name = "utstedt_dato")
    private LocalDate utstedtdato;

    @Column(name = "navn")
    private String navn;

    @OneToOne(optional = false)
    @JoinColumn(name = "familie_hendelse_id", nullable = false, updatable = false, unique = true)
    private FamilieHendelseEntitet familieHendelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    TerminbekreftelseEntitet() {
        // hibernate
    }

    TerminbekreftelseEntitet(TerminbekreftelseEntitet terminbekreftelse) {
        this.termindato = terminbekreftelse.getTermindato();
        this.utstedtdato = terminbekreftelse.getUtstedtdato();
        this.navn = terminbekreftelse.getNavnPå();
    }


    public LocalDate getTermindato() {
        return termindato;
    }


    public LocalDate getUtstedtdato() {
        return utstedtdato;
    }


    public String getNavnPå() {
        return navn;
    }

    public FamilieHendelseEntitet getFamilieHendelse() {
        return familieHendelse;
    }

    void setFamilieHendelse(FamilieHendelseEntitet familieHendelse) {
        this.familieHendelse = familieHendelse;
    }

    void setTermindato(LocalDate termindato) {
        this.termindato = termindato;
    }

    void setUtstedtdato(LocalDate utstedtdato) {
        this.utstedtdato = utstedtdato;
    }

    void setNavn(String navn) {
        this.navn = navn;
    }

    boolean hasValues() {
        return navn != null || utstedtdato != null || termindato != null;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TerminbekreftelseEntitet other)) {
            return false;
        }
        return Objects.equals(this.termindato, other.getTermindato())
                && Objects.equals(this.utstedtdato, other.getUtstedtdato())
                && Objects.equals(this.navn, other.getNavnPå());
    }


    @Override
    public int hashCode() {
        return Objects.hash(termindato, utstedtdato, navn);
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "<termindato=" + termindato
                + ", utstedtdato=" + utstedtdato
                + ", navn=" + navn + ">";
    }

}
