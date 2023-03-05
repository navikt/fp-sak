package no.nav.foreldrepenger.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

public class AktivitetsAvtaleBuilder {
    private final AktivitetsAvtale aktivitetsAvtale;
    private boolean oppdatering = false;

    AktivitetsAvtaleBuilder(AktivitetsAvtale aktivitetsAvtaleEntitet, boolean oppdatering) {
        this.aktivitetsAvtale = aktivitetsAvtaleEntitet;
        this.oppdatering = oppdatering;
    }

    public static AktivitetsAvtaleBuilder ny() {
        return new AktivitetsAvtaleBuilder(new AktivitetsAvtale(), false);
    }

    static AktivitetsAvtaleBuilder oppdater(Optional<AktivitetsAvtale> aktivitetsAvtale) {
        return new AktivitetsAvtaleBuilder(aktivitetsAvtale.orElse(new AktivitetsAvtale()), aktivitetsAvtale.isPresent());
    }

    public AktivitetsAvtaleBuilder medProsentsats(Stillingsprosent prosentsats) {
        this.aktivitetsAvtale.setProsentsats(prosentsats);
        return this;
    }

    public AktivitetsAvtaleBuilder medProsentsats(BigDecimal prosentsats) {
        this.aktivitetsAvtale.setProsentsats(prosentsats == null ? null : new Stillingsprosent(prosentsats));
        return this;
    }

    public AktivitetsAvtaleBuilder medPeriode(DatoIntervallEntitet periode) {
        this.aktivitetsAvtale.setPeriode(periode);
        return this;
    }

    public AktivitetsAvtaleBuilder medBeskrivelse(String begrunnelse) {
        this.aktivitetsAvtale.setBeskrivelse(begrunnelse);
        return this;
    }

    public AktivitetsAvtale build() {
        if (aktivitetsAvtale.hasValues()) {
            return aktivitetsAvtale;
        }
        throw new IllegalStateException();
    }

    public boolean isOppdatering() {
        return oppdatering;
    }

    public AktivitetsAvtaleBuilder medSisteLønnsendringsdato(LocalDate sisteLønnsendringsdato) {
        this.aktivitetsAvtale.sisteLønnsendringsdato(sisteLønnsendringsdato);
        return this;
    }
}
