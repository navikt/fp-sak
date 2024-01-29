package no.nav.foreldrepenger.domene.iay.modell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

public class OppgittFrilans {

    @ChangeTracked
    private boolean harInntektFraFosterhjem;

    @ChangeTracked
    private boolean erNyoppstartet;

    @ChangeTracked
    private boolean harNærRelasjon;

    @ChangeTracked
    private List<OppgittFrilansoppdrag> frilansoppdrag;

    public OppgittFrilans() {
    }

    public OppgittFrilans(boolean harInntektFraFosterhjem, boolean erNyoppstartet, boolean harNærRelasjon) {
        this.harInntektFraFosterhjem = harInntektFraFosterhjem;
        this.erNyoppstartet = erNyoppstartet;
        this.harNærRelasjon = harNærRelasjon;
    }

    public OppgittFrilans(OppgittFrilans oppgittFrilans) {
        this.harInntektFraFosterhjem = oppgittFrilans.harInntektFraFosterhjem;
        this.erNyoppstartet = oppgittFrilans.erNyoppstartet;
        this.harNærRelasjon = oppgittFrilans.harNærRelasjon;
        this.frilansoppdrag = new ArrayList<>(oppgittFrilans.getFrilansoppdrag().stream().map(OppgittFrilansoppdrag::new).toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OppgittFrilans that)) {
            return false;
        }
        return harInntektFraFosterhjem == that.harInntektFraFosterhjem && erNyoppstartet == that.erNyoppstartet
            && harNærRelasjon == that.harNærRelasjon && Objects.equals(frilansoppdrag, that.frilansoppdrag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(harInntektFraFosterhjem, erNyoppstartet, harNærRelasjon, frilansoppdrag);
    }

    @Override
    public String toString() {
        return "FrilansEntitet{" +
                ", harInntektFraFosterhjem=" + harInntektFraFosterhjem +
                ", erNyoppstartet=" + erNyoppstartet +
                ", harNærRelasjon=" + harNærRelasjon +
                ", frilansoppdrag=" + frilansoppdrag +
                '}';
    }

    void leggTilFrilansoppdrag(OppgittFrilansoppdrag oppgittFrilansoppdrag) {
        if (this.frilansoppdrag == null) {
            this.frilansoppdrag = new ArrayList<>();
        }
        frilansoppdrag.add(oppgittFrilansoppdrag);
    }

    public boolean getHarInntektFraFosterhjem() {
        return harInntektFraFosterhjem;
    }

    void setHarInntektFraFosterhjem(boolean harInntektFraFosterhjem) {
        this.harInntektFraFosterhjem = harInntektFraFosterhjem;
    }

    void setErNyoppstartet(boolean erNyoppstartet) {
        this.erNyoppstartet = erNyoppstartet;
    }

    void setHarNærRelasjon(boolean harNærRelasjon) {
        this.harNærRelasjon = harNærRelasjon;
    }

    public boolean getErNyoppstartet() {
        return erNyoppstartet;
    }

    public boolean getHarNærRelasjon() {
        return harNærRelasjon;
    }

    public List<OppgittFrilansoppdrag> getFrilansoppdrag() {
        if (frilansoppdrag != null) {
            return Collections.unmodifiableList(frilansoppdrag);
        }
        return Collections.emptyList();
    }
}
