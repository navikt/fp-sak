package no.nav.foreldrepenger.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.domene.typer.AntallTimer;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class AktivitetsAvtaleBuilder {
    private static final BigDecimal MAKS_ANTALL_TIMER_I_UKEN = BigDecimal.valueOf(168); // https://jira.adeo.no/browse/TFP-1259
    private final AktivitetsAvtale aktivitetsAvtale;
    private boolean oppdatering = false;

    AktivitetsAvtaleBuilder(AktivitetsAvtale aktivitetsAvtaleEntitet, boolean oppdatering) {
        this.aktivitetsAvtale = aktivitetsAvtaleEntitet; // NOSONAR
        this.oppdatering = oppdatering;
    }

    public static AktivitetsAvtaleBuilder ny() {
        return new AktivitetsAvtaleBuilder(new AktivitetsAvtale(), false);
    }

    static AktivitetsAvtaleBuilder oppdater(Optional<AktivitetsAvtale> aktivitetsAvtale) {
        return new AktivitetsAvtaleBuilder(aktivitetsAvtale.orElse(new AktivitetsAvtale()), aktivitetsAvtale.isPresent());
    }

    /**
     * @deprecated Bruker ikke antall timer lenger. Dersom det brekker tester, bruk sisteLønnsendringsdato i stedf. antall timer for å definere som ansettelsesavtale
     */
    @Deprecated
    public AktivitetsAvtaleBuilder medAntallTimer(BigDecimal antallTimer) {
        AntallTimer avkortetAntallTimer = antallTimer == null ? null : new AntallTimer(antallTimer.min(MAKS_ANTALL_TIMER_I_UKEN));
        this.aktivitetsAvtale.setAntallTimer(avkortetAntallTimer);
        return this;
    }

    /**
     * @deprecated Bruker ikke antall timer lenger. Dersom det brekker tester, bruk sisteLønnsendringsdato i stedf. antall timer for å definere som ansettelsesavtale
     */
    @Deprecated
    public AktivitetsAvtaleBuilder medAntallTimerFulltid(BigDecimal antallTimerFulltid) {
        AntallTimer avkortetAntallTimerFulltid = antallTimerFulltid == null ? null : new AntallTimer(antallTimerFulltid.min(MAKS_ANTALL_TIMER_I_UKEN));
        this.aktivitetsAvtale.setAntallTimerFulltid(avkortetAntallTimerFulltid);
        return this;
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
