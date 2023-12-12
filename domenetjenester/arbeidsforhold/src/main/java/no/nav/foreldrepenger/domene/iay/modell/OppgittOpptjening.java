package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

public class OppgittOpptjening extends BaseEntitet {

    private UUID uuid;

    @ChangeTracked
    private List<OppgittArbeidsforhold> oppgittArbeidsforhold;

    @ChangeTracked
    private List<OppgittEgenNæring> egenNæring;

    @ChangeTracked
    private List<OppgittAnnenAktivitet> annenAktivitet;

    @ChangeTracked
    private OppgittFrilans frilans;

    @SuppressWarnings("unused")
    private OppgittOpptjening() {
        // hibernate
    }

    public OppgittOpptjening(UUID eksternReferanse) {
        Objects.requireNonNull(eksternReferanse, "eksternReferanse");
        this.uuid = eksternReferanse;
        // setter tidspunkt til nå slik at dette også er satt for nybakte objekter uten
        // å lagring
        setOpprettetTidspunkt(LocalDateTime.now());
    }
    public OppgittOpptjening(OppgittOpptjening oppgittOpptjening, UUID uuid) {
        this.uuid = uuid;
        this.frilans = oppgittOpptjening.frilans;
        this.oppgittArbeidsforhold = oppgittOpptjening.oppgittArbeidsforhold;
        this.annenAktivitet = oppgittOpptjening.annenAktivitet;
        this.egenNæring = oppgittOpptjening.egenNæring;
        setOpprettetTidspunkt(LocalDateTime.now());
    }

    OppgittOpptjening(UUID eksternReferanse, LocalDateTime opprettetTidspunktOriginalt) {
        Objects.requireNonNull(eksternReferanse, "eksternReferanse");
        this.uuid = eksternReferanse;
        super.setOpprettetTidspunkt(opprettetTidspunktOriginalt);
    }

    /**
     * Identifisere en immutable instans av grunnlaget unikt og er egnet for
     * utveksling (eks. til abakus eller andre systemer)
     */
    public UUID getEksternReferanse() {
        return uuid;
    }

    public List<OppgittArbeidsforhold> getOppgittArbeidsforhold() {
        if (this.oppgittArbeidsforhold == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(oppgittArbeidsforhold);
    }

    public List<OppgittEgenNæring> getEgenNæring() {
        if (this.egenNæring == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(egenNæring);
    }

    public List<OppgittAnnenAktivitet> getAnnenAktivitet() {
        if (this.annenAktivitet == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(annenAktivitet);
    }

    public Optional<OppgittFrilans> getFrilans() {
        return Optional.ofNullable(frilans);
    }

    void leggTilFrilans(OppgittFrilans frilans) {
        if (frilans != null) {
            this.frilans = frilans;
        } else {
            this.frilans = null;
        }
    }

    void leggTilAnnenAktivitet(OppgittAnnenAktivitet annenAktivitet) {
        if (this.annenAktivitet == null) {
            this.annenAktivitet = new ArrayList<>();
        }
        if (annenAktivitet != null) {
            this.annenAktivitet.add(annenAktivitet);
        }
    }

    void leggTilEgenNæring(OppgittEgenNæring egenNæring) {
        if (this.egenNæring == null) {
            this.egenNæring = new ArrayList<>();
        }
        if (egenNæring != null) {
            this.egenNæring.add(egenNæring);
        }
    }

    void leggTilOppgittArbeidsforhold(OppgittArbeidsforhold oppgittArbeidsforhold) {
        if (this.oppgittArbeidsforhold == null) {
            this.oppgittArbeidsforhold = new ArrayList<>();
        }
        if (oppgittArbeidsforhold != null) {
            this.oppgittArbeidsforhold.add(oppgittArbeidsforhold);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OppgittOpptjening that)) {
            return false;
        }
        return Objects.equals(oppgittArbeidsforhold, that.oppgittArbeidsforhold) &&
                Objects.equals(egenNæring, that.egenNæring) &&
                Objects.equals(annenAktivitet, that.annenAktivitet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittArbeidsforhold, egenNæring, annenAktivitet);
    }

    @Override
    public String toString() {
        return "OppgittOpptjeningEntitet{" +
                "oppgittArbeidsforhold=" + oppgittArbeidsforhold +
                ", egenNæring=" + egenNæring +
                ", annenAktivitet=" + annenAktivitet +
                '}';
    }

    /**
     * Brukes til å filtrere bort tomme oppgitt opptjening elementer ved migrering.
     * Bør ikke være nødvendig til annet.
     *
     * har minst noe av oppgitt arbeidsforhold, egen næring, annen aktivitet eller
     * frilans.
     */
    public boolean harOpptjening() {
        return !getOppgittArbeidsforhold().isEmpty() || !getEgenNæring().isEmpty() || !getAnnenAktivitet().isEmpty() || getFrilans().isPresent();
    }
}
